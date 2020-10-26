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

import net.gsantner.markor.format.AutoFormatter;

public class MarkdownAutoFormat implements InputFilter {
    private final AutoFormatter _autoFormatter;

    public MarkdownAutoFormat() {
        _autoFormatter = new AutoFormatter(
                MarkdownReplacePatternGenerator.PREFIX_ORDERED_LIST,
                MarkdownReplacePatternGenerator.PREFIX_UNORDERED_LIST,
                MarkdownReplacePatternGenerator.PREFIX_UNCHECKED_LIST,
                MarkdownReplacePatternGenerator.PREFIX_CHECKED_LIST);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        return _autoFormatter.filter(source, start, end, dest, dstart, dend);
    }
}
