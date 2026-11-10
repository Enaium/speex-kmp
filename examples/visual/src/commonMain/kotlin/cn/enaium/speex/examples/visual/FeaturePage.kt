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

package cn.enaium.speex.examples.visual

/** One visual feature page. */
internal abstract class FeaturePage(val title: String) {

    /** Advances the synthetic pipeline by one frame, if it is not paused. */
    abstract fun pump()

    /** Draws the controls, stats and plots of the page. */
    abstract fun draw()

    /** Clears the page state and starts over. */
    open fun reset() {}

    /** Called when this page becomes the visible one. */
    open fun onShown() {}

    /** Called when another page becomes visible. */
    open fun onHidden() {}

    /** Selects the microphone as this page's source, for `--record`. */
    open fun startMicrophone() {}

    /** Replays the newest microphone window, for `--replay`. */
    open fun replayMicrophone() {}

    /** Releases device or native resources when the window closes. */
    open fun close() {}

    /** One line printed at the end of a run, so a headless run shows what ran. */
    abstract fun summary(): String
}
