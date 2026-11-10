/*
 * Copyright (c) 2026 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cn.enaium.speex

/** Result of [SpeexJitterBuffer.get] / [SpeexJitterBuffer.getAnother]. */
enum class SpeexJitterStatus(val code: Int) {
    /** A packet was returned. */
    OK(0),

    /** The requested span is missing; insert concealment for it. */
    MISSING(1),

    /** Insert a fake packet to increase buffering. */
    INSERTION(2),

    /** Internal jitter-buffer error. */
    INTERNAL_ERROR(-1),

    /** Invalid argument. */
    BAD_ARGUMENT(-2),
    ;

    internal companion object {
        fun fromCode(code: Int): SpeexJitterStatus =
            entries.firstOrNull { it.code == code }
                ?: throw SpeexException("unknown SpeexDSP jitter status $code")
    }
}

/** One packet returned by the jitter buffer. Timestamps and spans are the caller's units. */
class SpeexJitterPacket(
    val data: ByteArray,
    val timestamp: Int,
    val span: Int,
    val sequence: Int,
    val userData: Int,
) {
    override fun toString(): String =
        "SpeexJitterPacket(bytes=${data.size}, timestamp=$timestamp, span=$span, sequence=$sequence)"
}

/** A status plus the packet metadata; [packet] has empty data for MISSING/INSERTION. */
class SpeexJitterResult(val status: SpeexJitterStatus, val packet: SpeexJitterPacket)

/**
 * SpeexDSP adaptive jitter buffer.
 *
 * Push received packets with [put] and pull them with [get], in timestamp
 * order. Timestamps are unsigned 32-bit values exposed as raw [Int] bits, so
 * wrap-around behaves like RTP. Call [tick] once per received frame (or use
 * [get] with the desired span) and [remainingSpan] to report how much audio is
 * still queued by the application.
 *
 * ```kotlin
 * SpeexJitterBuffer.create(stepSize = 160).use { jitter ->
 *     jitter.put(packet, timestamp = rtpTimestamp, span = 160)
 *     val result = jitter.get(desiredSpan = 160)
 *     if (result.status == SpeexJitterStatus.MISSING) {
 *         decoder.decode(null)       // conceal the lost frame
 *     } else {
 *         decoder.decode(result.packet.data)
 *     }
 * }
 * ```
 */
class SpeexJitterBuffer private constructor(private val handle: SpeexJitterHandle) : AutoCloseable {
    private var closed = false

    /** Timestamp the jitter buffer will return next. */
    val pointerTimestamp: Int get() = SpeexDspNative.jitterPointerTimestamp(openHandle())

    /** Number of packets currently buffered and not yet returned. */
    val available: Int get() = getInt(SpeexJitterRequest.GET_AVAILABLE_COUNT)

    /** Minimum extra buffering required, in timestamp units. */
    var margin: Int
        get() = getInt(SpeexJitterRequest.GET_MARGIN)
        set(value) = setInt(SpeexJitterRequest.SET_MARGIN, value)

    /** Delay and concealment adjustment step. */
    var delayStep: Int
        get() = getInt(SpeexJitterRequest.GET_DELAY_STEP)
        set(value) {
            require(value > 0) { "delayStep must be positive, got $value" }
            setInt(SpeexJitterRequest.SET_DELAY_STEP, value)
        }

    /** Concealment duration in timestamp units. */
    var concealmentSize: Int
        get() = getInt(SpeexJitterRequest.GET_CONCEALMENT_SIZE)
        set(value) {
            require(value > 0) { "concealmentSize must be positive, got $value" }
            setInt(SpeexJitterRequest.SET_CONCEALMENT_SIZE, value)
        }

    /** Maximum tolerated late-packet percentage, 1-100. */
    var maxLateRate: Int
        get() = getInt(SpeexJitterRequest.GET_MAX_LATE_RATE)
        set(value) {
            require(value in 1..100) { "maxLateRate must be 1..100, got $value" }
            setInt(SpeexJitterRequest.SET_MAX_LATE_RATE, value)
        }

    /** Cost of one percent of late packets, in timestamp units. */
    var lateCost: Int
        get() = getInt(SpeexJitterRequest.GET_LATE_COST)
        set(value) = setInt(SpeexJitterRequest.SET_LATE_COST, value)

    /** Queues a packet (at most 64 KiB). The data is copied by the jitter buffer. */
    fun put(data: ByteArray, timestamp: Int, span: Int, sequence: Int = 0, userData: Int = 0) {
        require(data.size <= 64 * 1024) { "jitter packets are limited to 64 KiB, got ${data.size}" }
        require(span >= 0) { "span must not be negative, got $span" }
        val result = SpeexDspNative.jitterPut(openHandle(), data, timestamp, span, sequence, userData)
        if (result < 0) throw SpeexException("SpeexDSP rejected jitter packet (code $result)")
    }

    /** Returns the next packet spanning [desiredSpan] timestamp units. */
    fun get(desiredSpan: Int): SpeexJitterResult {
        require(desiredSpan > 0) { "desiredSpan must be positive, got $desiredSpan" }
        val handle = openHandle()
        val status = SpeexJitterStatus.fromCode(SpeexDspNative.jitterGet(handle, desiredSpan))
        return SpeexJitterResult(status, packet(handle))
    }

    /** Returns another packet with the same timestamp as the last [get] result. */
    fun getAnother(): SpeexJitterResult {
        val handle = openHandle()
        val status = SpeexJitterStatus.fromCode(SpeexDspNative.jitterGetAnother(handle))
        return SpeexJitterResult(status, packet(handle))
    }

    /** Advances the jitter buffer by one delay step. */
    fun tick() {
        SpeexDspNative.jitterTick(openHandle())
    }

    /** Reports how much audio is still queued by the application, in timestamp units. */
    fun remainingSpan(remaining: Int) {
        require(remaining >= 0) { "remaining must not be negative, got $remaining" }
        SpeexDspNative.jitterRemainingSpan(openHandle(), remaining)
    }

    /** Drops every buffered packet and resynchronises. */
    fun reset() {
        SpeexDspNative.jitterReset(openHandle())
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexDspNative.jitterFree(handle)
    }

    override fun toString(): String = "SpeexJitterBuffer(available=$available, pointerTimestamp=$pointerTimestamp)"

    private fun packet(handle: SpeexJitterHandle): SpeexJitterPacket = SpeexJitterPacket(
        data = SpeexDspNative.jitterPacket(handle) ?: ByteArray(0),
        timestamp = SpeexDspNative.jitterTimestamp(handle),
        span = SpeexDspNative.jitterSpan(handle),
        sequence = SpeexDspNative.jitterSequence(handle),
        userData = SpeexDspNative.jitterUserData(handle),
    )

    private fun openHandle(): SpeexJitterHandle {
        check(!closed) { "SpeexJitterBuffer is closed" }
        return handle
    }

    private fun getInt(request: Int): Int {
        val value = SpeexDspNative.jitterGetInt(openHandle(), request)
        if (value == XDSP_INVALID) throw SpeexException("SpeexDSP rejected jitter request $request")
        return value
    }

    private fun setInt(request: Int, value: Int) {
        val result = SpeexDspNative.jitterCtlInt(openHandle(), request, value)
        if (result < 0) throw SpeexException("SpeexDSP rejected jitter request $request=$value (code $result)")
    }

    companion object {
        /** Creates a jitter buffer; [stepSize] is the initial delay/concealment step. */
        fun create(stepSize: Int): SpeexJitterBuffer {
            require(stepSize > 0) { "stepSize must be positive, got $stepSize" }
            val handle = SpeexDspNative.jitterCreate(stepSize)
                ?: throw SpeexException("SpeexDSP could not allocate a jitter buffer")
            return SpeexJitterBuffer(handle)
        }
    }
}
