package com.example.retrovoicesynth.synth

import java.util.Locale

class TextPreprocessor {
    fun normalize(input: String): String {
        return input
            .lowercase(Locale.US)
            .flatMap { char ->
                when {
                    char in 'a'..'z' -> listOf(char)
                    char in cyrillicSounds -> cyrillicSounds.getValue(char).toList()
                    char.isWhitespace() -> listOf(' ')
                    char in listOf('.', ',', '!', '?', ';', ':', '-') -> listOf(' ')
                    else -> listOf(' ')
                }
            }
            .joinToString(separator = "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private companion object {
        val cyrillicSounds = mapOf(
            'а' to "a",
            'б' to "b",
            'в' to "v",
            'г' to "g",
            'д' to "d",
            'е' to "e",
            'ё' to "yo",
            'ж' to "zh",
            'з' to "z",
            'и' to "i",
            'й' to "j",
            'к' to "k",
            'л' to "l",
            'м' to "m",
            'н' to "n",
            'о' to "o",
            'п' to "p",
            'р' to "r",
            'с' to "s",
            'т' to "t",
            'у' to "u",
            'ф' to "f",
            'х' to "h",
            'ц' to "ts",
            'ч' to "ch",
            'ш' to "sh",
            'щ' to "sh",
            'ы' to "i",
            'э' to "e",
            'ю' to "yu",
            'я' to "ya"
        )
    }
}
