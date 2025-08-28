# geminilive4s

A library for integrating real-time, conversational AI voice with Scala and the Google Gemini Live API.

---

## Goal

The project's goal is to provide a Scala-friendly wrapper over [google-genai](https://cloud.google.com/vertex-ai/generative-ai/docs/sdks/overview) SDK, being simple enough to get started while allowing to override any of the `google-genai` settings.

As of now, this exposes a [fs2](https://fs2.io) stream where you can an audio stream to Gemini, producing Gemini's audio stream.

One of the key features is supporting [Automatic Function Calling](https://ai.google.dev/gemini-api/docs/function-calling) which allows Gemini to invoke Scala functions.


## How to use

Pick the latest version from the [releases](https://github.com/AlexITC/geminilive4s/releases) page, then, add the dependency to your `build.sbt`:

```scala
libraryDependencies += "com.alexitc.geminilive4s" %% "audio" % "<version>"
```

This is how a minimal application looks like, it listens to your microphone and plays Gemini audio over your speaker:

```scala
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
      _ <- MicSource.stream(audioFormat)
        .through(gemini.conversationPipe(geminiMustSpeakFirst = true))
        .observe(in => in.map(_.chunk).through(SpeakerSink.pipe(audioFormat)))
    } yield ()

    pipeline.compile.drain
  }
}
```


## Try it

The simplest way to try this is by picking one of the [examples](./examples/README.md) and run it with [scala-cli](https://scala-cli.virtuslab.org/), like:

- `scala-cli https://raw.githubusercontent.com/AlexITC/geminilive4s/refs/heads/main/examples/NoteTakerDemo.scala`
