/*
 * JNI bridge between cn.enaium.speex.JniSpeexDsp and the SpeexDSP shim.
 *
 * Handles are raw xdsp_* pointers stored in a jlong. All arrays are copied
 * into/out of the shim for the duration of a call.
 */
#include <jni.h>
#include <stdint.h>

#include "speexdsp_kmp.h"

static xdsp_resampler *resampler_from(jlong handle) {
  return (xdsp_resampler *)(intptr_t)handle;
}

static xdsp_preprocess *preprocess_from(jlong handle) {
  return (xdsp_preprocess *)(intptr_t)handle;
}

static xdsp_echo *echo_from(jlong handle) { return (xdsp_echo *)(intptr_t)handle; }

static xdsp_decorr *decorr_from(jlong handle) { return (xdsp_decorr *)(intptr_t)handle; }

static xdsp_jitter *jitter_from(jlong handle) { return (xdsp_jitter *)(intptr_t)handle; }

static xdsp_buffer *buffer_from(jlong handle) { return (xdsp_buffer *)(intptr_t)handle; }

static jshortArray xdsp_new_shorts(JNIEnv *env, const int16_t *data, int count) {
  if (!data || count <= 0) return NULL;
  jshortArray result = (*env)->NewShortArray(env, count);
  if (!result) return NULL;
  (*env)->SetShortArrayRegion(env, result, 0, count, (const jshort *)data);
  return result;
}

static jintArray xdsp_new_ints(JNIEnv *env, const int32_t *data, int count) {
  if (!data || count <= 0) return NULL;
  jintArray result = (*env)->NewIntArray(env, count);
  if (!result) return NULL;
  (*env)->SetIntArrayRegion(env, result, 0, count, (const jint *)data);
  return result;
}

static jbyteArray xdsp_new_bytes(JNIEnv *env, const uint8_t *data, int count) {
  if (!data || count <= 0) return NULL;
  jbyteArray result = (*env)->NewByteArray(env, count);
  if (!result) return NULL;
  (*env)->SetByteArrayRegion(env, result, 0, count, (const jbyte *)data);
  return result;
}

JNIEXPORT jstring JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeVersionString(JNIEnv *env, jobject self) {
  (void)self;
  const char *version = xdsp_version_string();
  return (*env)->NewStringUTF(env, version ? version : "");
}

/* ------------------------------------------------------------------------- */
/* Resampler                                                                  */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerCreate(JNIEnv *env, jobject self,
                                                       jint channels, jint in_rate,
                                                       jint out_rate, jint quality) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xdsp_resampler_create((uint32_t)channels, (uint32_t)in_rate,
                                                (uint32_t)out_rate, quality);
}

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerCreateFrac(JNIEnv *env, jobject self,
                                                           jint channels, jint ratio_num,
                                                           jint ratio_den, jint in_rate,
                                                           jint out_rate, jint quality) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xdsp_resampler_create_frac(
      (uint32_t)channels, (uint32_t)ratio_num, (uint32_t)ratio_den, (uint32_t)in_rate,
      (uint32_t)out_rate, quality);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerError(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xdsp_resampler_error(resampler_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_resampler_free(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerChannels(JNIEnv *env, jobject self,
                                                         jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_resampler_channels(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerSetRate(JNIEnv *env, jobject self, jlong handle,
                                                        jint in_rate, jint out_rate) {
  (void)env;
  (void)self;
  return xdsp_resampler_set_rate(resampler_from(handle), (uint32_t)in_rate, (uint32_t)out_rate);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerSetRateFrac(JNIEnv *env, jobject self,
                                                            jlong handle, jint ratio_num,
                                                            jint ratio_den, jint in_rate,
                                                            jint out_rate) {
  (void)env;
  (void)self;
  return xdsp_resampler_set_rate_frac(resampler_from(handle), (uint32_t)ratio_num,
                                      (uint32_t)ratio_den, (uint32_t)in_rate,
                                      (uint32_t)out_rate);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerInputRate(JNIEnv *env, jobject self,
                                                          jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_resampler_input_rate(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerOutputRate(JNIEnv *env, jobject self,
                                                           jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_resampler_output_rate(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerRatioNum(JNIEnv *env, jobject self,
                                                         jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_resampler_ratio_num(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerRatioDen(JNIEnv *env, jobject self,
                                                         jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_resampler_ratio_den(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerSetQuality(JNIEnv *env, jobject self,
                                                           jlong handle, jint quality) {
  (void)env;
  (void)self;
  return xdsp_resampler_set_quality(resampler_from(handle), quality);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerQuality(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xdsp_resampler_quality(resampler_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerSetInputStride(JNIEnv *env, jobject self,
                                                               jlong handle, jint stride) {
  (void)env;
  (void)self;
  xdsp_resampler_set_input_stride(resampler_from(handle), (uint32_t)stride);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerInputStride(JNIEnv *env, jobject self,
                                                            jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_resampler_input_stride(resampler_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerSetOutputStride(JNIEnv *env, jobject self,
                                                                jlong handle, jint stride) {
  (void)env;
  (void)self;
  xdsp_resampler_set_output_stride(resampler_from(handle), (uint32_t)stride);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerOutputStride(JNIEnv *env, jobject self,
                                                             jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_resampler_output_stride(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerInputLatency(JNIEnv *env, jobject self,
                                                             jlong handle) {
  (void)env;
  (void)self;
  return xdsp_resampler_input_latency(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerOutputLatency(JNIEnv *env, jobject self,
                                                              jlong handle) {
  (void)env;
  (void)self;
  return xdsp_resampler_output_latency(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerSkipZeros(JNIEnv *env, jobject self,
                                                          jlong handle) {
  (void)env;
  (void)self;
  return xdsp_resampler_skip_zeros(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerResetMem(JNIEnv *env, jobject self,
                                                         jlong handle) {
  (void)env;
  (void)self;
  return xdsp_resampler_reset_mem(resampler_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerProcess(JNIEnv *env, jobject self, jlong handle,
                                                        jshortArray samples, jint offset,
                                                        jint count) {
  (void)self;
  xdsp_resampler *resampler = resampler_from(handle);
  int channels = (int)xdsp_resampler_channels(resampler);
  if (!samples || offset < 0 || count < 0 || channels <= 0) return XDSP_INVALID;
  jsize length = (*env)->GetArrayLength(env, samples);
  if ((int64_t)offset + (int64_t)count * channels > length) return XDSP_INVALID;
  if (count == 0) return xdsp_resampler_process_interleaved(resampler, NULL, 0);
  jshort *elements = (*env)->GetShortArrayElements(env, samples, NULL);
  if (!elements) return XDSP_INVALID;
  int result = xdsp_resampler_process_interleaved(
      resampler, (const int16_t *)(elements + offset), count);
  (*env)->ReleaseShortArrayElements(env, samples, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jshortArray JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerOutput(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  xdsp_resampler *resampler = resampler_from(handle);
  int out_len = 0;
  const int16_t *output = xdsp_resampler_output(resampler, &out_len);
  int channels = (int)xdsp_resampler_channels(resampler);
  return xdsp_new_shorts(env, output, out_len * channels);
}

JNIEXPORT jstring JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeResamplerStrerror(JNIEnv *env, jobject self, jint error) {
  (void)self;
  const char *message = xdsp_resampler_strerror(error);
  return (*env)->NewStringUTF(env, message ? message : "");
}

/* ------------------------------------------------------------------------- */
/* Preprocessor                                                               */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessCreate(JNIEnv *env, jobject self,
                                                        jint frame_size, jint sampling_rate) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xdsp_preprocess_create(frame_size, sampling_rate);
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_preprocess_free(preprocess_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessFrameSize(JNIEnv *env, jobject self,
                                                           jlong handle) {
  (void)env;
  (void)self;
  return xdsp_preprocess_frame_size(preprocess_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessRun(JNIEnv *env, jobject self, jlong handle,
                                                     jshortArray samples, jint offset,
                                                     jint count) {
  (void)self;
  if (!samples || offset < 0 || count <= 0) return XDSP_INVALID;
  jsize length = (*env)->GetArrayLength(env, samples);
  if (offset > length - count) return XDSP_INVALID;
  jshort *elements = (*env)->GetShortArrayElements(env, samples, NULL);
  if (!elements) return XDSP_INVALID;
  int result = xdsp_preprocess_run(preprocess_from(handle), (int16_t *)(elements + offset),
                                   (int)count);
  (*env)->ReleaseShortArrayElements(env, samples, elements, 0);
  return result;
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessEstimateUpdate(JNIEnv *env, jobject self,
                                                                jlong handle,
                                                                jshortArray samples,
                                                                jint offset, jint count) {
  (void)self;
  if (!samples || offset < 0 || count <= 0) return;
  jsize length = (*env)->GetArrayLength(env, samples);
  if (offset > length - count) return;
  jshort *elements = (*env)->GetShortArrayElements(env, samples, NULL);
  if (!elements) return;
  xdsp_preprocess_estimate_update(preprocess_from(handle), (int16_t *)(elements + offset),
                                  (int)count);
  (*env)->ReleaseShortArrayElements(env, samples, elements, JNI_ABORT);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessCtlInt(JNIEnv *env, jobject self, jlong handle,
                                                       jint request, jint value) {
  (void)env;
  (void)self;
  return xdsp_preprocess_ctl_int(preprocess_from(handle), request, value);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessGetInt(JNIEnv *env, jobject self, jlong handle,
                                                        jint request) {
  (void)env;
  (void)self;
  return xdsp_preprocess_get_int(preprocess_from(handle), request);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessCtlFloat(JNIEnv *env, jobject self,
                                                          jlong handle, jint request,
                                                          jfloat value) {
  (void)env;
  (void)self;
  return xdsp_preprocess_ctl_float(preprocess_from(handle), request, (float)value);
}

JNIEXPORT jfloat JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessGetFloat(JNIEnv *env, jobject self,
                                                          jlong handle, jint request) {
  (void)env;
  (void)self;
  return (jfloat)xdsp_preprocess_get_float(preprocess_from(handle), request);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessSetEcho(JNIEnv *env, jobject self,
                                                         jlong handle, jlong echo) {
  (void)env;
  (void)self;
  return xdsp_preprocess_set_echo(preprocess_from(handle), echo_from(echo));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessPsdSize(JNIEnv *env, jobject self,
                                                         jlong handle) {
  (void)env;
  (void)self;
  return xdsp_preprocess_psd_size(preprocess_from(handle));
}

JNIEXPORT jintArray JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessPsd(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  int size = 0;
  const int32_t *psd = xdsp_preprocess_psd(preprocess_from(handle), &size);
  return xdsp_new_ints(env, psd, size);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessNoisePsdSize(JNIEnv *env, jobject self,
                                                              jlong handle) {
  (void)env;
  (void)self;
  return xdsp_preprocess_noise_psd_size(preprocess_from(handle));
}

JNIEXPORT jintArray JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativePreprocessNoisePsd(JNIEnv *env, jobject self,
                                                          jlong handle) {
  (void)self;
  int size = 0;
  const int32_t *psd = xdsp_preprocess_noise_psd(preprocess_from(handle), &size);
  return xdsp_new_ints(env, psd, size);
}

/* ------------------------------------------------------------------------- */
/* Echo canceller                                                             */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoCreate(JNIEnv *env, jobject self, jint frame_size,
                                                  jint filter_length, jint nb_mic,
                                                  jint nb_speakers) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xdsp_echo_create(frame_size, filter_length, nb_mic, nb_speakers);
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_echo_free(echo_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoReset(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_echo_reset(echo_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoFrameSize(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xdsp_echo_frame_size(echo_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoMicrophones(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xdsp_echo_microphones(echo_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoSpeakers(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xdsp_echo_speakers(echo_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoCancellation(JNIEnv *env, jobject self, jlong handle,
                                                        jshortArray rec, jint rec_offset,
                                                        jshortArray play, jint play_offset) {
  (void)self;
  if (!rec || !play || rec_offset < 0 || play_offset < 0) return XDSP_INVALID;
  jsize rec_length = (*env)->GetArrayLength(env, rec);
  jsize play_length = (*env)->GetArrayLength(env, play);
  if (rec_offset > rec_length || play_offset > play_length) return XDSP_INVALID;
  jshort *rec_elements = (*env)->GetShortArrayElements(env, rec, NULL);
  jshort *play_elements = (*env)->GetShortArrayElements(env, play, NULL);
  if (!rec_elements || !play_elements) {
    if (rec_elements) (*env)->ReleaseShortArrayElements(env, rec, rec_elements, JNI_ABORT);
    if (play_elements) (*env)->ReleaseShortArrayElements(env, play, play_elements, JNI_ABORT);
    return XDSP_INVALID;
  }
  int result = xdsp_echo_cancellation(echo_from(handle), (const int16_t *)(rec_elements + rec_offset),
                                      (int)(rec_length - rec_offset),
                                      (const int16_t *)(play_elements + play_offset),
                                      (int)(play_length - play_offset));
  (*env)->ReleaseShortArrayElements(env, play, play_elements, JNI_ABORT);
  (*env)->ReleaseShortArrayElements(env, rec, rec_elements, JNI_ABORT);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoCapture(JNIEnv *env, jobject self, jlong handle,
                                                   jshortArray rec, jint offset) {
  (void)self;
  if (!rec || offset < 0) return XDSP_INVALID;
  jsize length = (*env)->GetArrayLength(env, rec);
  if (offset > length) return XDSP_INVALID;
  jshort *elements = (*env)->GetShortArrayElements(env, rec, NULL);
  if (!elements) return XDSP_INVALID;
  int result = xdsp_echo_capture(echo_from(handle), (const int16_t *)(elements + offset),
                                 (int)(length - offset));
  (*env)->ReleaseShortArrayElements(env, rec, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoPlayback(JNIEnv *env, jobject self, jlong handle,
                                                    jshortArray play, jint offset) {
  (void)self;
  if (!play || offset < 0) return XDSP_INVALID;
  jsize length = (*env)->GetArrayLength(env, play);
  if (offset > length) return XDSP_INVALID;
  jshort *elements = (*env)->GetShortArrayElements(env, play, NULL);
  if (!elements) return XDSP_INVALID;
  int result = xdsp_echo_playback(echo_from(handle), (const int16_t *)(elements + offset),
                                  (int)(length - offset));
  (*env)->ReleaseShortArrayElements(env, play, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jshortArray JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoOutput(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  int count = 0;
  const int16_t *output = xdsp_echo_output(echo_from(handle), &count);
  return xdsp_new_shorts(env, output, count);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoCtlInt(JNIEnv *env, jobject self, jlong handle,
                                                  jint request, jint value) {
  (void)env;
  (void)self;
  return xdsp_echo_ctl_int(echo_from(handle), request, value);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoGetInt(JNIEnv *env, jobject self, jlong handle,
                                                  jint request) {
  (void)env;
  (void)self;
  return xdsp_echo_get_int(echo_from(handle), request);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoImpulseResponseSize(JNIEnv *env, jobject self,
                                                               jlong handle) {
  (void)env;
  (void)self;
  return xdsp_echo_impulse_response_size(echo_from(handle));
}

JNIEXPORT jintArray JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeEchoImpulseResponse(JNIEnv *env, jobject self,
                                                           jlong handle) {
  (void)self;
  int size = 0;
  const int32_t *impulse = xdsp_echo_impulse_response(echo_from(handle), &size);
  return xdsp_new_ints(env, impulse, size);
}

/* ------------------------------------------------------------------------- */
/* Decorrelator                                                               */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeDecorrCreate(JNIEnv *env, jobject self, jint rate,
                                                    jint channels, jint frame_size) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xdsp_decorr_create(rate, channels, frame_size);
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeDecorrFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_decorr_free(decorr_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeDecorrChannels(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xdsp_decorr_channels(decorr_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeDecorrFrameSize(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xdsp_decorr_frame_size(decorr_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeDecorrProcess(JNIEnv *env, jobject self, jlong handle,
                                                     jshortArray input, jint offset,
                                                     jint count, jint strength) {
  (void)self;
  if (!input || offset < 0 || count <= 0) return XDSP_INVALID;
  jsize length = (*env)->GetArrayLength(env, input);
  if (offset > length - count) return XDSP_INVALID;
  jshort *elements = (*env)->GetShortArrayElements(env, input, NULL);
  if (!elements) return XDSP_INVALID;
  int result = xdsp_decorr_process(decorr_from(handle),
                                   (const int16_t *)(elements + offset), (int)count, strength);
  (*env)->ReleaseShortArrayElements(env, input, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jshortArray JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeDecorrOutput(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  int count = 0;
  const int16_t *output = xdsp_decorr_output(decorr_from(handle), &count);
  return xdsp_new_shorts(env, output, count);
}

/* ------------------------------------------------------------------------- */
/* Jitter buffer                                                              */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterCreate(JNIEnv *env, jobject self, jint step_size) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xdsp_jitter_create(step_size);
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_jitter_free(jitter_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterReset(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_jitter_reset(jitter_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterPut(JNIEnv *env, jobject self, jlong handle,
                                                 jbyteArray data, jint timestamp, jint span,
                                                 jint sequence, jint user_data) {
  (void)self;
  if (!data) return XDSP_INVALID;
  jsize length = (*env)->GetArrayLength(env, data);
  if (length == 0) {
    return xdsp_jitter_put(jitter_from(handle), NULL, 0, (uint32_t)timestamp, (uint32_t)span,
                           (uint16_t)sequence, (uint32_t)user_data);
  }
  jbyte *elements = (*env)->GetByteArrayElements(env, data, NULL);
  if (!elements) return XDSP_INVALID;
  int result = xdsp_jitter_put(jitter_from(handle), (const uint8_t *)elements, (int)length,
                               (uint32_t)timestamp, (uint32_t)span, (uint16_t)sequence,
                               (uint32_t)user_data);
  (*env)->ReleaseByteArrayElements(env, data, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterGet(JNIEnv *env, jobject self, jlong handle,
                                                 jint desired_span) {
  (void)env;
  (void)self;
  return xdsp_jitter_get(jitter_from(handle), desired_span);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterGetAnother(JNIEnv *env, jobject self,
                                                        jlong handle) {
  (void)env;
  (void)self;
  return xdsp_jitter_get_another(jitter_from(handle));
}

JNIEXPORT jbyteArray JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterPacket(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  int length = 0;
  const uint8_t *data = xdsp_jitter_packet(jitter_from(handle), &length);
  return xdsp_new_bytes(env, data, length);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterTimestamp(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_jitter_timestamp(jitter_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterSpan(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_jitter_span(jitter_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterSequence(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_jitter_sequence(jitter_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterUserData(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return (jint)xdsp_jitter_user_data(jitter_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterPointerTimestamp(JNIEnv *env, jobject self,
                                                              jlong handle) {
  (void)env;
  (void)self;
  return xdsp_jitter_pointer_timestamp(jitter_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterTick(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_jitter_tick(jitter_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterRemainingSpan(JNIEnv *env, jobject self,
                                                           jlong handle, jint remaining) {
  (void)env;
  (void)self;
  xdsp_jitter_remaining_span(jitter_from(handle), (uint32_t)remaining);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterCtlInt(JNIEnv *env, jobject self, jlong handle,
                                                    jint request, jint value) {
  (void)env;
  (void)self;
  return xdsp_jitter_ctl_int(jitter_from(handle), request, value);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeJitterGetInt(JNIEnv *env, jobject self, jlong handle,
                                                    jint request) {
  (void)env;
  (void)self;
  return xdsp_jitter_get_int(jitter_from(handle), request);
}

/* ------------------------------------------------------------------------- */
/* Ring buffer                                                                */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeRingBufferCreate(JNIEnv *env, jobject self, jint size) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xdsp_buffer_create(size);
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeRingBufferFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xdsp_buffer_free(buffer_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeRingBufferWrite(JNIEnv *env, jobject self, jlong handle,
                                                       jbyteArray data, jint offset,
                                                       jint count) {
  (void)self;
  if (!data || offset < 0 || count < 0) return XDSP_INVALID;
  jsize length = (*env)->GetArrayLength(env, data);
  if (offset > length - count) return XDSP_INVALID;
  if (count == 0) return xdsp_buffer_write(buffer_from(handle), NULL, 0);
  jbyte *elements = (*env)->GetByteArrayElements(env, data, NULL);
  if (!elements) return XDSP_INVALID;
  int result = xdsp_buffer_write(buffer_from(handle), (const uint8_t *)(elements + offset),
                                 (int)count);
  (*env)->ReleaseByteArrayElements(env, data, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeRingBufferWriteZeros(JNIEnv *env, jobject self,
                                                            jlong handle, jint len) {
  (void)env;
  (void)self;
  return xdsp_buffer_write_zeros(buffer_from(handle), len);
}

JNIEXPORT jbyteArray JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeRingBufferRead(JNIEnv *env, jobject self, jlong handle,
                                                      jint len) {
  (void)self;
  int result = xdsp_buffer_read(buffer_from(handle), len);
  if (result < 0) return NULL;
  int size = 0;
  const uint8_t *data = xdsp_buffer_data(buffer_from(handle), &size);
  return xdsp_new_bytes(env, data, size);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeRingBufferAvailable(JNIEnv *env, jobject self,
                                                           jlong handle) {
  (void)env;
  (void)self;
  return xdsp_buffer_available(buffer_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeexDsp_nativeRingBufferResize(JNIEnv *env, jobject self, jlong handle,
                                                        jint len) {
  (void)env;
  (void)self;
  return xdsp_buffer_resize(buffer_from(handle), len);
}
