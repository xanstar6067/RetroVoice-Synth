package com.example.retrovoicesynth.presets

data class VoicePreset(
    val displayName: String,
    val sampleRate: Int,
    val basePitchHz: Float,
    val speedMultiplier: Float,
    val pitchMultiplier: Float,
    val robotness: Float,
    val formantShift: Float,
    val noiseLevel: Float,
    val bitDepth: Int
) {
    companion object {
        val all = listOf(
            VoicePreset(
                displayName = "80s Computer",
                sampleRate = 22050,
                basePitchHz = 104f,
                speedMultiplier = 1.0f,
                pitchMultiplier = 1.0f,
                robotness = 0.72f,
                formantShift = 1.0f,
                noiseLevel = 0.08f,
                bitDepth = 9
            ),
            VoicePreset(
                displayName = "Terminal",
                sampleRate = 11025,
                basePitchHz = 92f,
                speedMultiplier = 1.15f,
                pitchMultiplier = 0.92f,
                robotness = 0.88f,
                formantShift = 0.92f,
                noiseLevel = 0.12f,
                bitDepth = 8
            ),
            VoicePreset(
                displayName = "Arcade",
                sampleRate = 22050,
                basePitchHz = 132f,
                speedMultiplier = 1.25f,
                pitchMultiplier = 1.22f,
                robotness = 0.64f,
                formantShift = 1.08f,
                noiseLevel = 0.06f,
                bitDepth = 10
            ),
            VoicePreset(
                displayName = "Broken AI",
                sampleRate = 11025,
                basePitchHz = 84f,
                speedMultiplier = 0.82f,
                pitchMultiplier = 0.82f,
                robotness = 1.0f,
                formantShift = 0.86f,
                noiseLevel = 0.2f,
                bitDepth = 7
            )
        )

        fun byName(name: String?): VoicePreset {
            return all.firstOrNull { it.displayName == name } ?: all.first()
        }
    }
}
