package net.gsantner.markor.format.zimwiki;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;

import net.gsantner.markor.format.markdown.MarkdownAutoFormat;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

public class ZimWikiHighlighter extends Highlighter {
    private final boolean _highlightBiggerHeadings;

    private static final int COLOR_HEADING = 0xff4e9a06;
    private static final int MARKED_BACKGROUND_COLOR = 0xffffff00;
    private static final int UNORDERED_LIST_BULLET_COLOR = 0xffdaa521;
    private static final int ORDERED_LIST_NUMBER_COLOR = 0xffdaa521;

    public ZimWikiHighlighter(HighlightingEditor editor, Document document) {
        super(editor, document);
        _highlightBiggerHeadings = false;   // TODO(WIP): introduce an option for zim wiki bigger headings
    }

    @Override
    protected Spannable run(final Spannable spannable) {
        clearSpans(spannable);

        if (spannable.length() == 0) {
            return spannable;
        }

        _profiler.start(true, "ZimWiki Highlighting");
        generalHighlightRun(spannable);

        _profiler.restart("Heading");
        if (_highlightBiggerHeadings) {
            // TODO: resize text according to heading level
        } else {
            createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.HEADING.pattern, COLOR_HEADING);
        }

        _profiler.restart("Bold");
        createStyleSpanForMatches(spannable, ZimWikiHighlighterPattern.BOLD.pattern, Typeface.BOLD);

        _profiler.restart("Italics");
        createStyleSpanForMatches(spannable, ZimWikiHighlighterPattern.ITALICS.pattern, Typeface.ITALIC);

        _profiler.restart("Marked (highlighted)");
        createColorBackgroundSpan(spannable, ZimWikiHighlighterPattern.MARKED.pattern, MARKED_BACKGROUND_COLOR);

        _profiler.restart("Strikethrough");
        createSpanWithStrikeThroughForMatches(spannable, ZimWikiHighlighterPattern.STRIKETHROUGH.pattern);

        _profiler.restart("Preformatted (monospaced) inline");
        createMonospaceSpanForMatches(spannable, ZimWikiHighlighterPattern.PREFORMATTED_INLINE.pattern);

        _profiler.restart("Unordered list");
        createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.LIST_UNORDERED.pattern, UNORDERED_LIST_BULLET_COLOR);

        _profiler.restart("Ordered list");
        createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.LIST_ORDERED.pattern, ORDERED_LIST_NUMBER_COLOR);

        _profiler.end();
        _profiler.printProfilingGroup();

        return spannable;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new MarkdownAutoFormat(); // TODO: adapt to ZimWiki
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return new AppSettings(context).getMarkdownHighlightingDelay(); // TODO: adapt to ZimWiki settings
    }
}