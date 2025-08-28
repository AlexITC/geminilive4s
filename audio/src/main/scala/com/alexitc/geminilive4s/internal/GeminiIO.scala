package com.alexitc.geminilive4s.internal

import cats.effect.std.Dispatcher
import cats.effect.{IO, Resource}
import com.alexitc.geminilive4s.models.*
import com.google.genai.*
import com.google.genai.types.*

/** The Gemini layer exposing IO instead of CompletableFuture
  */
private[geminilive4s] class GeminiIO(
    session: AsyncSession,
    dispatcher: Dispatcher[IO]
) {
  def sendFunctionResult(response: FunctionResponse): IO[Unit] = {
    val f = IO {
      session.sendToolResponse(
        LiveSendToolResponseParameters
          .builder()
          .functionResponses(response)
          .build()
      )
    }

    IO.fromCompletableFuture(f).void
  }

  def sendAudio(chunk: Array[Byte]): IO[Unit] = {
    val f = IO {
      session.sendRealtimeInput(
        LiveSendRealtimeInputParameters
          .builder()
          .media(
            Blob.builder().mimeType("audio/pcm").data(chunk)
          )
          .build
      )
    }

    IO.fromCompletableFuture(f).void
  }

  def sendMessage(message: String): IO[Unit] = {
    val f = IO {
      session.sendRealtimeInput(
        LiveSendRealtimeInputParameters
          .builder()
          .text(message)
          .build()
      )
    }

    IO.fromCompletableFuture(f).void
  }

  def close(): IO[Unit] = {
    IO.fromCompletableFuture(IO.blocking(session.close())).void
  }

  def receiveMessages(consumer: LiveServerMessage => IO[Unit]): IO[Unit] = {
    val f = IO {
      session.receive { message =>
        dispatcher.unsafeRunAndForget(consumer(message))
      }
    }

    IO.fromCompletableFuture(f).void
  }

}

private[geminilive4s] object GeminiIO {

  def make(
      apiKey: String,
      promptSettings: GeminiPromptSettings,
      functionDefs: List[FunctionDeclaration],
      makeGeminiConfig: GeminiConfigParams => LiveConnectConfig
  ): Resource[IO, GeminiIO] = {
    def acquire(dispatcher: Dispatcher[IO]) = for {
      client <- IO(Client.builder().apiKey(apiKey).build())

      configParams = GeminiConfigParams(
        prompt = promptSettings.prompt,
        voiceLanguage = promptSettings.language,
        voiceName = promptSettings.voiceName,
        functionDefs = functionDefs
      )
      config = makeGeminiConfig(configParams)
      session <- makeSession(client, config, model = promptSettings.model)

      _ <- IO.println("✅ Connected to Gemini Live API")
    } yield GeminiIO(session, dispatcher)

    val release = (session: GeminiIO) =>
      session.close() >> IO.println("✅ Gemini session closed")

    for {
      dispatcher <- Dispatcher.sequential[IO]
      gemini <- Resource.make(acquire(dispatcher))(release)
    } yield gemini
  }

  private def makeSession(
      client: Client,
      config: LiveConnectConfig,
      model: String
  ): IO[AsyncSession] = {
    val f = IO.blocking {
      client.async.live.connect(model, config)
    }

    IO.fromCompletableFuture(f)
  }
}
