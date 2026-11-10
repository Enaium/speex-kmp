/*
 * C shim for the SpeexDSP part of the Kotlin Multiplatform binding.
 *
 * The same implementation is compiled into the static archive linked by every
 * Kotlin/Native cinterop klib, the JNI shared library and the standalone
 * WebAssembly module. See speex_kmp.h for the libspeex half of the ABI.
 *
 * Conventions:
 *   - functions returning int return 0 or a SpeexDSP status/error code on
 *     success and XDSP_INVALID for rejected arguments;
 *   - buffers returned as `const` pointers are owned by the handle and stay
 *     valid until the next call that writes them;
 *   - every `*_create()` result must be released with the matching `*_free()`.
 *
 * Request values passed to the `*_ctl` functions are the upstream
 * `SPEEX_PREPROCESS_*`, `SPEEX_ECHO_*` and `JITTER_BUFFER_*` constants.
 */
#ifndef SPEEXDSP_KMP_H
#define SPEEXDSP_KMP_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/** Argument rejected by the shim or an allocation failure. */
#define XDSP_INVALID (-3)

/** SpeexDSP version, e.g. "1.2.1". */
const char *xdsp_version_string(void);

typedef struct xdsp_resampler xdsp_resampler;
typedef struct xdsp_preprocess xdsp_preprocess;
typedef struct xdsp_echo xdsp_echo;
typedef struct xdsp_decorr xdsp_decorr;
typedef struct xdsp_jitter xdsp_jitter;
typedef struct xdsp_buffer xdsp_buffer;

/* ------------------------------------------------------------------ */
/* Resampler                                                          */
/* ------------------------------------------------------------------ */

/**
 * Creates a resampler. The returned wrapper is never NULL unless allocation
 * fails; when Speex rejects the configuration, `xdsp_resampler_error()` returns
 * the `RESAMPLER_ERR_*` code and the wrapper must still be freed.
 */
xdsp_resampler *xdsp_resampler_create(uint32_t channels, uint32_t in_rate,
                                      uint32_t out_rate, int quality);

xdsp_resampler *xdsp_resampler_create_frac(uint32_t channels, uint32_t ratio_num,
                                           uint32_t ratio_den, uint32_t in_rate,
                                           uint32_t out_rate, int quality);

/** 0 for a usable state, otherwise the `RESAMPLER_ERR_*` creation error. */
int xdsp_resampler_error(const xdsp_resampler *resampler);

void xdsp_resampler_free(xdsp_resampler *resampler);

uint32_t xdsp_resampler_channels(const xdsp_resampler *resampler);

int xdsp_resampler_set_rate(xdsp_resampler *resampler, uint32_t in_rate, uint32_t out_rate);

int xdsp_resampler_set_rate_frac(xdsp_resampler *resampler, uint32_t ratio_num,
                                 uint32_t ratio_den, uint32_t in_rate, uint32_t out_rate);

uint32_t xdsp_resampler_input_rate(const xdsp_resampler *resampler);

uint32_t xdsp_resampler_output_rate(const xdsp_resampler *resampler);

uint32_t xdsp_resampler_ratio_num(const xdsp_resampler *resampler);

uint32_t xdsp_resampler_ratio_den(const xdsp_resampler *resampler);

int xdsp_resampler_set_quality(xdsp_resampler *resampler, int quality);

int xdsp_resampler_quality(const xdsp_resampler *resampler);

void xdsp_resampler_set_input_stride(xdsp_resampler *resampler, uint32_t stride);

uint32_t xdsp_resampler_input_stride(const xdsp_resampler *resampler);

void xdsp_resampler_set_output_stride(xdsp_resampler *resampler, uint32_t stride);

uint32_t xdsp_resampler_output_stride(const xdsp_resampler *resampler);

int xdsp_resampler_input_latency(const xdsp_resampler *resampler);

int xdsp_resampler_output_latency(const xdsp_resampler *resampler);

int xdsp_resampler_skip_zeros(xdsp_resampler *resampler);

int xdsp_resampler_reset_mem(xdsp_resampler *resampler);

/**
 * Resamples `in_len` interleaved samples per channel from `in`. The output is
 * written to the handle's buffer; read it with `xdsp_resampler_output()`.
 * Returns 0 on success or a `RESAMPLER_ERR_*` code.
 */
int xdsp_resampler_process_interleaved(xdsp_resampler *resampler, const int16_t *in,
                                       int in_len);

/** Output of the last process call; `out_len` is per channel. */
const int16_t *xdsp_resampler_output(xdsp_resampler *resampler, int *out_len);

/** English description of a `RESAMPLER_ERR_*` code. */
const char *xdsp_resampler_strerror(int error);

/* ------------------------------------------------------------------ */
/* Preprocessor                                                       */
/* ------------------------------------------------------------------ */

xdsp_preprocess *xdsp_preprocess_create(int frame_size, int sampling_rate);

void xdsp_preprocess_free(xdsp_preprocess *preprocess);

int xdsp_preprocess_frame_size(const xdsp_preprocess *preprocess);

/** Processes one frame in place; returns the VAD flag (0 or 1). */
int xdsp_preprocess_run(xdsp_preprocess *preprocess, int16_t *samples, int count);

void xdsp_preprocess_estimate_update(xdsp_preprocess *preprocess, int16_t *samples, int count);

/** Sends an integer `SPEEX_PREPROCESS_*` request. */
int xdsp_preprocess_ctl_int(xdsp_preprocess *preprocess, int request, int value);

int xdsp_preprocess_get_int(xdsp_preprocess *preprocess, int request);

/** Sends the float-valued `SPEEX_PREPROCESS_SET_AGC_LEVEL` request. */
int xdsp_preprocess_ctl_float(xdsp_preprocess *preprocess, int request, float value);

float xdsp_preprocess_get_float(xdsp_preprocess *preprocess, int request);

/** Attaches an echo canceller for residual echo suppression; NULL detaches. */
int xdsp_preprocess_set_echo(xdsp_preprocess *preprocess, xdsp_echo *echo);

int xdsp_preprocess_psd_size(xdsp_preprocess *preprocess);

/** Power spectrum of the last frame; owned by the preprocessor. */
const int32_t *xdsp_preprocess_psd(xdsp_preprocess *preprocess, int *size);

int xdsp_preprocess_noise_psd_size(xdsp_preprocess *preprocess);

/** Noise estimate of the last frame; owned by the preprocessor. */
const int32_t *xdsp_preprocess_noise_psd(xdsp_preprocess *preprocess, int *size);

/* ------------------------------------------------------------------ */
/* Echo canceller                                                     */
/* ------------------------------------------------------------------ */

xdsp_echo *xdsp_echo_create(int frame_size, int filter_length, int nb_mic,
                            int nb_speakers);

void xdsp_echo_free(xdsp_echo *echo);

void xdsp_echo_reset(xdsp_echo *echo);

int xdsp_echo_frame_size(const xdsp_echo *echo);

int xdsp_echo_microphones(const xdsp_echo *echo);

int xdsp_echo_speakers(const xdsp_echo *echo);

/** Cancels echo; `rec` holds frame*mics samples, `play` frame*speakers. */
int xdsp_echo_cancellation(xdsp_echo *echo, const int16_t *rec, int rec_count,
                           const int16_t *play, int play_count);

/** Split capture path; `rec` holds frame*mics samples. */
int xdsp_echo_capture(xdsp_echo *echo, const int16_t *rec, int rec_count);

/** Split playback path; `play` holds frame*speakers samples. */
int xdsp_echo_playback(xdsp_echo *echo, const int16_t *play, int play_count);

/** Output of the last cancellation/capture; frame*mics samples. */
const int16_t *xdsp_echo_output(xdsp_echo *echo, int *count);

int xdsp_echo_ctl_int(xdsp_echo *echo, int request, int value);

int xdsp_echo_get_int(xdsp_echo *echo, int request);

int xdsp_echo_impulse_response_size(xdsp_echo *echo);

/** Impulse response of the adaptive filter; owned by the echo state. */
const int32_t *xdsp_echo_impulse_response(xdsp_echo *echo, int *size);

/* ------------------------------------------------------------------ */
/* Decorrelator                                                       */
/* ------------------------------------------------------------------ */

xdsp_decorr *xdsp_decorr_create(int rate, int channels, int frame_size);

void xdsp_decorr_free(xdsp_decorr *decorr);

int xdsp_decorr_channels(const xdsp_decorr *decorr);

int xdsp_decorr_frame_size(const xdsp_decorr *decorr);

/** Decorrelates one interleaved frame of channels*frame_size samples. */
int xdsp_decorr_process(xdsp_decorr *decorr, const int16_t *in, int count, int strength);

/** Output of the last process call; channels*frame_size samples. */
const int16_t *xdsp_decorr_output(xdsp_decorr *decorr, int *count);

/* ------------------------------------------------------------------ */
/* Jitter buffer                                                      */
/* ------------------------------------------------------------------ */

/** Status values returned by `xdsp_jitter_get()`. */
#define XDSP_JITTER_OK 0
#define XDSP_JITTER_MISSING 1
#define XDSP_JITTER_INSERTION 2
#define XDSP_JITTER_INTERNAL_ERROR (-1)
#define XDSP_JITTER_BAD_ARGUMENT (-2)

xdsp_jitter *xdsp_jitter_create(int step_size);

void xdsp_jitter_free(xdsp_jitter *jitter);

void xdsp_jitter_reset(xdsp_jitter *jitter);

/** Copies one packet of at most 64 KiB into the buffer. */
int xdsp_jitter_put(xdsp_jitter *jitter, const uint8_t *data, int len,
                    uint32_t timestamp, uint32_t span, uint16_t sequence,
                    uint32_t user_data);

/** Returns a `XDSP_JITTER_*` status; the packet is read with the getters below. */
int xdsp_jitter_get(xdsp_jitter *jitter, int32_t desired_span);

int xdsp_jitter_get_another(xdsp_jitter *jitter);

const uint8_t *xdsp_jitter_packet(xdsp_jitter *jitter, int *len);

uint32_t xdsp_jitter_timestamp(const xdsp_jitter *jitter);

uint32_t xdsp_jitter_span(const xdsp_jitter *jitter);

uint16_t xdsp_jitter_sequence(const xdsp_jitter *jitter);

uint32_t xdsp_jitter_user_data(const xdsp_jitter *jitter);

int xdsp_jitter_pointer_timestamp(xdsp_jitter *jitter);

void xdsp_jitter_tick(xdsp_jitter *jitter);

void xdsp_jitter_remaining_span(xdsp_jitter *jitter, uint32_t remaining);

int xdsp_jitter_ctl_int(xdsp_jitter *jitter, int request, int value);

int xdsp_jitter_get_int(xdsp_jitter *jitter, int request);

/* ------------------------------------------------------------------ */
/* Ring buffer                                                        */
/* ------------------------------------------------------------------ */

xdsp_buffer *xdsp_buffer_create(int size);

void xdsp_buffer_free(xdsp_buffer *buffer);

int xdsp_buffer_write(xdsp_buffer *buffer, const uint8_t *data, int len);

int xdsp_buffer_write_zeros(xdsp_buffer *buffer, int len);

/** Reads `len` bytes (zero-filled when fewer are available) into the handle. */
int xdsp_buffer_read(xdsp_buffer *buffer, int len);

/** Bytes produced by the last read. */
const uint8_t *xdsp_buffer_data(xdsp_buffer *buffer, int *size);

int xdsp_buffer_available(xdsp_buffer *buffer);

int xdsp_buffer_resize(xdsp_buffer *buffer, int len);

#ifdef __cplusplus
}
#endif

#endif /* SPEEXDSP_KMP_H */
