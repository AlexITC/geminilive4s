package com.alexitc.geminilive4s.models

import com.google.genai

case class GeminiConfigParams(
    prompt: String,
    voiceLanguage: GeminiLanguage,
    voiceName: GeminiVoice,
    functionDefs: List[genai.types.FunctionDeclaration]
)
