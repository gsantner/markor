/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.Spannable;

import net.gsantner.markor.format.ListHandler;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Pattern;

import other.writeily.format.zimwiki.WrZimWikiHeaderSpanCreator;

public class ZimWikiHighlighter extends Highlighter {
    private final boolean _highlightBiggerHeadings;
    public final String _fontType;
    public final Integer _fontSize;

    private static final int COLOR_HEADING = 0xff4e9a06;
    private static final int HIGHLIGHT_BACKGROUND_COLOR = 0xffFFA062;   // zim original color: 0xffffff00
    private static final int UNORDERED_LIST_BULLET_COLOR = 0xffdaa521;
    private static final int ORDERED_LIST_NUMBER_COLOR = 0xffdaa521;
    private static final int LINK_COLOR = 0xff1ea3fd; // zim original color: 0xff0000ff
    private static final int CHECKLIST_BASE_COLOR = UNORDERED_LIST_BULLET_COLOR;
    private static final int CHECKLIST_CHECKED_COLOR = 0xff54a309;
    private static final int CHECKLIST_CROSSED_COLOR = 0xffa90000;
    private static final int CHECKLIST_ARROW_COLOR = CHECKLIST_BASE_COLOR;
    private static final int ZIMHEADER_COLOR = 0xff808080;

    public ZimWikiHighlighter(HighlightingEditor editor, Document document) {
        super(editor, document);
        _highlightLinks = false;
        _highlightBiggerHeadings = _appSettings.isZimWikiBiggerHeadings();
        _fontType = _appSettings.getFontFamily();
        _fontSize = _appSettings.getFontSize();
        setTextModifier(new ListHandler(true, ZimWikiAutoFormat.getPrefixPatterns()));  // TODO: introduce a setting for enabling/disabling reordering
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
            createHeaderSpanForMatches(spannable, ZimWikiHighlighterPattern.HEADING, COLOR_HEADING);
        } else {
            createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.HEADING.pattern, COLOR_HEADING);
        }

        _profiler.restart("Bold");
        createStyleSpanForMatches(spannable, ZimWikiHighlighterPattern.BOLD.pattern, Typeface.BOLD);

        _profiler.restart("Italics");
        createStyleSpanForMatches(spannable, ZimWikiHighlighterPattern.ITALICS.pattern, Typeface.ITALIC);

        _profiler.restart("Marked (highlighted)");
        createColorBackgroundSpan(spannable, ZimWikiHighlighterPattern.HIGHLIGHTED.pattern, HIGHLIGHT_BACKGROUND_COLOR);

        _profiler.restart("Strikethrough");
        createSpanWithStrikeThroughForMatches(spannable, ZimWikiHighlighterPattern.STRIKETHROUGH.pattern);

        _profiler.restart("Preformatted (monospaced) inline");
        createMonospaceSpanForMatches(spannable, ZimWikiHighlighterPattern.PREFORMATTED_INLINE.pattern);

        _profiler.restart("Preformatted (monospaced) multiline");
        createMonospaceSpanForMatches(spannable, ZimWikiHighlighterPattern.PREFORMATTED_MULTILINE.pattern); // TODO: also indent a bit

        _profiler.restart("Unordered list");
        createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.LIST_UNORDERED.pattern, UNORDERED_LIST_BULLET_COLOR);

        _profiler.restart("Ordered list");
        createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.LIST_ORDERED.pattern, ORDERED_LIST_NUMBER_COLOR);

        _profiler.restart("Link");
        createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.LINK.pattern, LINK_COLOR);

        _profiler.restart("Superscript");
        createSuperscriptStyleSpanForMatches(spannable, ZimWikiHighlighterPattern.SUPERSCRIPT.pattern);

        _profiler.restart("Subscript");
        createSubscriptStyleSpanForMatches(spannable, ZimWikiHighlighterPattern.SUBSCRIPT.pattern);

        _profiler.restart("Checklist");
        createCheckboxSpansForAllCheckStates(spannable);

        _profiler.restart("Zim Header");
        createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.ZIMHEADER.pattern, ZIMHEADER_COLOR);

        _profiler.end();
        _profiler.printProfilingGroup();

        return spannable;
    }

    private void createHeaderSpanForMatches(Spannable spannable, ZimWikiHighlighterPattern pattern, int headerColor) {
        createSpanForMatches(spannable, pattern.pattern, new WrZimWikiHeaderSpanCreator(this, spannable, headerColor, _highlightBiggerHeadings));
    }

    private void createCheckboxSpansForAllCheckStates(Spannable spannable) {
        createCheckboxSpanWithDifferentColors(spannable, ZimWikiHighlighterPattern.CHECKLIST_UNCHECKED.pattern, 0xffffffff);
        createCheckboxSpanWithDifferentColors(spannable, ZimWikiHighlighterPattern.CHECKLIST_CHECKED.pattern, CHECKLIST_CHECKED_COLOR);
        createCheckboxSpanWithDifferentColors(spannable, ZimWikiHighlighterPattern.CHECKLIST_CROSSED.pattern, CHECKLIST_CROSSED_COLOR);
        createCheckboxSpanWithDifferentColors(spannable, ZimWikiHighlighterPattern.CHECKLIST_ARROW.pattern, CHECKLIST_ARROW_COLOR);
    }

    private void createCheckboxSpanWithDifferentColors(Spannable spannable, Pattern checkboxPattern, int symbolColor) {
        createColorSpanForMatches(spannable, checkboxPattern,
                CHECKLIST_BASE_COLOR, ZimWikiHighlighterPattern.CHECKBOX_LEFT_BRACKET_GROUP);
        createColorSpanForMatches(spannable, checkboxPattern,
                symbolColor, ZimWikiHighlighterPattern.CHECKBOX_SYMBOL_GROUP);
        createColorSpanForMatches(spannable, checkboxPattern,
                CHECKLIST_BASE_COLOR, ZimWikiHighlighterPattern.CHECKBOX_RIGHT_BRACKET_GROUP);
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new ZimWikiAutoFormat();
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return new AppSettings(context).getMarkdownHighlightingDelay(); // TODO: adapt to ZimWiki settings
    }
}