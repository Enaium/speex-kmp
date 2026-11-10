/*
 * Fixed-width Speex types for the platforms that use the configure-style
 * speex_config_types.h instead of the built-in Windows/Apple typedefs.
 * Kept outside the submodule so the checkout stays pristine.
 */
#ifndef __SPEEX_TYPES_H__
#define __SPEEX_TYPES_H__

#include <stdint.h>

typedef int16_t spx_int16_t;
typedef uint16_t spx_uint16_t;
typedef int32_t spx_int32_t;
typedef uint32_t spx_uint32_t;

#endif
