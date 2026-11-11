package cn.enaium.speex.examples.android

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import cn.enaium.speex.Speex
import cn.enaium.speex.SpeexDecoder
import cn.enaium.speex.SpeexEncoder
import cn.enaium.speex.SpeexMode
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Minimal Android consumer of the published AAR.
 *
 * The app has no native sources: `:speex-kmp`'s Android variant supplies the
 * Kotlin/JNI classes and `libspeex_jni.so` for every ABI. The first button runs
 * a deterministic encode/decode self-test; the second records three seconds
 * from the microphone, encodes and decodes it, then plays the result.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var selfTestButton: Button
    private lateinit var recordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
        selfTestButton.isEnabled = false
        runSelfTest()
    }

    private fun buildLayout(): ViewGroup {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "speex-kmp Android example"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Bindings ${Speex.VERSION} · Speex ${Speex.libraryVersion}"
            textSize = 13f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(4), 0, dp(16))
        })

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        selfTestButton = Button(this).apply {
            text = "Run codec self-test"
            setOnClickListener { selfTestButton.isEnabled = false; runSelfTest() }
        }
        recordButton = Button(this).apply {
            text = "Record 3 s"
            setOnClickListener { startRecording() }
        }
        buttons.addView(selfTestButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        buttons.addView(recordButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(buttons)

        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(16), 0, dp(8))
        }
        root.addView(status)

        log = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        root.addView(log, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        return ScrollView(this).apply { addView(root) }
    }

    private fun append(message: String) {
        runOnUiThread {
            log.append(if (log.text.isEmpty()) message else "\n$message")
        }
    }

    private fun setStatus(message: String) {
        runOnUiThread { status.text = message }
    }

    /** Deterministic 440 Hz tone: no device or permission is involved. */
    private fun runSelfTest() {
        setStatus("Running codec self-test…")
        thread(name = "speex-self-test") {
            try {
                val encoder = SpeexEncoder.open(SpeexMode.WIDEBAND)
                val decoder = SpeexDecoder.open(SpeexMode.WIDEBAND)
                try {
                    val frameSize = encoder.frameSize
                    val frame = ShortArray(frameSize)
                    var packets = 0
                    var bytes = 0
                    var decodedSamples = 0
                    var energy = 0L
                    for (frameIndex in 0 until 50) {
                        for (i in 0 until frameSize) {
                            val phase = 2.0 * PI * 440.0 * (frameIndex * frameSize + i) / 16_000.0
                            frame[i] = (sin(phase) * 12_000).toInt().toShort()
                        }
                        val packet = encoder.encode(frame)
                        if (packet != null) {
                            packets++
                            bytes += packet.size
                        }
                        val decoded = decoder.decode(packet)
                        decodedSamples += decoded.size
                        for (sample in decoded) energy += sample.toLong() * sample
                    }
                    val summary = buildString {
                        append("Codec OK: wideband, 50 frames, $packets packets, $bytes bytes, ")
                        append("$decodedSamples decoded samples, energy $energy")
                    }
                    setStatus(summary)
                    append(summary)
                } finally {
                    encoder.close()
                    decoder.close()
                }
            } catch (t: Throwable) {
                setStatus("Codec self-test failed: ${t.message}")
                append(t.stackTraceToString())
            } finally {
                runOnUiThread { selfTestButton.isEnabled = true }
            }
        }
    }

    private fun startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_REQUEST)
            return
        }
        recordButton.isEnabled = false
        thread(name = "speex-record") { recordEncodeDecodeAndPlay() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else if (requestCode == RECORD_REQUEST) {
            setStatus("Microphone permission denied")
        }
    }

    private fun recordEncodeDecodeAndPlay() {
        val sampleRate = 16_000
        val frameSize = 320
        val frameCount = 150 // 150 * 20 ms = 3 s
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            finishRecording("AudioRecord is unavailable (${minBuffer})")
            return
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, frameSize * 2 * 4),
        )
        val encoder = SpeexEncoder.open(SpeexMode.WIDEBAND)
        val decoder = SpeexDecoder.open(SpeexMode.WIDEBAND)
        val output = ShortArray(sampleRate * 3)
        var outputOffset = 0
        var packets = 0
        var bytes = 0

        try {
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                finishRecording("Microphone could not be initialized")
                return
            }
            record.startRecording()
            setStatus("Recording, encoding and decoding…")
            val pcm = ShortArray(frameSize)
            for (frame in 0 until frameCount) {
                var read = 0
                while (read < frameSize) {
                    val count = record.read(pcm, read, frameSize - read)
                    if (count <= 0) break
                    read += count
                }
                if (read < frameSize) break

                val packet = encoder.encode(pcm)
                if (packet != null) {
                    packets++
                    bytes += packet.size
                }
                val decoded = decoder.decode(packet)
                val copy = minOf(decoded.size, output.size - outputOffset)
                decoded.copyInto(output, outputOffset, 0, copy)
                outputOffset += copy

                if (frame % 25 == 0) {
                    setStatus("Recorded ${frame + 1}/$frameCount frames, ${bytes} encoded bytes")
                }
            }
        } catch (t: Throwable) {
            append(t.stackTraceToString())
        } finally {
            runCatching { record.stop() }
            record.release()
            encoder.close()
            decoder.close()
        }

        if (outputOffset == 0) {
            finishRecording("No microphone samples were captured")
            return
        }

        play(output, outputOffset, sampleRate, minBuffer)
        finishRecording(
            "Recorded ${outputOffset / frameSize} frames, $packets packets, $bytes bytes, " +
                "played ${outputOffset * 1000L / sampleRate} ms",
        )
    }

    private fun play(samples: ShortArray, count: Int, sampleRate: Int, minBuffer: Int) {
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, count * 2),
            AudioTrack.MODE_STATIC,
        )
        try {
            val written = track.write(samples, 0, count)
            if (written <= 0) {
                append("AudioTrack.write returned $written")
                return
            }
            track.play()
            setStatus("Playing back ${count * 1000L / sampleRate} ms…")
            Thread.sleep(count * 1000L / sampleRate + 150)
        } finally {
            track.release()
        }
    }

    private fun finishRecording(message: String) {
        setStatus(message)
        append(message)
        runOnUiThread { recordButton.isEnabled = true }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val RECORD_REQUEST = 1001
    }
}
