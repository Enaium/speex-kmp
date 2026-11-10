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
 * A Speex bit-stream: the bit-packing buffer that [SpeexEncoder] and
 * [SpeexDecoder] use internally, exposed for custom framing and for
 * [SpeexStereo].
 *
 * ```kotlin
 * SpeexBits().use { bits ->
 *     bits.pack(14, 5)          // in-band marker
 *     bits.insertTerminator()
 *     val packet = bits.write()
 * }
 * ```
 *
 * Instances are not thread-safe.
 */
class SpeexBits : AutoCloseable {
    internal val handle: SpeexBitsHandle = SpeexNative.bitsCreate()
        ?: throw SpeexException("Speex could not allocate a bit-stream")

    private var closed = false

    /** Size of the stream in bytes, including a partially filled last byte. */
    val nbytes: Int get() = SpeexNative.bitsNbytes(checkOpen())

    /** Bits still readable from the current cursor; -1 after an overflow. */
    val remaining: Int get() = SpeexNative.bitsRemaining(checkOpen())

    /** Clears the stream and rewinds the cursor. */
    fun reset() {
        SpeexNative.bitsReset(checkOpen())
    }

    /** Rewinds the read cursor without clearing the content. */
    fun rewind() {
        SpeexNative.bitsRewind(checkOpen())
    }

    /** Copies [data] into the stream and rewinds the read cursor. */
    fun readFrom(data: ByteArray) {
        val result = SpeexNative.bitsReadFrom(checkOpen(), data)
        if (result < 0) throw SpeexException("Speex rejected a ${data.size}-byte bit-stream")
    }

    /** Appends [data] after the bits already in the stream. */
    fun readWholeBytes(data: ByteArray) {
        val result = SpeexNative.bitsReadWholeBytes(checkOpen(), data)
        if (result < 0) throw SpeexException("Speex rejected appending ${data.size} bytes")
    }

    /** Appends [nbits] low bits of [data]. */
    fun pack(data: Int, nbits: Int) {
        require(nbits in 0..32) { "nbits must be between 0 and 32, got $nbits" }
        SpeexNative.bitsPack(checkOpen(), data, nbits)
    }

    /** Reads [nbits] as a signed two's-complement integer. */
    fun unpackSigned(nbits: Int): Int {
        require(nbits in 0..32) { "nbits must be between 0 and 32, got $nbits" }
        return SpeexNative.bitsUnpackSigned(checkOpen(), nbits)
    }

    /** Reads [nbits] as an unsigned integer, returned in an `Int` register. */
    fun unpackUnsigned(nbits: Int): Int {
        require(nbits in 0..32) { "nbits must be between 0 and 32, got $nbits" }
        return SpeexNative.bitsUnpackUnsigned(checkOpen(), nbits)
    }

    /** Next bit without moving the cursor; `-1` if the stream overflowed. */
    fun peek(): Int = SpeexNative.bitsPeek(checkOpen())

    /** Next [nbits] bits without moving the cursor. */
    fun peekUnsigned(nbits: Int): Int {
        require(nbits in 0..32) { "nbits must be between 0 and 32, got $nbits" }
        return SpeexNative.bitsPeekUnsigned(checkOpen(), nbits)
    }

    /** Moves the read cursor by [nbits]. */
    fun advance(nbits: Int) {
        require(nbits >= 0) { "nbits must not be negative, got $nbits" }
        SpeexNative.bitsAdvance(checkOpen(), nbits)
    }

    /** Pads the last byte with the stream terminator, as packets require. */
    fun insertTerminator() {
        SpeexNative.bitsInsertTerminator(checkOpen())
    }

    /**
     * Serialises the stream. Like `speex_bits_write`, the last byte is padded
     * with the stream terminator, so the result is safe to send as a packet.
     */
    fun write(): ByteArray = SpeexNative.bitsWrite(checkOpen())

    /** Same as [write]. */
    fun toByteArray(): ByteArray = write()

    override fun close() {
        if (closed) return
        closed = true
        SpeexNative.bitsFree(handle)
    }

    /** The native handle, for objects that hand it to another shim call. */
    internal fun requireHandle(): SpeexBitsHandle = checkOpen()

    override fun toString(): String = if (closed) "SpeexBits(closed)" else "SpeexBits($nbytes bytes, $remaining bits left)"

    private fun checkOpen(): SpeexBitsHandle {
        check(!closed) { "SpeexBits is closed" }
        return handle
    }
}
