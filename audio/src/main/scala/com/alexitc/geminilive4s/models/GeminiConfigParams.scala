package com.alexitc.geminilive4s.models

import com.google.genai

case class GeminiConfigParams(
    prompt: String,
    voiceLanguage: String,
    voiceName: String,
    functionDefs: List[genai.types.FunctionDeclaration]
)
