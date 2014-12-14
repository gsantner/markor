/* autolink.h - versatile autolinker */

#ifndef HOEDOWN_AUTOLINK_H
#define HOEDOWN_AUTOLINK_H

#include "buffer.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
	HOEDOWN_AUTOLINK_SHORT_DOMAINS = (1 << 0)
};

int
hoedown_autolink_is_safe(const uint8_t *link, size_t link_len);

size_t
hoedown_autolink__www(size_t *rewind_p, hoedown_buffer *link,
	uint8_t *data, size_t offset, size_t size, unsigned int flags);

size_t
hoedown_autolink__email(size_t *rewind_p, hoedown_buffer *link,
	uint8_t *data, size_t offset, size_t size, unsigned int flags);

size_t
hoedown_autolink__url(size_t *rewind_p, hoedown_buffer *link,
	uint8_t *data, size_t offset, size_t size, unsigned int flags);

#ifdef __cplusplus
}
#endif

#endif /** HOEDOWN_AUTOLINK_H **/
