/*
 * JNI bridge between cn.enaium.speex.JniSpeex and the C shim.
 *
 * The declarations on the Kotlin side live in
 * speex-kmp/src/jvmMain/kotlin/cn/enaium/speex/SpeexNative.jvm.kt.
 *
 * Handles are the raw xk_* pointers stored in a jlong. Byte and short arrays
 * are copied into/out of the shim; the shim keeps only a pointer for the
 * duration of a call.
 */
#include <jni.h>
#include <stdint.h>

#include "speex_kmp.h"

static xk_bits *xk_bits_from(jlong handle) { return (xk_bits *)(intptr_t)handle; }

static xk_encoder *xk_encoder_from(jlong handle) { return (xk_encoder *)(intptr_t)handle; }

static xk_decoder *xk_decoder_from(jlong handle) { return (xk_decoder *)(intptr_t)handle; }

static xk_stereo *xk_stereo_from(jlong handle) { return (xk_stereo *)(intptr_t)handle; }

static xk_header *xk_header_from(jlong handle) { return (xk_header *)(intptr_t)handle; }

static jstring xk_new_string(JNIEnv *env, const char *text) {
  return (*env)->NewStringUTF(env, text ? text : "");
}

static jbyteArray xk_new_bytes(JNIEnv *env, const uint8_t *data, int size) {
  if (!data || size <= 0) return NULL;
  jbyteArray result = (*env)->NewByteArray(env, size);
  if (!result) return NULL;
  (*env)->SetByteArrayRegion(env, result, 0, size, (const jbyte *)data);
  return result;
}

JNIEXPORT jstring JNICALL
Java_cn_enaium_speex_JniSpeex_nativeVersionString(JNIEnv *env, jobject self) {
  (void)self;
  return xk_new_string(env, xk_version_string());
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeVersionMajor(JNIEnv *env, jobject self) {
  (void)env;
  (void)self;
  return xk_version_major();
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeVersionMinor(JNIEnv *env, jobject self) {
  (void)env;
  (void)self;
  return xk_version_minor();
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeVersionMicro(JNIEnv *env, jobject self) {
  (void)env;
  (void)self;
  return xk_version_micro();
}

JNIEXPORT jstring JNICALL
Java_cn_enaium_speex_JniSpeex_nativeVersionExtra(JNIEnv *env, jobject self) {
  (void)self;
  return xk_new_string(env, xk_version_extra());
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeModeFrameSize(JNIEnv *env, jobject self, jint mode) {
  (void)env;
  (void)self;
  return xk_mode_frame_size(mode);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeModeSubmodeBits(JNIEnv *env, jobject self, jint mode,
                                                    jint submode) {
  (void)env;
  (void)self;
  return xk_mode_submode_bits(mode, submode);
}

JNIEXPORT jstring JNICALL
Java_cn_enaium_speex_JniSpeex_nativeModeName(JNIEnv *env, jobject self, jint mode) {
  (void)self;
  return xk_new_string(env, xk_mode_name(mode));
}

/* ------------------------------------------------------------------------- */
/* Bit-stream                                                                 */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsCreate(JNIEnv *env, jobject self) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xk_bits_create();
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xk_bits_free(xk_bits_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsReset(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xk_bits_reset(xk_bits_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsRewind(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xk_bits_rewind(xk_bits_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsReadFrom(JNIEnv *env, jobject self, jlong handle,
                                                 jbyteArray data) {
  (void)self;
  jsize size = data ? (*env)->GetArrayLength(env, data) : 0;
  jbyte *elements = data ? (*env)->GetByteArrayElements(env, data, NULL) : NULL;
  int result = xk_bits_read_from(xk_bits_from(handle), (const uint8_t *)elements, (int)size);
  if (elements) (*env)->ReleaseByteArrayElements(env, data, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsReadWholeBytes(JNIEnv *env, jobject self, jlong handle,
                                                       jbyteArray data) {
  (void)self;
  jsize size = data ? (*env)->GetArrayLength(env, data) : 0;
  jbyte *elements = data ? (*env)->GetByteArrayElements(env, data, NULL) : NULL;
  int result = xk_bits_read_whole_bytes(xk_bits_from(handle), (const uint8_t *)elements,
                                        (int)size);
  if (elements) (*env)->ReleaseByteArrayElements(env, data, elements, JNI_ABORT);
  return result;
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsPack(JNIEnv *env, jobject self, jlong handle,
                                             jint data, jint nbits) {
  (void)env;
  (void)self;
  xk_bits_pack(xk_bits_from(handle), data, nbits);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsUnpackSigned(JNIEnv *env, jobject self, jlong handle,
                                                     jint nbits) {
  (void)env;
  (void)self;
  return xk_bits_unpack_signed(xk_bits_from(handle), nbits);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsUnpackUnsigned(JNIEnv *env, jobject self, jlong handle,
                                                       jint nbits) {
  (void)env;
  (void)self;
  return (jint)xk_bits_unpack_unsigned(xk_bits_from(handle), nbits);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsPeek(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xk_bits_peek(xk_bits_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsPeekUnsigned(JNIEnv *env, jobject self, jlong handle,
                                                     jint nbits) {
  (void)env;
  (void)self;
  return (jint)xk_bits_peek_unsigned(xk_bits_from(handle), nbits);
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsAdvance(JNIEnv *env, jobject self, jlong handle,
                                                jint nbits) {
  (void)env;
  (void)self;
  xk_bits_advance(xk_bits_from(handle), nbits);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsRemaining(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xk_bits_remaining(xk_bits_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsNbytes(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xk_bits_nbytes(xk_bits_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsInsertTerminator(JNIEnv *env, jobject self,
                                                         jlong handle) {
  (void)env;
  (void)self;
  xk_bits_insert_terminator(xk_bits_from(handle));
}

JNIEXPORT jbyteArray JNICALL
Java_cn_enaium_speex_JniSpeex_nativeBitsWrite(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  int size = xk_bits_nbytes(xk_bits_from(handle));
  if (size <= 0) return (*env)->NewByteArray(env, 0);
  jbyteArray result = (*env)->NewByteArray(env, size);
  if (!result) return NULL;
  jbyte *elements = (*env)->GetByteArrayElements(env, result, NULL);
  if (!elements) return NULL;
  int written = xk_bits_write(xk_bits_from(handle), (uint8_t *)elements, size);
  (*env)->ReleaseByteArrayElements(env, result, elements, 0);
  if (written <= 0) return (*env)->NewByteArray(env, 0);
  return result;
}

/* ------------------------------------------------------------------------- */
/* Encoder                                                                    */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderCreate(JNIEnv *env, jobject self, jint mode) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xk_encoder_create(mode);
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xk_encoder_free(xk_encoder_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderFrameSize(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xk_encoder_frame_size(xk_encoder_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderEncode(JNIEnv *env, jobject self, jlong handle,
                                                  jshortArray samples, jint offset,
                                                  jint count) {
  (void)self;
  if (!samples || offset < 0) return XK_INVALID;
  jsize length = (*env)->GetArrayLength(env, samples);
  if (count <= 0 || offset > length - count) return XK_INVALID;
  jshort *elements = (*env)->GetShortArrayElements(env, samples, NULL);
  if (!elements) return XK_INVALID;
  int result = xk_encoder_encode(xk_encoder_from(handle), (int16_t *)(elements + offset),
                                 (int)count);
  (*env)->ReleaseShortArrayElements(env, samples, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderEncodeStereo(JNIEnv *env, jobject self, jlong handle,
                                                        jshortArray samples, jint offset,
                                                        jint count) {
  (void)self;
  if (!samples || offset < 0) return XK_INVALID;
  jsize length = (*env)->GetArrayLength(env, samples);
  if (count <= 0 || offset > length - count) return XK_INVALID;
  jshort *elements = (*env)->GetShortArrayElements(env, samples, NULL);
  if (!elements) return XK_INVALID;
  int result = xk_encoder_encode_stereo(xk_encoder_from(handle),
                                        (int16_t *)(elements + offset), (int)count);
  // The first half of the range is folded to mono in place.
  (*env)->ReleaseShortArrayElements(env, samples, elements, 0);
  return result;
}

JNIEXPORT jbyteArray JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderPacket(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  int size = 0;
  const uint8_t *packet = xk_encoder_packet(xk_encoder_from(handle), &size);
  return xk_new_bytes(env, packet, size);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderCtl(JNIEnv *env, jobject self, jlong handle,
                                               jint request, jint value) {
  (void)env;
  (void)self;
  return xk_encoder_ctl(xk_encoder_from(handle), request, value);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderGet(JNIEnv *env, jobject self, jlong handle,
                                               jint request) {
  (void)env;
  (void)self;
  return xk_encoder_get(xk_encoder_from(handle), request);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderSetVbrQuality(JNIEnv *env, jobject self, jlong handle,
                                                         jfloat value) {
  (void)env;
  (void)self;
  return xk_encoder_set_vbr_quality(xk_encoder_from(handle), (float)value);
}

JNIEXPORT jfloat JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderGetVbrQuality(JNIEnv *env, jobject self,
                                                         jlong handle) {
  (void)env;
  (void)self;
  return (jfloat)xk_encoder_get_vbr_quality(xk_encoder_from(handle));
}

JNIEXPORT jfloat JNICALL
Java_cn_enaium_speex_JniSpeex_nativeEncoderGetRelativeQuality(JNIEnv *env, jobject self,
                                                              jlong handle) {
  (void)env;
  (void)self;
  return (jfloat)xk_encoder_get_relative_quality(xk_encoder_from(handle));
}

/* ------------------------------------------------------------------------- */
/* Decoder                                                                    */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeex_nativeDecoderCreate(JNIEnv *env, jobject self, jint mode) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xk_decoder_create(mode);
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeDecoderFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xk_decoder_free(xk_decoder_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeDecoderFrameSize(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  return xk_decoder_frame_size(xk_decoder_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeDecoderDecode(JNIEnv *env, jobject self, jlong handle,
                                                  jbyteArray packet) {
  (void)self;
  jsize size = packet ? (*env)->GetArrayLength(env, packet) : 0;
  jbyte *elements = packet ? (*env)->GetByteArrayElements(env, packet, NULL) : NULL;
  int result = xk_decoder_decode(xk_decoder_from(handle), (const uint8_t *)elements, (int)size);
  if (elements) (*env)->ReleaseByteArrayElements(env, packet, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeDecoderDecodeStereo(JNIEnv *env, jobject self, jlong handle,
                                                        jbyteArray packet, jlong stereo) {
  (void)self;
  jsize size = packet ? (*env)->GetArrayLength(env, packet) : 0;
  jbyte *elements = packet ? (*env)->GetByteArrayElements(env, packet, NULL) : NULL;
  int result = xk_decoder_decode_stereo(xk_decoder_from(handle), (const uint8_t *)elements,
                                        (int)size, xk_stereo_from(stereo));
  if (elements) (*env)->ReleaseByteArrayElements(env, packet, elements, JNI_ABORT);
  return result;
}

JNIEXPORT jshortArray JNICALL
Java_cn_enaium_speex_JniSpeex_nativeDecoderSamples(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  int count = 0;
  const int16_t *samples = xk_decoder_samples(xk_decoder_from(handle), &count);
  if (!samples || count <= 0) return NULL;
  jshortArray result = (*env)->NewShortArray(env, count);
  if (!result) return NULL;
  (*env)->SetShortArrayRegion(env, result, 0, count, (const jshort *)samples);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeDecoderCtl(JNIEnv *env, jobject self, jlong handle,
                                               jint request, jint value) {
  (void)env;
  (void)self;
  return xk_decoder_ctl(xk_decoder_from(handle), request, value);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeDecoderGet(JNIEnv *env, jobject self, jlong handle,
                                               jint request) {
  (void)env;
  (void)self;
  return xk_decoder_get(xk_decoder_from(handle), request);
}

/* ------------------------------------------------------------------------- */
/* Stereo                                                                     */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeex_nativeStereoCreate(JNIEnv *env, jobject self) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xk_stereo_create();
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeStereoFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xk_stereo_free(xk_stereo_from(handle));
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeStereoReset(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xk_stereo_reset(xk_stereo_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeStereoEncode(JNIEnv *env, jobject self, jlong handle,
                                                 jshortArray samples, jint offset, jint count,
                                                 jlong bits) {
  (void)self;
  if (!samples || offset < 0) return XK_INVALID;
  jsize length = (*env)->GetArrayLength(env, samples);
  if (count <= 0 || offset > length - count) return XK_INVALID;
  jshort *elements = (*env)->GetShortArrayElements(env, samples, NULL);
  if (!elements) return XK_INVALID;
  int result = xk_stereo_encode(xk_stereo_from(handle), (int16_t *)(elements + offset),
                                (int)count, xk_bits_from(bits));
  (*env)->ReleaseShortArrayElements(env, samples, elements, 0);
  return result;
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeStereoDecode(JNIEnv *env, jobject self, jlong handle,
                                                 jshortArray samples, jint count) {
  (void)self;
  if (!samples || count <= 0) return XK_INVALID;
  jsize length = (*env)->GetArrayLength(env, samples);
  if (count > length) return XK_INVALID;
  jshort *elements = (*env)->GetShortArrayElements(env, samples, NULL);
  if (!elements) return XK_INVALID;
  int result = xk_stereo_decode(xk_stereo_from(handle), (int16_t *)elements, (int)count);
  (*env)->ReleaseShortArrayElements(env, samples, elements, 0);
  return result;
}

/* ------------------------------------------------------------------------- */
/* Header                                                                     */
/* ------------------------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeex_nativeHeaderCreate(JNIEnv *env, jobject self, jint rate,
                                                 jint channels, jint mode) {
  (void)env;
  (void)self;
  return (jlong)(intptr_t)xk_header_create(rate, channels, mode);
}

JNIEXPORT jlong JNICALL
Java_cn_enaium_speex_JniSpeex_nativeHeaderParse(JNIEnv *env, jobject self, jbyteArray packet) {
  (void)self;
  if (!packet) return 0;
  jsize size = (*env)->GetArrayLength(env, packet);
  jbyte *elements = (*env)->GetByteArrayElements(env, packet, NULL);
  if (!elements) return 0;
  xk_header *header = xk_header_parse((const uint8_t *)elements, (int)size);
  (*env)->ReleaseByteArrayElements(env, packet, elements, JNI_ABORT);
  return (jlong)(intptr_t)header;
}

JNIEXPORT void JNICALL
Java_cn_enaium_speex_JniSpeex_nativeHeaderFree(JNIEnv *env, jobject self, jlong handle) {
  (void)env;
  (void)self;
  xk_header_free(xk_header_from(handle));
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeHeaderGetInt(JNIEnv *env, jobject self, jlong handle,
                                                 jint field) {
  (void)env;
  (void)self;
  return xk_header_get_int(xk_header_from(handle), field);
}

JNIEXPORT jint JNICALL
Java_cn_enaium_speex_JniSpeex_nativeHeaderSetInt(JNIEnv *env, jobject self, jlong handle,
                                                 jint field, jint value) {
  (void)env;
  (void)self;
  return xk_header_set_int(xk_header_from(handle), field, value);
}

JNIEXPORT jstring JNICALL
Java_cn_enaium_speex_JniSpeex_nativeHeaderString(JNIEnv *env, jobject self, jlong handle,
                                                 jint which) {
  (void)self;
  return xk_new_string(env, xk_header_string(xk_header_from(handle), which));
}

JNIEXPORT jbyteArray JNICALL
Java_cn_enaium_speex_JniSpeex_nativeHeaderPacket(JNIEnv *env, jobject self, jlong handle) {
  (void)self;
  int size = 0;
  const uint8_t *packet = xk_header_packet(xk_header_from(handle), &size);
  return xk_new_bytes(env, packet, size);
}
