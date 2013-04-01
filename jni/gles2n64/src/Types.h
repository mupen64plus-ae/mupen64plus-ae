#ifndef TYPES_H
#define TYPES_H

#include <stdint.h>

typedef uint8_t   u8;  /* unsigned  8-bit */
typedef uint16_t  u16; /* unsigned 16-bit */
typedef uint32_t  u32; /* unsigned 32-bit */
typedef uint64_t  u64; /* unsigned 64-bit */

typedef int8_t   s8;  /* signed  8-bit */
typedef int16_t  s16; /* signed 16-bit */
typedef int32_t  s32; /* signed 32-bit */
typedef int64_t  s64; /* signed 64-bit */

typedef volatile uint8_t   vu8;    /* unsigned  8-bit */
typedef volatile uint16_t  vu16;   /* unsigned 16-bit */
typedef volatile uint32_t  vu32;   /* unsigned 32-bit */
typedef volatile uint64_t  vu64;   /* unsigned 64-bit */

typedef volatile int8_t    vs8;    /* signed  8-bit */
typedef volatile int16_t   vs16;   /* signed 16-bit */
typedef volatile int32_t   vs32;   /* signed 32-bit */
typedef volatile int64_t   vs64;   /* signed 64-bit */

typedef float              f32;    /* single prec floating point */
typedef double             f64;    /* double prec floating point */

#ifndef TRUE
#define TRUE    1
#endif

#ifndef FALSE
#define FALSE   0
#endif

#ifndef NULL
#define NULL    0
#endif

#endif // TYPES_H

