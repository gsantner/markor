/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import android.text.InputFilter;
import android.text.Spanned;

import net.gsantner.markor.frontend.textview.AutoTextFormatter;

import java.util.regex.Pattern;

public class WikitextAutoTextFormatter implements InputFilter {
    private static final Pattern PREFIX_CHECKBOX_LIST = Pattern.compile("^(\\s*)((\\[)[\\sx*>](]\\s))");

    private final AutoTextFormatter _autoFormatter;

    public WikitextAutoTextFormatter() {
        _autoFormatter = new AutoTextFormatter(getPrefixPatterns(), '\t');
    }

    // TODO: write tests
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        return _autoFormatter.filter(source, start, end, dest, dstart, dend);
    }

    public static AutoTextFormatter.PrefixPatterns getPrefixPatterns() {
        return new AutoTextFormatter.PrefixPatterns(
                WikitextReplacePatternGenerator.PREFIX_UNORDERED_LIST,
                PREFIX_CHECKBOX_LIST,
                WikitextReplacePatternGenerator.PREFIX_ORDERED_LIST
        );
    }
}
