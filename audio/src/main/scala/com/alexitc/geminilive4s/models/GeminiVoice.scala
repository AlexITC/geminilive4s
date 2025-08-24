package com.alexitc.geminilive4s
package models

object GeminiVoice {
  val Puck = "puck"
  val Fenrir = "Fenrir"
  val Kore = "Kore"
  val Aoede = "Aoede"
  val Charon = "Charon"

  private val all = List(Puck, Fenrir, Kore, Aoede, Charon)

  def random: String = Utils.random(all)

}
