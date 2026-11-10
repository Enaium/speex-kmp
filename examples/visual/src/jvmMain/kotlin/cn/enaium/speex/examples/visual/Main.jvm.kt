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

import kotlin.system.exitProcess

/** Parses `--frames N`, `--page N`, `--record` and `--replay` (all optional). */
private fun parseArgs(args: Array<String>): Args {
    var frames = Int.MAX_VALUE
    var page = 0
    var record = false
    var replay = false
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--frames" -> if (i + 1 < args.size) {
                frames = args[i + 1].toIntOrNull() ?: Int.MAX_VALUE
                i++
            }

            "--page" -> if (i + 1 < args.size) {
                page = args[i + 1].toIntOrNull() ?: 0
                i++
            }

            "--record" -> record = true
            "--replay" -> replay = true
        }
        i++
    }
    return Args(frames, page, record, replay)
}

private class Args(val frames: Int, val page: Int, val record: Boolean, val replay: Boolean)

/** Set on the relaunched JVM so a failure there cannot start another one. */
private const val RELAUNCH_MARKER = "SPEEX_KMP_VISUAL_RELAUNCHED"

/** JVM entry point: `./gradlew :examples:visual:jvmRun`. */
fun main(args: Array<String>) {
    val parsed = parseArgs(args)
    if (runVisualExample(parsed.frames, parsed.page, parsed.record, parsed.replay)) return
    // macOS: a plain JVM does not own the first thread, which SDL needs to
    // create its window. Relaunch once with the flag rather than failing with
    // an instruction.
    if (isMacOs && System.getenv(RELAUNCH_MARKER) == null) {
        val status = relaunchOnFirstThread(args)
        if (status != null) exitProcess(status)
    }
    exitProcess(1)
}

private val isMacOs: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("mac")

/**
 * Starts this entry point again in a child JVM launched with
 * `-XstartOnFirstThread`, forwarding stdio and the exit status.
 */
private fun relaunchOnFirstThread(args: Array<String>): Int? = try {
    val java = ProcessHandle.current().info().command().orElse(null)
        ?: "${System.getProperty("java.home")}/bin/java"
    val command = listOf(java, "-XstartOnFirstThread", "-cp", System.getProperty("java.class.path"), MAIN_CLASS) + args
    report("rerunning on the first thread: ${command.joinToString(" ")}")
    val child = ProcessBuilder(command)
        .inheritIO()
        .apply { environment()[RELAUNCH_MARKER] = "1" }
        .start()
    Runtime.getRuntime().addShutdownHook(Thread { child.destroy() })
    child.waitFor()
} catch (e: Exception) {
    report("could not rerun the example: $e")
    null
}

private const val MAIN_CLASS = "cn.enaium.speex.examples.visual.Main_jvmKt"
