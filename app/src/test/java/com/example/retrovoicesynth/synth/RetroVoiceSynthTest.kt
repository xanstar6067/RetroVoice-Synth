package com.example.retrovoicesynth.synth

import com.example.retrovoicesynth.presets.VoicePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroVoiceSynthTest {
    @Test
    fun preprocessorKeepsEnglishLettersAndWordSpacing() {
        val normalized = TextPreprocessor().normalize("Warning. Main computer online!")

        assertEquals("warning main computer online", normalized)
    }

    @Test
    fun mapperAddsSimpleVowelsConsonantsAndWordPauses() {
        val phonemes = PhonemeMapper().map("main computer")

        assertTrue(phonemes.any { it.kind == PhonemeKind.Vowel })
        assertTrue(phonemes.any { it.kind == PhonemeKind.Stop })
        assertTrue(phonemes.any { it.kind == PhonemeKind.Pause })
    }

    @Test
    fun synthProducesAudibleMonoPcm() {
        val samples = RetroVoiceSynth().synthesize(
            text = "Warning. Main computer online.",
            preset = VoicePreset.all.first(),
            controls = SynthControls(speed = 1f, pitch = 1f, robotness = 0.72f)
        )

        assertTrue(samples.size > VoicePreset.all.first().sampleRate / 2)
        assertTrue(samples.any { it.toInt() != 0 })
    }
}
