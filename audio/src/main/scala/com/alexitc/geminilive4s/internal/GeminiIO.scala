package com.alexitc.geminilive4s.internal

import cats.effect.std.Dispatcher
import cats.effect.{IO, Ref, Resource}
import com.alexitc.geminilive4s.models.*
import com.google.genai.*
import com.google.genai.types.*

/** The Gemini layer exposing IO instead of CompletableFuture
  */
private[geminilive4s] class GeminiIO(
    sessionRef: Ref[IO, AsyncSession],
    dispatcher: Dispatcher[IO],
    makeSession: Option[String] => IO[AsyncSession],
    consumerRef: Ref[IO, Option[LiveServerMessage => IO[Unit]]]
) {
  def sendFunctionResult(response: FunctionResponse): IO[Unit] = {
    val f = for {
      session <- sessionRef.get
      result = session.sendToolResponse(
        LiveSendToolResponseParameters
          .builder()
          .functionResponses(response)
          .build()
      )
    } yield result

    IO.fromCompletableFuture(f).void
  }

  def sendAudio(chunk: Array[Byte]): IO[Unit] = {
    val f = for {
      session <- sessionRef.get
      result = session.sendRealtimeInput(
        LiveSendRealtimeInputParameters
          .builder()
          .media(Blob.builder().mimeType("audio/pcm").data(chunk))
          .build()
      )
    } yield result

    IO.fromCompletableFuture(f).void
  }

  def sendActivityStart(): IO[Unit] = {
    val f = for {
      session <- sessionRef.get
      result = session.sendRealtimeInput(
        LiveSendRealtimeInputParameters
          .builder()
          .activityStart(ActivityStart.builder())
          .build
      )
    } yield result

    IO.fromCompletableFuture(f).void
  }

  def sendActivityEnd(): IO[Unit] = {
    val f = for {
      session <- sessionRef.get
      result = session.sendRealtimeInput(
        LiveSendRealtimeInputParameters
          .builder()
          .activityEnd(ActivityEnd.builder())
          .build
      )
    } yield result

    IO.fromCompletableFuture(f).void
  }

  def sendMessage(message: String): IO[Unit] = {
    val f = for {
      session <- sessionRef.get
      result = session.sendRealtimeInput(
        LiveSendRealtimeInputParameters
          .builder()
          .text(message)
          .build()
      )
    } yield result

    IO.fromCompletableFuture(f).void
  }

  def openResumption(handle: Option[String]): IO[Unit] = {
    for {
      maybeConsumer <- consumerRef.get
      newSession <- makeSession(handle)
      _ <- maybeConsumer match {
        case Some(consumer) =>
          IO.fromCompletableFuture(IO {
            newSession.receive { msg =>
              dispatcher.unsafeRunAndForget(consumer(msg))
            }
          }).void
        case None => IO.unit
      }
      _ <- sessionRef.set(newSession)
    } yield ()
  }

  def close(): IO[Unit] = {
    val f = for {
      session <- sessionRef.get
      result = session.close()
    } yield result
    IO.fromCompletableFuture(f).void
  }

  def receiveMessages(consumer: LiveServerMessage => IO[Unit]): IO[Unit] = {
    val f = for {
      _ <- consumerRef.set(Some(consumer))
      session <- sessionRef.get
      result = session.receive { message =>
        dispatcher.unsafeRunAndForget(consumer(message))
      }
    } yield result

    IO.fromCompletableFuture(f).void
  }

}

private[geminilive4s] object GeminiIO {

  def make(
      apiKey: String,
      config: GeminiConfig,
      liveConfig: LiveConnectConfig
  ): Resource[IO, GeminiIO] = {
    def acquire(dispatcher: Dispatcher[IO]) = for {
      client <- config.customApiVersion match {
        case None =>
          IO(Client.builder().apiKey(apiKey).build())
        case Some(version) =>
          IO(
            Client
              .builder()
              .apiKey(apiKey)
              .httpOptions(
                HttpOptions.builder().apiVersion(version.string).build()
              )
              .build()
          )
      }
      session <- makeSession(client, liveConfig, config.model)
      sessionRef <- Ref.of[IO, AsyncSession](session)
      newSession = (handle: Option[String]) =>
        makeSession(
          client,
          GeminiLiveConfigBuilder.make(config, handle),
          config.model
        )
      consumerRef <- Ref.of[IO, Option[LiveServerMessage => IO[Unit]]](None)
      _ <- IO.println("✅ Connected to Gemini Live API")
    } yield new GeminiIO(sessionRef, dispatcher, newSession, consumerRef)

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
      model: GeminiModel
  ): IO[AsyncSession] = {
    val f = IO.blocking {
      client.async.live.connect(model.string, config)
    }

    IO.fromCompletableFuture(f)
  }
}
