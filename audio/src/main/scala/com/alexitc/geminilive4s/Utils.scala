package com.alexitc.geminilive4s

private[geminilive4s] object Utils {
  def random[A](collection: Seq[A]): A = collection(
    scala.util.Random.nextInt(collection.length)
  )
}
