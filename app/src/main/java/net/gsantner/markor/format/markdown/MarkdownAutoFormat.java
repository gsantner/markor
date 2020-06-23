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

import net.gsantner.opoc.util.StringUtils;

import java.util.regex.Matcher;

public class MarkdownAutoFormat implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            if (start < source.length()
                    && dstart <= dest.length()
                    && isNewLine(source, start, end)) {

                return autoIndent(source, dest, dstart, dend);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
        }
        return source;
    }

    private static Boolean isNewLine(CharSequence source, int start, int end) {
        return ((source.charAt(start) == '\n') || (source.charAt(end - 1) == '\n'));
    }

    private CharSequence autoIndent(CharSequence source, Spanned dest, int dstart, int dend) {
        int iStart = StringUtils.getLineStart(dest, dstart);

        // append white space of previous line and new indent
        return source + createIndentForNextLine(dest, dend, iStart);
    }

    private String createIndentForNextLine(Spanned dest, int dend, int istart) {

        // Determine leading whitespace
        int iEnd = StringUtils.getNextNonWhitespace(dest, istart);

        // Construct whitespace
        String indentString = StringUtils.repeatChars(' ', iEnd - istart);

        String previousLine = dest.toString().substring(iEnd, dend);

        Matcher uMatch = MarkdownHighlighterPattern.LIST_UNORDERED.pattern.matcher(previousLine);
        if (uMatch.find()) {
            String bullet = uMatch.group() + " ";
            boolean emptyList = previousLine.equals(bullet);
            return indentString + (emptyList ? "" : bullet);
        }

        Matcher oMatch = MarkdownHighlighterPattern.LIST_ORDERED.pattern.matcher(previousLine);
        if (oMatch.find()) {
            boolean emptyList = previousLine.equals(oMatch.group(1) + ". ");
            return indentString + (emptyList ? "" : addNumericListItemIfNeeded(oMatch.group(1)));
        }

        return indentString;
    }

    private String addNumericListItemIfNeeded(String itemNumStr) {
        try {
            int nextC = Integer.parseInt(itemNumStr) + 1;
            return nextC + ". ";
        } catch (NumberFormatException e) {
            // This should never ever happen
            return "";
        }
    }
}
