#include "html.h"

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>

#include "escape.h"

#define USE_XHTML(opt) (opt->flags & HOEDOWN_HTML_USE_XHTML)

struct rndr_state {
	struct {
		int header_count;
		int current_level;
		int level_offset;
		int nesting_level;
	} toc_data;

	unsigned int flags;

	/* extra callbacks */
	void (*link_attributes)(hoedown_buffer *ob, const hoedown_buffer *url, void *self);
};

typedef struct rndr_state rndr_state;

int
hoedown_html_is_tag(const uint8_t *tag_data, size_t tag_size, const char *tagname)
{
	size_t i;
	int closed = 0;

	if (tag_size < 3 || tag_data[0] != '<')
		return HOEDOWN_HTML_TAG_NONE;

	i = 1;

	if (tag_data[i] == '/') {
		closed = 1;
		i++;
	}

	for (; i < tag_size; ++i, ++tagname) {
		if (*tagname == 0)
			break;

		if (tag_data[i] != *tagname)
			return HOEDOWN_HTML_TAG_NONE;
	}

	if (i == tag_size)
		return HOEDOWN_HTML_TAG_NONE;

	if (isspace(tag_data[i]) || tag_data[i] == '>')
		return closed ? HOEDOWN_HTML_TAG_CLOSE : HOEDOWN_HTML_TAG_OPEN;

	return HOEDOWN_HTML_TAG_NONE;
}

static inline void escape_html(hoedown_buffer *ob, const uint8_t *source, size_t length)
{
	hoedown_escape_html(ob, source, length, 0);
}

static inline void escape_href(hoedown_buffer *ob, const uint8_t *source, size_t length)
{
	hoedown_escape_href(ob, source, length);
}

/********************
 * GENERIC RENDERER *
 ********************/
static int
rndr_autolink(hoedown_buffer *ob, const hoedown_buffer *link, enum hoedown_autolink type, void *opaque)
{
	rndr_state *state = opaque;

	if (!link || !link->size)
		return 0;

	if ((state->flags & HOEDOWN_HTML_SAFELINK) != 0 &&
		!hoedown_autolink_is_safe(link->data, link->size) &&
		type != HOEDOWN_AUTOLINK_EMAIL)
		return 0;

	HOEDOWN_BUFPUTSL(ob, "<a href=\"");
	if (type == HOEDOWN_AUTOLINK_EMAIL)
		HOEDOWN_BUFPUTSL(ob, "mailto:");
	escape_href(ob, link->data, link->size);

	if (state->link_attributes) {
		hoedown_buffer_putc(ob, '\"');
		state->link_attributes(ob, link, opaque);
		hoedown_buffer_putc(ob, '>');
	} else {
		HOEDOWN_BUFPUTSL(ob, "\">");
	}

	/*
	 * Pretty printing: if we get an email address as
	 * an actual URI, e.g. `mailto:foo@bar.com`, we don't
	 * want to print the `mailto:` prefix
	 */
	if (hoedown_buffer_prefix(link, "mailto:") == 0) {
		escape_html(ob, link->data + 7, link->size - 7);
	} else {
		escape_html(ob, link->data, link->size);
	}

	HOEDOWN_BUFPUTSL(ob, "</a>");

	return 1;
}

static void
rndr_blockcode(hoedown_buffer *ob, const hoedown_buffer *text, const hoedown_buffer *lang, void *opaque)
{
	rndr_state *state = opaque;

	if (ob->size) hoedown_buffer_putc(ob, '\n');

	if (lang && lang->size) {
		size_t i, cls = 0;
		if (state->flags & HOEDOWN_HTML_PRETTIFY) {
			HOEDOWN_BUFPUTSL(ob, "<pre><code class=\"prettyprint");
			cls++;
		} else {
			HOEDOWN_BUFPUTSL(ob, "<pre><code class=\"");
		}

		for (i = 0; i < lang->size; ++i, ++cls) {
			while (i < lang->size && isspace(lang->data[i]))
				i++;

			if (i < lang->size) {
				size_t org = i;
				while (i < lang->size && !isspace(lang->data[i]))
					i++;

				if (lang->data[org] == '.')
					org++;

				if (cls) hoedown_buffer_putc(ob, ' ');
				escape_html(ob, lang->data + org, i - org);
			}
		}

		HOEDOWN_BUFPUTSL(ob, "\">");
	} else if (state->flags & HOEDOWN_HTML_PRETTIFY) {
		HOEDOWN_BUFPUTSL(ob, "<pre><code class=\"prettyprint\">");
	} else {
		HOEDOWN_BUFPUTSL(ob, "<pre><code>");
	}

	if (text)
		escape_html(ob, text->data, text->size);

	HOEDOWN_BUFPUTSL(ob, "</code></pre>\n");
}

static void
rndr_blockquote(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (ob->size) hoedown_buffer_putc(ob, '\n');
	HOEDOWN_BUFPUTSL(ob, "<blockquote>\n");
	if (text) hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</blockquote>\n");
}

static int
rndr_codespan(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	rndr_state *state = opaque;
	if (state->flags & HOEDOWN_HTML_PRETTIFY)
		HOEDOWN_BUFPUTSL(ob, "<code class=\"prettyprint\">");
	else
		HOEDOWN_BUFPUTSL(ob, "<code>");
	if (text) escape_html(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</code>");
	return 1;
}

static int
rndr_strikethrough(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (!text || !text->size)
		return 0;

	HOEDOWN_BUFPUTSL(ob, "<del>");
	hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</del>");
	return 1;
}

static int
rndr_double_emphasis(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (!text || !text->size)
		return 0;

	HOEDOWN_BUFPUTSL(ob, "<strong>");
	hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</strong>");

	return 1;
}

static int
rndr_emphasis(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (!text || !text->size) return 0;
	HOEDOWN_BUFPUTSL(ob, "<em>");
	if (text) hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</em>");
	return 1;
}

static int
rndr_underline(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (!text || !text->size)
		return 0;

	HOEDOWN_BUFPUTSL(ob, "<u>");
	hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</u>");

	return 1;
}

static int
rndr_highlight(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (!text || !text->size)
		return 0;

	HOEDOWN_BUFPUTSL(ob, "<mark>");
	hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</mark>");

	return 1;
}

static int
rndr_quote(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (!text || !text->size)
		return 0;

	HOEDOWN_BUFPUTSL(ob, "<q>");
	hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</q>");

	return 1;
}

static int
rndr_linebreak(hoedown_buffer *ob, void *opaque)
{
	rndr_state *state = opaque;
	hoedown_buffer_puts(ob, USE_XHTML(state) ? "<br/>\n" : "<br>\n");
	return 1;
}

static void
rndr_header(hoedown_buffer *ob, const hoedown_buffer *text, int level, void *opaque)
{
	rndr_state *state = opaque;

	if (ob->size)
		hoedown_buffer_putc(ob, '\n');

	if ((state->flags & HOEDOWN_HTML_TOC) && (level <= state->toc_data.nesting_level))
		hoedown_buffer_printf(ob, "<h%d id=\"toc_%d\">", level, state->toc_data.header_count++);
	else
		hoedown_buffer_printf(ob, "<h%d>", level);

	if (text) hoedown_buffer_put(ob, text->data, text->size);
	hoedown_buffer_printf(ob, "</h%d>\n", level);
}

static int
rndr_link(hoedown_buffer *ob, const hoedown_buffer *link, const hoedown_buffer *title, const hoedown_buffer *content, void *opaque)
{
	rndr_state *state = opaque;

	if (link != NULL && (state->flags & HOEDOWN_HTML_SAFELINK) != 0 && !hoedown_autolink_is_safe(link->data, link->size))
		return 0;

	HOEDOWN_BUFPUTSL(ob, "<a href=\"");

	if (link && link->size)
		escape_href(ob, link->data, link->size);

	if (title && title->size) {
		HOEDOWN_BUFPUTSL(ob, "\" title=\"");
		escape_html(ob, title->data, title->size);
	}

	if (state->link_attributes) {
		hoedown_buffer_putc(ob, '\"');
		state->link_attributes(ob, link, opaque);
		hoedown_buffer_putc(ob, '>');
	} else {
		HOEDOWN_BUFPUTSL(ob, "\">");
	}

	if (content && content->size) hoedown_buffer_put(ob, content->data, content->size);
	HOEDOWN_BUFPUTSL(ob, "</a>");
	return 1;
}

static void
rndr_list(hoedown_buffer *ob, const hoedown_buffer *text, int flags, void *opaque)
{
	if (ob->size) hoedown_buffer_putc(ob, '\n');
	hoedown_buffer_put(ob, flags & HOEDOWN_LIST_ORDERED ? "<ol>\n" : "<ul>\n", 5);
	if (text) hoedown_buffer_put(ob, text->data, text->size);
	hoedown_buffer_put(ob, flags & HOEDOWN_LIST_ORDERED ? "</ol>\n" : "</ul>\n", 6);
}

static void
rndr_listitem(hoedown_buffer *ob, const hoedown_buffer *text, int flags, void *opaque)
{
	HOEDOWN_BUFPUTSL(ob, "<li>");
	if (text) {
		size_t size = text->size;
		while (size && text->data[size - 1] == '\n')
			size--;

		hoedown_buffer_put(ob, text->data, size);
	}
	HOEDOWN_BUFPUTSL(ob, "</li>\n");
}

static void
rndr_paragraph(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	rndr_state *state = opaque;
	size_t i = 0;

	if (ob->size) hoedown_buffer_putc(ob, '\n');

	if (!text || !text->size)
		return;

	while (i < text->size && isspace(text->data[i])) i++;

	if (i == text->size)
		return;

	HOEDOWN_BUFPUTSL(ob, "<p>");
	if (state->flags & HOEDOWN_HTML_HARD_WRAP) {
		size_t org;
		while (i < text->size) {
			org = i;
			while (i < text->size && text->data[i] != '\n')
				i++;

			if (i > org)
				hoedown_buffer_put(ob, text->data + org, i - org);

			/*
			 * do not insert a line break if this newline
			 * is the last character on the paragraph
			 */
			if (i >= text->size - 1)
				break;

			rndr_linebreak(ob, opaque);
			i++;
		}
	} else {
		hoedown_buffer_put(ob, &text->data[i], text->size - i);
	}
	HOEDOWN_BUFPUTSL(ob, "</p>\n");
}

static void
rndr_raw_block(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	size_t org, sz;
	if (!text) return;
	sz = text->size;
	while (sz > 0 && text->data[sz - 1] == '\n') sz--;
	org = 0;
	while (org < sz && text->data[org] == '\n') org++;
	if (org >= sz) return;
	if (ob->size) hoedown_buffer_putc(ob, '\n');
	hoedown_buffer_put(ob, text->data + org, sz - org);
	hoedown_buffer_putc(ob, '\n');
}

static int
rndr_triple_emphasis(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (!text || !text->size) return 0;
	HOEDOWN_BUFPUTSL(ob, "<strong><em>");
	hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</em></strong>");
	return 1;
}

static void
rndr_hrule(hoedown_buffer *ob, void *opaque)
{
	rndr_state *state = opaque;
	if (ob->size) hoedown_buffer_putc(ob, '\n');
	hoedown_buffer_puts(ob, USE_XHTML(state) ? "<hr/>\n" : "<hr>\n");
}

static int
rndr_image(hoedown_buffer *ob, const hoedown_buffer *link, const hoedown_buffer *title, const hoedown_buffer *alt, void *opaque)
{
	rndr_state *state = opaque;
	if (!link || !link->size) return 0;

	HOEDOWN_BUFPUTSL(ob, "<img src=\"");
	escape_href(ob, link->data, link->size);
	HOEDOWN_BUFPUTSL(ob, "\" alt=\"");

	if (alt && alt->size)
		escape_html(ob, alt->data, alt->size);

	if (title && title->size) {
		HOEDOWN_BUFPUTSL(ob, "\" title=\"");
		escape_html(ob, title->data, title->size); }

	hoedown_buffer_puts(ob, USE_XHTML(state) ? "\"/>" : "\">");
	return 1;
}

static int
rndr_raw_html(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	rndr_state *state = opaque;

	/* HTML_ESCAPE overrides SKIP_HTML, SKIP_STYLE, SKIP_LINKS and SKIP_IMAGES
	* It doens't see if there are any valid tags, just escape all of them. */
	if((state->flags & HOEDOWN_HTML_ESCAPE) != 0) {
		escape_html(ob, text->data, text->size);
		return 1;
	}

	if ((state->flags & HOEDOWN_HTML_SKIP_HTML) != 0)
		return 1;

	if ((state->flags & HOEDOWN_HTML_SKIP_STYLE) != 0 &&
		hoedown_html_is_tag(text->data, text->size, "style"))
		return 1;

	if ((state->flags & HOEDOWN_HTML_SKIP_LINKS) != 0 &&
		hoedown_html_is_tag(text->data, text->size, "a"))
		return 1;

	if ((state->flags & HOEDOWN_HTML_SKIP_IMAGES) != 0 &&
		hoedown_html_is_tag(text->data, text->size, "img"))
		return 1;

	hoedown_buffer_put(ob, text->data, text->size);
	return 1;
}

static void
rndr_table(hoedown_buffer *ob, const hoedown_buffer *header, const hoedown_buffer *body, void *opaque)
{
	if (ob->size) hoedown_buffer_putc(ob, '\n');
	HOEDOWN_BUFPUTSL(ob, "<table><thead>\n");
	if (header)
		hoedown_buffer_put(ob, header->data, header->size);
	HOEDOWN_BUFPUTSL(ob, "</thead><tbody>\n");
	if (body)
		hoedown_buffer_put(ob, body->data, body->size);
	HOEDOWN_BUFPUTSL(ob, "</tbody></table>\n");
}

static void
rndr_tablerow(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	HOEDOWN_BUFPUTSL(ob, "<tr>\n");
	if (text)
		hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</tr>\n");
}

static void
rndr_tablecell(hoedown_buffer *ob, const hoedown_buffer *text, int flags, void *opaque)
{
	if (flags & HOEDOWN_TABLE_HEADER) {
		HOEDOWN_BUFPUTSL(ob, "<th");
	} else {
		HOEDOWN_BUFPUTSL(ob, "<td");
	}

	switch (flags & HOEDOWN_TABLE_ALIGNMASK) {
	case HOEDOWN_TABLE_ALIGN_CENTER:
		HOEDOWN_BUFPUTSL(ob, " style=\"text-align: center\">");
		break;

	case HOEDOWN_TABLE_ALIGN_L:
		HOEDOWN_BUFPUTSL(ob, " style=\"text-align: left\">");
		break;

	case HOEDOWN_TABLE_ALIGN_R:
		HOEDOWN_BUFPUTSL(ob, " style=\"text-align: right\">");
		break;

	default:
		HOEDOWN_BUFPUTSL(ob, ">");
	}

	if (text)
		hoedown_buffer_put(ob, text->data, text->size);

	if (flags & HOEDOWN_TABLE_HEADER) {
		HOEDOWN_BUFPUTSL(ob, "</th>\n");
	} else {
		HOEDOWN_BUFPUTSL(ob, "</td>\n");
	}
}

static int
rndr_superscript(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (!text || !text->size) return 0;
	HOEDOWN_BUFPUTSL(ob, "<sup>");
	hoedown_buffer_put(ob, text->data, text->size);
	HOEDOWN_BUFPUTSL(ob, "</sup>");
	return 1;
}

static void
rndr_normal_text(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	if (text)
		escape_html(ob, text->data, text->size);
}

static void
rndr_footnotes(hoedown_buffer *ob, const hoedown_buffer *text, void *opaque)
{
	rndr_state *state = opaque;

	if (ob->size) hoedown_buffer_putc(ob, '\n');
	HOEDOWN_BUFPUTSL(ob, "<div class=\"footnotes\">\n");
	hoedown_buffer_puts(ob, USE_XHTML(state) ? "<hr/>\n" : "<hr>\n");
	HOEDOWN_BUFPUTSL(ob, "<ol>\n");
	
	if (text)
		hoedown_buffer_put(ob, text->data, text->size);
	
	HOEDOWN_BUFPUTSL(ob, "\n</ol>\n</div>\n");
}

static void
rndr_footnote_def(hoedown_buffer *ob, const hoedown_buffer *text, unsigned int num, void *opaque)
{
	size_t i = 0;
	int pfound = 0;
	
	/* insert anchor at the end of first paragraph block */
	if (text) {
		while ((i+3) < text->size) {
			if (text->data[i++] != '<') continue;
			if (text->data[i++] != '/') continue;
			if (text->data[i++] != 'p' && text->data[i] != 'P') continue;
			if (text->data[i] != '>') continue;
			i -= 3;
			pfound = 1;
			break;
		}
	}
	
	hoedown_buffer_printf(ob, "\n<li id=\"fn%d\">\n", num);
	if (pfound) {
		hoedown_buffer_put(ob, text->data, i);
		hoedown_buffer_printf(ob, "&nbsp;<a href=\"#fnref%d\" rev=\"footnote\">&#8617;</a>", num);
		hoedown_buffer_put(ob, text->data + i, text->size - i);
	} else if (text) {
		hoedown_buffer_put(ob, text->data, text->size);
	}
	HOEDOWN_BUFPUTSL(ob, "</li>\n");
}

static int
rndr_footnote_ref(hoedown_buffer *ob, unsigned int num, void *opaque)
{
	hoedown_buffer_printf(ob, "<sup id=\"fnref%d\"><a href=\"#fn%d\" rel=\"footnote\">%d</a></sup>", num, num, num);
	return 1;
}

static void
toc_header(hoedown_buffer *ob, const hoedown_buffer *text, int level, void *opaque)
{
	rndr_state *state = opaque;

	if (level <= state->toc_data.nesting_level) {
		/* set the level offset if this is the first header
		 * we're parsing for the document */
		if (state->toc_data.current_level == 0)
			state->toc_data.level_offset = level - 1;

		level -= state->toc_data.level_offset;

		if (level > state->toc_data.current_level) {
			while (level > state->toc_data.current_level) {
				HOEDOWN_BUFPUTSL(ob, "<ul>\n<li>\n");
				state->toc_data.current_level++;
			}
		} else if (level < state->toc_data.current_level) {
			HOEDOWN_BUFPUTSL(ob, "</li>\n");
			while (level < state->toc_data.current_level) {
				HOEDOWN_BUFPUTSL(ob, "</ul>\n</li>\n");
				state->toc_data.current_level--;
			}
			HOEDOWN_BUFPUTSL(ob,"<li>\n");
		} else {
			HOEDOWN_BUFPUTSL(ob,"</li>\n<li>\n");
		}

		hoedown_buffer_printf(ob, "<a href=\"#toc_%d\">", state->toc_data.header_count++);
		if (text) escape_html(ob, text->data, text->size);
		HOEDOWN_BUFPUTSL(ob, "</a>\n");
	}
}

static int
toc_link(hoedown_buffer *ob, const hoedown_buffer *link, const hoedown_buffer *title, const hoedown_buffer *content, void *opaque)
{
	if (content && content->size)
		hoedown_buffer_put(ob, content->data, content->size);
	return 1;
}

static void
toc_finalize(hoedown_buffer *ob, void *opaque)
{
	rndr_state *state = opaque;

	while (state->toc_data.current_level > 0) {
		HOEDOWN_BUFPUTSL(ob, "</li>\n</ul>\n");
		state->toc_data.current_level--;
	}
}

hoedown_renderer *
hoedown_html_toc_renderer_new(int nesting_level)
{
	static const hoedown_renderer cb_default = {
		NULL,
		NULL,
		NULL,
		toc_header,
		NULL,
		NULL,
		NULL,
		NULL,
		NULL,
		NULL,
		NULL,
		NULL,
		NULL,

		NULL,
		rndr_codespan,
		rndr_double_emphasis,
		rndr_emphasis,
		rndr_underline,
		rndr_highlight,
		rndr_quote,
		NULL,
		NULL,
		toc_link,
		NULL,
		rndr_triple_emphasis,
		rndr_strikethrough,
		rndr_superscript,
		NULL,

		NULL,
		NULL,

		NULL,
		toc_finalize,
		
		NULL
	};

	rndr_state       *state;
	hoedown_renderer *renderer;

	/* Prepare the state pointer */
	state = malloc(sizeof(rndr_state));
	if (!state)
		return NULL;

	memset(state, 0x0, sizeof(rndr_state));

	if (nesting_level > 0) {
		state->flags |= HOEDOWN_HTML_TOC;
		state->toc_data.nesting_level = nesting_level;
	}

	/* Prepare the renderer */
	renderer = malloc(sizeof(hoedown_renderer));
	if (!renderer) {
		free(state);
		return NULL;
	}

	memcpy(renderer, &cb_default, sizeof(hoedown_renderer));
	
	renderer->opaque = state;
	return renderer;
}

hoedown_renderer *
hoedown_html_renderer_new(unsigned int render_flags, int nesting_level)
{
	static const hoedown_renderer cb_default = {
		rndr_blockcode,
		rndr_blockquote,
		rndr_raw_block,
		rndr_header,
		rndr_hrule,
		rndr_list,
		rndr_listitem,
		rndr_paragraph,
		rndr_table,
		rndr_tablerow,
		rndr_tablecell,
		rndr_footnotes,
		rndr_footnote_def,

		rndr_autolink,
		rndr_codespan,
		rndr_double_emphasis,
		rndr_emphasis,
		rndr_underline,
		rndr_highlight,
		rndr_quote,
		rndr_image,
		rndr_linebreak,
		rndr_link,
		rndr_raw_html,
		rndr_triple_emphasis,
		rndr_strikethrough,
		rndr_superscript,
		rndr_footnote_ref,

		NULL,
		rndr_normal_text,

		NULL,
		NULL,
		
		NULL
	};

	rndr_state       *state;
	hoedown_renderer *renderer;

	/* Prepare the state pointer */
	state = malloc(sizeof(rndr_state));
	if (!state)
		return NULL;

	memset(state, 0x0, sizeof(rndr_state));

	state->flags = render_flags;

	if (nesting_level > 0) {
		state->flags |= HOEDOWN_HTML_TOC;
		state->toc_data.nesting_level = nesting_level;
	}

	/* Prepare the renderer */
	renderer = malloc(sizeof(hoedown_renderer));
	if (!renderer) {
		free(state);
		return NULL;
	}

	memcpy(renderer, &cb_default, sizeof(hoedown_renderer));

	if (render_flags & HOEDOWN_HTML_SKIP_IMAGES)
		renderer->image = NULL;

	if (render_flags & HOEDOWN_HTML_SKIP_LINKS) {
		renderer->link = NULL;
		renderer->autolink = NULL;
	}

	if (render_flags & HOEDOWN_HTML_SKIP_HTML || render_flags & HOEDOWN_HTML_ESCAPE)
		renderer->blockhtml = NULL;
	
	renderer->opaque = state;
	return renderer;
}

void
hoedown_html_renderer_free(hoedown_renderer *renderer)
{
	free(renderer->opaque);
	free(renderer);
}
