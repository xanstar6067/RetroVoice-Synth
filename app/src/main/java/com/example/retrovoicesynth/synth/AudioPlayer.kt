package com.example.retrovoicesynth.synth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlin.concurrent.thread
import kotlin.math.max

class AudioPlayer {
    @Volatile
    private var currentTrack: AudioTrack? = null

    @Volatile
    private var shouldStop = false

    fun play(samples: ShortArray, sampleRate: Int, onFinished: () -> Unit) {
        stop()
        shouldStop = false

        thread(name = "vox-80-audio") {
            var completed = false
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSizeInShorts = max(minBufferSize / Short.SIZE_BYTES, sampleRate / 10)
                val playbackSamples = withDrainSilence(samples, sampleRate, bufferSizeInShorts)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSizeInShorts * Short.SIZE_BYTES)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                currentTrack = track
                track.play()

                var offset = 0
                val chunkSize = max(512, bufferSizeInShorts / 2)
                while (!shouldStop && offset < playbackSamples.size) {
                    val count = minOf(chunkSize, playbackSamples.size - offset)
                    val written = track.write(playbackSamples, offset, count, AudioTrack.WRITE_BLOCKING)
                    if (written <= 0) break
                    offset += written
                }
                completed = !shouldStop && offset >= playbackSamples.size
                if (completed) {
                    waitForPlaybackToDrain(track, playbackSamples.size, sampleRate)
                }
                safelyRelease(track)
            } finally {
                currentTrack = null
                if (completed) {
                    onFinished()
                }
            }
        }
    }

    fun stop() {
        shouldStop = true
        currentTrack?.let { safelyRelease(it) }
        currentTrack = null
    }

    private fun withDrainSilence(samples: ShortArray, sampleRate: Int, bufferSizeInShorts: Int): ShortArray {
        val drainSamples = max(bufferSizeInShorts, sampleRate / 5)
        val output = ShortArray(samples.size + drainSamples)
        samples.copyInto(output)
        return output
    }

    private fun waitForPlaybackToDrain(track: AudioTrack, sampleCount: Int, sampleRate: Int) {
        val timeoutMs = max(250L, sampleCount * 1000L / sampleRate + 250L)
        val deadline = SystemClock.elapsedRealtime() + timeoutMs

        while (!shouldStop && SystemClock.elapsedRealtime() < deadline) {
            if (track.playbackHeadPosition >= sampleCount) {
                return
            }
            SystemClock.sleep(12L)
        }
    }

    private fun safelyRelease(track: AudioTrack) {
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
            }
        }
        runCatching { track.flush() }
        runCatching { track.release() }
    }
}
