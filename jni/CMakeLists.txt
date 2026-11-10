#
# Compiles Speex, SpeexDSP and the KMP C shims into one static archive and,
# optionally, the JNI shared library or the standalone WebAssembly module.
#
#   cmake -S jni -B build -DBUILD_JNI=ON               desktop JVM / Android JNI
#   cmake -S jni -B build                              static archive only
#   emcmake cmake -S jni -B build-wasm -DBUILD_WASM=ON WebAssembly
#
# Speex and SpeexDSP ship autotools/meson builds, so this file lists their
# library sources. The floating-point build with the portable smallft FFT is
# used everywhere; architecture assembly is disabled, leaving one code path
# across x86, ARM, Android, MinGW and WebAssembly.
#
# SPEEX_DIR and SPEEXDSP_DIR point at the checkouts to compile; the defaults are
# the git submodules next to this directory.
#
cmake_minimum_required(VERSION 3.20)

project(speex_kmp C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

option(BUILD_JNI "Build the JNI shared library" OFF)
option(BUILD_WASM "Build the standalone WebAssembly module" OFF)

set(SPEEX_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../speex" CACHE PATH "Speex checkout to build")
if(NOT EXISTS "${SPEEX_DIR}/include/speex/speex.h")
  message(FATAL_ERROR "Speex sources are missing at ${SPEEX_DIR}")
endif()

file(READ "${SPEEX_DIR}/meson.build" XK_MESON)
string(REGEX MATCH "project\\('speex', 'c', version: '([0-9]+\\.[0-9]+\\.[0-9]+)'" XK_PROJECT "${XK_MESON}")
set(XK_SPEEX_VERSION "${CMAKE_MATCH_1}")
if(NOT XK_SPEEX_VERSION)
  message(FATAL_ERROR "Could not read the Speex version from ${SPEEX_DIR}/meson.build")
endif()
string(REPLACE "." ";" XK_VERSION_PARTS "${XK_SPEEX_VERSION}")
list(GET XK_VERSION_PARTS 0 XK_SPEEX_MAJOR)
list(GET XK_VERSION_PARTS 1 XK_SPEEX_MINOR)
list(GET XK_VERSION_PARTS 2 XK_SPEEX_MICRO)

set(SPEEXDSP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../speexdsp" CACHE PATH "SpeexDSP checkout to build")
if(NOT EXISTS "${SPEEXDSP_DIR}/include/speex/speex_resampler.h")
  message(FATAL_ERROR "SpeexDSP sources are missing at ${SPEEXDSP_DIR}")
endif()

file(READ "${SPEEXDSP_DIR}/configure.ac" XK_DSP_CONFIGURE)
string(REGEX MATCH "AC_INIT\\(\\[speexdsp\\],\\[([0-9]+\\.[0-9]+\\.[0-9]+)\\]" XK_DSP_PROJECT "${XK_DSP_CONFIGURE}")
set(XK_SPEEXDSP_VERSION "${CMAKE_MATCH_1}")
if(NOT XK_SPEEXDSP_VERSION)
  message(FATAL_ERROR "Could not read the SpeexDSP version from ${SPEEXDSP_DIR}/configure.ac")
endif()

configure_file(
  "${CMAKE_CURRENT_SOURCE_DIR}/config.h.in"
  "${CMAKE_CURRENT_BINARY_DIR}/generated/config.h"
)

set(XK_SOURCES
  "${SPEEX_DIR}/libspeex/cb_search.c"
  "${SPEEX_DIR}/libspeex/exc_10_32_table.c"
  "${SPEEX_DIR}/libspeex/exc_8_128_table.c"
  "${SPEEX_DIR}/libspeex/filters.c"
  "${SPEEX_DIR}/libspeex/gain_table.c"
  "${SPEEX_DIR}/libspeex/hexc_table.c"
  "${SPEEX_DIR}/libspeex/high_lsp_tables.c"
  "${SPEEX_DIR}/libspeex/lsp.c"
  "${SPEEX_DIR}/libspeex/ltp.c"
  "${SPEEX_DIR}/libspeex/speex.c"
  "${SPEEX_DIR}/libspeex/stereo.c"
  "${SPEEX_DIR}/libspeex/vbr.c"
  "${SPEEX_DIR}/libspeex/vq.c"
  "${SPEEX_DIR}/libspeex/bits.c"
  "${SPEEX_DIR}/libspeex/exc_10_16_table.c"
  "${SPEEX_DIR}/libspeex/exc_20_32_table.c"
  "${SPEEX_DIR}/libspeex/exc_5_256_table.c"
  "${SPEEX_DIR}/libspeex/exc_5_64_table.c"
  "${SPEEX_DIR}/libspeex/gain_table_lbr.c"
  "${SPEEX_DIR}/libspeex/hexc_10_32_table.c"
  "${SPEEX_DIR}/libspeex/lpc.c"
  "${SPEEX_DIR}/libspeex/lsp_tables_nb.c"
  "${SPEEX_DIR}/libspeex/modes.c"
  "${SPEEX_DIR}/libspeex/modes_wb.c"
  "${SPEEX_DIR}/libspeex/nb_celp.c"
  "${SPEEX_DIR}/libspeex/quant_lsp.c"
  "${SPEEX_DIR}/libspeex/sb_celp.c"
  "${SPEEX_DIR}/libspeex/speex_callbacks.c"
  "${SPEEX_DIR}/libspeex/speex_header.c"
  "${SPEEX_DIR}/libspeex/window.c"
  "${CMAKE_CURRENT_SOURCE_DIR}/speex_kmp.c"
  # SpeexDSP. Its smallft.c provides spx_drft_* for fftwrap.c/preprocess.c;
  # libspeex's copy is not pulled by any compiled codec source (vorbis_psy is
  # disabled), so only this one is linked to avoid duplicate symbols.
  "${SPEEXDSP_DIR}/libspeexdsp/preprocess.c"
  "${SPEEXDSP_DIR}/libspeexdsp/jitter.c"
  "${SPEEXDSP_DIR}/libspeexdsp/mdf.c"
  "${SPEEXDSP_DIR}/libspeexdsp/fftwrap.c"
  "${SPEEXDSP_DIR}/libspeexdsp/filterbank.c"
  "${SPEEXDSP_DIR}/libspeexdsp/resample.c"
  "${SPEEXDSP_DIR}/libspeexdsp/buffer.c"
  "${SPEEXDSP_DIR}/libspeexdsp/scal.c"
  "${SPEEXDSP_DIR}/libspeexdsp/smallft.c"
  "${CMAKE_CURRENT_SOURCE_DIR}/speexdsp_kmp.c"
)

add_library(speex-static STATIC ${XK_SOURCES})
target_include_directories(speex-static
  PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/include"
  PRIVATE
    "${SPEEX_DIR}"
    "${SPEEX_DIR}/include"
    "${SPEEXDSP_DIR}"
    "${SPEEXDSP_DIR}/include"
    "${CMAKE_CURRENT_BINARY_DIR}/generated"
)
target_compile_definitions(speex-static PRIVATE HAVE_CONFIG_H=1)
if(CMAKE_C_COMPILER_ID MATCHES "GNU|Clang")
  # Speex's C code predates modern strict-aliasing rules. GCC 15 (the MinGW
  # toolchain on the Windows runner) was observed to miscompile the
  # preprocessor into a heap-corrupting write; keep the old C contract.
  target_compile_options(speex-static PRIVATE
    -Wno-unused-function
    -fno-strict-aliasing
  )
  if(CMAKE_C_COMPILER_ID STREQUAL "GNU")
    target_compile_options(speex-static PRIVATE -fno-aggressive-loop-optimizations)
  endif()
endif()

# The archive name is what the cinterop `-staticLibrary` flag and the JNI link
# line expect: libspeex_kmp.a on Unix/MinGW.
set_target_properties(speex-static PROPERTIES OUTPUT_NAME speex_kmp)

if(UNIX)
  find_library(XK_MATH_LIBRARY m)
  if(XK_MATH_LIBRARY)
    target_link_libraries(speex-static PUBLIC "${XK_MATH_LIBRARY}")
  endif()
elseif(WIN32)
  target_link_libraries(speex-static PUBLIC m)
endif()

if(BUILD_JNI)
  add_library(speex_jni SHARED
    "${CMAKE_CURRENT_SOURCE_DIR}/speex_jni.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/speexdsp_jni.c"
  )
  target_link_libraries(speex_jni PRIVATE speex-static)
  set_target_properties(speex_jni PROPERTIES OUTPUT_NAME speex_jni)

  if(WIN32)
    # MinGW names shared libraries lib*.dll by default; the Kotlin loader uses
    # the unprefixed speex_jni.dll. The library is pure C: only the MinGW
    # runtime itself has to be linked statically.
    set_target_properties(speex_jni PROPERTIES PREFIX "")
    target_link_options(speex_jni PRIVATE -static -static-libgcc)
  endif()

  if(ANDROID)
    # 16 KB page-size devices (Android 15+).
    target_link_options(speex_jni PRIVATE
      "-Wl,-z,max-page-size=16384"
      "-Wl,-z,common-page-size=16384"
    )
  else()
    # Derive the headers from JAVA_HOME first: FindJNI refuses to configure
    # when the target platform differs from the host (cross builds for MinGW).
    if(NOT JNI_INCLUDE_DIR AND DEFINED ENV{JAVA_HOME})
      set(JNI_INCLUDE_DIR "$ENV{JAVA_HOME}/include")
      if(WIN32)
        set(JNI_INCLUDE_DIR_PLATFORM "$ENV{JAVA_HOME}/include/win32")
      elseif(APPLE)
        set(JNI_INCLUDE_DIR_PLATFORM "$ENV{JAVA_HOME}/include/darwin")
      else()
        set(JNI_INCLUDE_DIR_PLATFORM "$ENV{JAVA_HOME}/include/linux")
      endif()
    endif()
    if(NOT JNI_INCLUDE_DIR)
      find_package(JNI REQUIRED)
    endif()
    if(JNI_INCLUDE_DIR)
      target_include_directories(speex_jni PRIVATE "${JNI_INCLUDE_DIR}")
    endif()
    if(JNI_INCLUDE_DIR_PLATFORM)
      target_include_directories(speex_jni PRIVATE "${JNI_INCLUDE_DIR_PLATFORM}")
    endif()
    if(JNI_INCLUDE_DIRS)
      target_include_directories(speex_jni PRIVATE ${JNI_INCLUDE_DIRS})
    endif()
  endif()
endif()

if(BUILD_WASM OR EMSCRIPTEN OR CMAKE_SYSTEM_NAME STREQUAL "Emscripten")
  add_executable(speex_wasm
    "${CMAKE_CURRENT_SOURCE_DIR}/speex_wasm_entry.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/speexdsp_wasm_entry.c"
  )
  target_link_libraries(speex_wasm PRIVATE speex-static)
  set_target_properties(speex_wasm PROPERTIES SUFFIX ".wasm")
  target_link_options(speex_wasm PRIVATE
    "-sSTANDALONE_WASM=1"
    "-sALLOW_MEMORY_GROWTH=1"
    "-sINITIAL_MEMORY=33554432"
    # Speex's floating-point CELP routines use sizeable stack frames.
    "-sSTACK_SIZE=8388608"
    "-sERROR_ON_UNDEFINED_SYMBOLS=1"
    "-sEXPORTED_FUNCTIONS=_xk_version_string,_xk_version_major,_xk_version_minor,_xk_version_micro,_xk_version_extra,_xk_mode_frame_size,_xk_mode_submode_bits,_xk_mode_name,_xk_bits_create,_xk_bits_free,_xk_bits_reset,_xk_bits_rewind,_xk_bits_read_from,_xk_bits_read_whole_bytes,_xk_bits_pack,_xk_bits_unpack_signed,_xk_bits_unpack_unsigned,_xk_bits_peek,_xk_bits_peek_unsigned,_xk_bits_advance,_xk_bits_remaining,_xk_bits_nbytes,_xk_bits_insert_terminator,_xk_bits_write,_xk_bits_data,_xk_encoder_create,_xk_encoder_free,_xk_encoder_frame_size,_xk_encoder_encode,_xk_encoder_encode_stereo,_xk_encoder_packet,_xk_encoder_ctl,_xk_encoder_get,_xk_encoder_set_vbr_quality,_xk_encoder_get_vbr_quality,_xk_encoder_get_relative_quality,_xk_decoder_create,_xk_decoder_free,_xk_decoder_frame_size,_xk_decoder_decode,_xk_decoder_decode_stereo,_xk_decoder_samples,_xk_decoder_ctl,_xk_decoder_get,_xk_stereo_create,_xk_stereo_free,_xk_stereo_reset,_xk_stereo_encode,_xk_stereo_decode,_xk_header_create,_xk_header_parse,_xk_header_free,_xk_header_get_int,_xk_header_set_int,_xk_header_string,_xk_header_packet,_xk_alloc,_xk_free,_malloc,_free,_xdsp_version_string,_xdsp_resampler_create,_xdsp_resampler_create_frac,_xdsp_resampler_error,_xdsp_resampler_free,_xdsp_resampler_channels,_xdsp_resampler_set_rate,_xdsp_resampler_set_rate_frac,_xdsp_resampler_input_rate,_xdsp_resampler_output_rate,_xdsp_resampler_ratio_num,_xdsp_resampler_ratio_den,_xdsp_resampler_set_quality,_xdsp_resampler_quality,_xdsp_resampler_set_input_stride,_xdsp_resampler_input_stride,_xdsp_resampler_set_output_stride,_xdsp_resampler_output_stride,_xdsp_resampler_input_latency,_xdsp_resampler_output_latency,_xdsp_resampler_skip_zeros,_xdsp_resampler_reset_mem,_xdsp_resampler_process_interleaved,_xdsp_resampler_output,_xdsp_resampler_strerror,_xdsp_preprocess_create,_xdsp_preprocess_free,_xdsp_preprocess_frame_size,_xdsp_preprocess_run,_xdsp_preprocess_estimate_update,_xdsp_preprocess_ctl_int,_xdsp_preprocess_get_int,_xdsp_preprocess_ctl_float,_xdsp_preprocess_get_float,_xdsp_preprocess_set_echo,_xdsp_preprocess_psd_size,_xdsp_preprocess_psd,_xdsp_preprocess_noise_psd_size,_xdsp_preprocess_noise_psd,_xdsp_echo_create,_xdsp_echo_free,_xdsp_echo_reset,_xdsp_echo_frame_size,_xdsp_echo_microphones,_xdsp_echo_speakers,_xdsp_echo_cancellation,_xdsp_echo_capture,_xdsp_echo_playback,_xdsp_echo_output,_xdsp_echo_ctl_int,_xdsp_echo_get_int,_xdsp_echo_impulse_response_size,_xdsp_echo_impulse_response,_xdsp_decorr_create,_xdsp_decorr_free,_xdsp_decorr_channels,_xdsp_decorr_frame_size,_xdsp_decorr_process,_xdsp_decorr_output,_xdsp_jitter_create,_xdsp_jitter_free,_xdsp_jitter_reset,_xdsp_jitter_put,_xdsp_jitter_get,_xdsp_jitter_get_another,_xdsp_jitter_packet,_xdsp_jitter_timestamp,_xdsp_jitter_span,_xdsp_jitter_sequence,_xdsp_jitter_user_data,_xdsp_jitter_pointer_timestamp,_xdsp_jitter_tick,_xdsp_jitter_remaining_span,_xdsp_jitter_ctl_int,_xdsp_jitter_get_int,_xdsp_buffer_create,_xdsp_buffer_free,_xdsp_buffer_write,_xdsp_buffer_write_zeros,_xdsp_buffer_read,_xdsp_buffer_data,_xdsp_buffer_available,_xdsp_buffer_resize"
    "--no-entry"
  )
endif()
