package com.alexitc.geminilive4s.models

case class GeminiInputChunk(
    chunk: Array[Byte],
    // required when Gemini's automatic activity detection is disabled
    activity: Option[GeminiInputChunk.ActivityEvent] = None
)

object GeminiInputChunk {
  enum ActivityEvent:
    case Start, End;
}
