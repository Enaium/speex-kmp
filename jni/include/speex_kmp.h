/*
 * C shim for the Speex Kotlin Multiplatform binding.
 *
 * The same implementation is compiled three ways:
 *   - into the static archive linked by every Kotlin/Native cinterop klib,
 *   - into the JNI shared library (speex_jni.c),
 *   - into the standalone WebAssembly module (speex_wasm_entry.c).
 *
 * All three expose this API, so argument validation and the encode/decode
 * contract are identical on every platform.
 *
 * Ownership:
 *   - every `*_create()` result must be released with the matching `*_free()`;
 *   - buffers returned as `const uint8_t *` / `const int16_t *` are owned by
 *     the handle and stay valid until the next call that writes them;
 *     callers copy what they keep.
 *
 * Modes are `SPEEX_MODEID_*` values (0 narrowband, 1 wideband, 2 UWB).
 */
#ifndef SPEEX_KMP_H
#define SPEEX_KMP_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/** The call wrote a packet. */
#define XK_PACKET 1

/** The encoder used DTX: nothing needs to be transmitted. */
#define XK_NO_PACKET 0

/** Failure inside the shim or an invalid argument. */
#define XK_ERROR (-1)

/** A shim function rejected one of its arguments (or allocation failed). */
#define XK_INVALID (-3)

/** Speex decoder status: end of stream. */
#define XK_DECODE_END_OF_STREAM (-1)

/** Speex decoder status: corrupt stream (concealment was applied). */
#define XK_DECODE_CORRUPT (-2)

/** Speex mode ids. */
#define XK_MODE_NARROWBAND 0
#define XK_MODE_WIDEBAND 1
#define XK_MODE_ULTRAWIDEBAND 2

/** Header fields used by `xk_header_get_int()` / `xk_header_set_int()`. */
enum {
  XK_HEADER_VERSION_ID = 1,
  XK_HEADER_HEADER_SIZE = 2,
  XK_HEADER_RATE = 3,
  XK_HEADER_MODE = 4,
  XK_HEADER_MODE_BITSTREAM_VERSION = 5,
  XK_HEADER_CHANNELS = 6,
  XK_HEADER_BITRATE = 7,
  XK_HEADER_FRAME_SIZE = 8,
  XK_HEADER_VBR = 9,
  XK_HEADER_FRAMES_PER_PACKET = 10,
  XK_HEADER_EXTRA_HEADERS = 11,
  XK_HEADER_RESERVED1 = 12,
  XK_HEADER_RESERVED2 = 13,
};

/** String selectors used by `xk_header_string()`. */
#define XK_HEADER_STRING 1
#define XK_HEADER_VERSION_STRING 2

typedef struct xk_bits xk_bits;
typedef struct xk_encoder xk_encoder;
typedef struct xk_decoder xk_decoder;
typedef struct xk_stereo xk_stereo;
typedef struct xk_header xk_header;

/* ------------------------------------------------------------------ */
/* Library version                                                    */
/* ------------------------------------------------------------------ */

/** Speex library version, e.g. "1.2.1". */
const char *xk_version_string(void);

int xk_version_major(void);

int xk_version_minor(void);

int xk_version_micro(void);

/** Version suffix, usually the empty string. */
const char *xk_version_extra(void);

/* ------------------------------------------------------------------ */
/* Modes                                                              */
/* ------------------------------------------------------------------ */

/** Frame size, in samples, of a mode; 0 for an unknown mode. */
int xk_mode_frame_size(int mode);

/** Bits used by one sub-mode frame; -1 for an unknown sub-mode. */
int xk_mode_submode_bits(int mode, int submode);

/** Mode name, or NULL for an unknown mode. */
const char *xk_mode_name(int mode);

/* ------------------------------------------------------------------ */
/* Bit-stream                                                         */
/* ------------------------------------------------------------------ */

xk_bits *xk_bits_create(void);

void xk_bits_free(xk_bits *bits);

void xk_bits_reset(xk_bits *bits);

void xk_bits_rewind(xk_bits *bits);

/** Copies `size` bytes into the bit-stream and rewinds the read cursor. */
int xk_bits_read_from(xk_bits *bits, const uint8_t *data, int size);

/** Appends `size` bytes after the bits already in the stream. */
int xk_bits_read_whole_bytes(xk_bits *bits, const uint8_t *data, int size);

void xk_bits_pack(xk_bits *bits, int data, int nbits);

int xk_bits_unpack_signed(xk_bits *bits, int nbits);

unsigned int xk_bits_unpack_unsigned(xk_bits *bits, int nbits);

int xk_bits_peek(xk_bits *bits);

unsigned int xk_bits_peek_unsigned(xk_bits *bits, int nbits);

void xk_bits_advance(xk_bits *bits, int nbits);

int xk_bits_remaining(xk_bits *bits);

/** Number of bytes the stream occupies, including a partial last byte. */
int xk_bits_nbytes(xk_bits *bits);

/** Pads the last byte with the stream terminator. */
void xk_bits_insert_terminator(xk_bits *bits);

/** Serialises the stream into `out`; returns the bytes written or XK_ERROR. */
int xk_bits_write(xk_bits *bits, uint8_t *out, int max_size);

/**
 * Direct view of the stream. `size` receives `xk_bits_nbytes()`. The pointer
 * is invalidated by the next operation that writes the stream.
 */
const uint8_t *xk_bits_data(xk_bits *bits, int *size);

/* ------------------------------------------------------------------ */
/* Encoder                                                            */
/* ------------------------------------------------------------------ */

xk_encoder *xk_encoder_create(int mode);

void xk_encoder_free(xk_encoder *encoder);

/** Frame size expected by `xk_encoder_encode()`. */
int xk_encoder_frame_size(const xk_encoder *encoder);

/**
 * Encodes the first frame-size samples of a buffer of at least `count`
 * samples. Returns XK_PACKET, XK_NO_PACKET (DTX) or XK_ERROR. The input buffer
 * may be overwritten.
 */
int xk_encoder_encode(xk_encoder *encoder, int16_t *samples, int count);

/**
 * Encodes one interleaved stereo frame from a buffer of at least `count`
 * samples; the first `2 * frame size` samples are folded to mono in place.
 */
int xk_encoder_encode_stereo(xk_encoder *encoder, int16_t *samples, int count);

/** Last packet, valid until the next encode call. */
const uint8_t *xk_encoder_packet(xk_encoder *encoder, int *size);

/** Sends an integer-valued `SPEEX_SET_*` request. */
int xk_encoder_ctl(xk_encoder *encoder, int request, int value);

/** Reads an integer-valued `SPEEX_GET_*` request. */
int xk_encoder_get(xk_encoder *encoder, int request);

/* `SPEEX_*_VBR_QUALITY` and `SPEEX_GET_RELATIVE_QUALITY` exchange float, not
 * int, so they have dedicated entry points instead of the int bridge. */
int xk_encoder_set_vbr_quality(xk_encoder *encoder, float value);

float xk_encoder_get_vbr_quality(xk_encoder *encoder);

float xk_encoder_get_relative_quality(xk_encoder *encoder);

/* ------------------------------------------------------------------ */
/* Decoder                                                            */
/* ------------------------------------------------------------------ */

xk_decoder *xk_decoder_create(int mode);

void xk_decoder_free(xk_decoder *decoder);

/** Frame size produced by `xk_decoder_decode()`. */
int xk_decoder_frame_size(const xk_decoder *decoder);

/**
 * Decodes one packet. A NULL packet (or an empty one) triggers packet-loss
 * concealment. Returns 0, XK_DECODE_END_OF_STREAM, XK_DECODE_CORRUPT or
 * XK_INVALID.
 */
int xk_decoder_decode(xk_decoder *decoder, const uint8_t *packet, int size);

/**
 * Decodes one packet and expands it to interleaved stereo. The decoder must
 * have been created with `xk_decoder_frame_size()` samples per frame; the
 * returned buffer holds 2 * frame size samples.
 */
int xk_decoder_decode_stereo(xk_decoder *decoder, const uint8_t *packet, int size,
                             xk_stereo *stereo);

/** Decoded samples: frame size, or 2 * frame size after a stereo decode. */
const int16_t *xk_decoder_samples(xk_decoder *decoder, int *count);

int xk_decoder_ctl(xk_decoder *decoder, int request, int value);

int xk_decoder_get(xk_decoder *decoder, int request);

/* ------------------------------------------------------------------ */
/* Stereo                                                             */
/* ------------------------------------------------------------------ */

xk_stereo *xk_stereo_create(void);

void xk_stereo_free(xk_stereo *stereo);

void xk_stereo_reset(xk_stereo *stereo);

/**
 * Folds one interleaved stereo frame (`count` samples, 2 * frame size) to mono
 * in place and appends the intensity-stereo request to `bits`.
 */
int xk_stereo_encode(xk_stereo *stereo, int16_t *samples, int count, xk_bits *bits);

/** Expands a mono frame in place to `count` interleaved stereo samples. */
int xk_stereo_decode(xk_stereo *stereo, int16_t *samples, int count);

/* ------------------------------------------------------------------ */
/* Ogg / Speex header                                                 */
/* ------------------------------------------------------------------ */

/** Builds a header with `speex_init_header()`. */
xk_header *xk_header_create(int rate, int channels, int mode);

/** Parses a header packet; NULL when the packet is not a Speex header. */
xk_header *xk_header_parse(const uint8_t *packet, int size);

void xk_header_free(xk_header *header);

int xk_header_get_int(const xk_header *header, int field);

int xk_header_set_int(xk_header *header, int field, int value);

/** Field XK_HEADER_STRING or XK_HEADER_VERSION_STRING. */
const char *xk_header_string(const xk_header *header, int which);

/** Serialises the header (little-endian) into a packet owned by the header. */
const uint8_t *xk_header_packet(xk_header *header, int *size);

/* ------------------------------------------------------------------ */
/* Memory (used by the WebAssembly bridge)                            */
/* ------------------------------------------------------------------ */

void *xk_alloc(size_t size);

void xk_free(void *pointer);

#ifdef __cplusplus
}
#endif

#endif /* SPEEX_KMP_H */
