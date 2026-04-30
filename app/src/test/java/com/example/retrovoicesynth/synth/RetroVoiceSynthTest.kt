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
    fun mapperKeepsVoicedStopsAndLiquidsAudible() {
        val phonemes = PhonemeMapper().map("android lull")

        val d = phonemes.first { it.symbol == "d" }
        val l = phonemes.first { it.symbol == "l" }

        assertTrue(d.durationMs > 72)
        assertTrue(d.intensity > 1f)
        assertTrue(l.durationMs > 96)
        assertTrue(l.intensity > 0.9f)
    }

    @Test
    fun mapperUsesSpeechLikeRulesForCommonWords() {
        val warning = PhonemeMapper().map("warning")
        val online = PhonemeMapper().map("online")

        assertTrue(warning.any { it.symbol == "r" && it.intensity > 1f })
        assertTrue(warning.any { it.symbol == "ng" && it.kind == PhonemeKind.Nasal })
        assertTrue(online.any { it.symbol == "l" && it.durationMs > 140 })
        assertTrue(online.any { it.symbol == "ai" })
        assertTrue(online.none { it.symbol == "e" })
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
