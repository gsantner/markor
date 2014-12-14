/* markdown.h - generic markdown parser */

#ifndef HOEDOWN_MARKDOWN_H
#define HOEDOWN_MARKDOWN_H

#include "buffer.h"
#include "autolink.h"

#ifdef __cplusplus
extern "C" {
#endif

#define HOEDOWN_VERSION "2.0.0"
#define HOEDOWN_VERSION_MAJOR 2
#define HOEDOWN_VERSION_MINOR 0
#define HOEDOWN_VERSION_REVISION 0

/********************
 * TYPE DEFINITIONS *
 ********************/

/* hoedown_autolink - type of autolink */
enum hoedown_autolink {
	HOEDOWN_AUTOLINK_NONE,		/* used internally when it is not an autolink*/
	HOEDOWN_AUTOLINK_NORMAL,	/* normal http/http/ftp/mailto/etc link */
	HOEDOWN_AUTOLINK_EMAIL		/* e-mail link without explit mailto: */
};

enum hoedown_tableflags {
	HOEDOWN_TABLE_ALIGN_L = 1,
	HOEDOWN_TABLE_ALIGN_R = 2,
	HOEDOWN_TABLE_ALIGN_CENTER = 3,
	HOEDOWN_TABLE_ALIGNMASK = 3,
	HOEDOWN_TABLE_HEADER = 4
};

enum hoedown_extensions {
	HOEDOWN_EXT_NO_INTRA_EMPHASIS = (1 << 0),
	HOEDOWN_EXT_TABLES = (1 << 1),
	HOEDOWN_EXT_FENCED_CODE = (1 << 2),
	HOEDOWN_EXT_AUTOLINK = (1 << 3),
	HOEDOWN_EXT_STRIKETHROUGH = (1 << 4),
	HOEDOWN_EXT_UNDERLINE = (1 << 5),
	HOEDOWN_EXT_SPACE_HEADERS = (1 << 6),
	HOEDOWN_EXT_SUPERSCRIPT = (1 << 7),
	HOEDOWN_EXT_LAX_SPACING = (1 << 8),
	HOEDOWN_EXT_DISABLE_INDENTED_CODE = (1 << 9),
	HOEDOWN_EXT_HIGHLIGHT = (1 << 10),
	HOEDOWN_EXT_FOOTNOTES = (1 << 11),
	HOEDOWN_EXT_QUOTE = (1 << 12)
};

/* hoedown_renderer - functions for rendering parsed data */
struct hoedown_renderer {
	/* block level callbacks - NULL skips the block */
	void (*blockcode)(hoedown_buffer *ob, const hoedown_buffer *text, const hoedown_buffer *lang, void *opaque);
	void (*blockquote)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	void (*blockhtml)(hoedown_buffer *ob,const  hoedown_buffer *text, void *opaque);
	void (*header)(hoedown_buffer *ob, const hoedown_buffer *text, int level, void *opaque);
	void (*hrule)(hoedown_buffer *ob, void *opaque);
	void (*list)(hoedown_buffer *ob, const hoedown_buffer *text, int flags, void *opaque);
	void (*listitem)(hoedown_buffer *ob, const hoedown_buffer *text, int flags, void *opaque);
	void (*paragraph)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	void (*table)(hoedown_buffer *ob, const hoedown_buffer *header, const hoedown_buffer *body, void *opaque);
	void (*table_row)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	void (*table_cell)(hoedown_buffer *ob, const hoedown_buffer *text, int flags, void *opaque);
	void (*footnotes)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	void (*footnote_def)(hoedown_buffer *ob, const hoedown_buffer *text, unsigned int num, void *opaque);

	/* span level callbacks - NULL or return 0 prints the span verbatim */
	int (*autolink)(hoedown_buffer *ob, const hoedown_buffer *link, enum hoedown_autolink type, void *opaque);
	int (*codespan)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*double_emphasis)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*emphasis)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*underline)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*highlight)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*quote)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*image)(hoedown_buffer *ob, const hoedown_buffer *link, const hoedown_buffer *title, const hoedown_buffer *alt, void *opaque);
	int (*linebreak)(hoedown_buffer *ob, void *opaque);
	int (*link)(hoedown_buffer *ob, const hoedown_buffer *link, const hoedown_buffer *title, const hoedown_buffer *content, void *opaque);
	int (*raw_html_tag)(hoedown_buffer *ob, const hoedown_buffer *tag, void *opaque);
	int (*triple_emphasis)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*strikethrough)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*superscript)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);
	int (*footnote_ref)(hoedown_buffer *ob, unsigned int num, void *opaque);

	/* low level callbacks - NULL copies input directly into the output */
	void (*entity)(hoedown_buffer *ob, const hoedown_buffer *entity, void *opaque);
	void (*normal_text)(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque);

	/* header and footer */
	void (*doc_header)(hoedown_buffer *ob, void *opaque);
	void (*doc_footer)(hoedown_buffer *ob, void *opaque);

	/* state object */
	void *opaque;
};

typedef struct hoedown_renderer hoedown_renderer;

struct hoedown_markdown;

typedef struct hoedown_markdown hoedown_markdown;

/*********
 * FLAGS *
 *********/

/* list/listitem flags */
#define HOEDOWN_LIST_ORDERED	1
#define HOEDOWN_LI_BLOCK		2	/* <li> containing block data */

/**********************
 * EXPORTED FUNCTIONS *
 **********************/

extern hoedown_markdown *
hoedown_markdown_new(
	unsigned int extensions,
	size_t max_nesting,
	const hoedown_renderer *renderer);

extern void
hoedown_markdown_render(hoedown_buffer *ob, const uint8_t *document, size_t doc_size, hoedown_markdown *md);

extern void
hoedown_markdown_free(hoedown_markdown *md);

extern void
hoedown_version(int *major, int *minor, int *revision);

#ifdef __cplusplus
}
#endif

#endif /** HOEDOWN_MARKDOWN_H **/
