package com.example.retrovoicesynth.synth

import com.example.retrovoicesynth.presets.VoicePreset

class RetroVoiceSynth(
    private val preprocessor: TextPreprocessor = TextPreprocessor(),
    private val mapper: PhonemeMapper = PhonemeMapper(),
    private val generator: FormantGenerator = FormantGenerator()
) {
    fun synthesize(
        text: String,
        preset: VoicePreset,
        controls: SynthControls
    ): ShortArray {
        val normalized = preprocessor.normalize(text)
        val phonemes = mapper.map(normalized)
        val buffer = PcmBuffer(initialCapacity = preset.sampleRate * 2)

        phonemes.forEach { phoneme ->
            buffer.appendAll(generator.generate(phoneme, preset.sampleRate, preset, controls))
            if (phoneme.kind != PhonemeKind.Pause) {
                buffer.appendSilence((preset.sampleRate * 0.012f / controls.speed.coerceAtLeast(0.4f)).toInt())
            }
        }

        return buffer.toShortArray()
    }
}
