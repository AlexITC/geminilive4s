package com.alexitc.geminilive4s

import cats.effect.*
import cats.effect.std.Queue
import cats.implicits.*
import com.alexitc.geminilive4s.internal.*
import com.alexitc.geminilive4s.models.*
import com.google.genai
import fs2.Pipe
import scala.concurrent.duration.*

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

/** A service that provides a clean, stream-based interface to the Gemini Live
  * API. It follows the "pipeline" model, where the entire bidirectional
  * conversation is represented as a single, composable `fs2.Pipe`.
  */
class GeminiService(
    gemini: GeminiIO,
    functions: List[GeminiFunction],
    wakeUpMessage: String
) {
  import GeminiService.*

  /** Represents the entire bidirectional conversation with Gemini as a single
    * pipe. It takes a stream of audio bytes from a client (e.g., Twilio) and
    * returns a stream of audio bytes from Gemini.
    */
  def conversationPipe(
      geminiMustSpeakFirst: Boolean
  ): Pipe[IO, Array[Byte], GeminiOutputChunk] = { in =>
    for {
      currentTurnIdRef <- fs2.Stream.eval(Ref.of[IO, Long](0L))
      fromGeminiStream = fs2.Stream
        .eval(Queue.unbounded[IO, TaggedMessage])
        .flatMap { queue =>
          val subscribe = receiveMessages(queue, currentTurnIdRef)

          // The main stream logic runs concurrently with the subscription.
          // When the main stream ends, the queue is terminated, closing the subscription.
          fs2.Stream
            .fromQueueUnterminated(queue)
            .concurrently(fs2.Stream.exec(subscribe))
        }

      processedOutput: fs2.Stream[IO, GeminiOutputChunk] = fromGeminiStream
        .through(handleInterruptionsPipe(currentTurnIdRef))
        .through(extractAudioPipe)

      // Send the processed chunk to Gemini
      toGeminiSink: fs2.Stream[IO, Unit] = in.evalMap(gemini.sendAudio)

      // artificially start the conversation which causes gemini to speak
      wakeUpStream = fs2.Stream.exec {
        IO.whenA(geminiMustSpeakFirst) {
          IO.sleep(1.seconds) >>
            IO.println("Sending wake up signal") >>
            gemini.sendMessage(wakeUpMessage)
        }
      }

      // The output of this `conversationPipe` is the
      //    processed audio from Gemini. The input stream `in` is concurrently
      //    drained into the `toGeminiSink`.
      out <- processedOutput
        .concurrently(toGeminiSink)
        .concurrently(wakeUpStream)
    } yield out
  }

  /** A pipe that listens for interruption events from Gemini to filter out old
    * messages
    */
  private def handleInterruptionsPipe(currentTurnIdRef: Ref[IO, Long]): Pipe[
    IO,
    TaggedMessage,
    genai.types.LiveServerMessage
  ] = _.evalFilter { taggedMessage =>
    for {
      currentTurn <- currentTurnIdRef.get
    } yield taggedMessage.turnId == currentTurn
  }.map(_.message)

  /** A pipe that extracts raw audio byte chunks from Gemini server messages.
    */
  private val extractAudioPipe
      : Pipe[IO, genai.types.LiveServerMessage, GeminiOutputChunk] = {
    _.flatMap(msg => fs2.Stream.fromOption(msg.serverContent().toScala))
      .flatMap { content =>
        val output = transcriptionToText(content.outputTranscription)
        val input = transcriptionToText(content.inputTranscription)
        val transcription = Transcription(input = input, output = output)
        val data = content.modelTurn.toScala
          .flatMap(_.parts().toScala)
          .map(_.asScala.toSeq)
          .getOrElse(Seq.empty)
          .flatMap(_.inlineData().toScala)
          .flatMap(_.data().toScala)
          .flatten
          .toArray

        val opt = Option
          .when(data.nonEmpty)(GeminiOutputChunk(transcription, data))

        fs2.Stream.fromOption(opt)
      }
  }

  private def receiveMessages(
      queue: Queue[IO, TaggedMessage],
      currentTurnIdRef: Ref[IO, Long]
  ): IO[Unit] = {
    gemini.receiveMessages { message =>
      val content = message.serverContent().toScala
      val isInterrupted = content
        .flatMap(_.interrupted().toScala)
        .exists(identity)

      val toolCalls = message
        .toolCall()
        .toScala
        .flatMap(_.functionCalls().toScala)
        .map(_.asScala.toList)
        .getOrElse(List.empty)

      val execCalls = toolCalls.map(execFunction)

      val action = if (isInterrupted) {
        // Increment turn ID
        IO.println(
          "Gemini detected an interruption. Resetting stream state."
        ) >> currentTurnIdRef.updateAndGet(_ + 1)
      } else currentTurnIdRef.get

      // Get the current turn ID and offer the tagged message to the queue
      for {
        _ <- execCalls.sequence_
        id <- action
        _ <- queue.offer(TaggedMessage(id, message))
      } yield ()
    }
  }

  private def execFunction(call: genai.types.FunctionCall): IO[Unit] = {
    val nameOpt = call.name().toScala
    val callIdOpt = call.id().toScala
    val args: Map[String, Object] = call
      .args()
      .toScala
      .map(_.asScala.toMap)
      .getOrElse(Map.empty)

    (nameOpt, callIdOpt) match {
      case (Some(name), Some(callId)) =>
        val responseBuilder = genai.types.FunctionResponse
          .builder()
          .id(callId)
          .name(name)

        val invocationParams = GeminiFunctionInvocation(
          call = call,
          args = args,
          responseBuilder = responseBuilder
        )

        functions.find(_.declaration.name().toScala.contains(name)) match {
          case Some(func) =>
            func.executor(invocationParams).attempt.flatMap { outcome =>
              val response = outcome match {
                case Right(map) => map
                case Left(ex)   => Map("error" -> ex.getMessage)
              }

              gemini.sendFunctionResult(
                responseBuilder.response(response.asJava).build
              )
            }

          case None => IO.println(s"Function call can't be invoked: $call")
        }

      case _ => IO.println(s"Function call ignored: $call")
    }
  }
}

object GeminiService {

  def make(
      apiKey: String,
      promptSettings: GeminiPromptSettings,
      functions: List[GeminiFunction],
      customApiVersion: Option[GeminiCustomApi] = None
  ): fs2.Stream[IO, GeminiService] = {
    val wakeUpMessage =
      if (promptSettings.language.startsWith("es")) "Hola" else "Hello"
    for {
      gemini <- fs2.Stream.resource(
        GeminiIO.make(
          apiKey,
          promptSettings,
          functions.map(_.declaration),
          makeGeminiConfig,
          customApiVersion
        )
      )
    } yield new GeminiService(gemini, functions, wakeUpMessage)
  }

  def makeGeminiConfig(
      configParams: GeminiConfigParams
  ): genai.types.LiveConnectConfig = {
    import configParams.*

    val tool = genai.types.Tool
      .builder()
      .functionDeclarations(functionDefs*)
      .build()

    genai.types.LiveConnectConfig
      .builder()
      .inputAudioTranscription(
        genai.types.AudioTranscriptionConfig.builder().build()
      )
      .outputAudioTranscription(
        genai.types.AudioTranscriptionConfig.builder().build()
      )
      .responseModalities(genai.types.Modality.Known.AUDIO)
      .systemInstruction(
        genai.types.Content
          .builder()
          .parts(genai.types.Part.builder().text(prompt))
          .build()
      )
      .speechConfig(
        genai.types.SpeechConfig
          .builder()
          .voiceConfig(
            genai.types.VoiceConfig
              .builder()
              .prebuiltVoiceConfig(
                genai.types.PrebuiltVoiceConfig.builder().voiceName(voiceName)
              )
          )
          .languageCode(voiceLanguage)
      )
      .tools(tool)
      .temperature(0.7f)
      //      .enableAffectiveDialog(true) // not supported by a all models
      //      .proactivity(
      //        genai.types.ProactivityConfig.builder().proactiveAudio(true).build()
      //      )
      .build()
  }

  private case class TaggedMessage(
      turnId: Long,
      message: genai.types.LiveServerMessage
  )

  private def transcriptionToText(
      transcription: java.util.Optional[genai.types.Transcription]
  ): String = transcription.toScala.flatMap(_.text().toScala).getOrElse("")

}
