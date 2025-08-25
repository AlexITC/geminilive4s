#!/usr/bin/env scala-cli

//> using scala "3"
//> using scala "21"
//> using dep "com.alexitc.geminilive4s::audio:0.2.0"

import cats.effect.{IO, IOApp}
import com.alexitc.geminilive4s.GeminiService
import com.alexitc.geminilive4s.demo.{MicSource, SpeakerSink}
import com.alexitc.geminilive4s.models.{AudioStreamFormat, GeminiPromptSettings}

object MinimalDemo extends IOApp.Simple {
  val promptSettings = GeminiPromptSettings(
    prompt = "You are a comedian and your goal is making me laugh"
  )

  override def run: IO[Unit] = {
    val audioFormat = AudioStreamFormat.GeminiOutput
    val pipeline = for {
      gemini <- GeminiService.make(
        apiKey = sys.env("GEMINI_API_KEY"),
        promptSettings = promptSettings,
        functions = List.empty
      )
      micStream = MicSource.stream(audioFormat)
      speaker = SpeakerSink.open(audioFormat)

      _ <- micStream
        .through(gemini.conversationPipe) // mic to gemini
        .foreach { chunk =>
          // gemini to speaker
          IO.blocking(speaker.write(chunk.chunk, 0, chunk.chunk.length)).void
        }
    } yield ()

    pipeline.compile.drain
  }
}