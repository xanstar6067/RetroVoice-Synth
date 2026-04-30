package com.example.retrovoicesynth.synth

class PhonemeMapper {
    fun map(text: String): List<Phoneme> {
        if (text.isBlank()) return emptyList()

        val result = mutableListOf<Phoneme>()
        val words = text.split(' ').filter { it.isNotBlank() }
        words.forEachIndexed { wordIndex, word ->
            appendWord(word, result)
            if (wordIndex < words.lastIndex) {
                result += Phoneme("word-gap", PhonemeKind.Pause, 90, intensity = 0f)
            }
        }
        return result
    }

    private fun appendWord(word: String, output: MutableList<Phoneme>) {
        var index = 0
        while (index < word.length) {
            val pair = word.getOrNull(index)?.let { first ->
                word.getOrNull(index + 1)?.let { second -> "$first$second" }
            }

            when (pair) {
                "th" -> {
                    output += fricative("th", 82, 0.78f)
                    index += 2
                }
                "sh" -> {
                    output += fricative("sh", 92, 0.92f)
                    index += 2
                }
                "ch" -> {
                    output += stop("ch", 88, 1f)
                    output += fricative("sh", 52, 0.72f)
                    index += 2
                }
                "oo" -> {
                    output += vowel("oo", 150, VowelFormants(320f, 760f, 2200f))
                    index += 2
                }
                "ee", "ea" -> {
                    output += vowel("ee", 140, VowelFormants(300f, 2200f, 3000f))
                    index += 2
                }
                "ai", "ay" -> {
                    output += vowel("ai", 145, VowelFormants(700f, 1750f, 2550f))
                    index += 2
                }
                else -> {
                    output += mapChar(word[index])
                    index += 1
                }
            }
        }
    }

    private fun mapChar(char: Char): Phoneme {
        return when (char) {
            'a' -> vowel("a", 135, VowelFormants(730f, 1090f, 2440f))
            'e' -> vowel("e", 125, VowelFormants(530f, 1840f, 2480f))
            'i', 'y' -> vowel("i", 122, VowelFormants(360f, 2100f, 2900f))
            'o' -> vowel("o", 150, VowelFormants(570f, 840f, 2410f))
            'u' -> vowel("u", 138, VowelFormants(350f, 900f, 2240f))
            's', 'z' -> fricative(char.toString(), 78, 0.9f)
            'f', 'v' -> fricative(char.toString(), 72, 0.72f)
            'h' -> fricative("h", 58, 0.52f)
            't', 'k', 'p', 'b', 'd', 'g', 'q', 'x', 'c' -> stop(char.toString(), 72, 0.95f)
            'm', 'n' -> Phoneme(char.toString(), PhonemeKind.Nasal, 112, intensity = 0.86f)
            'l', 'r', 'w', 'j' -> Phoneme(char.toString(), PhonemeKind.Liquid, 96, intensity = 0.72f)
            else -> Phoneme("gap", PhonemeKind.Pause, 42, intensity = 0f)
        }
    }

    private fun vowel(symbol: String, durationMs: Int, formants: VowelFormants): Phoneme {
        return Phoneme(symbol, PhonemeKind.Vowel, durationMs, formants, intensity = 1f)
    }

    private fun fricative(symbol: String, durationMs: Int, intensity: Float): Phoneme {
        return Phoneme(symbol, PhonemeKind.Fricative, durationMs, intensity = intensity)
    }

    private fun stop(symbol: String, durationMs: Int, intensity: Float): Phoneme {
        return Phoneme(symbol, PhonemeKind.Stop, durationMs, intensity = intensity)
    }
}
