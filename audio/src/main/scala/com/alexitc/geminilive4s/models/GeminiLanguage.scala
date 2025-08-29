package com.alexitc.geminilive4s.models

sealed abstract class GeminiLanguage(val string: String)

object GeminiLanguage {
  object EnglishUS extends GeminiLanguage("en-US")
  object SpanishSpain extends GeminiLanguage("es-ES")

  case class Custom(override val string: String) extends GeminiLanguage(string)

}
