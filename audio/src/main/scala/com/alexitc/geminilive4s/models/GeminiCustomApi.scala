package com.alexitc.geminilive4s.models

abstract class GeminiCustomApi(val string: String)

object GeminiCustomApi {

  // Required for native audio model
  object V1Alpha extends GeminiCustomApi("v1alpha")
}
