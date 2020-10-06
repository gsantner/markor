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

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.Spannable;

import net.gsantner.markor.format.markdown.MarkdownHighlighterPattern;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;

import java.util.regex.Pattern;

public class KeyValueHighlighter extends Highlighter {
    public KeyValueHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
    }

    @Override
    protected Spannable run(final Spannable spannable) {
        try {
            clearSpans(spannable);

            if (spannable.length() == 0) {
                return spannable;
            }

            _profiler.start(true, "KeyValue Highlighting");
            generalHighlightRun(spannable);

            _profiler.restart("KeyValue: Generic key-value");
            createStyleSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_KEY_VALUE.getPattern(), Typeface.BOLD);
            createStyleSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_KEY_VALUE_QUOTED.getPattern(), Typeface.BOLD);
            createColorSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_UNORDERED_LIST.getPattern(), 0xffef6D00);
            _profiler.restart("KeyValue: vcard");
            createStyleSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_VCARD_KEY.getPattern(), Typeface.BOLD);
            _profiler.restart("KeyValue: ini");
            createStyleSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_INI_KEY.getPattern(), Typeface.BOLD);
            createRelativeSizeSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_INI_HEADER.getPattern(), 1.25f);
            createColorSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_INI_HEADER.getPattern(), 0xffef6D00);
            createColorSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_INI_COMMENT.getPattern(), 0xff88b04b);
            _profiler.restart("KeyValue: comment");
            createColorSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_COMMENT.getPattern(), 0xff88b04b);

            /*
            // Too expensive
            if (getFilepath().toLowerCase().endsWith(".csv")) {
                _profiler.restart("KeyValue: csv");
                createStyleSpanForMatches(spannable, KeyValueHighlighterPattern.PATTERN_CSV.getPattern(), Typeface.BOLD);
            }
            */

            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return spannable;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return AUTOFORMATTER_NONE;
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return LONG_HIGHLIGHTING_DELAY;
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

