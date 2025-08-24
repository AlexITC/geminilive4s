package com.alexitc.geminilive4s.demo

import com.alexitc.geminilive4s.models.AudioStreamFormat

import javax.sound.sampled.{AudioSystem, DataLine, SourceDataLine}

/** Demo to write audio chunks to the computer's speaker line.
  */
object SpeakerSink {

  def open(audioFormat: AudioStreamFormat): SourceDataLine = {
    val speakerDataLineInfo =
      new DataLine.Info(classOf[SourceDataLine], audioFormat.underlying)

    if (!AudioSystem.isLineSupported(speakerDataLineInfo)) {
      throw new RuntimeException("Speaker line not supported")
    }

    val speakerDataLine = AudioSystem
      .getLine(speakerDataLineInfo)
      .asInstanceOf[SourceDataLine]

    speakerDataLine.open(audioFormat.underlying)
    speakerDataLine.start()
    speakerDataLine
  }
}
