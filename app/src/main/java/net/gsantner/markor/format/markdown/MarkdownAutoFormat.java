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

import java.util.regex.Matcher;
import java.util.Arrays;

public class MarkdownAutoFormat implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            if (start < source.length() && dstart <= dest.length()) {
                if (source.charAt(start) == '\n' || source.charAt(end - 1) == '\n') {
                    return autoIndent(source, dest, dstart, dend);
                }
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
        }
        return source;
    }

    private CharSequence autoIndent(CharSequence source, Spanned dest, int dstart, int dend) {
        int istart = findLineBreakPosition(dest, dstart);

        // append white space of previous line and new indent
        return source + createIndentForNextLine(dest, dend, istart);
    }

    private int findLineBreakPosition(Spanned dest, int dstart) {
        int istart = dstart - 1;

        for (; istart > -1; --istart) {
            char c = dest.charAt(istart);
            if (c == '\n') {
                break;
            }
        }
        return istart;
    }

    private String createIndentForNextLine(Spanned dest, int dend, int istart) {

        // Determine leading whitespace
        int iend;
        for (iend = istart + 1; iend < dest.length(); ++iend) {
            char c = dest.charAt(iend);
            if (c != ' ' && c != '\t') {
                break;
            }
        }

        // Construct whitespace
        int indentSize = iend - istart - 1;
        char[] indentChars = new char[indentSize];
        Arrays.fill(indentChars, ' ');
        String indentString = new String(indentChars);

        // Add appropriate list identifier
        Matcher uMatch = MarkdownHighlighterPattern.LIST_UNORDERED.pattern.matcher(dest.toString().substring(iend, dend));
        if (uMatch.find()) {
            indentString += uMatch.group() + " ";
        } else {
            Matcher oMatch = MarkdownHighlighterPattern.LIST_ORDERED.pattern.matcher(dest.toString().substring(iend, dend));
            if (oMatch.find()) {
                indentString += addNumericListItemIfNeeded(oMatch.group(1));
            }
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
