package com.example.retrovoicesynth.synth

data class VowelFormants(
    val f1: Float,
    val f2: Float,
    val f3: Float
)

enum class PhonemeKind {
    Vowel,
    Fricative,
    Stop,
    Nasal,
    Liquid,
    Pause
}

data class Phoneme(
    val symbol: String,
    val kind: PhonemeKind,
    val durationMs: Int,
    val formants: VowelFormants? = null,
    val intensity: Float = 1f
)
