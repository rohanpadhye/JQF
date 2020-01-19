#ifndef PROXY_H
#define PROXY_H

/*
 Collection of definitions taken
 from AFL's config.h and types.h.

 These definitions are duplicated here so
 that JQF would not need to depend on AFL
 being locally installed with header files, et al.
*/

#include <stdint.h>
#include <stdlib.h>

typedef uint8_t  u8;
typedef uint16_t u16;
typedef uint32_t u32;

#define MAP_SIZE  (1 << 16)
#define PERF_SIZE (1 << 14)

#define SHM_ENV_VAR    "__AFL_SHM_ID"
#define FORKSRV_FD      198

#endif // PROXY_H
