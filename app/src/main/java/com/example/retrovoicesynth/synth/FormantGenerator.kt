package com.example.retrovoicesynth.synth

import com.example.retrovoicesynth.presets.VoicePreset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class FormantGenerator {
    private val random = Random(1980)

    fun generate(
        phoneme: Phoneme,
        sampleRate: Int,
        preset: VoicePreset,
        controls: SynthControls
    ): ShortArray {
        val speed = (preset.speedMultiplier * controls.speed).coerceIn(0.45f, 2.2f)
        val durationMs = max(16, (phoneme.durationMs / speed).toInt())
        val sampleCount = max(1, sampleRate * durationMs / 1000)
        val robotness = ((preset.robotness + controls.robotness) * 0.5f).coerceIn(0f, 1f)

        return when (phoneme.kind) {
            PhonemeKind.Vowel -> vowel(phoneme, sampleCount, sampleRate, preset, controls, robotness)
            PhonemeKind.Fricative -> fricative(phoneme, sampleCount, preset, robotness)
            PhonemeKind.Stop -> stop(phoneme, sampleCount, sampleRate, preset, controls, robotness)
            PhonemeKind.Nasal -> nasal(phoneme, sampleCount, sampleRate, preset, controls, robotness)
            PhonemeKind.Liquid -> liquid(phoneme, sampleCount, sampleRate, preset, controls, robotness)
            PhonemeKind.Pause -> ShortArray(sampleCount)
        }
    }

    private fun vowel(
        phoneme: Phoneme,
        sampleCount: Int,
        sampleRate: Int,
        preset: VoicePreset,
        controls: SynthControls,
        robotness: Float
    ): ShortArray {
        val formants = phoneme.formants ?: VowelFormants(500f, 1500f, 2500f)
        val basePitch = preset.basePitchHz * preset.pitchMultiplier * controls.pitch
        val phaseStep = basePitch / sampleRate
        var phase = 0f
        val output = ShortArray(sampleCount)

        for (index in output.indices) {
            val time = index.toDouble() / sampleRate
            val envelope = envelope(index, sampleCount, attack = 0.08f, release = 0.14f)
            val buzz = if (phase < 0.5f) 1f else -1f
            val buzzSoft = (buzz * robotness) + (sin(2.0 * PI * phase) * (1f - robotness)).toFloat()
            phase = (phase + phaseStep) % 1f

            val shift = preset.formantShift
            val f1 = sin(2.0 * PI * formants.f1 * shift * time).toFloat() * 0.52f
            val f2 = sin(2.0 * PI * formants.f2 * shift * time).toFloat() * 0.27f
            val f3 = sin(2.0 * PI * formants.f3 * shift * time).toFloat() * 0.12f
            val flutter = sin(2.0 * PI * (5.5 + robotness * 6.0) * time).toFloat() * 0.035f
            val noise = centeredNoise() * preset.noiseLevel * 0.18f
            val sample = (buzzSoft * (f1 + f2 + f3 + flutter) + noise) * envelope * 0.78f

            output[index] = toPcm(applyRetro(sample, preset.bitDepth, robotness))
        }
        return output
    }

    private fun fricative(
        phoneme: Phoneme,
        sampleCount: Int,
        preset: VoicePreset,
        robotness: Float
    ): ShortArray {
        val output = ShortArray(sampleCount)
        var last = 0f

        for (index in output.indices) {
            val envelope = envelope(index, sampleCount, attack = 0.04f, release = 0.2f)
            val raw = centeredNoise()
            val highPassed = raw - last * 0.72f
            last = raw
            val sample = highPassed * envelope * phoneme.intensity * (0.42f + preset.noiseLevel)
            output[index] = toPcm(applyRetro(sample, preset.bitDepth, robotness))
        }
        return output
    }

    private fun stop(
        phoneme: Phoneme,
        sampleCount: Int,
        sampleRate: Int,
        preset: VoicePreset,
        controls: SynthControls,
        robotness: Float
    ): ShortArray {
        val output = ShortArray(sampleCount)
        val pitch = preset.basePitchHz * controls.pitch * 2.4f
        val clickSamples = min(sampleCount, sampleRate / 90)
        val voiced = phoneme.symbol in voicedStops
        val tailSamples = if (voiced) min(sampleCount, sampleRate / 28) else clickSamples

        for (index in 0 until clickSamples) {
            val progress = index.toFloat() / clickSamples
            val decay = 1f - progress
            val tone = sin(2.0 * PI * pitch * index / sampleRate).toFloat()
            val burst = if (voiced) 0.44f else 0.32f
            val sample = (tone * 0.46f + centeredNoise() * burst) * decay * phoneme.intensity
            output[index] = toPcm(applyRetro(sample, preset.bitDepth, robotness))
        }
        if (voiced) {
            val buzzPitch = preset.basePitchHz * preset.pitchMultiplier * controls.pitch * 0.74f
            for (index in clickSamples until tailSamples) {
                val progress = (index - clickSamples).toFloat() / max(1, tailSamples - clickSamples)
                val decay = 1f - progress
                val time = index.toDouble() / sampleRate
                val buzz = sin(2.0 * PI * buzzPitch * time).toFloat() * 0.34f
                val edge = centeredNoise() * 0.08f
                val sample = (buzz + edge) * decay * phoneme.intensity
                output[index] = toPcm(applyRetro(sample, preset.bitDepth, robotness))
            }
        }
        return output
    }

    private fun nasal(
        phoneme: Phoneme,
        sampleCount: Int,
        sampleRate: Int,
        preset: VoicePreset,
        controls: SynthControls,
        robotness: Float
    ): ShortArray {
        val output = ShortArray(sampleCount)
        val pitch = preset.basePitchHz * preset.pitchMultiplier * controls.pitch * 0.82f
        val isNg = phoneme.symbol == "ng"

        for (index in output.indices) {
            val time = index.toDouble() / sampleRate
            val envelope = envelope(
                index = index,
                total = sampleCount,
                attack = if (isNg) 0.055f else 0.1f,
                release = if (isNg) 0.24f else 0.16f
            )
            val hum = sin(2.0 * PI * pitch * time).toFloat() * if (isNg) 0.54f else 0.46f
            val nasalPeak = sin(2.0 * PI * 280.0 * preset.formantShift * time).toFloat() * 0.24f
            val backPeak = if (isNg) {
                sin(2.0 * PI * 980.0 * preset.formantShift * time).toFloat() * 0.18f
            } else {
                0f
            }
            val sample = (hum + nasalPeak + backPeak) * envelope * phoneme.intensity
            output[index] = toPcm(applyRetro(sample, preset.bitDepth, robotness))
        }
        return output
    }

    private fun liquid(
        phoneme: Phoneme,
        sampleCount: Int,
        sampleRate: Int,
        preset: VoicePreset,
        controls: SynthControls,
        robotness: Float
    ): ShortArray {
        val output = ShortArray(sampleCount)
        val pitch = preset.basePitchHz * preset.pitchMultiplier * controls.pitch
        val isL = phoneme.symbol == "l"
        val isR = phoneme.symbol == "r"

        for (index in output.indices) {
            val time = index.toDouble() / sampleRate
            val progress = index.toFloat() / max(1, sampleCount - 1)
            val envelope = envelope(
                index = index,
                total = sampleCount,
                attack = when {
                    isL -> 0.025f
                    isR -> 0.04f
                    else -> 0.12f
                },
                release = when {
                    isL -> 0.08f
                    isR -> 0.12f
                    else -> 0.14f
                }
            )
            val sweep = when {
                isL -> 330f + 520f * progress
                isR -> 900f - 320f * progress
                else -> 550f + 520f * progress
            }
            val buzz = if (((time * pitch) % 1.0) < 0.5) 1f else -1f
            val tone = buzz.toFloat() * sin(2.0 * PI * sweep * preset.formantShift * time).toFloat()
            val lowerBody = when {
                isL -> sin(2.0 * PI * 250.0 * preset.formantShift * time).toFloat() * 0.34f
                isR -> sin(2.0 * PI * 1350.0 * preset.formantShift * time).toFloat() * 0.24f
                else -> 0f
            }
            val onset = if ((isL || isR) && index < sampleRate / 180) {
                centeredNoise() * (1f - index.toFloat() / max(1, sampleRate / 180)) * 0.09f
            } else {
                0f
            }
            val amplitude = when {
                isL -> 0.7f
                isR -> 0.66f
                else -> 0.45f
            }
            val sample = ((tone + lowerBody) * envelope + onset) * phoneme.intensity * amplitude
            output[index] = toPcm(applyRetro(sample, preset.bitDepth, robotness))
        }
        return output
    }

    private fun envelope(index: Int, total: Int, attack: Float, release: Float): Float {
        val progress = index.toFloat() / max(1, total - 1)
        val attackValue = if (attack <= 0f) 1f else min(1f, progress / attack)
        val releaseValue = if (release <= 0f) 1f else min(1f, (1f - progress) / release)
        return min(attackValue, releaseValue).coerceIn(0f, 1f)
    }

    private fun applyRetro(sample: Float, bitDepth: Int, robotness: Float): Float {
        val clipped = sample.coerceIn(-1f, 1f)
        val levels = 1 shl bitDepth.coerceIn(5, 15)
        val quantized = (clipped * levels).toInt().toFloat() / levels
        return quantized * (0.55f + robotness * 0.45f) + clipped * (1f - robotness) * 0.25f
    }

    private fun toPcm(sample: Float): Short {
        val protected = if (abs(sample) < 0.0005f) 0f else sample.coerceIn(-1f, 1f)
        return (protected * Short.MAX_VALUE).toInt().toShort()
    }

    private fun centeredNoise(): Float = random.nextFloat() * 2f - 1f

    private companion object {
        val voicedStops = setOf("b", "d", "g")
    }
}
