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

import android.graphics.Paint;
import android.graphics.Typeface;

import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.util.AppSettings;

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
        LIST_UNORDERED(Pattern.compile("(?<=((\\n|^)\\s{0,10}))\\*(?= )")),
        LIST_ORDERED(Pattern.compile("(?<=((\\n|^)(\\s{0,10})))(\\d+|[a-zA-Z])(\\.)(?= )")),
        LINK(ZimWikiLinkResolver.Patterns.LINK.pattern),
        IMAGE(Pattern.compile("(\\{\\{(?!\\{)(.*?)\\}\\})")),
        CHECKLIST(Pattern.compile("(?<=(\\n|^))\t*(\\[)([ x*>])(])(?= )")),
        CHECKLIST_UNCHECKED(Pattern.compile("(?<=(\\n|^))\t*(\\[)( )(])(?= )")),
        CHECKLIST_CHECKED(Pattern.compile("(?<=(\\n|^))\t*(\\[)(\\*)(])(?= )")),
        CHECKLIST_CROSSED(Pattern.compile("(?<=(\\n|^))\t*(\\[)(x)(])(?= )")),
        CHECKLIST_ARROW(Pattern.compile("(?<=(\\n|^))\t*(\\[)(>)(])(?= )")),
        SUBSCRIPT(Pattern.compile("(_\\{(?!~)(.+?)\\})")),
        SUPERSCRIPT(Pattern.compile("(\\^\\{(?!~)(.+?)\\})")),
        ZIMHEADER_CONTENT_TYPE_ONLY(Pattern.compile("^\\s*Content-Type:\\s*text/x-zim-wiki")),
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
        private static final int CODEBLOCK_COLOR = 0xff8c8c8c;
    }


    //
    // Members
    //

    public ZimWikiHighlighter(AppSettings as) {
        super(as);
    }

    private boolean _isZimWikiBiggerHeadings = false;
    private String _fontFamily = "";
    private boolean _isHighlightCodeBlock = false;
    private boolean _isHighlightCodeMonospace = false;

    @Override
    public Highlighter configure(final Paint paint) {
        _delay = _appSettings.getMarkdownHighlightingDelay();
        _isZimWikiBiggerHeadings = _appSettings.isZimWikiBiggerHeadings();
        _fontFamily = _appSettings.getFontFamily();
        _isHighlightCodeMonospace = _appSettings.isHighlightCodeMonospaceFont();
        _isHighlightCodeBlock = _appSettings.isHighlightCodeBlock();
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {

        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();

        if (_isZimWikiBiggerHeadings) {
            createSpanForMatches(Patterns.HEADING.pattern,
                    new WrZimWikiHeaderSpanCreator(_spannable, Colors.COLOR_HEADING, _textSize));
        } else {
            createColorSpanForMatches(Patterns.HEADING.pattern, Colors.COLOR_HEADING);
        }

        createStyleSpanForMatches(Patterns.BOLD.pattern, Typeface.BOLD);

        createStyleSpanForMatches(Patterns.ITALICS.pattern, Typeface.ITALIC);

        createColorBackgroundSpan(Patterns.HIGHLIGHTED.pattern, Colors.HIGHLIGHT_BACKGROUND_COLOR);

        createStrikeThroughSpanForMatches(Patterns.STRIKETHROUGH.pattern);

        if (_isHighlightCodeMonospace) {
            createMonospaceSpanForMatches(Patterns.PREFORMATTED_INLINE.pattern);
            createMonospaceSpanForMatches(Patterns.PREFORMATTED_MULTILINE.pattern);
        }

        if (_isHighlightCodeBlock) {
            createColorBackgroundSpan(Patterns.PREFORMATTED_INLINE.pattern, Colors.CODEBLOCK_COLOR);
            createColorBackgroundSpan(Patterns.PREFORMATTED_MULTILINE.pattern, Colors.CODEBLOCK_COLOR);
        }

        createColorSpanForMatches(Patterns.LIST_UNORDERED.pattern, Colors.UNORDERED_LIST_BULLET_COLOR);

        createColorSpanForMatches(Patterns.LIST_ORDERED.pattern, Colors.ORDERED_LIST_NUMBER_COLOR);

        createSmallBlueLinkSpans();
        createColorSpanForMatches(Patterns.LINK.pattern, Colors.LINK_COLOR);

        createSuperscriptStyleSpanForMatches(Patterns.SUPERSCRIPT.pattern);

        createSubscriptStyleSpanForMatches(Patterns.SUBSCRIPT.pattern);

        createCheckboxSpansForAllCheckStates();

        createColorSpanForMatches(Patterns.ZIMHEADER.pattern, Colors.ZIMHEADER_COLOR);

    }

    private void createCheckboxSpansForAllCheckStates() {
        createCheckboxSpanWithDifferentColors(Patterns.CHECKLIST_UNCHECKED.pattern, 0xffffffff);
        createCheckboxSpanWithDifferentColors(Patterns.CHECKLIST_CHECKED.pattern, Colors.CHECKLIST_CHECKED_COLOR);
        createCheckboxSpanWithDifferentColors(Patterns.CHECKLIST_CROSSED.pattern, Colors.CHECKLIST_CROSSED_COLOR);
        createCheckboxSpanWithDifferentColors(Patterns.CHECKLIST_ARROW.pattern, Colors.CHECKLIST_ARROW_COLOR);
    }

    private void createCheckboxSpanWithDifferentColors(final Pattern checkboxPattern, final int symbolColor) {
        createColorSpanForMatches(checkboxPattern, Colors.CHECKLIST_BASE_COLOR, Patterns.CHECKBOX_LEFT_BRACKET_GROUP);
        createColorSpanForMatches(checkboxPattern, symbolColor, Patterns.CHECKBOX_SYMBOL_GROUP);
        createColorSpanForMatches(checkboxPattern, Colors.CHECKLIST_BASE_COLOR, Patterns.CHECKBOX_RIGHT_BRACKET_GROUP);
    }
}