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

import cn.enaium.imgui.ImFontConfig
import cn.enaium.imgui.ImGui
import cn.enaium.imgui.ImTextureID
import cn.enaium.imgui.backends.sdl.ImGuiSdlBackend
import cn.enaium.imgui.backends.sdl.ImGuiSdlGpuBackend
import cn.enaium.imgui.backends.sdl.ImGuiSdlRendererBackend
import cn.enaium.imgui.extensions.implot.ImPlot
import cn.enaium.sdl.SDL
import cn.enaium.sdl.SDLColor
import cn.enaium.sdl.SDLEvent
import cn.enaium.sdl.SDLGPU
import cn.enaium.sdl.SDLGPUDevice
import cn.enaium.sdl.SDLInitFlags
import cn.enaium.sdl.SDLWindow
import cn.enaium.sdl.SDLWindowEventType
import cn.enaium.sdl.SDLWindowFlags

/** Milliseconds between two frame rate reports. */
private const val FRAME_REPORT_MILLIS = 2000u

/**
 * Platform specific advice appended when SDL cannot open a video device, or
 * empty when the platform has nothing to add.
 */
internal expect val videoInitHint: String

/**
 * Boots an SDL3 window with the GPU API, the published imgui-kmp SDL backends
 * and an ImPlot context, then runs the frame loop calling [draw].
 */
internal object ImGuiSdlApp {

    /**
     * Runs until the window is closed or [frames] frames were drawn; returns
     * `false` when no window could be opened at all.
     */
    fun run(
        title: String,
        frames: Int = Int.MAX_VALUE,
        onClose: (() -> Unit)? = null,
        draw: (frame: Int) -> Unit,
    ): Boolean {
        SDL.setMainReady()
        if (!SDL.init(SDLInitFlags.VIDEO or SDLInitFlags.EVENTS)) {
            // Headless runs set SDL_VIDEO_DRIVER=dummy, which SDL picks up by
            // itself; an invisible fallback would hide a real failure.
            report("SDL_Init failed: ${SDL.error()}$videoInitHint")
            return false
        }
        val version = SDL.version()
        report("SDL ${version.major}.${version.minor}.${version.micro} (${SDL.revision()})")
        report("video driver: ${SDL.getCurrentVideoDriver()}")

        SDL.createWindow(
            title = title,
            width = 1280,
            height = 800,
            flags = SDLWindowFlags.RESIZABLE or SDLWindowFlags.HIGH_PIXEL_DENSITY,
        ).use { window ->
            // SDL_GPU goes through Vulkan/Metal/D3D12; where it is unavailable
            // (headless dummy driver, no usable GPU) the 2D renderer takes over.
            val device = if (SDLGPU.isSupported) SDLGPU.createDevice() else null
            val gpuDevice = when {
                device == null -> {
                    report("SDL_GPU is not supported on this platform; using the SDL renderer backend")
                    null
                }

                device.claimWindow(window) -> {
                    report("renderer: SDL_GPU [${SDLGPU.drivers.joinToString()}]")
                    device
                }

                else -> {
                    report("SDL_GPU cannot drive this window (${SDL.error()}); using the SDL renderer backend")
                    device.close()
                    null
                }
            }

            try {
                if (gpuDevice != null) {
                    runLoop(window, frames, draw) { platform -> GpuFrameBackend(gpuDevice, window, platform) }
                } else {
                    runLoop(window, frames, draw) { RendererFrameBackend(window) }
                }
            } finally {
                gpuDevice?.releaseDrawable(window)
                gpuDevice?.close()
            }
        }
        onClose?.invoke()
        SDL.quit()
        return true
    }

    private fun runLoop(
        window: SDLWindow,
        frames: Int,
        draw: (frame: Int) -> Unit,
        createBackend: (ImGuiSdlBackend) -> FrameBackend,
    ) {
        val context = ImGui.createContext()
        try {
            val platform = ImGuiSdlBackend(window)
            platform.init()
            val frameBackend = createBackend(platform)
            try {
                // Bake the font atlas at the display's pixel density so the text
                // stays crisp on Retina screens.
                val fonts = ImGui.getIO().fonts
                val density = maxOf(platform.framebufferScale.x, platform.framebufferScale.y, 1f)
                fonts.addFontDefault(ImFontConfig(sizePixels = 13f * density, rasterizerDensity = density))
                check(fonts.build()) { "font atlas build failed" }
                val texData = fonts.getTexDataAsRGBA32()
                fonts.setTexID(frameBackend.uploadFontTexture(texData.pixels, texData.width, texData.height))

                val plotContext = ImPlot.createContext()
                ImPlot.setImGuiContext(ImGui.getCurrentContext() ?: error("no imgui context"))

                var running = true
                var frame = 0
                var lastReport = SDL.getTicks()
                var framesAtReport = 0
                var drawMillis = 0.0
                var renderMillis = 0.0
                var buildMillis = 0.0
                while (running && frame < frames) {
                    while (true) {
                        val event = SDL.pollEvent() ?: break
                        when (event) {
                            is SDLEvent.Quit -> running = false
                            is SDLEvent.Window ->
                                if (event.type == SDLWindowEventType.CLOSE_REQUESTED) running = false

                            else -> platform.processEvent(event)
                        }
                    }

                    val t0 = SDL.getTicks()
                    platform.newFrame()
                    draw(frame)
                    val t1 = SDL.getTicks()
                    ImGui.render()
                    val t2 = SDL.getTicks()
                    frameBackend.render()
                    val t3 = SDL.getTicks()
                    frame++
                    drawMillis += (t1 - t0).toDouble()
                    buildMillis += (t2 - t1).toDouble()
                    renderMillis += (t3 - t2).toDouble()

                    val now = SDL.getTicks()
                    if (now - lastReport >= FRAME_REPORT_MILLIS) {
                        val elapsed = (now - lastReport).toDouble() / 1000.0
                        val count = (frame - framesAtReport).toDouble()
                        report(
                            "render: ${fixed(count / elapsed, 1)} fps, draw ${fixed(drawMillis / count, 1)} ms, " +
                                "imgui ${fixed(buildMillis / count, 1)} ms, present ${fixed(renderMillis / count, 1)} ms",
                        )
                        framesAtReport = frame
                        drawMillis = 0.0
                        renderMillis = 0.0
                        buildMillis = 0.0
                        lastReport = now
                    }
                }

                ImPlot.destroyContext(plotContext)
            } finally {
                frameBackend.close()
            }
        } finally {
            ImGui.destroyContext(context)
        }
    }

    private interface FrameBackend {
        fun uploadFontTexture(pixels: ByteArray, width: Int, height: Int): ImTextureID
        fun render()
        fun close()
    }

    private class GpuFrameBackend(
        private val device: SDLGPUDevice,
        private val window: SDLWindow,
        private val platform: ImGuiSdlBackend,
    ) : FrameBackend {

        private val backend = ImGuiSdlGpuBackend(device, window)

        override fun uploadFontTexture(pixels: ByteArray, width: Int, height: Int): ImTextureID =
            backend.uploadFontTexture(pixels, width, height)

        override fun render() {
            val commandBuffer = device.beginCommandBuffer()
            if (commandBuffer == null) {
                report("SDL_AcquireGPUCommandBuffer failed: ${SDL.error()}")
                return
            }
            val swapchain = device.acquireSwapchainTexture(commandBuffer, window)
            val target = swapchain?.texture
            if (target != null) {
                val size = platform.sizeInPixels
                backend.renderFrame(commandBuffer, target, size.x.toInt(), size.y.toInt())
            }
            device.submit(commandBuffer)
        }

        override fun close() {
            backend.close()
        }
    }

    private class RendererFrameBackend(window: SDLWindow) : FrameBackend {

        private val renderer = SDL.createRenderer(window)
        private val backend = ImGuiSdlRendererBackend(renderer)

        override fun uploadFontTexture(pixels: ByteArray, width: Int, height: Int): ImTextureID =
            backend.uploadFontTexture(pixels, width, height)

        override fun render() {
            renderer.drawColor = SDLColor(18, 18, 24)
            renderer.clear()
            backend.renderDrawData(ImGui.getDrawData())
            renderer.present()
        }

        override fun close() {
            backend.close()
            renderer.close()
        }
    }
}
