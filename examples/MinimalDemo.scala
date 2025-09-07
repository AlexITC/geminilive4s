#!/usr/bin/env scala-cli

//> using scala "3"
//> using scala "21"
//> using dep "com.alexitc.geminilive4s::audio:0.3.0"

import cats.effect.{IO, IOApp}
import com.alexitc.geminilive4s.GeminiService
import com.alexitc.geminilive4s.demo.{MicSource, SpeakerSink}
import com.alexitc.geminilive4s.models.{
  AudioStreamFormat,
  GeminiConfig,
  GeminiInputChunk
}

object MinimalDemo extends IOApp.Simple {
  val apiKey = sys.env.getOrElse(
    "GEMINI_API_KEY",
    throw new RuntimeException("GEMINI_API_KEY is required")
  )

  val config = GeminiConfig(
    prompt = "You are a comedian and your goal is making me laugh",
    functions = List.empty
  )

  override def run: IO[Unit] = {
    val audioFormat = AudioStreamFormat.GeminiOutput
    val pipeline = for {
      gemini <- GeminiService.make(apiKey, config)

      // mic to gemini, gemini to speaker
      _ <- MicSource
        .stream(audioFormat)
        .map(bytes => GeminiInputChunk(bytes))
        .through(gemini.conversationPipe(geminiMustSpeakFirst = true))
        .observe(in => in.map(_.chunk).through(SpeakerSink.pipe(audioFormat)))
    } yield ()

    pipeline.compile.drain
  }
}
