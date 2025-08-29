package com.alexitc.geminilive4s.internal

import com.alexitc.geminilive4s.models.GeminiConfig
import com.google.genai.types.*

object GeminiLiveConfigBuilder {
  def make(params: GeminiConfig): LiveConnectConfig = {
    def transform(when: Boolean)(
        f: LiveConnectConfig.Builder => LiveConnectConfig.Builder
    )(builder: LiveConnectConfig.Builder): LiveConnectConfig.Builder = {
      if (when) f(builder) else builder
    }

    val options = List(
      transform(params.disableAutomaticActivityDetection)(builder =>
        builder // TODO: Enable when the app supports handling this
//        _.realtimeInputConfig(
//          RealtimeInputConfig
//            .builder()
//            .automaticActivityDetection(
//              AutomaticActivityDetection.builder().disabled(true).build()
//            )
//            .build()
//        )
      ),
      transform(params.inputAudioTranscription)(
        _.inputAudioTranscription(AudioTranscriptionConfig.builder().build())
      ),
      transform(params.outputAudioTranscription)(
        _.outputAudioTranscription(AudioTranscriptionConfig.builder().build())
      ),
      transform(params.enableAffectiveDialog)(_.enableAffectiveDialog(true)),
      transform(params.proactivity)(
        _.proactivity(ProactivityConfig.builder().proactiveAudio(true).build())
      )
    )

    val functionDefs = params.functions.map(_.declaration)
    val tool = Tool
      .builder()
      .functionDeclarations(functionDefs*)
      .build()

    val base = LiveConnectConfig
      .builder()
      .responseModalities(Modality.Known.AUDIO)
      .systemInstruction(
        Content
          .builder()
          .parts(Part.builder().text(params.prompt))
          .build()
      )
      .speechConfig(
        SpeechConfig
          .builder()
          .voiceConfig(
            VoiceConfig
              .builder()
              .prebuiltVoiceConfig(
                PrebuiltVoiceConfig.builder().voiceName(params.voiceName.string)
              )
          )
          .languageCode(params.language.string)
      )
      .tools(tool)
      .temperature(params.temperature)

    options
      .foldLeft(base) { case (builder, apply) => apply(builder) }
      .build()
  }
}
