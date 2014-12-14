#include "buffer.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#define BUFFER_MAX_ALLOC_SIZE (1024 * 1024 * 16) /* 16mb */

/* hoedown_buffer_new: allocation of a new buffer */
hoedown_buffer *
hoedown_buffer_new(size_t unit)
{
	hoedown_buffer *ret;
	ret = malloc(sizeof (hoedown_buffer));

	if (ret) {
		ret->data = 0;
		ret->size = ret->asize = 0;
		ret->unit = unit;
	}
	return ret;
}

/* hoedown_buffer_free: decrease the reference count and free the buffer if needed */
void
hoedown_buffer_free(hoedown_buffer *buf)
{
	if (!buf)
		return;

	free(buf->data);
	free(buf);
}

/* hoedown_buffer_reset: frees internal data of the buffer */
void
hoedown_buffer_reset(hoedown_buffer *buf)
{
	if (!buf)
		return;

	free(buf->data);
	buf->data = NULL;
	buf->size = buf->asize = 0;
}

/* hoedown_buffer_grow: increasing the allocated size to the given value */
int
hoedown_buffer_grow(hoedown_buffer *buf, size_t neosz)
{
	size_t neoasz;
	void *neodata;

	assert(buf && buf->unit);

	if (neosz > BUFFER_MAX_ALLOC_SIZE)
		return HOEDOWN_BUF_ENOMEM;

	if (buf->asize >= neosz)
		return HOEDOWN_BUF_OK;

	neoasz = buf->asize + buf->unit;
	while (neoasz < neosz)
		neoasz += buf->unit;

	neodata = realloc(buf->data, neoasz);
	if (!neodata)
		return HOEDOWN_BUF_ENOMEM;

	buf->data = neodata;
	buf->asize = neoasz;
	return HOEDOWN_BUF_OK;
}

/* hoedown_buffer_put: appends raw data to a buffer */
void
hoedown_buffer_put(hoedown_buffer *buf, const void *data, size_t len)
{
	assert(buf && buf->unit);

	if (buf->size + len > buf->asize && hoedown_buffer_grow(buf, buf->size + len) < 0)
		return;

	memcpy(buf->data + buf->size, data, len);
	buf->size += len;
}

/* hoedown_buffer_puts: appends a NUL-terminated string to a buffer */
void
hoedown_buffer_puts(hoedown_buffer *buf, const char *str)
{
	hoedown_buffer_put(buf, str, strlen(str));
}


/* hoedown_buffer_putc: appends a single uint8_t to a buffer */
void
hoedown_buffer_putc(hoedown_buffer *buf, uint8_t c)
{
	assert(buf && buf->unit);

	if (buf->size + 1 > buf->asize && hoedown_buffer_grow(buf, buf->size + 1) < 0)
		return;

	buf->data[buf->size] = c;
	buf->size += 1;
}

int
hoedown_buffer_prefix(const hoedown_buffer *buf, const char *prefix)
{
	size_t i;
	assert(buf && buf->unit);

	for (i = 0; i < buf->size; ++i) {
		if (prefix[i] == 0)
			return 0;

		if (buf->data[i] != prefix[i])
			return buf->data[i] - prefix[i];
	}

	return 0;
}

/* hoedown_buffer_slurp: removes a given number of bytes from the head of the array */
void
hoedown_buffer_slurp(hoedown_buffer *buf, size_t len)
{
	assert(buf && buf->unit);

	if (len >= buf->size) {
		buf->size = 0;
		return;
	}

	buf->size -= len;
	memmove(buf->data, buf->data + len, buf->size);
}

/* hoedown_buffer_cstr: NULL-termination of the string array */
const char *
hoedown_buffer_cstr(hoedown_buffer *buf)
{
	assert(buf && buf->unit);

	if (buf->size < buf->asize && buf->data[buf->size] == 0)
		return (char *)buf->data;

	if (buf->size + 1 <= buf->asize || hoedown_buffer_grow(buf, buf->size + 1) == 0) {
		buf->data[buf->size] = 0;
		return (char *)buf->data;
	}

	return NULL;
}

/* hoedown_buffer_printf: formatted printing to a buffer */
void
hoedown_buffer_printf(hoedown_buffer *buf, const char *fmt, ...)
{
	va_list ap;
	int n;

	assert(buf && buf->unit);

	if (buf->size >= buf->asize && hoedown_buffer_grow(buf, buf->size + 1) < 0)
		return;
	
	va_start(ap, fmt);
	n = vsnprintf((char *)buf->data + buf->size, buf->asize - buf->size, fmt, ap);
	va_end(ap);

	if (n < 0) {
#ifndef _MSC_VER
		return;
#else
		va_start(ap, fmt);
		n = _vscprintf(fmt, ap);
		va_end(ap);
#endif
	}

	if ((size_t)n >= buf->asize - buf->size) {
		if (hoedown_buffer_grow(buf, buf->size + n + 1) < 0)
			return;

		va_start(ap, fmt);
		n = vsnprintf((char *)buf->data + buf->size, buf->asize - buf->size, fmt, ap);
		va_end(ap);
	}

	if (n < 0)
		return;

	buf->size += n;
}
