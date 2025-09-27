#!/usr/bin/env scala-cli

//> using scala "3"
//> using scala "21"
//> using dep "com.alexitc.geminilive4s::audio:0.3.0"

import cats.effect.{IO, IOApp}
import com.alexitc.geminilive4s.GeminiService
import com.alexitc.geminilive4s.demo.{MicSource, SpeakerSink}
import com.alexitc.geminilive4s.models.*
import fs2.concurrent.Topic
import fs2.{Stream, text}

object TakeManualTurns extends IOApp.Simple {

  val apiKey = sys.env.getOrElse(
    "GEMINI_API_KEY",
    throw new RuntimeException("GEMINI_API_KEY is required")
  )

  val config = GeminiConfig(
    prompt = """
      |We are playing connected words, we choose on each turn which must start with the last letter
      |from the previous word, any word is accepted on the first turn.
      |
      |You must repeat the word I said and correct me when I make mistakes.
      |""".stripMargin,
    language = GeminiLanguage.EnglishUS,
    functions = List.empty,
    // We disable the automatic voice detection since we are sending manual signals
    disableAutomaticActivityDetection = true,
    // required to disable VAD
    customApiVersion = Some(GeminiCustomApi.V1Alpha)
  )

  override def run: IO[Unit] = {
    val audioFormat = AudioStreamFormat.GeminiOutput

    val pipeline = for {
      gemini <- GeminiService.make(apiKey, config)

      // when gemini ends its turn, we signal that our mic turn starts
      startTurnTopic <- fs2.Stream.eval(Topic[IO, GeminiInputChunk])
      geminiInput: fs2.Stream[IO, GeminiInputChunk] = MicSource
        .stream(audioFormat)
        .map(bytes => GeminiInputChunk(bytes))
        .merge(startTurnTopic.subscribeUnbounded)
        .merge(keyboardInput) // Combine mic audio with keyboard signals

      _ <- geminiInput
        .through(gemini.conversationPipe(geminiMustSpeakFirst = true))
        .observe { stream =>
          // This sink plays the audio from Gemini to the speaker
          stream.map(_.chunk).through(SpeakerSink.pipe(audioFormat))
        }
        .observe { stream =>
          // This sink prints a message when Gemini is done speaking
          stream
            .filter(_.turnComplete)
            .map { _ =>
              GeminiInputChunk(
                new Array(0),
                Some(GeminiInputChunk.ActivityEvent.Start)
              )
            }
            .observe(
              _.foreach { _ =>
                IO.println(
                  "🤖 Gemini has finished speaking. You can speak now. Press [ENTER] when you are done"
                )
              }
            )
            .through(startTurnTopic.publish)
        }
    } yield ()

    pipeline.compile.drain
  }

  // A stream that emits an End event every time the user hits Enter.
  def keyboardInput: Stream[IO, GeminiInputChunk] = fs2.io
    .stdin[IO](4096)
    .through(text.utf8.decode)
    .through(text.lines)
    .evalTap(_ =>
      IO.println(
        "🤫 You manually ended your turn. Gemini is now responding..."
      )
    )
    .map(_ =>
      GeminiInputChunk(
        Array.empty,
        Some(GeminiInputChunk.ActivityEvent.End)
      )
    )
}
