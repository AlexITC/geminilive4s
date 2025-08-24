package com.alexitc.geminilive4s.models

case class GeminiPromptSettings(
    language: String, // like en-US, es-ES
    prompt: String,
    voiceName: String = GeminiVoice.random,
    model: String = GeminiModel.flashPreview
)
