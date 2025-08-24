package com.alexitc.geminilive4s.models

import javax.sound.sampled.AudioFormat

/** Non-ambiguous AudioFormat wrapping the Java one.
  *
  * @param underlying
  *   the actual Java AudioFormat
  */
abstract class AudioStreamFormat(val underlying: AudioFormat)
    extends Product
    with Serializable

object AudioStreamFormat {
  // Gemini produces 16-bit PCM, 24kHz mono
  // ffplay -f s16le -ar 24000 -ac 1 response.raw
  // "-f s16le": 16-bit signed little-endian PCM
  // "-ar 24000": sample rate 24000 Hz
  // "-ac 1": mono
  case object GeminiOutput
      extends AudioStreamFormat(
        new AudioFormat(
          AudioFormat.Encoding.PCM_SIGNED, // Signed PCM
          24000, // target sampleRate (e.g., 24kHz)
          16, // target sampleSizeInBits (e.g., 16-bit)
          1, // target channels (mono)
          2, // frameSize (bytes per frame: 2 bytes for 16-bit mono)
          24000, // frameRate (frames per second, same as sampleRate for mono)
          false // bigEndian (depends on your Gemini spec, usually false for little-endian)
        )
      )
}
