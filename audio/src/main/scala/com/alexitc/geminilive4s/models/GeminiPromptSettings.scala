package com.alexitc.geminilive4s.models

case class GeminiPromptSettings(
    prompt: String,
    language: GeminiLanguage = GeminiLanguage.EnglishUS,
    voiceName: GeminiVoice = GeminiVoice.random,
    model: GeminiModel = GeminiModel.FlashPreview
)
