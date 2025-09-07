#!/usr/bin/env scala-cli

//> using scala "3"
//> using scala "21"
//> using dep "com.alexitc.geminilive4s::audio:0.3.0"

import cats.effect.{IO, IOApp}
import com.alexitc.geminilive4s.GeminiService
import com.alexitc.geminilive4s.demo.{MicSource, SpeakerSink}
import com.alexitc.geminilive4s.models.*
import com.google.genai
import fs2.concurrent.SignallingRef

import scala.jdk.CollectionConverters.*

/** Example application using Gemini Live API to capture audio from the
  * computer's microphone which is send to Gemini, Gemini's audio is played over
  * the computer's speaker.
  *
  * Define GEMINI_API_KEY environment variable with the Gemini API key, then:
  *   - scala-cli NoteTakerDemo.scala
  *
  * The app goal is to take voice notes and prepare a summary which invokes a
  * Scala function.
  */
object NoteTakerDemo extends IOApp.Simple {

  val apiKey = sys.env.getOrElse(
    "GEMINI_API_KEY",
    throw new RuntimeException("GEMINI_API_KEY is required")
  )

  val baseConfig = GeminiConfig(
    prompt = """
       |You are 'Note taker', your goal is asking about today events,
       |organize the thoughts and prepare a clean summary,
       |avoid repeated what you are told unless it is strictly necessary to understand the other part,
       |make sure to ask follow up questions when it is necessary,
       |
       |when you are done, you should invoke a function with your summary.
       |""".stripMargin,
    language = GeminiLanguage.EnglishUS,
    functions = List.empty // must be defined later
  )

  override def run: IO[Unit] = {
    val audioFormat = AudioStreamFormat.GeminiOutput

    SignallingRef[IO, Boolean](false).flatMap { haltSignal =>
      // gemini can halt the stream process
      val functionDef = makeGeminiFunction(haltSignal.set(true))
      val config = baseConfig.copy(functions = List(functionDef))
      val pipeline = for {
        gemini <- GeminiService.make(apiKey, config)

        // mic to gemini, gemini to speaker
        _ <- MicSource
          .stream(audioFormat)
          .map(bytes => GeminiInputChunk(bytes))
          .through(gemini.conversationPipe(geminiMustSpeakFirst = true))
          .interruptWhen(haltSignal)
          .observe(in => in.map(_.chunk).through(SpeakerSink.pipe(audioFormat)))
      } yield ()

      pipeline.compile.drain
    }
  }

  def makeGeminiFunction(haltProcess: IO[Unit]): GeminiFunction = {
    GeminiFunction(
      declaration = genai.types.FunctionDeclaration
        .builder()
        .name("process_completed")
        .description(
          "Complete the process when the user say bye or similar, make sure to include the summary you captured"
        )
        .parameters(
          genai.types.Schema
            .builder()
            .`type`(genai.types.Type.Known.OBJECT)
            .properties(
              Map(
                "summary" -> genai.types.Schema
                  .builder()
                  .`type`(genai.types.Type.Known.STRING)
                  .example(
                    "Today was a great day because we got a lot of sunlight."
                  )
                  .description("The summary from the conversation")
                  .build()
              ).asJava
            )
            .required("summary")
            .build()
        )
        .build(),
      executor = invocation =>
        val summary = invocation.args.get("summary")
        IO.println(s"Process completed, summary: $summary") >>
          haltProcess.as(Map("response" -> "ok", "scheduling" -> "INTERRUPT"))
    )
  }
}
