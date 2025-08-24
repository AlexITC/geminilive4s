package com.alexitc.geminilive4s.models

case class GeminiPromptSettings(
    prompt: String,
    language: String = "en-US", // like en-US, es-ES
    voiceName: String = GeminiVoice.random,
    model: String = GeminiModel.flashPreview
)
