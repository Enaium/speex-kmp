# speex-kmp

Kotlin Multiplatform bindings for [Speex](https://github.com/xiph/speex) and
[SpeexDSP](https://github.com/xiph/speexdsp): speech encoding/decoding plus
resampling, preprocessing, echo cancellation and jitter buffering on JVM,
Android, Kotlin/Native and the web.

`speex` and `speexdsp` are included as Git submodules and compiled from source
for every target. Two C ABIs drive the bridges:
[`jni/include/speex_kmp.h`](jni/include/speex_kmp.h) for the codec and
[`jni/include/speexdsp_kmp.h`](jni/include/speexdsp_kmp.h) for the DSP modules.
Both feed the JNI bridge, the Kotlin/Native cinterop and the WebAssembly build,
so every platform shares the same validation, bit-stream semantics and buffer
ownership. The floating-point build with the portable `smallft` FFT is used
everywhere; architecture assembly is disabled, leaving one code path across
x86, ARM, Android, MinGW and WebAssembly.

Binding version: **1.2.1.1** (Speex and SpeexDSP 1.2.1).

The artifact contains:

- **libspeex**: narrowband/wideband/ultra-wideband codec, bit-stream helpers,
  Ogg/Speex header and intensity stereo.
- **libspeexdsp**: sample-rate converter, preprocessor (denoise/AGC/VAD),
  acoustic echo canceller, channel decorrelator, adaptive jitter buffer and
  byte ring buffer.

## Supported targets

| Platform | Kotlin target | Native artifact |
|---|---|---|
| JVM | `jvm` | `speex-kmp-jni-jvm-{linux,darwin}-{x86_64,aarch64}`, `speex-kmp-jni-jvm-windows-x86_64` |
| Android | `android` | `speex-kmp` AAR with `libspeex_jni.so` for every ABI |
| Android Native | `androidNativeArm64`, `androidNativeArm32`, `androidNativeX64`, `androidNativeX86` | static archive embedded in the cinterop klib |
| Linux | `linuxX64`, `linuxArm64` | static archive embedded in the cinterop klib |
| Windows | `mingwX64` | static archive embedded in the cinterop klib |
| macOS | `macosX64`, `macosArm64` | static archive embedded in the cinterop klib |
| iOS | `iosX64`, `iosArm64`, `iosSimulatorArm64` | static archive embedded in the cinterop klib |
| tvOS | `tvosArm64`, `tvosSimulatorArm64` | static archive embedded in the cinterop klib |
| watchOS | `watchosArm64`, `watchosSimulatorArm64`, `watchosDeviceArm64` | static archive embedded in the cinterop klib |
| Kotlin/JS | `js` | embedded WebAssembly module |
| Kotlin/Wasm | `wasmJs` | embedded WebAssembly module |

The web targets compile Speex and SpeexDSP to a standalone `.wasm` module with
Emscripten, embed the binary into the published klib and instantiate it
synchronously through a small JS bridge. No resource lookup or async
initialization is required at runtime.

The same module is also published to npm as `@enaium/speex-kmp` for plain
JavaScript/TypeScript web apps. Stage it with
`./gradlew :speex-kmp:assembleWasmNpm`; it contains `speex_wasm.wasm` and
`speex_kmp_glue.js`, whose `initSpeexKmp()` installs the C shim on
`globalThis.__speexWeb`. Kotlin consumers do not need the npm package.

## Using the library

```kotlin
dependencies {
    implementation("cn.enaium.speex:speex-kmp:1.2.1.1")
}
```

The Android AAR and the JVM artifacts contain the native libraries. JVM
consumers automatically receive the per-OS/arch JNI artifacts and
`NativeLoader` extracts the matching one from the classpath.

### Encoding and decoding

One frame is 20 ms: 160 samples at 8 kHz, 320 at 16 kHz and 640 at 32 kHz.

```kotlin
import cn.enaium.speex.*

val encoder = SpeexEncoder.open(SpeexMode.WIDEBAND)
val decoder = SpeexDecoder.open(SpeexMode.WIDEBAND)

try {
    encoder.quality = 8          // 0..10
    encoder.vbr = true
    encoder.complexity = 3

    while (hasPcm()) {
        val pcm = readFrame(encoder.frameSize)        // ShortArray, 16-bit PCM
        val packet = encoder.encode(pcm)               // null when DTX drops the frame
        if (packet != null) {
            sink.write(packet)
            speaker.write(decoder.decode(packet))
        }
    }

    // A lost packet: run packet-loss concealment instead of consuming input.
    speaker.write(decoder.decode(null))
} finally {
    encoder.close()
    decoder.close()
}
```

`encode` accepts an `offset` so a larger capture buffer can be consumed in
frames. The input array may be overwritten by Speex.

### Stereo

Speex sends an intensity-stereo request in the mono bit-stream. The encoder
folds an interleaved frame to mono in place; the decoder needs a
`SpeexStereo` state that persists for the whole stream.

```kotlin
SpeexEncoder.open(SpeexMode.ULTRA_WIDEBAND).use { encoder ->
    SpeexDecoder.open(SpeexMode.ULTRA_WIDEBAND).use { decoder ->
        SpeexStereo().use { stereo ->
            val interleaved = readStereoFrame(encoder.frameSize)
            val packet = encoder.encodeStereo(interleaved) ?: return@use
            val pcm = decoder.decodeStereo(packet, stereo)   // 2 * frameSize samples
        }
    }
}
```

### Header and Ogg metadata

RTP does not use the Ogg/Speex header; this is for `.spx` files and stream
metadata.

```kotlin
SpeexHeader.create(rate = 16_000, channels = 1, mode = SpeexMode.WIDEBAND).use { header ->
    header.bitrate = 27_800
    header.vbr = true
    val packet = header.toPacket()          // 80 bytes, little-endian

    SpeexHeader.fromPacket(packet).use { parsed ->
        println("${parsed.rate} Hz, ${parsed.channels} channel(s), ${parsed.frameSize} samples/frame")
    }
}
```

### Bit-streams

`SpeexBits` exposes the bit-packing layer for custom framing. `write()` pads
the last byte with the stream terminator, so the result is a valid packet.

```kotlin
SpeexBits().use { bits ->
    bits.pack(14, 5)
    bits.pack(9, 4)                 // in-band stereo marker
    val packet = bits.write()

    SpeexBits().use { reader ->
        reader.readFrom(packet)
        val marker = reader.unpackUnsigned(5)
        val request = reader.unpackUnsigned(4)
    }
}
```

## SpeexDSP

### Resampling

`SpeexResampler` converts interleaved 16-bit PCM. The input length is per
channel; feed consecutive frames to the same instance so the filter state is
preserved.

```kotlin
SpeexResampler.create(channels = 1, inRate = 16_000, outRate = 8_000).use { resampler ->
    val pcm8k = resampler.resample(pcm16k)
}
```

Fractional ratios are available through
`SpeexResampler.createFractional(...)`. Quality is 0-10; the class exposes
`QUALITY_VOIP`, `QUALITY_DEFAULT` and `QUALITY_DESKTOP`.

### Preprocessing and echo cancellation

The preprocessor performs noise suppression, automatic gain control and voice
activity detection. The echo canceller needs the microphone frame and the
far-end frame; attach it to the preprocessor for residual echo suppression.

```kotlin
SpeexEchoCanceller.create(frameSize = 160, filterLength = 800).use { echo ->
    SpeexPreprocessor.create(frameSize = 160, samplingRate = 8_000).use { preprocessor ->
        preprocessor.denoise = true
        preprocessor.agc = true
        preprocessor.setEchoState(echo)

        val clean = echo.cancellation(microphone, speaker)
        val voice = preprocessor.process(clean)      // clean is modified in place
    }
}
```

`SpeexDecorrelator` provides the channel decorrelation used for multi-channel
echo cancellation. `SpeexPreprocessor.powerSpectrum()` and
`noisePowerSpectrum()` expose the last frame's spectra.

### Jitter buffer

```kotlin
SpeexJitterBuffer.create(stepSize = 160).use { jitter ->
    jitter.put(packet, timestamp = rtpTimestamp, span = 160)

    val result = jitter.get(desiredSpan = 160)
    if (result.status == SpeexJitterStatus.OK) {
        decoder.decode(result.packet.data)
    } else {
        decoder.decode(null)                 // conceal a lost frame
    }
}
```

Timestamps and spans are the caller's units (usually RTP timestamp ticks).
They are unsigned 32-bit values exposed as raw `Int` bits, so wrap-around
behaves like RTP. `SpeexRingBuffer` is also available for simple byte queues.

## API notes

- Audio samples are `ShortArray` (signed 16-bit PCM); the float API is not
  bound.
- `SpeexEncoder.encode` returns `null` for a DTX frame: nothing should be
  transmitted.
- `SpeexDecoder.decode(null)` performs packet-loss concealment and still
  returns one frame.
- In-band callback registration is internal: the stereo request is wired by
  `decodeStereo`, and custom `SpeexCallback` handlers are not exposed.
- The encoders, decoders, resamplers, preprocessors, echo cancellers and jitter
  buffers are stateful and not thread-safe; use one instance per stream and
  thread.
- SpeexDSP's float processing API is not bound; PCM crosses the boundary as
  `ShortArray`. The resampler error strings come from
  `SpeexDsp.resamplerErrorString(code)`.

## Visual examples

[`examples/visual`](examples/visual) is a desktop visualizer for every feature
in this binding. It uses Dear ImGui/ImPlot inside an SDL3 window. Pages that
analyse a time-domain signal (Codec, Resampler, Preprocess, Echo, Decorrelate)
have a `source` choice: synthetic speech by default, or the real microphone.
When the microphone is selected, Record/Pause, Monitor and Replay controls are
available on that same page. The module builds for the JVM and as a standalone
executable on macOS, Linux and Windows; Android is covered by the library's
AAR/JNI and native tests.

| Page | What is drawn |
|---|---|
| Codec | input vs decoded waveform, packet size and average bitrate; source, mode, quality, VBR, DTX and stereo controls |
| Resampler | input and output overlaid on a millisecond axis, ratio/latency, output level history; source, rates and quality |
| Preprocess | noisy vs denoised/AGC waveform, VAD probability and gain history, power spectrum and noise estimate; source |
| Echo | microphone vs cancelled waveform, ERLE convergence, adaptive impulse response, echo delay/gain and filter length; near-end source |
| Decorrelate | two near-identical channels before/after decorrelation and the inter-channel correlation; source |
| Jitter | buffer occupancy and buffering delay under configurable network jitter/loss, OK/MISSING/INSERTION counters |
| Ring buffer | occupancy history, write/read counters and the last read bytes |

```bash
# Desktop JVM window.
./gradlew :examples:visual:jvmRun

# Native desktop executable (macOS arm64 shown).
./gradlew :examples:visual:linkDebugExecutableMacosArm64
./examples/visual/build/bin/macosArm64/debugExecutable/visual.kexe

# Headless smoke run: 120 UI frames, selected page pumped once per frame.
SDL_VIDEO_DRIVER=dummy ./gradlew :examples:visual:jvmRun --args="--frames 120"

# Select a starting page (0 codec, 1 resampler, 2 preprocess, 3 echo,
# 4 decorrelate, 5 jitter, 6 ring buffer). JVM uses --page; native uses
# SPEEX_KMP_PAGE.
SDL_VIDEO_DRIVER=dummy ./gradlew :examples:visual:jvmRun --args="--frames 20 --page 4"
SPEEX_KMP_FRAMES=120 SPEEX_KMP_PAGE=4 SDL_VIDEO_DRIVER=dummy \
  ./examples/visual/build/bin/macosArm64/debugExecutable/visual.kexe

# Select the microphone source on the current page and replay the newest
# captured window once. JVM uses --record/--replay; native uses
# SPEEX_KMP_RECORD=1 / SPEEX_KMP_REPLAY=1.
./gradlew :examples:visual:jvmRun --args="--record --replay"
SPEEX_KMP_RECORD=1 SPEEX_KMP_REPLAY=1 \
  ./examples/visual/build/bin/macosArm64/debugExecutable/visual.kexe
```

The source controls use the system default input/output devices at 48 kHz
mono and resample to the page's rate. Record/Pause opens and releases the
microphone; Monitor loops the raw capture to the speaker; Replay plays the
newest raw capture window. On macOS the JVM needs the microphone permission
granted to the terminal/IDE; the native executable asks through the usual
system prompt.

The entry points print a final summary per page (frames processed, packet
count/bitrate, resampler in/out counts, VAD/AGC state, ERLE, jitter statuses and
ring-buffer throughput), which is what the headless runs report.

### Android example

[`examples/android`](examples/android) is a plain Views app that consumes the
published AAR (no native sources of its own). It runs a deterministic
wideband encode/decode self-test on launch and can record three seconds from
the microphone, encode/decode it, and play the result back.

```bash
./gradlew :examples:android:assembleDebug
# examples/android/build/outputs/apk/debug/android-debug.apk
```

The APK packages `libspeex_jni.so` for `arm64-v8a`, `armeabi-v7a`, `x86` and
`x86_64`; `RECORD_AUDIO` is requested at runtime for the recording button.

## Building

Requirements:

- JDK 21+
- CMake 3.20+ and a C compiler
- Android SDK + NDK 27.0.12077973 for the AAR and Android Native targets
- Emscripten (`EMSDK` or `~/emsdk`) for the JS/Wasm targets

```bash
git submodule update --init --recursive

./gradlew :speex-kmp:jvmTest
./gradlew :speex-kmp:macosArm64Test
./gradlew :speex-kmp:jsNodeTest :speex-kmp:wasmJsNodeTest
./gradlew :speex-kmp:buildAndroidJniLibs
./gradlew :examples:visual:jvmRun
```

The Gradle module drives CMake for every target. The JNI shared library and
the static archives are compiled from the `speex` and `speexdsp` submodules;
the files outside them are the shims (`jni/speex_kmp.c`,
`jni/speexdsp_kmp.c`), the JNI bridges (`jni/speex_jni.c`,
`jni/speexdsp_jni.c`), the WebAssembly entry points (`jni/speex_wasm_entry.c`,
`jni/speexdsp_wasm_entry.c`) and the fixed-width type headers
(`jni/include/speex_config_types.h`, `jni/include/speexdsp_config_types.h`).
`jni/include/speex_kmp_all.h` combines both ABI headers for cinterop.

## License

The Kotlin bindings and the C shims are MIT-licensed. Speex and SpeexDSP are
BSD-3-Clause; see [`speex/COPYING`](speex/COPYING) and
[`speexdsp/COPYING`](speexdsp/COPYING).
