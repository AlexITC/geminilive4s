package com.alexitc.geminilive4s.models

sealed abstract class GeminiModel(val string: String)

object GeminiModel {
  // limit to 2 requests/minute.
  object FlashPreview extends GeminiModel("gemini-live-2.5-flash-preview")
  object NativeAudio
      extends GeminiModel("gemini-2.5-flash-preview-native-audio-dialog")
  object NativeThinking
      extends GeminiModel("gemini-2.5-flash-exp-native-audio-thinking-dialog")

  case class Custom(override val string: String) extends GeminiModel(string)

}
