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

import cn.enaium.imgui.ImGui

/**
 * Wrapping row layout for the controls.
 *
 * A row of controls does not fit on a narrow window, and a widget cut off at
 * the edge cannot be operated. Every helper measures what it is about to draw
 * and starts a new row when the rest of the current one is too small.
 */
internal object Ui {

    private const val ITEM_GAP = 10f
    private const val LABEL_GAP = 6f
    private const val BUTTON_PADDING = 8f
    private const val MIN_PLOT_HEIGHT = 80f
    private const val PLOT_GAP = 8f

    private var rowWidth = 0f

    /** Resets the row tracker at the start of a page. */
    fun beginPage() {
        rowWidth = 0f
    }

    fun newRow() {
        rowWidth = 0f
    }

    private fun reserve(width: Float) {
        val available = ImGui.getContentRegionAvail().x
        val needed = if (rowWidth > 0f) ITEM_GAP + width else width
        if (rowWidth > 0f && rowWidth + needed > available) {
            rowWidth = width
            return
        }
        if (rowWidth > 0f) {
            ImGui.sameLine()
            rowWidth += ITEM_GAP
        }
        rowWidth += width
    }

    fun button(label: String, onClick: () -> Unit) {
        // "##"/"###" hides the id suffix from the label ImGui draws; the row
        // must reserve the visible width, not the id's.
        reserve(ImGui.calcTextSize(label.substringBefore("##")).x + BUTTON_PADDING * 2f)
        if (ImGui.button(label)) onClick()
    }

    fun checkbox(label: String, values: BooleanArray, onChange: () -> Unit) {
        reserve(ImGui.getFrameHeight() + LABEL_GAP + ImGui.calcTextSize(label.substringBefore("##")).x)
        if (ImGui.checkbox(label, values)) onChange()
    }

    fun sliderInt(
        label: String,
        values: IntArray,
        min: Int,
        max: Int,
        format: String,
        itemWidth: Float = 180f,
        onChange: () -> Unit,
    ) {
        reserve(itemWidth + LABEL_GAP + ImGui.calcTextSize(label.substringBefore("##")).x)
        ImGui.setNextItemWidth(itemWidth)
        if (ImGui.sliderInt(label, values, min, max, format)) onChange()
    }

    fun sliderFloat(
        label: String,
        values: FloatArray,
        min: Float,
        max: Float,
        format: String,
        itemWidth: Float = 180f,
        onChange: () -> Unit,
    ) {
        reserve(itemWidth + LABEL_GAP + ImGui.calcTextSize(label.substringBefore("##")).x)
        ImGui.setNextItemWidth(itemWidth)
        if (ImGui.sliderFloat(label, values, min, max, format)) onChange()
    }

    fun combo(
        label: String,
        values: IntArray,
        labels: Array<String>,
        itemWidth: Float = 180f,
        onChange: () -> Unit,
    ) {
        reserve(itemWidth + LABEL_GAP + ImGui.calcTextSize(label.substringBefore("##")).x)
        ImGui.setNextItemWidth(itemWidth)
        if (ImGui.combo(label, values, labels)) onChange()
    }

    /** Height one of [plots] stacked plots may use after the controls. */
    fun plotHeight(plots: Int): Float {
        val available = ImGui.getContentRegionAvail().y
        return maxOf((available - PLOT_GAP * (plots - 1)) / plots, MIN_PLOT_HEIGHT)
    }
}
