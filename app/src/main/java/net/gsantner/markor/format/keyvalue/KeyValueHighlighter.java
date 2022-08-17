/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.keyvalue;

import android.graphics.Typeface;

import net.gsantner.markor.format.markdown.MarkdownHighlighterPattern;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Pattern;

public class KeyValueHighlighter extends Highlighter {

    public KeyValueHighlighter(AppSettings as) {
        super(as);
    }

    @Override
    protected void generateSpans() {

        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        createSmallBlueLinkSpans();

        createStyleSpanForMatches(KeyValueHighlighterPattern.PATTERN_KEY_VALUE.getPattern(), Typeface.BOLD);
        createStyleSpanForMatches(KeyValueHighlighterPattern.PATTERN_KEY_VALUE_QUOTED.getPattern(), Typeface.BOLD);
        createColorSpanForMatches(KeyValueHighlighterPattern.PATTERN_UNORDERED_LIST.getPattern(), 0xffef6D00);
        createStyleSpanForMatches(KeyValueHighlighterPattern.PATTERN_VCARD_KEY.getPattern(), Typeface.BOLD);
        createStyleSpanForMatches(KeyValueHighlighterPattern.PATTERN_INI_KEY.getPattern(), Typeface.BOLD);
        createRelativeSizeSpanForMatches(KeyValueHighlighterPattern.PATTERN_INI_HEADER.getPattern(), 1.25f);
        createColorSpanForMatches(KeyValueHighlighterPattern.PATTERN_INI_HEADER.getPattern(), 0xffef6D00);
        createColorSpanForMatches(KeyValueHighlighterPattern.PATTERN_INI_COMMENT.getPattern(), 0xff88b04b);
        createColorSpanForMatches(KeyValueHighlighterPattern.PATTERN_COMMENT.getPattern(), 0xff88b04b);

        /*
        // Too expensive
        if (getFilepath().toLowerCase().endsWith(".csv")) {
            _profiler.restart("KeyValue: csv");
            createStyleSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_CSV.getPattern(), Typeface.BOLD);
        }
        */
}

    enum KeyValueHighlighterPattern {
        PATTERN_KEY_VALUE(Pattern.compile("(?im)^([a-z_0-9]+)[-:=]")),
        PATTERN_KEY_VALUE_QUOTED(Pattern.compile("(?i)([\"'][a-z_0-9\\- ]+[\"']\\s*[-:=])")),
        PATTERN_VCARD_KEY(Pattern.compile("(?im)^(?<FIELD>[^\\s:;]+)(;(?<PARAM>[^=:;]+)=\"?(?<VALUE>[^:;]+)\"?)*:")),
        PATTERN_INI_HEADER(Pattern.compile("(?im)^(\\[.*\\])$")),
        PATTERN_INI_KEY(Pattern.compile("(?im)^([a-z_0-9]+)\\s*[=]")),
        PATTERN_INI_COMMENT(Pattern.compile("(?im)^(;.*)$")),
        PATTERN_UNORDERED_LIST(MarkdownHighlighterPattern.LIST_UNORDERED.pattern),
        PATTERN_COMMENT(Pattern.compile("(?im)^((#|//)\\s+.*)$")),
        PATTERN_CSV(Pattern.compile("[,;:]")),
        ;

        private Pattern pattern;

        KeyValueHighlighterPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }
}

