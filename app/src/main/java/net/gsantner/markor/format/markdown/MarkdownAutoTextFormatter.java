/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.text.InputFilter;
import android.text.Spanned;

import net.gsantner.markor.frontend.textview.AutoTextFormatter;

public class MarkdownAutoTextFormatter implements InputFilter {

    private final AutoTextFormatter _autoFormatter;

    public MarkdownAutoTextFormatter() {
        _autoFormatter = new AutoTextFormatter(getPrefixPatterns());
    }

    // TODO: write tests
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        return _autoFormatter.filter(source, start, end, dest, dstart, dend);
    }

    public static AutoTextFormatter.PrefixPatterns getPrefixPatterns() {
        return new AutoTextFormatter.PrefixPatterns(
                MarkdownReplacePatternGenerator.PREFIX_UNORDERED_LIST,
                MarkdownReplacePatternGenerator.PREFIX_CHECKBOX_LIST,
                MarkdownReplacePatternGenerator.PREFIX_ORDERED_LIST);
    }
}
