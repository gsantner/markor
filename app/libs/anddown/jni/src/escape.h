/* escape.h - escape utilities */

#ifndef HOEDOWN_ESCAPE_H
#define HOEDOWN_ESCAPE_H

#include "buffer.h"

#ifdef __cplusplus
extern "C" {
#endif

extern void hoedown_escape_html(hoedown_buffer *ob, const uint8_t *src, size_t size, int secure);
extern void hoedown_escape_href(hoedown_buffer *ob, const uint8_t *src, size_t size);

#ifdef __cplusplus
}
#endif

#endif /** HOEDOWN_ESCAPE_H **/
