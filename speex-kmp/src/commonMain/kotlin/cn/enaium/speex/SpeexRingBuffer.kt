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

/**
 * SpeexDSP's simple byte ring buffer.
 *
 * Writing more than the capacity may discard buffered bytes. Reading beyond
 * the available data zero-fills the result. The buffer does no locking;
 * serialise access yourself.
 */
class SpeexRingBuffer private constructor(private val handle: SpeexRingBufferHandle) : AutoCloseable {
    private var closed = false

    /** Bytes currently available for reading. */
    val available: Int get() = SpeexDspNative.ringBufferAvailable(openHandle())

    /** Writes [count] bytes from [offset]; returns the bytes accepted. */
    fun write(data: ByteArray, offset: Int = 0, count: Int = data.size - offset): Int {
        val handle = openHandle()
        require(offset >= 0 && count >= 0 && offset + count <= data.size) {
            "range $offset..${offset + count} is outside a ${data.size}-byte buffer"
        }
        val result = SpeexDspNative.ringBufferWrite(handle, data, offset, count)
        if (result < 0) throw SpeexException("SpeexDSP rejected ring-buffer write (code $result)")
        return result
    }

    /** Writes [len] zero bytes; returns the bytes accepted. */
    fun writeZeros(len: Int): Int {
        require(len >= 0) { "len must not be negative, got $len" }
        val result = SpeexDspNative.ringBufferWriteZeros(openHandle(), len)
        if (result < 0) throw SpeexException("SpeexDSP rejected ring-buffer write (code $result)")
        return result
    }

    /** Reads up to [len] bytes; the tail is zero-filled when the buffer is short. */
    fun read(len: Int): ByteArray {
        require(len >= 0) { "len must not be negative, got $len" }
        if (len == 0) return ByteArray(0)
        val result = SpeexDspNative.ringBufferRead(openHandle(), len)
            ?: throw SpeexException("SpeexDSP rejected ring-buffer read of $len bytes")
        return result
    }

    /**
     * Resizes the backing storage. Upstream reallocates without moving
     * wrapped/live data, so call it while the buffer is empty.
     */
    fun resize(len: Int): Int {
        require(len > 0) { "len must be positive, got $len" }
        val result = SpeexDspNative.ringBufferResize(openHandle(), len)
        if (result < 0) throw SpeexException("SpeexDSP rejected ring-buffer resize (len=$len, code $result)")
        return result
    }

    override fun close() {
        if (closed) return
        closed = true
        SpeexDspNative.ringBufferFree(handle)
    }

    override fun toString(): String = "SpeexRingBuffer(available=$available)"

    private fun openHandle(): SpeexRingBufferHandle {
        check(!closed) { "SpeexRingBuffer is closed" }
        return handle
    }

    companion object {
        /** Creates a ring buffer holding [size] bytes. */
        fun create(size: Int): SpeexRingBuffer {
            require(size > 0) { "size must be positive, got $size" }
            val handle = SpeexDspNative.ringBufferCreate(size)
                ?: throw SpeexException("SpeexDSP could not allocate a ring buffer")
            return SpeexRingBuffer(handle)
        }
    }
}
