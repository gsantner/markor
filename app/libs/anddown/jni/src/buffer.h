/* buffer.h - simple, fast buffers */

#ifndef HOEDOWN_BUFFER_H
#define HOEDOWN_BUFFER_H

#include <stddef.h>
#include <stdarg.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#if defined(_MSC_VER)
#define __attribute__(x)
#define inline __inline
#endif

typedef enum {
	HOEDOWN_BUF_OK = 0,
	HOEDOWN_BUF_ENOMEM = -1
} hoedown_buferror_t;

/* hoedown_buffer: character array buffer */
struct hoedown_buffer {
	uint8_t *data;	/* actual character data */
	size_t size;	/* size of the string */
	size_t asize;	/* allocated size (0 = volatile buffer) */
	size_t unit;	/* reallocation unit size (0 = read-only buffer) */
};

typedef struct hoedown_buffer hoedown_buffer;

/* HOEDOWN_BUFPUTSL: optimized hoedown_buffer_puts of a string literal */
#define HOEDOWN_BUFPUTSL(output, literal) \
	hoedown_buffer_put(output, literal, sizeof(literal) - 1)

/* hoedown_buffer_new: allocation of a new buffer */
hoedown_buffer *hoedown_buffer_new(size_t unit) __attribute__ ((malloc));

/* hoedown_buffer_free: decrease the reference count and free the buffer if needed */
void hoedown_buffer_free(hoedown_buffer *buf);

/* hoedown_buffer_reset: frees internal data of the buffer */
void hoedown_buffer_reset(hoedown_buffer *buf);

/* hoedown_buffer_grow: increasing the allocated size to the given value */
int hoedown_buffer_grow(hoedown_buffer *buf, size_t neosz);

/* hoedown_buffer_put: appends raw data to a buffer */
void hoedown_buffer_put(hoedown_buffer *buf, const void *data, size_t len);

/* hoedown_buffer_puts: appends a NUL-terminated string to a buffer */
void hoedown_buffer_puts(hoedown_buffer *buf, const char *str);

/* hoedown_buffer_putc: appends a single char to a buffer */
void hoedown_buffer_putc(hoedown_buffer *buf, uint8_t c);

/* hoedown_buffer_prefix: compare the beginning of a buffer with a string */
int hoedown_buffer_prefix(const hoedown_buffer *buf, const char *prefix);

/* hoedown_buffer_slurp: removes a given number of bytes from the head of the array */
void hoedown_buffer_slurp(hoedown_buffer *buf, size_t len);

/* hoedown_buffer_cstr: NUL-termination of the string array (making a C-string) */
const char *hoedown_buffer_cstr(hoedown_buffer *buf);

/* hoedown_buffer_printf: formatted printing to a buffer */
void hoedown_buffer_printf(hoedown_buffer *buf, const char *fmt, ...) __attribute__ ((format (printf, 2, 3)));

#ifdef __cplusplus
}
#endif

#endif /** HOEDOWN_BUFFER_H **/
