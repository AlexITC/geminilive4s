package com.alexitc.geminilive4s.models

import com.google.genai

case class GeminiFunctionInvocation(
    call: genai.types.FunctionCall,
    args: Map[String, Object],
    responseBuilder: genai.types.FunctionResponse.Builder
)
