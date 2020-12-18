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

import java.util.regex.Pattern;

import other.writeily.format.zimwiki.WrZimWikiHeaderSpanCreator;

public class ZimWikiHighlighter extends Highlighter {
    //
    // Statics
    //
    public enum Patterns {
        BOLD(Pattern.compile("(?<=(\\n|^|\\s|\\*))(\\*{2})[^*\\s](?=\\S)(.*?)[^*\\s]?\\2(?=(\\n|$|\\s|\\*))")),
        ITALICS(Pattern.compile("(?<=(\\n|^|\\s|/))(/{2})[^/\\s](.*?)[^/\\s]?\\2(?=(\\n|$|\\s|/))")),
        HIGHLIGHTED(Pattern.compile("(?<=(\\n|^|\\s|_))(_{2})[^_\\s](.*?)[^_\\s]?\\2(?=(\\n|$|\\s|_))")),
        STRIKETHROUGH(Pattern.compile("(?<=(\\n|^|\\s|~))(~{2})[^~\\s](.*?)[^~\\s]?\\2(?=(\\n|$|\\s|~))")),
        HEADING(Pattern.compile("(?<=(\\n|^|\\s))(==+)[ \\t]+(.*?)[ \\t]\\2(?=(\\n|$|\\s))")),
        PREFORMATTED_INLINE(Pattern.compile("''(?!')(.+?)''")),
        PREFORMATTED_MULTILINE(Pattern.compile("(?s)(?<=[\\n^])'''[\\n$](.*?)[\\n^]'''(?=[\\n$])")),
        LIST_UNORDERED(Pattern.compile("(?<=((\n|^)\\s{0,16}))\\*(?= )")),
        LIST_ORDERED(Pattern.compile("(?<=((\n|^)(\\s{0,16})))(\\d+|[a-zA-Z])(\\.)(?= )")),
        LINK(Pattern.compile("(\\[\\[(?!\\[)(.+?\\]*)]\\])")),
        IMAGE(Pattern.compile("(\\{\\{(?!\\{)(.*?)\\}\\})")),
        CHECKLIST(Pattern.compile("(?<=(\\n|^))\t*(\\[)([ x*>])(])(?= )")),
        CHECKLIST_UNCHECKED(Pattern.compile("(?<=(\\n|^))\t*(\\[)( )(])(?= )")),
        CHECKLIST_CHECKED(Pattern.compile("(?<=(\\n|^))\t*(\\[)(\\*)(])(?= )")),
        CHECKLIST_CROSSED(Pattern.compile("(?<=(\\n|^))\t*(\\[)(x)(])(?= )")),
        CHECKLIST_ARROW(Pattern.compile("(?<=(\\n|^))\t*(\\[)(>)(])(?= )")),
        SUBSCRIPT(Pattern.compile("(_\\{(?!~)(.+?)\\})")),
        SUPERSCRIPT(Pattern.compile("(\\^\\{(?!~)(.+?)\\})")),
        ZIMHEADER(Pattern.compile("^Content-Type: text/x-zim-wiki(\r\n|\r|\n)" +
                "Wiki-Format: zim \\d+\\.\\d+(\r\n|\r|\n)" +
                "Creation-Date: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.+:\\d]+"));

        // groups for matching individual parts of the checklist regex
        public static final int CHECKBOX_LEFT_BRACKET_GROUP = 2;
        public static final int CHECKBOX_SYMBOL_GROUP = 3;
        public static final int CHECKBOX_RIGHT_BRACKET_GROUP = 4;

        public final Pattern pattern;

        Patterns(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    public static class Colors {
        private static final int COLOR_HEADING = 0xff4e9a06;
        private static final int HIGHLIGHT_BACKGROUND_COLOR = 0xffFFA062;   // zim original color: 0xffffff00
        private static final int UNORDERED_LIST_BULLET_COLOR = 0xffdaa521;
        private static final int CHECKLIST_BASE_COLOR = UNORDERED_LIST_BULLET_COLOR;
        private static final int CHECKLIST_ARROW_COLOR = CHECKLIST_BASE_COLOR;
        private static final int ORDERED_LIST_NUMBER_COLOR = 0xffdaa521;
        private static final int LINK_COLOR = 0xff1ea3fd; // zim original color: 0xff0000ff
        private static final int CHECKLIST_CHECKED_COLOR = 0xff54a309;
        private static final int CHECKLIST_CROSSED_COLOR = 0xffa90000;
        private static final int ZIMHEADER_COLOR = 0xff808080;
    }


    //
    // Members
    //

    public ZimWikiHighlighter(HighlightingEditor editor, Document document) {
        super(editor, document);
        _highlightLinks = false;
        setTextModifier(new ListHandler(true, ZimWikiAutoFormat.getPrefixPatterns()));  // TODO: introduce a setting for enabling/disabling reordering
    }

    @Override
    protected Spannable run(final Spannable spannable) {
        clearSpans(spannable);
        if (spannable.length() == 0) {
            return spannable;
        }

        //
        // Do highlight
        //
        _profiler.start(true, "ZimWiki Highlighting");
        generalHighlightRun(spannable);

        _profiler.restart("Heading");
        if (_appSettings.isZimWikiBiggerHeadings()) {
            createHeaderSpanForMatches(spannable, Patterns.HEADING, Colors.COLOR_HEADING);
        } else {
            createColorSpanForMatches(spannable, Patterns.HEADING.pattern, Colors.COLOR_HEADING);
        }

        _profiler.restart("Bold");
        createStyleSpanForMatches(spannable, Patterns.BOLD.pattern, Typeface.BOLD);

        _profiler.restart("Italics");
        createStyleSpanForMatches(spannable, Patterns.ITALICS.pattern, Typeface.ITALIC);

        _profiler.restart("Marked (highlighted)");
        createColorBackgroundSpan(spannable, Patterns.HIGHLIGHTED.pattern, Colors.HIGHLIGHT_BACKGROUND_COLOR);

        _profiler.restart("Strikethrough");
        createSpanWithStrikeThroughForMatches(spannable, Patterns.STRIKETHROUGH.pattern);

        _profiler.restart("Preformatted (monospaced) inline");
        createMonospaceSpanForMatches(spannable, Patterns.PREFORMATTED_INLINE.pattern);

        _profiler.restart("Preformatted (monospaced) multiline");
        createMonospaceSpanForMatches(spannable, Patterns.PREFORMATTED_MULTILINE.pattern); // TODO: also indent a bit

        _profiler.restart("Unordered list");
        createColorSpanForMatches(spannable, Patterns.LIST_UNORDERED.pattern, Colors.UNORDERED_LIST_BULLET_COLOR);

        _profiler.restart("Ordered list");
        createColorSpanForMatches(spannable, Patterns.LIST_ORDERED.pattern, Colors.ORDERED_LIST_NUMBER_COLOR);

        _profiler.restart("Link");
        createColorSpanForMatches(spannable, Patterns.LINK.pattern, Colors.LINK_COLOR);

        _profiler.restart("Superscript");
        createSuperscriptStyleSpanForMatches(spannable, Patterns.SUPERSCRIPT.pattern);

        _profiler.restart("Subscript");
        createSubscriptStyleSpanForMatches(spannable, Patterns.SUBSCRIPT.pattern);

        _profiler.restart("Checklist");
        createCheckboxSpansForAllCheckStates(spannable);

        _profiler.restart("Zim Header");
        createColorSpanForMatches(spannable, Patterns.ZIMHEADER.pattern, Colors.ZIMHEADER_COLOR);

        _profiler.end();
        _profiler.printProfilingGroup();

        return spannable;
    }

    private void createHeaderSpanForMatches(Spannable spannable, Patterns pattern, int headerColor) {
        createSpanForMatches(spannable, pattern.pattern, new WrZimWikiHeaderSpanCreator(this, spannable, headerColor, _appSettings.isZimWikiBiggerHeadings(), _appSettings.getFontFamily(), _appSettings.getFontSize()));
    }

    private void createCheckboxSpansForAllCheckStates(Spannable spannable) {
        createCheckboxSpanWithDifferentColors(spannable, Patterns.CHECKLIST_UNCHECKED.pattern, 0xffffffff);
        createCheckboxSpanWithDifferentColors(spannable, Patterns.CHECKLIST_CHECKED.pattern, Colors.CHECKLIST_CHECKED_COLOR);
        createCheckboxSpanWithDifferentColors(spannable, Patterns.CHECKLIST_CROSSED.pattern, Colors.CHECKLIST_CROSSED_COLOR);
        createCheckboxSpanWithDifferentColors(spannable, Patterns.CHECKLIST_ARROW.pattern, Colors.CHECKLIST_ARROW_COLOR);
    }

    private void createCheckboxSpanWithDifferentColors(Spannable spannable, Pattern checkboxPattern, int symbolColor) {
        createColorSpanForMatches(spannable, checkboxPattern, Colors.CHECKLIST_BASE_COLOR, Patterns.CHECKBOX_LEFT_BRACKET_GROUP);
        createColorSpanForMatches(spannable, checkboxPattern, symbolColor, Patterns.CHECKBOX_SYMBOL_GROUP);
        createColorSpanForMatches(spannable, checkboxPattern, Colors.CHECKLIST_BASE_COLOR, Patterns.CHECKBOX_RIGHT_BRACKET_GROUP);
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new ZimWikiAutoFormat();
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return _appSettings.getMarkdownHighlightingDelay();
    }
}