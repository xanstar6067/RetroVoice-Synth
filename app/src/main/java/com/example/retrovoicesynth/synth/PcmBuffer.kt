package com.example.retrovoicesynth.synth

internal class PcmBuffer(initialCapacity: Int = 4096) {
    private var samples = ShortArray(initialCapacity)
    var size: Int = 0
        private set

    fun append(value: Short) {
        ensureCapacity(size + 1)
        samples[size] = value
        size += 1
    }

    fun appendAll(values: ShortArray) {
        ensureCapacity(size + values.size)
        values.copyInto(samples, destinationOffset = size)
        size += values.size
    }

    fun appendSilence(sampleCount: Int) {
        ensureCapacity(size + sampleCount)
        for (index in 0 until sampleCount) {
            samples[size + index] = 0
        }
        size += sampleCount
    }

    fun toShortArray(): ShortArray = samples.copyOf(size)

    private fun ensureCapacity(targetSize: Int) {
        if (targetSize <= samples.size) return

        var nextSize = samples.size
        while (nextSize < targetSize) {
            nextSize *= 2
        }
        samples = samples.copyOf(nextSize)
    }
}
