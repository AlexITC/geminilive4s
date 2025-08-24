package com.alexitc.geminilive4s.models

import cats.effect.IO
import com.google.genai

case class GeminiFunction(
    declaration: genai.types.FunctionDeclaration,
    executor: GeminiFunctionInvocation => IO[Map[String, AnyRef]]
)
