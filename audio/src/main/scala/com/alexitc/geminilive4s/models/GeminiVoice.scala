package com.alexitc.geminilive4s
package models

abstract class GeminiVoice(val string: String)

object GeminiVoice {
  object Puck extends GeminiVoice("puck")
  object Fenrir extends GeminiVoice("Fenrir")
  object Kore extends GeminiVoice("Kore")
  object Aoede extends GeminiVoice("Aoede")
  object Charon extends GeminiVoice("Charon")

  case class Custom(override val string: String) extends GeminiVoice(string)

  private val all = List(Puck, Fenrir, Kore, Aoede, Charon)

  def random: GeminiVoice = Utils.random(all)

}
