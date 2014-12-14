/* html.h - HTML renderer */

#ifndef HOEDOWN_HTML_H
#define HOEDOWN_HTML_H

#include "markdown.h"
#include "buffer.h"
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
	HOEDOWN_HTML_SKIP_HTML = (1 << 0),
	HOEDOWN_HTML_SKIP_STYLE = (1 << 1),
	HOEDOWN_HTML_SKIP_IMAGES = (1 << 2),
	HOEDOWN_HTML_SKIP_LINKS = (1 << 3),
	HOEDOWN_HTML_EXPAND_TABS = (1 << 4),
	HOEDOWN_HTML_SAFELINK = (1 << 5),
	HOEDOWN_HTML_TOC = (1 << 6),
	HOEDOWN_HTML_HARD_WRAP = (1 << 7),
	HOEDOWN_HTML_USE_XHTML = (1 << 8),
	HOEDOWN_HTML_ESCAPE = (1 << 9),
	HOEDOWN_HTML_PRETTIFY = (1 << 10)
} hoedown_html_render_mode;

typedef enum {
	HOEDOWN_HTML_TAG_NONE = 0,
	HOEDOWN_HTML_TAG_OPEN,
	HOEDOWN_HTML_TAG_CLOSE
} hoedown_html_tag;

int
hoedown_html_is_tag(const uint8_t *tag_data, size_t tag_size, const char *tagname);

extern hoedown_renderer *
hoedown_html_renderer_new(unsigned int render_flags, int nesting_level);

extern hoedown_renderer *
hoedown_html_toc_renderer_new(int nesting_level);

extern void
hoedown_html_renderer_free(hoedown_renderer *renderer);

extern void
hoedown_html_smartypants(hoedown_buffer *ob, const uint8_t *text, size_t size);

#ifdef __cplusplus
}
#endif

#endif /** HOEDOWN_HTML_H **/
