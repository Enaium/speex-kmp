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

/** Version information for the bundled SpeexDSP build. */
object SpeexDsp {
    /** Version of the Kotlin bindings, independent of the SpeexDSP library version. */
    const val VERSION: String = "1.2.1.1"

    /** SpeexDSP library version, e.g. `"1.2.1"`. */
    val libraryVersion: String by lazy { SpeexDspNative.versionString() }

    /** English description of a `RESAMPLER_ERR_*` code. */
    fun resamplerErrorString(error: Int): String = SpeexDspNative.resamplerStrerror(error)

    override fun toString(): String = "SpeexDSP $libraryVersion (bindings $VERSION)"
}
