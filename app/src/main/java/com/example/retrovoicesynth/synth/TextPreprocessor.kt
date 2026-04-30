package com.example.retrovoicesynth.synth

import java.util.Locale

class TextPreprocessor {
    fun normalize(input: String): String {
        return input
            .lowercase(Locale.US)
            .map { char ->
                when {
                    char in 'a'..'z' -> char
                    char.isWhitespace() -> ' '
                    char in listOf('.', ',', '!', '?', ';', ':', '-') -> ' '
                    else -> ' '
                }
            }
            .joinToString(separator = "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
