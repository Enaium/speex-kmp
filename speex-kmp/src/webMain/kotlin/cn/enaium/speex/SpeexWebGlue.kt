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
 * JavaScript glue that owns the WebAssembly instance.
 *
 * Speex is compiled to a standalone WASI module. The glue provides the handful
 * of WASI imports the C runtime references, exposes one function per C entry
 * point on `globalThis.__speexWeb`, and marshals strings through the module's
 * own `xk_alloc`.
 *
 * Bulk data (PCM samples and packets) crosses the boundary as base64 strings
 * because Kotlin/JS and Kotlin/Wasm cannot share typed arrays with a module
 * compiled outside their own toolchain.
 */
internal const val SPEEX_WEB_INIT_JS: String = """
(base64) => {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  let wasmExports = null;
  const memory = () => new Uint8Array(wasmExports.memory.buffer);
  const dataView = () => new DataView(wasmExports.memory.buffer);
  const decoder = new TextDecoder();

  const encodeBase64 = (data) => {
    let text = '';
    const chunk = 0x8000;
    for (let i = 0; i < data.length; i += chunk) {
      text += String.fromCharCode.apply(null, data.subarray(i, i + chunk));
    }
    return btoa(text);
  };

  const decodeBase64 = (text) => {
    const raw = atob(text);
    const out = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
    return out;
  };

  const stringToUtf8 = (text) => {
    const out = [];
    for (let i = 0; i < text.length; i++) {
      let code = text.codePointAt(i);
      if (code > 0xffff) i++;
      if (code < 0x80) out.push(code);
      else if (code < 0x800) out.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
      else if (code < 0x10000) out.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
      else out.push(0xf0 | (code >> 18), 0x80 | ((code >> 12) & 0x3f), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
    }
    return new Uint8Array(out);
  };

  const writeString = (text) => {
    const data = stringToUtf8(text);
    const pointer = wasmExports.xk_alloc(data.length + 1);
    if (!pointer) return 0;
    memory().set(data, pointer);
    memory()[pointer + data.length] = 0;
    return pointer;
  };

  const readString = (pointer) => {
    if (!pointer) return '';
    const data = memory();
    let end = pointer;
    while (data[end] !== 0) end++;
    return decoder.decode(data.subarray(pointer, end));
  };

  const writeBytes = (data) => {
    const pointer = wasmExports.xk_alloc(data.length);
    if (!pointer) return 0;
    memory().set(data, pointer);
    return pointer;
  };

  const bytesAt = (pointer, size) => {
    if (!pointer || size <= 0) return '';
    return encodeBase64(memory().slice(pointer, pointer + size));
  };

  const sizePointer = () => wasmExports.xk_alloc(4);

  const readSize = (pointer) => dataView().getUint32(pointer, true);

  const imports = {
    env: {
      emscripten_notify_memory_growth() {},
      __syscall_unlinkat() { return 0; },
      __syscall_renameat() { return 0; },
    },
    wasi_snapshot_preview1: {
      fd_write(fd, iovs, iovsLength, written) {
        let total = 0;
        for (let i = 0; i < iovsLength; i++) {
          const pointer = dataView().getUint32(iovs + i * 8, true);
          const length = dataView().getUint32(iovs + i * 8 + 4, true);
          const text = decoder.decode(memory().subarray(pointer, pointer + length));
          if (text.trim().length > 0) {
            if (fd === 2) console.error(text.trimEnd());
            else console.log(text.trimEnd());
          }
          total += length;
        }
        dataView().setUint32(written, total, true);
        return 0;
      },
      fd_read() { return 0; },
      fd_seek() { return 0; },
      fd_close() { return 0; },
      fd_fdstat_get() { return 0; },
      clock_time_get(clock, precision, out) {
        const now = typeof performance !== 'undefined' ? performance.now() : Date.now();
        dataView().setBigUint64(out, BigInt(Math.floor(now * 1e6)), true);
        return 0;
      },
      environ_sizes_get(count, size) {
        dataView().setUint32(count, 0, true);
        dataView().setUint32(size, 0, true);
        return 0;
      },
      environ_get() { return 0; },
      random_get(pointer, size) {
        const data = memory();
        for (let i = 0; i < size; i++) data[pointer + i] = (Math.random() * 256) | 0;
        return 0;
      },
      proc_exit(code) {
        throw new Error('Speex WebAssembly exited with code ' + code);
      },
    },
  };

  const instance = new WebAssembly.Instance(new WebAssembly.Module(bytes), imports);
  wasmExports = instance.exports;
  if (wasmExports._initialize) wasmExports._initialize();

  globalThis.__speexWeb = {
    versionString() {
      return readString(wasmExports.xk_version_string());
    },
    versionMajor() {
      return wasmExports.xk_version_major();
    },
    versionMinor() {
      return wasmExports.xk_version_minor();
    },
    versionMicro() {
      return wasmExports.xk_version_micro();
    },
    versionExtra() {
      return readString(wasmExports.xk_version_extra());
    },
    modeFrameSize(mode) {
      return wasmExports.xk_mode_frame_size(mode);
    },
    modeSubmodeBits(mode, submode) {
      return wasmExports.xk_mode_submode_bits(mode, submode);
    },
    modeName(mode) {
      return readString(wasmExports.xk_mode_name(mode));
    },
    bitsCreate() {
      return wasmExports.xk_bits_create();
    },
    bitsFree(handle) {
      wasmExports.xk_bits_free(handle);
    },
    bitsReset(handle) {
      wasmExports.xk_bits_reset(handle);
    },
    bitsRewind(handle) {
      wasmExports.xk_bits_rewind(handle);
    },
    bitsReadFrom(handle, payloadBase64) {
      const data = decodeBase64(payloadBase64);
      const pointer = writeBytes(data);
      if (!pointer && data.length > 0) return -3;
      const code = wasmExports.xk_bits_read_from(handle, pointer, data.length);
      wasmExports.xk_free(pointer);
      return code;
    },
    bitsReadWholeBytes(handle, payloadBase64) {
      const data = decodeBase64(payloadBase64);
      const pointer = writeBytes(data);
      if (!pointer && data.length > 0) return -3;
      const code = wasmExports.xk_bits_read_whole_bytes(handle, pointer, data.length);
      wasmExports.xk_free(pointer);
      return code;
    },
    bitsPack(handle, data, nbits) {
      wasmExports.xk_bits_pack(handle, data, nbits);
    },
    bitsUnpackSigned(handle, nbits) {
      return wasmExports.xk_bits_unpack_signed(handle, nbits);
    },
    bitsUnpackUnsigned(handle, nbits) {
      return wasmExports.xk_bits_unpack_unsigned(handle, nbits);
    },
    bitsPeek(handle) {
      return wasmExports.xk_bits_peek(handle);
    },
    bitsPeekUnsigned(handle, nbits) {
      return wasmExports.xk_bits_peek_unsigned(handle, nbits);
    },
    bitsAdvance(handle, nbits) {
      wasmExports.xk_bits_advance(handle, nbits);
    },
    bitsRemaining(handle) {
      return wasmExports.xk_bits_remaining(handle);
    },
    bitsNbytes(handle) {
      return wasmExports.xk_bits_nbytes(handle);
    },
    bitsInsertTerminator(handle) {
      wasmExports.xk_bits_insert_terminator(handle);
    },
    bitsWrite(handle) {
      const size = wasmExports.xk_bits_nbytes(handle);
      if (size <= 0) return '';
      const pointer = wasmExports.xk_alloc(size);
      if (!pointer) return '';
      const written = wasmExports.xk_bits_write(handle, pointer, size);
      const result = written > 0 ? bytesAt(pointer, written) : '';
      wasmExports.xk_free(pointer);
      return result;
    },
    encoderCreate(mode) {
      return wasmExports.xk_encoder_create(mode);
    },
    encoderFree(handle) {
      wasmExports.xk_encoder_free(handle);
    },
    encoderFrameSize(handle) {
      return wasmExports.xk_encoder_frame_size(handle);
    },
    encoderEncode(handle, samplesBase64) {
      const data = decodeBase64(samplesBase64);
      const pointer = writeBytes(data);
      if (!pointer) return -3;
      const code = wasmExports.xk_encoder_encode(handle, pointer, data.length >> 1);
      wasmExports.xk_free(pointer);
      return code;
    },
    encoderEncodeStereo(handle, samplesBase64) {
      const data = decodeBase64(samplesBase64);
      const pointer = writeBytes(data);
      if (!pointer) return '-3:';
      const code = wasmExports.xk_encoder_encode_stereo(handle, pointer, data.length >> 1);
      const samples = bytesAt(pointer, data.length);
      wasmExports.xk_free(pointer);
      return code + ':' + samples;
    },
    encoderPacket(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const packet = wasmExports.xk_encoder_packet(handle, pointer);
      const size = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(packet, size);
    },
    encoderCtl(handle, request, value) {
      return wasmExports.xk_encoder_ctl(handle, request, value);
    },
    encoderGet(handle, request) {
      return wasmExports.xk_encoder_get(handle, request);
    },
    encoderSetVbrQuality(handle, value) {
      return wasmExports.xk_encoder_set_vbr_quality(handle, value);
    },
    encoderGetVbrQuality(handle) {
      return wasmExports.xk_encoder_get_vbr_quality(handle);
    },
    encoderGetRelativeQuality(handle) {
      return wasmExports.xk_encoder_get_relative_quality(handle);
    },
    decoderCreate(mode) {
      return wasmExports.xk_decoder_create(mode);
    },
    decoderFree(handle) {
      wasmExports.xk_decoder_free(handle);
    },
    decoderFrameSize(handle) {
      return wasmExports.xk_decoder_frame_size(handle);
    },
    decoderDecode(handle, packetBase64) {
      const data = packetBase64 ? decodeBase64(packetBase64) : new Uint8Array(0);
      const pointer = data.length > 0 ? writeBytes(data) : 0;
      if (!pointer && data.length > 0) return -3;
      const code = wasmExports.xk_decoder_decode(handle, pointer, data.length);
      wasmExports.xk_free(pointer);
      return code;
    },
    decoderDecodeStereo(handle, packetBase64, stereo) {
      const data = packetBase64 ? decodeBase64(packetBase64) : new Uint8Array(0);
      const pointer = data.length > 0 ? writeBytes(data) : 0;
      if (!pointer && data.length > 0) return -3;
      const code = wasmExports.xk_decoder_decode_stereo(handle, pointer, data.length, stereo);
      wasmExports.xk_free(pointer);
      return code;
    },
    decoderSamples(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const samples = wasmExports.xk_decoder_samples(handle, pointer);
      const count = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(samples, count * 2);
    },
    decoderCtl(handle, request, value) {
      return wasmExports.xk_decoder_ctl(handle, request, value);
    },
    decoderGet(handle, request) {
      return wasmExports.xk_decoder_get(handle, request);
    },
    stereoCreate() {
      return wasmExports.xk_stereo_create();
    },
    stereoFree(handle) {
      wasmExports.xk_stereo_free(handle);
    },
    stereoReset(handle) {
      wasmExports.xk_stereo_reset(handle);
    },
    stereoEncode(handle, samplesBase64, offset, count, bits) {
      const data = decodeBase64(samplesBase64);
      const pointer = writeBytes(data);
      if (!pointer) return '-3:';
      const code = wasmExports.xk_stereo_encode(handle, pointer, count, bits);
      const samples = bytesAt(pointer, count * 2);
      wasmExports.xk_free(pointer);
      return code + ':' + samples;
    },
    stereoDecode(handle, samplesBase64, count) {
      const data = decodeBase64(samplesBase64);
      const pointer = writeBytes(data);
      if (!pointer) return '-3:';
      const code = wasmExports.xk_stereo_decode(handle, pointer, count);
      const samples = bytesAt(pointer, count * 2);
      wasmExports.xk_free(pointer);
      return code + ':' + samples;
    },
    headerCreate(rate, channels, mode) {
      return wasmExports.xk_header_create(rate, channels, mode);
    },
    headerParse(payloadBase64) {
      const data = decodeBase64(payloadBase64);
      const pointer = writeBytes(data);
      if (!pointer) return 0;
      const header = wasmExports.xk_header_parse(pointer, data.length);
      wasmExports.xk_free(pointer);
      return header;
    },
    headerFree(handle) {
      wasmExports.xk_header_free(handle);
    },
    headerGetInt(handle, field) {
      return wasmExports.xk_header_get_int(handle, field);
    },
    headerSetInt(handle, field, value) {
      return wasmExports.xk_header_set_int(handle, field, value);
    },
    headerString(handle, which) {
      return readString(wasmExports.xk_header_string(handle, which));
    },
    headerPacket(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const packet = wasmExports.xk_header_packet(handle, pointer);
      const size = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(packet, size);
    },
    dspVersionString() {
      return readString(wasmExports.xdsp_version_string());
    },
    dspResamplerCreate(channels, inRate, outRate, quality) {
      return wasmExports.xdsp_resampler_create(channels, inRate, outRate, quality);
    },
    dspResamplerCreateFrac(channels, ratioNum, ratioDen, inRate, outRate, quality) {
      return wasmExports.xdsp_resampler_create_frac(channels, ratioNum, ratioDen, inRate, outRate, quality);
    },
    dspResamplerError(handle) {
      return wasmExports.xdsp_resampler_error(handle);
    },
    dspResamplerFree(handle) {
      wasmExports.xdsp_resampler_free(handle);
    },
    dspResamplerChannels(handle) {
      return wasmExports.xdsp_resampler_channels(handle);
    },
    dspResamplerSetRate(handle, inRate, outRate) {
      return wasmExports.xdsp_resampler_set_rate(handle, inRate, outRate);
    },
    dspResamplerSetRateFrac(handle, ratioNum, ratioDen, inRate, outRate) {
      return wasmExports.xdsp_resampler_set_rate_frac(handle, ratioNum, ratioDen, inRate, outRate);
    },
    dspResamplerInputRate(handle) {
      return wasmExports.xdsp_resampler_input_rate(handle);
    },
    dspResamplerOutputRate(handle) {
      return wasmExports.xdsp_resampler_output_rate(handle);
    },
    dspResamplerRatioNum(handle) {
      return wasmExports.xdsp_resampler_ratio_num(handle);
    },
    dspResamplerRatioDen(handle) {
      return wasmExports.xdsp_resampler_ratio_den(handle);
    },
    dspResamplerSetQuality(handle, quality) {
      return wasmExports.xdsp_resampler_set_quality(handle, quality);
    },
    dspResamplerQuality(handle) {
      return wasmExports.xdsp_resampler_quality(handle);
    },
    dspResamplerSetInputStride(handle, stride) {
      wasmExports.xdsp_resampler_set_input_stride(handle, stride);
    },
    dspResamplerInputStride(handle) {
      return wasmExports.xdsp_resampler_input_stride(handle);
    },
    dspResamplerSetOutputStride(handle, stride) {
      wasmExports.xdsp_resampler_set_output_stride(handle, stride);
    },
    dspResamplerOutputStride(handle) {
      return wasmExports.xdsp_resampler_output_stride(handle);
    },
    dspResamplerInputLatency(handle) {
      return wasmExports.xdsp_resampler_input_latency(handle);
    },
    dspResamplerOutputLatency(handle) {
      return wasmExports.xdsp_resampler_output_latency(handle);
    },
    dspResamplerSkipZeros(handle) {
      return wasmExports.xdsp_resampler_skip_zeros(handle);
    },
    dspResamplerResetMem(handle) {
      return wasmExports.xdsp_resampler_reset_mem(handle);
    },
    dspResamplerProcess(handle, samplesBase64) {
      const data = decodeBase64(samplesBase64);
      const pointer = writeBytes(data);
      if (!pointer && data.length > 0) return -3;
      const channels = wasmExports.xdsp_resampler_channels(handle);
      const count = (data.length >> 1) / channels;
      const code = wasmExports.xdsp_resampler_process_interleaved(handle, pointer, count);
      wasmExports.xk_free(pointer);
      return code;
    },
    dspResamplerOutput(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const output = wasmExports.xdsp_resampler_output(handle, pointer);
      const count = readSize(pointer);
      const channels = wasmExports.xdsp_resampler_channels(handle);
      wasmExports.xk_free(pointer);
      return bytesAt(output, count * channels * 2);
    },
    dspResamplerStrerror(error) {
      return readString(wasmExports.xdsp_resampler_strerror(error));
    },
    dspPreprocessCreate(frameSize, samplingRate) {
      return wasmExports.xdsp_preprocess_create(frameSize, samplingRate);
    },
    dspPreprocessFree(handle) {
      wasmExports.xdsp_preprocess_free(handle);
    },
    dspPreprocessFrameSize(handle) {
      return wasmExports.xdsp_preprocess_frame_size(handle);
    },
    dspPreprocessRun(handle, samplesBase64) {
      const data = decodeBase64(samplesBase64);
      const pointer = writeBytes(data);
      if (!pointer) return '-3:';
      const vad = wasmExports.xdsp_preprocess_run(handle, pointer, data.length >> 1);
      const samples = bytesAt(pointer, data.length);
      wasmExports.xk_free(pointer);
      return vad + ':' + samples;
    },
    dspPreprocessEstimateUpdate(handle, samplesBase64) {
      const data = decodeBase64(samplesBase64);
      const pointer = writeBytes(data);
      if (!pointer) return;
      wasmExports.xdsp_preprocess_estimate_update(handle, pointer, data.length >> 1);
      wasmExports.xk_free(pointer);
    },
    dspPreprocessCtlInt(handle, request, value) {
      return wasmExports.xdsp_preprocess_ctl_int(handle, request, value);
    },
    dspPreprocessGetInt(handle, request) {
      return wasmExports.xdsp_preprocess_get_int(handle, request);
    },
    dspPreprocessCtlFloat(handle, request, value) {
      return wasmExports.xdsp_preprocess_ctl_float(handle, request, value);
    },
    dspPreprocessGetFloat(handle, request) {
      return wasmExports.xdsp_preprocess_get_float(handle, request);
    },
    dspPreprocessSetEcho(handle, echo) {
      return wasmExports.xdsp_preprocess_set_echo(handle, echo);
    },
    dspPreprocessPsdSize(handle) {
      return wasmExports.xdsp_preprocess_psd_size(handle);
    },
    dspPreprocessPsd(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const data = wasmExports.xdsp_preprocess_psd(handle, pointer);
      const size = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(data, size * 4);
    },
    dspPreprocessNoisePsdSize(handle) {
      return wasmExports.xdsp_preprocess_noise_psd_size(handle);
    },
    dspPreprocessNoisePsd(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const data = wasmExports.xdsp_preprocess_noise_psd(handle, pointer);
      const size = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(data, size * 4);
    },
    dspEchoCreate(frameSize, filterLength, microphones, speakers) {
      return wasmExports.xdsp_echo_create(frameSize, filterLength, microphones, speakers);
    },
    dspEchoFree(handle) {
      wasmExports.xdsp_echo_free(handle);
    },
    dspEchoReset(handle) {
      wasmExports.xdsp_echo_reset(handle);
    },
    dspEchoFrameSize(handle) {
      return wasmExports.xdsp_echo_frame_size(handle);
    },
    dspEchoMicrophones(handle) {
      return wasmExports.xdsp_echo_microphones(handle);
    },
    dspEchoSpeakers(handle) {
      return wasmExports.xdsp_echo_speakers(handle);
    },
    dspEchoCancellation(handle, recBase64, playBase64) {
      const rec = decodeBase64(recBase64);
      const play = decodeBase64(playBase64);
      const recPointer = writeBytes(rec);
      const playPointer = writeBytes(play);
      if (!recPointer || !playPointer) {
        wasmExports.xk_free(recPointer);
        wasmExports.xk_free(playPointer);
        return -3;
      }
      const code = wasmExports.xdsp_echo_cancellation(
        handle,
        recPointer,
        rec.length >> 1,
        playPointer,
        play.length >> 1,
      );
      wasmExports.xk_free(recPointer);
      wasmExports.xk_free(playPointer);
      return code;
    },
    dspEchoCapture(handle, recBase64) {
      const rec = decodeBase64(recBase64);
      const pointer = writeBytes(rec);
      if (!pointer) return -3;
      const code = wasmExports.xdsp_echo_capture(handle, pointer, rec.length >> 1);
      wasmExports.xk_free(pointer);
      return code;
    },
    dspEchoPlayback(handle, playBase64) {
      const play = decodeBase64(playBase64);
      const pointer = writeBytes(play);
      if (!pointer) return -3;
      const code = wasmExports.xdsp_echo_playback(handle, pointer, play.length >> 1);
      wasmExports.xk_free(pointer);
      return code;
    },
    dspEchoOutput(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const output = wasmExports.xdsp_echo_output(handle, pointer);
      const count = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(output, count * 2);
    },
    dspEchoCtlInt(handle, request, value) {
      return wasmExports.xdsp_echo_ctl_int(handle, request, value);
    },
    dspEchoGetInt(handle, request) {
      return wasmExports.xdsp_echo_get_int(handle, request);
    },
    dspEchoImpulseResponseSize(handle) {
      return wasmExports.xdsp_echo_impulse_response_size(handle);
    },
    dspEchoImpulseResponse(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const impulse = wasmExports.xdsp_echo_impulse_response(handle, pointer);
      const size = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(impulse, size * 4);
    },
    dspDecorrCreate(rate, channels, frameSize) {
      return wasmExports.xdsp_decorr_create(rate, channels, frameSize);
    },
    dspDecorrFree(handle) {
      wasmExports.xdsp_decorr_free(handle);
    },
    dspDecorrChannels(handle) {
      return wasmExports.xdsp_decorr_channels(handle);
    },
    dspDecorrFrameSize(handle) {
      return wasmExports.xdsp_decorr_frame_size(handle);
    },
    dspDecorrProcess(handle, inputBase64, strength) {
      const input = decodeBase64(inputBase64);
      const pointer = writeBytes(input);
      if (!pointer) return -3;
      const code = wasmExports.xdsp_decorr_process(handle, pointer, input.length >> 1, strength);
      wasmExports.xk_free(pointer);
      return code;
    },
    dspDecorrOutput(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const output = wasmExports.xdsp_decorr_output(handle, pointer);
      const count = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(output, count * 2);
    },
    dspJitterCreate(stepSize) {
      return wasmExports.xdsp_jitter_create(stepSize);
    },
    dspJitterFree(handle) {
      wasmExports.xdsp_jitter_free(handle);
    },
    dspJitterReset(handle) {
      wasmExports.xdsp_jitter_reset(handle);
    },
    dspJitterPut(handle, dataBase64, timestamp, span, sequence, userData) {
      const data = decodeBase64(dataBase64);
      const pointer = writeBytes(data);
      if (!pointer && data.length > 0) return -3;
      const code = wasmExports.xdsp_jitter_put(
        handle,
        pointer,
        data.length,
        timestamp >>> 0,
        span >>> 0,
        sequence & 0xffff,
        userData >>> 0,
      );
      wasmExports.xk_free(pointer);
      return code;
    },
    dspJitterGet(handle, desiredSpan) {
      return wasmExports.xdsp_jitter_get(handle, desiredSpan);
    },
    dspJitterGetAnother(handle) {
      return wasmExports.xdsp_jitter_get_another(handle);
    },
    dspJitterPacket(handle) {
      const pointer = sizePointer();
      if (!pointer) return '';
      const data = wasmExports.xdsp_jitter_packet(handle, pointer);
      const length = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(data, length);
    },
    dspJitterTimestamp(handle) {
      return wasmExports.xdsp_jitter_timestamp(handle);
    },
    dspJitterSpan(handle) {
      return wasmExports.xdsp_jitter_span(handle);
    },
    dspJitterSequence(handle) {
      return wasmExports.xdsp_jitter_sequence(handle);
    },
    dspJitterUserData(handle) {
      return wasmExports.xdsp_jitter_user_data(handle);
    },
    dspJitterPointerTimestamp(handle) {
      return wasmExports.xdsp_jitter_pointer_timestamp(handle);
    },
    dspJitterTick(handle) {
      wasmExports.xdsp_jitter_tick(handle);
    },
    dspJitterRemainingSpan(handle, remaining) {
      wasmExports.xdsp_jitter_remaining_span(handle, remaining >>> 0);
    },
    dspJitterCtlInt(handle, request, value) {
      return wasmExports.xdsp_jitter_ctl_int(handle, request, value);
    },
    dspJitterGetInt(handle, request) {
      return wasmExports.xdsp_jitter_get_int(handle, request);
    },
    dspRingBufferCreate(size) {
      return wasmExports.xdsp_buffer_create(size);
    },
    dspRingBufferFree(handle) {
      wasmExports.xdsp_buffer_free(handle);
    },
    dspRingBufferWrite(handle, dataBase64) {
      const data = decodeBase64(dataBase64);
      const pointer = writeBytes(data);
      if (!pointer && data.length > 0) return -3;
      const code = wasmExports.xdsp_buffer_write(handle, pointer, data.length);
      wasmExports.xk_free(pointer);
      return code;
    },
    dspRingBufferWriteZeros(handle, len) {
      return wasmExports.xdsp_buffer_write_zeros(handle, len);
    },
    dspRingBufferRead(handle, len) {
      const result = wasmExports.xdsp_buffer_read(handle, len);
      if (result < 0) return '';
      const pointer = sizePointer();
      if (!pointer) return '';
      const data = wasmExports.xdsp_buffer_data(handle, pointer);
      const size = readSize(pointer);
      wasmExports.xk_free(pointer);
      return bytesAt(data, size);
    },
    dspRingBufferAvailable(handle) {
      return wasmExports.xdsp_buffer_available(handle);
    },
    dspRingBufferResize(handle, len) {
      return wasmExports.xdsp_buffer_resize(handle, len);
    },
  };
}
"""
