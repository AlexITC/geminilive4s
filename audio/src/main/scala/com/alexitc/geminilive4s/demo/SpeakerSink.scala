package com.alexitc.geminilive4s.demo

import cats.effect.{IO, Resource}
import com.alexitc.geminilive4s.models.AudioStreamFormat

import javax.sound.sampled.{AudioSystem, DataLine, SourceDataLine}

/** Demo to write audio chunks to the computer's speaker line.
  */
object SpeakerSink {

  def pipe(
      audioFormat: AudioStreamFormat
  ): fs2.Pipe[IO, Array[Byte], Nothing] = {
    val speakerLineResource: Resource[IO, SourceDataLine] = Resource.make(
      IO {
        val speakerDataLineInfo =
          new DataLine.Info(classOf[SourceDataLine], audioFormat.underlying)

        if (!AudioSystem.isLineSupported(speakerDataLineInfo)) {
          throw new RuntimeException("Speaker line not supported")
        }

        AudioSystem
          .getLine(speakerDataLineInfo)
          .asInstanceOf[SourceDataLine]
      }.flatTap { line =>
        line.drain()
        IO(line.open(audioFormat.underlying)) >>
          IO(line.start()) >>
          IO.println("Speaker started...")
      }
    )(line =>
      val run = IO(line.stop()) >> IO(line.close()) >>
        IO.println("Speaker stopped and line closed.")
      run.handleErrorWith { e =>
        IO.println(s"Error closing speaker line: ${e.getMessage}")
      }
    )

    input =>
      fs2.Stream
        .resource(speakerLineResource)
        .flatMap { speaker =>
          input.foreach { bytes =>
            IO.blocking(speaker.write(bytes, 0, bytes.length)).void
          }
        }
  }
}
