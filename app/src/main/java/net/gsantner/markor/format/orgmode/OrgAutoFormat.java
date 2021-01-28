/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.orgmode;

import android.text.InputFilter;
import android.text.Spanned;

import net.gsantner.markor.format.AutoFormatter;

import java.util.regex.Pattern;

public class OrgAutoFormat implements InputFilter {
    public static final Pattern PREFIX_CHECKBOX_LIST = Pattern.compile("^(\\s*)(([-*+]\\s\\[)[\\sxX](]\\s))");

    private final AutoFormatter _autoFormatter;

    public OrgAutoFormat() {
        _autoFormatter = new AutoFormatter(getPrefixPatterns(), ' ');
    }

    // TODO: write tests
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        return _autoFormatter.filter(source, start, end, dest, dstart, dend);
    }

    public static AutoFormatter.PrefixPatterns getPrefixPatterns() {
        return new AutoFormatter.PrefixPatterns(
                OrgReplacePatternGenerator.PREFIX_UNORDERED_LIST,
                PREFIX_CHECKBOX_LIST,
                OrgReplacePatternGenerator.PREFIX_ORDERED_LIST);
    }
}
