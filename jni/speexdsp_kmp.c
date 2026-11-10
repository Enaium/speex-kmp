/*
 * C shim for SpeexDSP. See speexdsp_kmp.h for the ABI contract and
 * speex_kmp.c for the libspeex half.
 */
#include "speexdsp_kmp.h"

#include <limits.h>
#include <stdlib.h>
#include <string.h>

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "speex/speex_buffer.h"
#include "speex/speex_echo.h"
#include "speex/speex_jitter.h"
#include "speex/speex_preprocess.h"
#include "speex/speex_resampler.h"

#ifndef SPEEXDSP_VERSION
#define SPEEXDSP_VERSION "1.2.1"
#endif

/* 64 KiB covers any RTP audio packet and keeps the jitter wrapper free of
 * per-call allocation. */
#define XDSP_JITTER_PACKET_CAPACITY 65536

const char *xdsp_version_string(void) { return SPEEXDSP_VERSION; }

/* ------------------------------------------------------------------------- */
/* Resampler                                                                  */
/* ------------------------------------------------------------------------- */

struct xdsp_resampler {
  SpeexResamplerState *state;
  int error;
  uint32_t channels;
  uint32_t in_rate;
  uint32_t out_rate;
  uint32_t ratio_num;
  uint32_t ratio_den;
  int16_t *out;
  int out_capacity;
  int out_len;
};

static void xdsp_resampler_refresh(xdsp_resampler *resampler) {
  if (!resampler || !resampler->state) return;
  speex_resampler_get_rate(resampler->state, &resampler->in_rate, &resampler->out_rate);
  speex_resampler_get_ratio(resampler->state, &resampler->ratio_num, &resampler->ratio_den);
}

static xdsp_resampler *xdsp_resampler_alloc(uint32_t channels) {
  xdsp_resampler *resampler = (xdsp_resampler *)calloc(1, sizeof(xdsp_resampler));
  if (!resampler) return NULL;
  resampler->channels = channels;
  return resampler;
}

xdsp_resampler *xdsp_resampler_create(uint32_t channels, uint32_t in_rate,
                                      uint32_t out_rate, int quality) {
  xdsp_resampler *resampler = xdsp_resampler_alloc(channels);
  if (!resampler) return NULL;
  if (channels == 0 || in_rate == 0 || out_rate == 0) {
    resampler->error = RESAMPLER_ERR_INVALID_ARG;
    return resampler;
  }
  int error = RESAMPLER_ERR_SUCCESS;
  resampler->state = speex_resampler_init(channels, in_rate, out_rate, quality, &error);
  resampler->error = resampler->state ? RESAMPLER_ERR_SUCCESS : error;
  xdsp_resampler_refresh(resampler);
  return resampler;
}

xdsp_resampler *xdsp_resampler_create_frac(uint32_t channels, uint32_t ratio_num,
                                           uint32_t ratio_den, uint32_t in_rate,
                                           uint32_t out_rate, int quality) {
  xdsp_resampler *resampler = xdsp_resampler_alloc(channels);
  if (!resampler) return NULL;
  if (channels == 0 || ratio_num == 0 || ratio_den == 0) {
    resampler->error = RESAMPLER_ERR_INVALID_ARG;
    return resampler;
  }
  int error = RESAMPLER_ERR_SUCCESS;
  resampler->state = speex_resampler_init_frac(channels, ratio_num, ratio_den, in_rate,
                                               out_rate, quality, &error);
  resampler->error = resampler->state ? RESAMPLER_ERR_SUCCESS : error;
  xdsp_resampler_refresh(resampler);
  return resampler;
}

int xdsp_resampler_error(const xdsp_resampler *resampler) {
  if (!resampler) return RESAMPLER_ERR_ALLOC_FAILED;
  return resampler->state ? RESAMPLER_ERR_SUCCESS : resampler->error;
}

void xdsp_resampler_free(xdsp_resampler *resampler) {
  if (!resampler) return;
  if (resampler->state) speex_resampler_destroy(resampler->state);
  free(resampler->out);
  free(resampler);
}

uint32_t xdsp_resampler_channels(const xdsp_resampler *resampler) {
  return resampler ? resampler->channels : 0;
}

int xdsp_resampler_set_rate(xdsp_resampler *resampler, uint32_t in_rate, uint32_t out_rate) {
  if (!resampler || !resampler->state) return XDSP_INVALID;
  int result = speex_resampler_set_rate(resampler->state, in_rate, out_rate);
  if (result == RESAMPLER_ERR_SUCCESS) xdsp_resampler_refresh(resampler);
  return result;
}

int xdsp_resampler_set_rate_frac(xdsp_resampler *resampler, uint32_t ratio_num,
                                 uint32_t ratio_den, uint32_t in_rate, uint32_t out_rate) {
  if (!resampler || !resampler->state) return XDSP_INVALID;
  int result = speex_resampler_set_rate_frac(resampler->state, ratio_num, ratio_den,
                                             in_rate, out_rate);
  if (result == RESAMPLER_ERR_SUCCESS) xdsp_resampler_refresh(resampler);
  return result;
}

uint32_t xdsp_resampler_input_rate(const xdsp_resampler *resampler) {
  return resampler ? resampler->in_rate : 0;
}

uint32_t xdsp_resampler_output_rate(const xdsp_resampler *resampler) {
  return resampler ? resampler->out_rate : 0;
}

uint32_t xdsp_resampler_ratio_num(const xdsp_resampler *resampler) {
  return resampler ? resampler->ratio_num : 0;
}

uint32_t xdsp_resampler_ratio_den(const xdsp_resampler *resampler) {
  return resampler ? resampler->ratio_den : 0;
}

int xdsp_resampler_set_quality(xdsp_resampler *resampler, int quality) {
  if (!resampler || !resampler->state) return XDSP_INVALID;
  return speex_resampler_set_quality(resampler->state, quality);
}

int xdsp_resampler_quality(const xdsp_resampler *resampler) {
  int quality = -1;
  if (!resampler || !resampler->state) return -1;
  speex_resampler_get_quality(resampler->state, &quality);
  return quality;
}

void xdsp_resampler_set_input_stride(xdsp_resampler *resampler, uint32_t stride) {
  if (resampler && resampler->state) speex_resampler_set_input_stride(resampler->state, stride);
}

uint32_t xdsp_resampler_input_stride(const xdsp_resampler *resampler) {
  uint32_t stride = 0;
  if (!resampler || !resampler->state) return 0;
  speex_resampler_get_input_stride(resampler->state, &stride);
  return stride;
}

void xdsp_resampler_set_output_stride(xdsp_resampler *resampler, uint32_t stride) {
  if (resampler && resampler->state) speex_resampler_set_output_stride(resampler->state, stride);
}

uint32_t xdsp_resampler_output_stride(const xdsp_resampler *resampler) {
  uint32_t stride = 0;
  if (!resampler || !resampler->state) return 0;
  speex_resampler_get_output_stride(resampler->state, &stride);
  return stride;
}

int xdsp_resampler_input_latency(const xdsp_resampler *resampler) {
  return (resampler && resampler->state) ? speex_resampler_get_input_latency(resampler->state) : 0;
}

int xdsp_resampler_output_latency(const xdsp_resampler *resampler) {
  return (resampler && resampler->state) ? speex_resampler_get_output_latency(resampler->state) : 0;
}

int xdsp_resampler_skip_zeros(xdsp_resampler *resampler) {
  if (!resampler || !resampler->state) return XDSP_INVALID;
  return speex_resampler_skip_zeros(resampler->state);
}

int xdsp_resampler_reset_mem(xdsp_resampler *resampler) {
  if (!resampler || !resampler->state) return XDSP_INVALID;
  return speex_resampler_reset_mem(resampler->state);
}

int xdsp_resampler_process_interleaved(xdsp_resampler *resampler, const int16_t *in,
                                       int in_len) {
  if (!resampler || !resampler->state || in_len < 0 || (!in && in_len > 0)) {
    return XDSP_INVALID;
  }
  if (resampler->ratio_den == 0 || resampler->channels == 0) return XDSP_INVALID;

  int64_t per_channel = ((int64_t)in_len * resampler->ratio_num + resampler->ratio_den - 1) /
                        resampler->ratio_den;
  per_channel += speex_resampler_get_output_latency(resampler->state) + 32;
  if (per_channel < 64) per_channel = 64;
  if (per_channel > INT_MAX / 2) return XDSP_INVALID;

  int64_t total = per_channel * resampler->channels;
  if (total > INT_MAX / (int64_t)sizeof(int16_t)) return XDSP_INVALID;

  if (total > resampler->out_capacity) {
    int16_t *replacement = (int16_t *)realloc(resampler->out, (size_t)total * sizeof(int16_t));
    if (!replacement) return RESAMPLER_ERR_ALLOC_FAILED;
    resampler->out = replacement;
    resampler->out_capacity = (int)total;
  }

  uint32_t in_length = (uint32_t)in_len;
  uint32_t out_length = (uint32_t)per_channel;
  int result = speex_resampler_process_interleaved_int(
      resampler->state, in, &in_length, resampler->out, &out_length);
  resampler->out_len = (result == RESAMPLER_ERR_SUCCESS) ? (int)out_length : 0;
  return result;
}

const int16_t *xdsp_resampler_output(xdsp_resampler *resampler, int *out_len) {
  if (!resampler) {
    if (out_len) *out_len = 0;
    return NULL;
  }
  if (out_len) *out_len = resampler->out_len;
  return resampler->out;
}

const char *xdsp_resampler_strerror(int error) { return speex_resampler_strerror(error); }

/* ------------------------------------------------------------------------- */
/* Preprocessor                                                               */
/* ------------------------------------------------------------------------- */

struct xdsp_preprocess {
  SpeexPreprocessState *state;
  int frame_size;
  int sampling_rate;
  int32_t *psd;
  int psd_capacity;
  int psd_size;
  int32_t *noise_psd;
  int noise_psd_capacity;
  int noise_psd_size;
};

xdsp_preprocess *xdsp_preprocess_create(int frame_size, int sampling_rate) {
  if (frame_size <= 0 || sampling_rate <= 0) return NULL;
  xdsp_preprocess *preprocess = (xdsp_preprocess *)calloc(1, sizeof(xdsp_preprocess));
  if (!preprocess) return NULL;
  preprocess->frame_size = frame_size;
  preprocess->sampling_rate = sampling_rate;
  preprocess->state = speex_preprocess_state_init(frame_size, sampling_rate);
  if (!preprocess->state) {
    free(preprocess);
    return NULL;
  }
  return preprocess;
}

void xdsp_preprocess_free(xdsp_preprocess *preprocess) {
  if (!preprocess) return;
  if (preprocess->state) speex_preprocess_state_destroy(preprocess->state);
  free(preprocess->psd);
  free(preprocess->noise_psd);
  free(preprocess);
}

int xdsp_preprocess_frame_size(const xdsp_preprocess *preprocess) {
  return preprocess ? preprocess->frame_size : 0;
}

int xdsp_preprocess_run(xdsp_preprocess *preprocess, int16_t *samples, int count) {
  if (!preprocess || !preprocess->state || !samples || count < preprocess->frame_size) {
    return XDSP_INVALID;
  }
  return speex_preprocess_run(preprocess->state, (spx_int16_t *)samples);
}

void xdsp_preprocess_estimate_update(xdsp_preprocess *preprocess, int16_t *samples, int count) {
  if (!preprocess || !preprocess->state || !samples || count < preprocess->frame_size) return;
  speex_preprocess_estimate_update(preprocess->state, (spx_int16_t *)samples);
}

static int xdsp_preprocess_request_is_int(int request) {
  switch (request) {
    case SPEEX_PREPROCESS_SET_DENOISE:
    case SPEEX_PREPROCESS_GET_DENOISE:
    case SPEEX_PREPROCESS_SET_AGC:
    case SPEEX_PREPROCESS_GET_AGC:
    case SPEEX_PREPROCESS_SET_VAD:
    case SPEEX_PREPROCESS_GET_VAD:
    case SPEEX_PREPROCESS_SET_DEREVERB:
    case SPEEX_PREPROCESS_GET_DEREVERB:
    case SPEEX_PREPROCESS_SET_PROB_START:
    case SPEEX_PREPROCESS_GET_PROB_START:
    case SPEEX_PREPROCESS_SET_PROB_CONTINUE:
    case SPEEX_PREPROCESS_GET_PROB_CONTINUE:
    case SPEEX_PREPROCESS_SET_NOISE_SUPPRESS:
    case SPEEX_PREPROCESS_GET_NOISE_SUPPRESS:
    case SPEEX_PREPROCESS_SET_ECHO_SUPPRESS:
    case SPEEX_PREPROCESS_GET_ECHO_SUPPRESS:
    case SPEEX_PREPROCESS_SET_ECHO_SUPPRESS_ACTIVE:
    case SPEEX_PREPROCESS_GET_ECHO_SUPPRESS_ACTIVE:
    case SPEEX_PREPROCESS_SET_AGC_INCREMENT:
    case SPEEX_PREPROCESS_GET_AGC_INCREMENT:
    case SPEEX_PREPROCESS_SET_AGC_DECREMENT:
    case SPEEX_PREPROCESS_GET_AGC_DECREMENT:
    case SPEEX_PREPROCESS_SET_AGC_MAX_GAIN:
    case SPEEX_PREPROCESS_GET_AGC_MAX_GAIN:
    case SPEEX_PREPROCESS_GET_AGC_LOUDNESS:
    case SPEEX_PREPROCESS_GET_AGC_GAIN:
    case SPEEX_PREPROCESS_GET_PROB:
    case SPEEX_PREPROCESS_SET_AGC_TARGET:
    case SPEEX_PREPROCESS_GET_AGC_TARGET:
      return 1;
    default:
      return 0;
  }
}

int xdsp_preprocess_ctl_int(xdsp_preprocess *preprocess, int request, int value) {
  if (!preprocess || !preprocess->state || !xdsp_preprocess_request_is_int(request)) {
    return XDSP_INVALID;
  }
  return speex_preprocess_ctl(preprocess->state, request, &value);
}

int xdsp_preprocess_get_int(xdsp_preprocess *preprocess, int request) {
  int value = 0;
  if (!preprocess || !preprocess->state || !xdsp_preprocess_request_is_int(request)) {
    return XDSP_INVALID;
  }
  if (speex_preprocess_ctl(preprocess->state, request, &value) != 0) return XDSP_INVALID;
  return value;
}

int xdsp_preprocess_ctl_float(xdsp_preprocess *preprocess, int request, float value) {
  if (!preprocess || !preprocess->state || request != SPEEX_PREPROCESS_SET_AGC_LEVEL) {
    return XDSP_INVALID;
  }
  return speex_preprocess_ctl(preprocess->state, request, &value);
}

float xdsp_preprocess_get_float(xdsp_preprocess *preprocess, int request) {
  float value = 0;
  if (!preprocess || !preprocess->state || request != SPEEX_PREPROCESS_GET_AGC_LEVEL) {
    return 0;
  }
  if (speex_preprocess_ctl(preprocess->state, request, &value) != 0) return 0;
  return value;
}

int xdsp_preprocess_psd_size(xdsp_preprocess *preprocess) {
  int size = 0;
  if (!preprocess || !preprocess->state) return 0;
  if (speex_preprocess_ctl(preprocess->state, SPEEX_PREPROCESS_GET_PSD_SIZE, &size) != 0) return 0;
  return size;
}

const int32_t *xdsp_preprocess_psd(xdsp_preprocess *preprocess, int *size) {
  if (!preprocess || !preprocess->state) {
    if (size) *size = 0;
    return NULL;
  }
  int length = xdsp_preprocess_psd_size(preprocess);
  if (length <= 0) {
    if (size) *size = 0;
    return NULL;
  }
  if (length > preprocess->psd_capacity) {
    int32_t *replacement = (int32_t *)realloc(preprocess->psd, (size_t)length * sizeof(int32_t));
    if (!replacement) {
      if (size) *size = 0;
      return NULL;
    }
    preprocess->psd = replacement;
    preprocess->psd_capacity = length;
  }
  if (speex_preprocess_ctl(preprocess->state, SPEEX_PREPROCESS_GET_PSD, preprocess->psd) != 0) {
    if (size) *size = 0;
    return NULL;
  }
  preprocess->psd_size = length;
  if (size) *size = length;
  return preprocess->psd;
}

int xdsp_preprocess_noise_psd_size(xdsp_preprocess *preprocess) {
  int size = 0;
  if (!preprocess || !preprocess->state) return 0;
  if (speex_preprocess_ctl(preprocess->state, SPEEX_PREPROCESS_GET_NOISE_PSD_SIZE, &size) != 0) {
    return 0;
  }
  return size;
}

const int32_t *xdsp_preprocess_noise_psd(xdsp_preprocess *preprocess, int *size) {
  if (!preprocess || !preprocess->state) {
    if (size) *size = 0;
    return NULL;
  }
  int length = xdsp_preprocess_noise_psd_size(preprocess);
  if (length <= 0) {
    if (size) *size = 0;
    return NULL;
  }
  if (length > preprocess->noise_psd_capacity) {
    int32_t *replacement =
        (int32_t *)realloc(preprocess->noise_psd, (size_t)length * sizeof(int32_t));
    if (!replacement) {
      if (size) *size = 0;
      return NULL;
    }
    preprocess->noise_psd = replacement;
    preprocess->noise_psd_capacity = length;
  }
  if (speex_preprocess_ctl(preprocess->state, SPEEX_PREPROCESS_GET_NOISE_PSD,
                           preprocess->noise_psd) != 0) {
    if (size) *size = 0;
    return NULL;
  }
  preprocess->noise_psd_size = length;
  if (size) *size = length;
  return preprocess->noise_psd;
}

/* ------------------------------------------------------------------------- */
/* Echo canceller                                                             */
/* ------------------------------------------------------------------------- */

struct xdsp_echo {
  SpeexEchoState *state;
  int frame_size;
  int nb_mic;
  int nb_speakers;
  int16_t *out;
  int out_count;
  int32_t *impulse;
  int impulse_capacity;
  int impulse_size;
};

xdsp_echo *xdsp_echo_create(int frame_size, int filter_length, int nb_mic, int nb_speakers) {
  if (frame_size <= 0 || filter_length <= 0 || nb_mic <= 0 || nb_speakers <= 0) return NULL;
  xdsp_echo *echo = (xdsp_echo *)calloc(1, sizeof(xdsp_echo));
  if (!echo) return NULL;
  echo->frame_size = frame_size;
  echo->nb_mic = nb_mic;
  echo->nb_speakers = nb_speakers;
  echo->state = (nb_mic == 1 && nb_speakers == 1)
                    ? speex_echo_state_init(frame_size, filter_length)
                    : speex_echo_state_init_mc(frame_size, filter_length, nb_mic, nb_speakers);
  if (!echo->state) {
    free(echo);
    return NULL;
  }
  echo->out = (int16_t *)calloc((size_t)frame_size * nb_mic, sizeof(int16_t));
  if (!echo->out) {
    speex_echo_state_destroy(echo->state);
    free(echo);
    return NULL;
  }
  return echo;
}

void xdsp_echo_free(xdsp_echo *echo) {
  if (!echo) return;
  if (echo->state) speex_echo_state_destroy(echo->state);
  free(echo->out);
  free(echo->impulse);
  free(echo);
}

void xdsp_echo_reset(xdsp_echo *echo) {
  if (echo && echo->state) speex_echo_state_reset(echo->state);
}

int xdsp_echo_frame_size(const xdsp_echo *echo) { return echo ? echo->frame_size : 0; }

int xdsp_echo_microphones(const xdsp_echo *echo) { return echo ? echo->nb_mic : 0; }

int xdsp_echo_speakers(const xdsp_echo *echo) { return echo ? echo->nb_speakers : 0; }

int xdsp_echo_cancellation(xdsp_echo *echo, const int16_t *rec, int rec_count,
                           const int16_t *play, int play_count) {
  if (!echo || !echo->state || !rec || !play) return XDSP_INVALID;
  if (rec_count < echo->frame_size * echo->nb_mic ||
      play_count < echo->frame_size * echo->nb_speakers) {
    return XDSP_INVALID;
  }
  speex_echo_cancellation(echo->state, (const spx_int16_t *)rec,
                          (const spx_int16_t *)play, (spx_int16_t *)echo->out);
  echo->out_count = echo->frame_size * echo->nb_mic;
  return 0;
}

int xdsp_echo_capture(xdsp_echo *echo, const int16_t *rec, int rec_count) {
  if (!echo || !echo->state || !rec || rec_count < echo->frame_size * echo->nb_mic) {
    return XDSP_INVALID;
  }
  speex_echo_capture(echo->state, (const spx_int16_t *)rec, (spx_int16_t *)echo->out);
  echo->out_count = echo->frame_size * echo->nb_mic;
  return 0;
}

int xdsp_echo_playback(xdsp_echo *echo, const int16_t *play, int play_count) {
  if (!echo || !echo->state || !play || play_count < echo->frame_size * echo->nb_speakers) {
    return XDSP_INVALID;
  }
  speex_echo_playback(echo->state, (const spx_int16_t *)play);
  return 0;
}

const int16_t *xdsp_echo_output(xdsp_echo *echo, int *count) {
  if (!echo) {
    if (count) *count = 0;
    return NULL;
  }
  if (count) *count = echo->out_count;
  return echo->out;
}

static int xdsp_echo_request_is_int(int request) {
  switch (request) {
    case SPEEX_ECHO_GET_FRAME_SIZE:
    case SPEEX_ECHO_SET_SAMPLING_RATE:
    case SPEEX_ECHO_GET_SAMPLING_RATE:
    case SPEEX_ECHO_GET_IMPULSE_RESPONSE_SIZE:
      return 1;
    default:
      return 0;
  }
}

int xdsp_echo_ctl_int(xdsp_echo *echo, int request, int value) {
  if (!echo || !echo->state || !xdsp_echo_request_is_int(request)) return XDSP_INVALID;
  return speex_echo_ctl(echo->state, request, &value);
}

int xdsp_echo_get_int(xdsp_echo *echo, int request) {
  int value = 0;
  if (!echo || !echo->state || !xdsp_echo_request_is_int(request)) return XDSP_INVALID;
  if (speex_echo_ctl(echo->state, request, &value) != 0) return XDSP_INVALID;
  return value;
}

int xdsp_echo_impulse_response_size(xdsp_echo *echo) {
  int size = 0;
  if (!echo || !echo->state) return 0;
  if (speex_echo_ctl(echo->state, SPEEX_ECHO_GET_IMPULSE_RESPONSE_SIZE, &size) != 0) return 0;
  return size;
}

const int32_t *xdsp_echo_impulse_response(xdsp_echo *echo, int *size) {
  if (!echo || !echo->state) {
    if (size) *size = 0;
    return NULL;
  }
  int length = xdsp_echo_impulse_response_size(echo);
  if (length <= 0) {
    if (size) *size = 0;
    return NULL;
  }
  if (length > echo->impulse_capacity) {
    int32_t *replacement =
        (int32_t *)realloc(echo->impulse, (size_t)length * sizeof(int32_t));
    if (!replacement) {
      if (size) *size = 0;
      return NULL;
    }
    echo->impulse = replacement;
    echo->impulse_capacity = length;
  }
  if (speex_echo_ctl(echo->state, SPEEX_ECHO_GET_IMPULSE_RESPONSE, echo->impulse) != 0) {
    if (size) *size = 0;
    return NULL;
  }
  echo->impulse_size = length;
  if (size) *size = length;
  return echo->impulse;
}

/* Attach/detach the echo state used for residual echo suppression. */
int xdsp_preprocess_set_echo_impl(xdsp_preprocess *preprocess, xdsp_echo *echo) {
  if (!preprocess || !preprocess->state) return XDSP_INVALID;
  SpeexEchoState *state = echo ? echo->state : NULL;
  return speex_preprocess_ctl(preprocess->state, SPEEX_PREPROCESS_SET_ECHO_STATE, state);
}

int xdsp_preprocess_set_echo(xdsp_preprocess *preprocess, xdsp_echo *echo) {
  return xdsp_preprocess_set_echo_impl(preprocess, echo);
}

/* ------------------------------------------------------------------------- */
/* Decorrelator                                                               */
/* ------------------------------------------------------------------------- */

struct xdsp_decorr {
  SpeexDecorrState *state;
  int channels;
  int frame_size;
  int16_t *out;
};

xdsp_decorr *xdsp_decorr_create(int rate, int channels, int frame_size) {
  if (rate <= 0 || channels <= 0 || frame_size <= 0) return NULL;
  xdsp_decorr *decorr = (xdsp_decorr *)calloc(1, sizeof(xdsp_decorr));
  if (!decorr) return NULL;
  decorr->channels = channels;
  decorr->frame_size = frame_size;
  decorr->state = speex_decorrelate_new(rate, channels, frame_size);
  if (!decorr->state) {
    free(decorr);
    return NULL;
  }
  decorr->out = (int16_t *)calloc((size_t)channels * frame_size, sizeof(int16_t));
  if (!decorr->out) {
    speex_decorrelate_destroy(decorr->state);
    free(decorr);
    return NULL;
  }
  return decorr;
}

void xdsp_decorr_free(xdsp_decorr *decorr) {
  if (!decorr) return;
  if (decorr->state) speex_decorrelate_destroy(decorr->state);
  free(decorr->out);
  free(decorr);
}

int xdsp_decorr_channels(const xdsp_decorr *decorr) { return decorr ? decorr->channels : 0; }

int xdsp_decorr_frame_size(const xdsp_decorr *decorr) { return decorr ? decorr->frame_size : 0; }

int xdsp_decorr_process(xdsp_decorr *decorr, const int16_t *in, int count, int strength) {
  if (!decorr || !decorr->state || !in || count < decorr->channels * decorr->frame_size) {
    return XDSP_INVALID;
  }
  speex_decorrelate(decorr->state, (const spx_int16_t *)in, (spx_int16_t *)decorr->out,
                    strength);
  return 0;
}

const int16_t *xdsp_decorr_output(xdsp_decorr *decorr, int *count) {
  if (!decorr) {
    if (count) *count = 0;
    return NULL;
  }
  if (count) *count = decorr->channels * decorr->frame_size;
  return decorr->out;
}

/* ------------------------------------------------------------------------- */
/* Jitter buffer                                                              */
/* ------------------------------------------------------------------------- */

struct xdsp_jitter {
  JitterBuffer *state;
  uint8_t *packet;
  int packet_len;
  uint32_t timestamp;
  uint32_t span;
  uint16_t sequence;
  uint32_t user_data;
};

xdsp_jitter *xdsp_jitter_create(int step_size) {
  if (step_size <= 0) return NULL;
  xdsp_jitter *jitter = (xdsp_jitter *)calloc(1, sizeof(xdsp_jitter));
  if (!jitter) return NULL;
  jitter->state = jitter_buffer_init(step_size);
  if (!jitter->state) {
    free(jitter);
    return NULL;
  }
  jitter->packet = (uint8_t *)calloc(XDSP_JITTER_PACKET_CAPACITY, 1);
  if (!jitter->packet) {
    jitter_buffer_destroy(jitter->state);
    free(jitter);
    return NULL;
  }
  return jitter;
}

void xdsp_jitter_free(xdsp_jitter *jitter) {
  if (!jitter) return;
  if (jitter->state) jitter_buffer_destroy(jitter->state);
  free(jitter->packet);
  free(jitter);
}

void xdsp_jitter_reset(xdsp_jitter *jitter) {
  if (!jitter || !jitter->state) return;
  jitter_buffer_reset(jitter->state);
  jitter->packet_len = 0;
  jitter->timestamp = 0;
  jitter->span = 0;
  jitter->sequence = 0;
  jitter->user_data = 0;
}

int xdsp_jitter_put(xdsp_jitter *jitter, const uint8_t *data, int len, uint32_t timestamp,
                    uint32_t span, uint16_t sequence, uint32_t user_data) {
  if (!jitter || !jitter->state || len < 0 || len > XDSP_JITTER_PACKET_CAPACITY ||
      (!data && len > 0)) {
    return XDSP_INVALID;
  }
  JitterBufferPacket packet;
  memset(&packet, 0, sizeof(packet));
  packet.data = (char *)data;
  packet.len = (spx_uint32_t)len;
  packet.timestamp = timestamp;
  packet.span = span;
  packet.sequence = sequence;
  packet.user_data = user_data;
  jitter_buffer_put(jitter->state, &packet);
  return 0;
}

static void xdsp_jitter_store(xdsp_jitter *jitter, const JitterBufferPacket *packet) {
  jitter->packet_len = (int)packet->len;
  jitter->timestamp = packet->timestamp;
  jitter->span = packet->span;
  jitter->sequence = packet->sequence;
  jitter->user_data = packet->user_data;
}

int xdsp_jitter_get(xdsp_jitter *jitter, int32_t desired_span) {
  if (!jitter || !jitter->state) return XDSP_JITTER_BAD_ARGUMENT;
  JitterBufferPacket packet;
  memset(&packet, 0, sizeof(packet));
  packet.data = (char *)jitter->packet;
  packet.len = XDSP_JITTER_PACKET_CAPACITY;
  spx_int32_t start_offset = 0;
  int result = jitter_buffer_get(jitter->state, &packet, desired_span, &start_offset);
  xdsp_jitter_store(jitter, &packet);
  return result;
}

int xdsp_jitter_get_another(xdsp_jitter *jitter) {
  if (!jitter || !jitter->state) return XDSP_JITTER_BAD_ARGUMENT;
  JitterBufferPacket packet;
  memset(&packet, 0, sizeof(packet));
  packet.data = (char *)jitter->packet;
  packet.len = XDSP_JITTER_PACKET_CAPACITY;
  int result = jitter_buffer_get_another(jitter->state, &packet);
  xdsp_jitter_store(jitter, &packet);
  return result;
}

const uint8_t *xdsp_jitter_packet(xdsp_jitter *jitter, int *len) {
  if (!jitter) {
    if (len) *len = 0;
    return NULL;
  }
  if (len) *len = jitter->packet_len;
  return jitter->packet;
}

uint32_t xdsp_jitter_timestamp(const xdsp_jitter *jitter) {
  return jitter ? jitter->timestamp : 0;
}

uint32_t xdsp_jitter_span(const xdsp_jitter *jitter) { return jitter ? jitter->span : 0; }

uint16_t xdsp_jitter_sequence(const xdsp_jitter *jitter) {
  return jitter ? jitter->sequence : 0;
}

uint32_t xdsp_jitter_user_data(const xdsp_jitter *jitter) {
  return jitter ? jitter->user_data : 0;
}

int xdsp_jitter_pointer_timestamp(xdsp_jitter *jitter) {
  return (jitter && jitter->state) ? jitter_buffer_get_pointer_timestamp(jitter->state) : 0;
}

void xdsp_jitter_tick(xdsp_jitter *jitter) {
  if (jitter && jitter->state) jitter_buffer_tick(jitter->state);
}

void xdsp_jitter_remaining_span(xdsp_jitter *jitter, uint32_t remaining) {
  if (jitter && jitter->state) jitter_buffer_remaining_span(jitter->state, remaining);
}

static int xdsp_jitter_request_is_int(int request) {
  switch (request) {
    case JITTER_BUFFER_SET_MARGIN:
    case JITTER_BUFFER_GET_MARGIN:
    case JITTER_BUFFER_GET_AVAILABLE_COUNT:
    case JITTER_BUFFER_SET_DELAY_STEP:
    case JITTER_BUFFER_GET_DELAY_STEP:
    case JITTER_BUFFER_SET_CONCEALMENT_SIZE:
    case JITTER_BUFFER_GET_CONCEALMENT_SIZE:
    case JITTER_BUFFER_SET_MAX_LATE_RATE:
    case JITTER_BUFFER_GET_MAX_LATE_RATE:
    case JITTER_BUFFER_SET_LATE_COST:
    case JITTER_BUFFER_GET_LATE_COST:
      return 1;
    default:
      return 0;
  }
}

int xdsp_jitter_ctl_int(xdsp_jitter *jitter, int request, int value) {
  if (!jitter || !jitter->state || !xdsp_jitter_request_is_int(request)) return XDSP_INVALID;
  if (request == JITTER_BUFFER_SET_MAX_LATE_RATE && value <= 0) return XDSP_INVALID;
  return jitter_buffer_ctl(jitter->state, request, &value);
}

int xdsp_jitter_get_int(xdsp_jitter *jitter, int request) {
  int value = 0;
  if (!jitter || !jitter->state || !xdsp_jitter_request_is_int(request)) return XDSP_INVALID;
  if (jitter_buffer_ctl(jitter->state, request, &value) != 0) return XDSP_INVALID;
  return value;
}

/* ------------------------------------------------------------------------- */
/* Ring buffer                                                                */
/* ------------------------------------------------------------------------- */

struct xdsp_buffer {
  SpeexBuffer *state;
  uint8_t *out;
  int out_capacity;
  int out_size;
};

xdsp_buffer *xdsp_buffer_create(int size) {
  if (size <= 0) return NULL;
  xdsp_buffer *buffer = (xdsp_buffer *)calloc(1, sizeof(xdsp_buffer));
  if (!buffer) return NULL;
  buffer->state = speex_buffer_init(size);
  if (!buffer->state) {
    free(buffer);
    return NULL;
  }
  return buffer;
}

void xdsp_buffer_free(xdsp_buffer *buffer) {
  if (!buffer) return;
  if (buffer->state) speex_buffer_destroy(buffer->state);
  free(buffer->out);
  free(buffer);
}

int xdsp_buffer_write(xdsp_buffer *buffer, const uint8_t *data, int len) {
  if (!buffer || !buffer->state || len < 0 || (!data && len > 0)) return XDSP_INVALID;
  if (len == 0) return 0;
  return speex_buffer_write(buffer->state, (void *)data, len);
}

int xdsp_buffer_write_zeros(xdsp_buffer *buffer, int len) {
  if (!buffer || !buffer->state || len < 0) return XDSP_INVALID;
  if (len == 0) return 0;
  return speex_buffer_writezeros(buffer->state, len);
}

int xdsp_buffer_read(xdsp_buffer *buffer, int len) {
  if (!buffer || !buffer->state || len < 0) return XDSP_INVALID;
  if (len == 0) {
    buffer->out_size = 0;
    return 0;
  }
  if (len > buffer->out_capacity) {
    uint8_t *replacement = (uint8_t *)realloc(buffer->out, (size_t)len);
    if (!replacement) return XDSP_INVALID;
    buffer->out = replacement;
    buffer->out_capacity = len;
  }
  speex_buffer_read(buffer->state, buffer->out, len);
  buffer->out_size = len;
  return len;
}

const uint8_t *xdsp_buffer_data(xdsp_buffer *buffer, int *size) {
  if (!buffer) {
    if (size) *size = 0;
    return NULL;
  }
  if (size) *size = buffer->out_size;
  return buffer->out;
}

int xdsp_buffer_available(xdsp_buffer *buffer) {
  return (buffer && buffer->state) ? speex_buffer_get_available(buffer->state) : 0;
}

int xdsp_buffer_resize(xdsp_buffer *buffer, int len) {
  if (!buffer || !buffer->state || len <= 0) return XDSP_INVALID;
  return speex_buffer_resize(buffer->state, len);
}
