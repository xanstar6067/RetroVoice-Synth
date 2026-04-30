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

        phonemes.forEachIndexed { index, phoneme ->
            val nextPhoneme = phonemes.getOrNull(index + 1)
            buffer.appendAll(generator.generate(phoneme, nextPhoneme, preset.sampleRate, preset, controls))
            if (phoneme.kind != PhonemeKind.Pause) {
                val silenceSeconds = when (phoneme.kind) {
                    PhonemeKind.Stop, PhonemeKind.Fricative -> 0.008f
                    PhonemeKind.Liquid, PhonemeKind.Nasal -> 0.004f
                    PhonemeKind.Vowel -> 0.006f
                    PhonemeKind.Pause -> 0f
                }
                buffer.appendSilence((preset.sampleRate * silenceSeconds / controls.speed.coerceAtLeast(0.4f)).toInt())
            }
        }

        return buffer.toShortArray()
    }
}
