package com.alexitc.geminilive4s.models

case class GeminiConfig(
    prompt: String,
    functions: List[GeminiFunction],
    language: GeminiLanguage = GeminiLanguage.EnglishUS,
    voiceName: GeminiVoice = GeminiVoice.random,
    model: GeminiModel = GeminiModel.FlashPreview,
    temperature: Float = 0.7f,
    // Required when using experimental models
    customApiVersion: Option[GeminiCustomApi] = None,
    // When enabled, Gemini waits for messages to confirm when the voice starts/stops
    // Requires v1alpha API
    disableAutomaticActivityDetection: Boolean = false,
    // When enabled, Gemini transcribes the input voice
    inputAudioTranscription: Boolean = false,
    // When enabled, Gemini transcribes the output voice
    outputAudioTranscription: Boolean = false,
    // Only a few models support this
    enableAffectiveDialog: Boolean = false,
    // Only a few models support this
    proactivity: Boolean = false
)
