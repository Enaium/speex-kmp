/*
 * C shim for the Speex Kotlin Multiplatform binding. See speex_kmp.h for the
 * contract shared by the JNI, Kotlin/Native and WebAssembly bridges.
 */
#include "speex_kmp.h"

#include <stdlib.h>
#include <string.h>

#include "speex/speex.h"
#include "speex/speex_bits.h"
#include "speex/speex_callbacks.h"
#include "speex/speex_header.h"
#include "speex/speex_stereo.h"

struct xk_bits {
  SpeexBits bits;
};

struct xk_encoder {
  void *state;
  const SpeexMode *mode;
  SpeexBits bits;
  int frame_size;
  uint8_t *packet;
  int packet_capacity;
  int packet_size;
};

struct xk_decoder {
  void *state;
  const SpeexMode *mode;
  SpeexBits bits;
  int frame_size;
  int16_t *samples;
  int sample_count;
};

struct xk_stereo {
  SpeexStereoState state;
};

struct xk_header {
  SpeexHeader header;
  /* The SpeexHeader char arrays are fixed-width and not NUL-terminated. */
  char speex_string[SPEEX_HEADER_STRING_LENGTH + 1];
  char speex_version[SPEEX_HEADER_VERSION_LENGTH + 1];
  uint8_t *packet;
  int packet_size;
};

/* ------------------------------------------------------------------------- */
/* Memory                                                                     */
/* ------------------------------------------------------------------------- */

void *xk_alloc(size_t size) { return malloc(size); }

void xk_free(void *pointer) { free(pointer); }

/* ------------------------------------------------------------------------- */
/* Library version                                                            */
/* ------------------------------------------------------------------------- */

const char *xk_version_string(void) {
  const char *version = NULL;
  if (speex_lib_ctl(SPEEX_LIB_GET_VERSION_STRING, &version) != 0 || !version) {
    return "";
  }
  return version;
}

int xk_version_major(void) {
  int value = 0;
  speex_lib_ctl(SPEEX_LIB_GET_MAJOR_VERSION, &value);
  return value;
}

int xk_version_minor(void) {
  int value = 0;
  speex_lib_ctl(SPEEX_LIB_GET_MINOR_VERSION, &value);
  return value;
}

int xk_version_micro(void) {
  int value = 0;
  speex_lib_ctl(SPEEX_LIB_GET_MICRO_VERSION, &value);
  return value;
}

const char *xk_version_extra(void) {
  const char *value = NULL;
  if (speex_lib_ctl(SPEEX_LIB_GET_EXTRA_VERSION, &value) != 0 || !value) {
    return "";
  }
  return value;
}

/* ------------------------------------------------------------------------- */
/* Modes                                                                      */
/* ------------------------------------------------------------------------- */

static const SpeexMode *xk_mode(int mode) { return speex_lib_get_mode(mode); }

int xk_mode_frame_size(int mode) {
  const SpeexMode *pointer = xk_mode(mode);
  int value = 0;
  if (!pointer || speex_mode_query(pointer, SPEEX_MODE_FRAME_SIZE, &value) != 0) {
    return 0;
  }
  return value;
}

int xk_mode_submode_bits(int mode, int submode) {
  const SpeexMode *pointer = xk_mode(mode);
  int value = submode;
  if (!pointer || speex_mode_query(pointer, SPEEX_SUBMODE_BITS_PER_FRAME, &value) != 0) {
    return -1;
  }
  return value;
}

const char *xk_mode_name(int mode) {
  const SpeexMode *pointer = xk_mode(mode);
  return pointer ? pointer->modeName : NULL;
}

/* ------------------------------------------------------------------------- */
/* Bit-stream                                                                 */
/* ------------------------------------------------------------------------- */

xk_bits *xk_bits_create(void) {
  xk_bits *bits = (xk_bits *)calloc(1, sizeof(xk_bits));
  if (!bits) return NULL;
  speex_bits_init(&bits->bits);
  if (!bits->bits.chars) {
    free(bits);
    return NULL;
  }
  return bits;
}

void xk_bits_free(xk_bits *bits) {
  if (!bits) return;
  speex_bits_destroy(&bits->bits);
  free(bits);
}

void xk_bits_reset(xk_bits *bits) {
  if (bits) speex_bits_reset(&bits->bits);
}

void xk_bits_rewind(xk_bits *bits) {
  if (bits) speex_bits_rewind(&bits->bits);
}

int xk_bits_read_from(xk_bits *bits, const uint8_t *data, int size) {
  if (!bits || size < 0 || (!data && size > 0)) return XK_INVALID;
  if (size == 0) {
    speex_bits_reset(&bits->bits);
    return 0;
  }
  speex_bits_read_from(&bits->bits, (const char *)data, size);
  return 0;
}

int xk_bits_read_whole_bytes(xk_bits *bits, const uint8_t *data, int size) {
  if (!bits || size < 0 || (!data && size > 0)) return XK_INVALID;
  if (size == 0) return 0;
  speex_bits_read_whole_bytes(&bits->bits, (const char *)data, size);
  return 0;
}

void xk_bits_pack(xk_bits *bits, int data, int nbits) {
  if (bits) speex_bits_pack(&bits->bits, data, nbits);
}

int xk_bits_unpack_signed(xk_bits *bits, int nbits) {
  return bits ? speex_bits_unpack_signed(&bits->bits, nbits) : 0;
}

unsigned int xk_bits_unpack_unsigned(xk_bits *bits, int nbits) {
  return bits ? speex_bits_unpack_unsigned(&bits->bits, nbits) : 0;
}

int xk_bits_peek(xk_bits *bits) { return bits ? speex_bits_peek(&bits->bits) : -1; }

unsigned int xk_bits_peek_unsigned(xk_bits *bits, int nbits) {
  return bits ? speex_bits_peek_unsigned(&bits->bits, nbits) : 0;
}

void xk_bits_advance(xk_bits *bits, int nbits) {
  if (bits) speex_bits_advance(&bits->bits, nbits);
}

int xk_bits_remaining(xk_bits *bits) {
  return bits ? speex_bits_remaining(&bits->bits) : -1;
}

int xk_bits_nbytes(xk_bits *bits) { return bits ? speex_bits_nbytes(&bits->bits) : 0; }

void xk_bits_insert_terminator(xk_bits *bits) {
  if (bits) speex_bits_insert_terminator(&bits->bits);
}

int xk_bits_write(xk_bits *bits, uint8_t *out, int max_size) {
  if (!bits || !out || max_size <= 0) return XK_ERROR;
  return speex_bits_write(&bits->bits, (char *)out, max_size);
}

const uint8_t *xk_bits_data(xk_bits *bits, int *size) {
  if (!bits) {
    if (size) *size = 0;
    return NULL;
  }
  if (size) *size = speex_bits_nbytes(&bits->bits);
  return (const uint8_t *)bits->bits.chars;
}

/* ------------------------------------------------------------------------- */
/* Encoder                                                                    */
/* ------------------------------------------------------------------------- */

xk_encoder *xk_encoder_create(int mode) {
  const SpeexMode *speex_mode = xk_mode(mode);
  if (!speex_mode) return NULL;

  xk_encoder *encoder = (xk_encoder *)calloc(1, sizeof(xk_encoder));
  if (!encoder) return NULL;

  encoder->mode = speex_mode;
  encoder->state = speex_encoder_init(speex_mode);
  if (!encoder->state) {
    free(encoder);
    return NULL;
  }

  speex_bits_init(&encoder->bits);
  if (!encoder->bits.chars) {
    speex_encoder_destroy(encoder->state);
    free(encoder);
    return NULL;
  }

  int frame_size = 0;
  if (speex_encoder_ctl(encoder->state, SPEEX_GET_FRAME_SIZE, &frame_size) != 0 ||
      frame_size <= 0) {
    speex_bits_destroy(&encoder->bits);
    speex_encoder_destroy(encoder->state);
    free(encoder);
    return NULL;
  }
  encoder->frame_size = frame_size;
  return encoder;
}

void xk_encoder_free(xk_encoder *encoder) {
  if (!encoder) return;
  speex_bits_destroy(&encoder->bits);
  if (encoder->state) speex_encoder_destroy(encoder->state);
  free(encoder->packet);
  free(encoder);
}

int xk_encoder_frame_size(const xk_encoder *encoder) {
  return encoder ? encoder->frame_size : 0;
}

/* Pads the stream, copies it into the packet buffer and updates packet_size. */
static int xk_encoder_finish(xk_encoder *encoder, int encoded) {
  if (encoded < 0) return XK_ERROR;
  if (encoded == 0) {
    encoder->packet_size = 0;
    return XK_NO_PACKET;
  }

  speex_bits_insert_terminator(&encoder->bits);
  int size = speex_bits_nbytes(&encoder->bits);
  if (size <= 0) {
    encoder->packet_size = 0;
    return XK_ERROR;
  }
  if (size > encoder->packet_capacity) {
    uint8_t *replacement = (uint8_t *)realloc(encoder->packet, (size_t)size);
    if (!replacement) {
      encoder->packet_size = 0;
      return XK_ERROR;
    }
    encoder->packet = replacement;
    encoder->packet_capacity = size;
  }

  int written = speex_bits_write(&encoder->bits, (char *)encoder->packet,
                                 encoder->packet_capacity);
  if (written <= 0) {
    encoder->packet_size = 0;
    return XK_ERROR;
  }
  encoder->packet_size = written;
  return XK_PACKET;
}

int xk_encoder_encode(xk_encoder *encoder, int16_t *samples, int count) {
  if (!encoder || !samples || count < encoder->frame_size) return XK_INVALID;
  speex_bits_reset(&encoder->bits);
  int encoded = speex_encode_int(encoder->state, (spx_int16_t *)samples, &encoder->bits);
  return xk_encoder_finish(encoder, encoded);
}

int xk_encoder_encode_stereo(xk_encoder *encoder, int16_t *samples, int count) {
  if (!encoder || !samples) return XK_INVALID;
  if (count < 2 * encoder->frame_size) return XK_INVALID;

  speex_bits_reset(&encoder->bits);
  speex_encode_stereo_int((spx_int16_t *)samples, encoder->frame_size, &encoder->bits);
  int encoded = speex_encode_int(encoder->state, (spx_int16_t *)samples, &encoder->bits);
  return xk_encoder_finish(encoder, encoded);
}

const uint8_t *xk_encoder_packet(xk_encoder *encoder, int *size) {
  if (!encoder) {
    if (size) *size = 0;
    return NULL;
  }
  if (size) *size = encoder->packet_size;
  return encoder->packet;
}

static int xk_request_is_int(int request) {
  /* Handler requests take a SpeexCallback pointer; the VBR quality requests
   * and SPEEX_GET_RELATIVE_QUALITY take a float pointer. Everything else the
   * binding exposes is an int request. */
  switch (request) {
    case SPEEX_SET_ENH:
    case SPEEX_GET_ENH:
    case SPEEX_GET_FRAME_SIZE:
    case SPEEX_SET_QUALITY:
    case SPEEX_SET_MODE:
    case SPEEX_GET_MODE:
    case SPEEX_SET_LOW_MODE:
    case SPEEX_GET_LOW_MODE:
    case SPEEX_SET_HIGH_MODE:
    case SPEEX_GET_HIGH_MODE:
    case SPEEX_SET_VBR:
    case SPEEX_GET_VBR:
    case SPEEX_SET_COMPLEXITY:
    case SPEEX_GET_COMPLEXITY:
    case SPEEX_SET_BITRATE:
    case SPEEX_GET_BITRATE:
    case SPEEX_SET_SAMPLING_RATE:
    case SPEEX_GET_SAMPLING_RATE:
    case SPEEX_RESET_STATE:
    case SPEEX_SET_VAD:
    case SPEEX_GET_VAD:
    case SPEEX_SET_ABR:
    case SPEEX_GET_ABR:
    case SPEEX_SET_DTX:
    case SPEEX_GET_DTX:
    case SPEEX_SET_SUBMODE_ENCODING:
    case SPEEX_GET_SUBMODE_ENCODING:
    case SPEEX_GET_LOOKAHEAD:
    case SPEEX_SET_PLC_TUNING:
    case SPEEX_GET_PLC_TUNING:
    case SPEEX_SET_VBR_MAX_BITRATE:
    case SPEEX_GET_VBR_MAX_BITRATE:
    case SPEEX_SET_HIGHPASS:
    case SPEEX_GET_HIGHPASS:
    case SPEEX_GET_ACTIVITY:
      return 1;
    default:
      return 0;
  }
}

int xk_encoder_ctl(xk_encoder *encoder, int request, int value) {
  if (!encoder || !xk_request_is_int(request)) return XK_INVALID;
  return speex_encoder_ctl(encoder->state, request, &value);
}

int xk_encoder_get(xk_encoder *encoder, int request) {
  int value = 0;
  if (!encoder || !xk_request_is_int(request)) return XK_INVALID;
  if (speex_encoder_ctl(encoder->state, request, &value) != 0) return XK_INVALID;
  return value;
}

int xk_encoder_set_vbr_quality(xk_encoder *encoder, float value) {
  if (!encoder) return XK_INVALID;
  return speex_encoder_ctl(encoder->state, SPEEX_SET_VBR_QUALITY, &value);
}

float xk_encoder_get_vbr_quality(xk_encoder *encoder) {
  float value = 0;
  if (!encoder) return 0;
  if (speex_encoder_ctl(encoder->state, SPEEX_GET_VBR_QUALITY, &value) != 0) return 0;
  return value;
}

float xk_encoder_get_relative_quality(xk_encoder *encoder) {
  float value = 0;
  if (!encoder) return 0;
  if (speex_encoder_ctl(encoder->state, SPEEX_GET_RELATIVE_QUALITY, &value) != 0) return 0;
  return value;
}

/* ------------------------------------------------------------------------- */
/* Decoder                                                                    */
/* ------------------------------------------------------------------------- */

xk_decoder *xk_decoder_create(int mode) {
  const SpeexMode *speex_mode = xk_mode(mode);
  if (!speex_mode) return NULL;

  xk_decoder *decoder = (xk_decoder *)calloc(1, sizeof(xk_decoder));
  if (!decoder) return NULL;

  decoder->mode = speex_mode;
  decoder->state = speex_decoder_init(speex_mode);
  if (!decoder->state) {
    free(decoder);
    return NULL;
  }

  speex_bits_init(&decoder->bits);
  if (!decoder->bits.chars) {
    speex_decoder_destroy(decoder->state);
    free(decoder);
    return NULL;
  }

  int frame_size = 0;
  if (speex_decoder_ctl(decoder->state, SPEEX_GET_FRAME_SIZE, &frame_size) != 0 ||
      frame_size <= 0) {
    speex_bits_destroy(&decoder->bits);
    speex_decoder_destroy(decoder->state);
    free(decoder);
    return NULL;
  }
  decoder->frame_size = frame_size;
  /* Twice the frame size so an in-place stereo expansion always fits. */
  decoder->samples = (int16_t *)calloc((size_t)frame_size * 2, sizeof(int16_t));
  if (!decoder->samples) {
    speex_bits_destroy(&decoder->bits);
    speex_decoder_destroy(decoder->state);
    free(decoder);
    return NULL;
  }
  decoder->sample_count = frame_size;
  return decoder;
}

void xk_decoder_free(xk_decoder *decoder) {
  if (!decoder) return;
  speex_bits_destroy(&decoder->bits);
  if (decoder->state) speex_decoder_destroy(decoder->state);
  free(decoder->samples);
  free(decoder);
}

int xk_decoder_frame_size(const xk_decoder *decoder) {
  return decoder ? decoder->frame_size : 0;
}

static int xk_decoder_decode_into(xk_decoder *decoder, const uint8_t *packet, int size) {
  if (!decoder) return XK_INVALID;
  if (size < 0 || (!packet && size > 0)) return XK_INVALID;

  SpeexBits *bits = NULL;
  if (packet && size > 0) {
    speex_bits_read_from(&decoder->bits, (const char *)packet, size);
    bits = &decoder->bits;
  }
  int result = speex_decode_int(decoder->state, bits, (spx_int16_t *)decoder->samples);
  decoder->sample_count = decoder->frame_size;
  return result;
}

/* Points the in-band stereo request at `stereo`, or clears it when NULL. The
 * decoder keeps the data pointer, so the Kotlin SpeexStereo must outlive every
 * decode call that can read a stereo request. */
static void xk_decoder_set_stereo_handler(xk_decoder *decoder, xk_stereo *stereo) {
  SpeexCallback callback;
  memset(&callback, 0, sizeof(callback));
  callback.callback_id = SPEEX_INBAND_STEREO;
  callback.func = stereo ? speex_std_stereo_request_handler : NULL;
  callback.data = stereo ? &stereo->state : NULL;
  speex_decoder_ctl(decoder->state, SPEEX_SET_HANDLER, &callback);
}

int xk_decoder_decode(xk_decoder *decoder, const uint8_t *packet, int size) {
  if (!decoder) return XK_INVALID;
  xk_decoder_set_stereo_handler(decoder, NULL);
  return xk_decoder_decode_into(decoder, packet, size);
}

int xk_decoder_decode_stereo(xk_decoder *decoder, const uint8_t *packet, int size,
                             xk_stereo *stereo) {
  if (!decoder || !stereo) return XK_INVALID;
  xk_decoder_set_stereo_handler(decoder, stereo);
  int result = xk_decoder_decode_into(decoder, packet, size);
  if (result < 0) {
    decoder->sample_count = 0;
    return result;
  }
  speex_decode_stereo_int((spx_int16_t *)decoder->samples, decoder->frame_size, &stereo->state);
  decoder->sample_count = decoder->frame_size * 2;
  return result;
}

const int16_t *xk_decoder_samples(xk_decoder *decoder, int *count) {
  if (!decoder) {
    if (count) *count = 0;
    return NULL;
  }
  if (count) *count = decoder->sample_count;
  return decoder->samples;
}

int xk_decoder_ctl(xk_decoder *decoder, int request, int value) {
  if (!decoder || !xk_request_is_int(request)) return XK_INVALID;
  return speex_decoder_ctl(decoder->state, request, &value);
}

int xk_decoder_get(xk_decoder *decoder, int request) {
  int value = 0;
  if (!decoder || !xk_request_is_int(request)) return XK_INVALID;
  if (speex_decoder_ctl(decoder->state, request, &value) != 0) return XK_INVALID;
  return value;
}

/* ------------------------------------------------------------------------- */
/* Stereo                                                                     */
/* ------------------------------------------------------------------------- */

xk_stereo *xk_stereo_create(void) {
  xk_stereo *stereo = (xk_stereo *)calloc(1, sizeof(xk_stereo));
  if (!stereo) return NULL;
  speex_stereo_state_reset(&stereo->state);
  return stereo;
}

void xk_stereo_free(xk_stereo *stereo) { free(stereo); }

void xk_stereo_reset(xk_stereo *stereo) {
  if (stereo) speex_stereo_state_reset(&stereo->state);
}

int xk_stereo_encode(xk_stereo *stereo, int16_t *samples, int count, xk_bits *bits) {
  (void)stereo;
  if (!samples || !bits || count <= 0 || (count % 2) != 0) return XK_INVALID;
  speex_encode_stereo_int((spx_int16_t *)samples, count / 2, &bits->bits);
  return 0;
}

int xk_stereo_decode(xk_stereo *stereo, int16_t *samples, int count) {
  if (!stereo || !samples || count <= 0 || (count % 2) != 0) return XK_INVALID;
  speex_decode_stereo_int((spx_int16_t *)samples, count / 2, &stereo->state);
  return 0;
}

/* ------------------------------------------------------------------------- */
/* Header                                                                     */
/* ------------------------------------------------------------------------- */

static int xk_header_int_field(const SpeexHeader *header, int field, int *value) {
  switch (field) {
    case XK_HEADER_VERSION_ID: *value = header->speex_version_id; return 1;
    case XK_HEADER_HEADER_SIZE: *value = header->header_size; return 1;
    case XK_HEADER_RATE: *value = header->rate; return 1;
    case XK_HEADER_MODE: *value = header->mode; return 1;
    case XK_HEADER_MODE_BITSTREAM_VERSION: *value = header->mode_bitstream_version; return 1;
    case XK_HEADER_CHANNELS: *value = header->nb_channels; return 1;
    case XK_HEADER_BITRATE: *value = header->bitrate; return 1;
    case XK_HEADER_FRAME_SIZE: *value = header->frame_size; return 1;
    case XK_HEADER_VBR: *value = header->vbr; return 1;
    case XK_HEADER_FRAMES_PER_PACKET: *value = header->frames_per_packet; return 1;
    case XK_HEADER_EXTRA_HEADERS: *value = header->extra_headers; return 1;
    case XK_HEADER_RESERVED1: *value = header->reserved1; return 1;
    case XK_HEADER_RESERVED2: *value = header->reserved2; return 1;
    default: return 0;
  }
}

static int xk_header_store_int(SpeexHeader *header, int field, int value) {
  switch (field) {
    case XK_HEADER_VERSION_ID: header->speex_version_id = value; return 1;
    case XK_HEADER_HEADER_SIZE: header->header_size = value; return 1;
    case XK_HEADER_RATE: header->rate = value; return 1;
    case XK_HEADER_MODE: header->mode = value; return 1;
    case XK_HEADER_MODE_BITSTREAM_VERSION: header->mode_bitstream_version = value; return 1;
    case XK_HEADER_CHANNELS: header->nb_channels = value; return 1;
    case XK_HEADER_BITRATE: header->bitrate = value; return 1;
    case XK_HEADER_FRAME_SIZE: header->frame_size = value; return 1;
    case XK_HEADER_VBR: header->vbr = value; return 1;
    case XK_HEADER_FRAMES_PER_PACKET: header->frames_per_packet = value; return 1;
    case XK_HEADER_EXTRA_HEADERS: header->extra_headers = value; return 1;
    case XK_HEADER_RESERVED1: header->reserved1 = value; return 1;
    case XK_HEADER_RESERVED2: header->reserved2 = value; return 1;
    default: return 0;
  }
}

static void xk_header_copy_strings(xk_header *header) {
  memcpy(header->speex_string, header->header.speex_string, SPEEX_HEADER_STRING_LENGTH);
  header->speex_string[SPEEX_HEADER_STRING_LENGTH] = 0;
  memcpy(header->speex_version, header->header.speex_version, SPEEX_HEADER_VERSION_LENGTH);
  header->speex_version[SPEEX_HEADER_VERSION_LENGTH] = 0;
}

xk_header *xk_header_create(int rate, int channels, int mode) {
  const SpeexMode *speex_mode = xk_mode(mode);
  if (!speex_mode || rate <= 0 || channels <= 0) return NULL;

  xk_header *header = (xk_header *)calloc(1, sizeof(xk_header));
  if (!header) return NULL;
  speex_init_header(&header->header, rate, channels, speex_mode);
  xk_header_copy_strings(header);
  return header;
}

xk_header *xk_header_parse(const uint8_t *packet, int size) {
  if (!packet || size <= 0) return NULL;
  SpeexHeader *parsed = speex_packet_to_header((char *)packet, size);
  if (!parsed) return NULL;

  xk_header *header = (xk_header *)calloc(1, sizeof(xk_header));
  if (!header) {
    speex_header_free(parsed);
    return NULL;
  }
  header->header = *parsed;
  speex_header_free(parsed);
  xk_header_copy_strings(header);
  return header;
}

void xk_header_free(xk_header *header) {
  if (!header) return;
  free(header->packet);
  free(header);
}

int xk_header_get_int(const xk_header *header, int field) {
  int value = 0;
  if (!header || !xk_header_int_field(&header->header, field, &value)) return XK_INVALID;
  return value;
}

int xk_header_set_int(xk_header *header, int field, int value) {
  if (!header || !xk_header_store_int(&header->header, field, value)) return XK_INVALID;
  return 0;
}

const char *xk_header_string(const xk_header *header, int which) {
  if (!header) return NULL;
  if (which == XK_HEADER_STRING) return header->speex_string;
  if (which == XK_HEADER_VERSION_STRING) return header->speex_version;
  return NULL;
}

const uint8_t *xk_header_packet(xk_header *header, int *size) {
  if (!header) {
    if (size) *size = 0;
    return NULL;
  }
  free(header->packet);
  header->packet = (uint8_t *)speex_header_to_packet(&header->header, &header->packet_size);
  if (!header->packet) {
    header->packet_size = 0;
    if (size) *size = 0;
    return NULL;
  }
  if (size) *size = header->packet_size;
  return header->packet;
}
