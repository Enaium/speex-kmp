/*
 * WebAssembly entry point.
 *
 * The exported functions themselves live in speex_kmp.c. This translation unit
 * takes their addresses so the linker definitely pulls that object out of the
 * static archive; without a reference the archive member would be dropped and
 * the exports listed in CMakeLists.txt would be missing.
 */
#include "speex_kmp.h"

typedef void (*xk_wasm_export)(void);

static const xk_wasm_export xk_wasm_exports[] = {
    (xk_wasm_export)xk_version_string,
    (xk_wasm_export)xk_version_major,
    (xk_wasm_export)xk_version_minor,
    (xk_wasm_export)xk_version_micro,
    (xk_wasm_export)xk_version_extra,
    (xk_wasm_export)xk_mode_frame_size,
    (xk_wasm_export)xk_mode_submode_bits,
    (xk_wasm_export)xk_mode_name,
    (xk_wasm_export)xk_bits_create,
    (xk_wasm_export)xk_bits_free,
    (xk_wasm_export)xk_bits_reset,
    (xk_wasm_export)xk_bits_rewind,
    (xk_wasm_export)xk_bits_read_from,
    (xk_wasm_export)xk_bits_read_whole_bytes,
    (xk_wasm_export)xk_bits_pack,
    (xk_wasm_export)xk_bits_unpack_signed,
    (xk_wasm_export)xk_bits_unpack_unsigned,
    (xk_wasm_export)xk_bits_peek,
    (xk_wasm_export)xk_bits_peek_unsigned,
    (xk_wasm_export)xk_bits_advance,
    (xk_wasm_export)xk_bits_remaining,
    (xk_wasm_export)xk_bits_nbytes,
    (xk_wasm_export)xk_bits_insert_terminator,
    (xk_wasm_export)xk_bits_write,
    (xk_wasm_export)xk_bits_data,
    (xk_wasm_export)xk_encoder_create,
    (xk_wasm_export)xk_encoder_free,
    (xk_wasm_export)xk_encoder_frame_size,
    (xk_wasm_export)xk_encoder_encode,
    (xk_wasm_export)xk_encoder_encode_stereo,
    (xk_wasm_export)xk_encoder_packet,
    (xk_wasm_export)xk_encoder_ctl,
    (xk_wasm_export)xk_encoder_get,
    (xk_wasm_export)xk_encoder_set_vbr_quality,
    (xk_wasm_export)xk_encoder_get_vbr_quality,
    (xk_wasm_export)xk_encoder_get_relative_quality,
    (xk_wasm_export)xk_decoder_create,
    (xk_wasm_export)xk_decoder_free,
    (xk_wasm_export)xk_decoder_frame_size,
    (xk_wasm_export)xk_decoder_decode,
    (xk_wasm_export)xk_decoder_decode_stereo,
    (xk_wasm_export)xk_decoder_samples,
    (xk_wasm_export)xk_decoder_ctl,
    (xk_wasm_export)xk_decoder_get,
    (xk_wasm_export)xk_stereo_create,
    (xk_wasm_export)xk_stereo_free,
    (xk_wasm_export)xk_stereo_reset,
    (xk_wasm_export)xk_stereo_encode,
    (xk_wasm_export)xk_stereo_decode,
    (xk_wasm_export)xk_header_create,
    (xk_wasm_export)xk_header_parse,
    (xk_wasm_export)xk_header_free,
    (xk_wasm_export)xk_header_get_int,
    (xk_wasm_export)xk_header_set_int,
    (xk_wasm_export)xk_header_string,
    (xk_wasm_export)xk_header_packet,
    (xk_wasm_export)xk_alloc,
    (xk_wasm_export)xk_free,
};

/* Keep the table observable so no optimiser removes it. */
const xk_wasm_export *xk_wasm_keep_exports(void);

const xk_wasm_export *xk_wasm_keep_exports(void) { return xk_wasm_exports; }
