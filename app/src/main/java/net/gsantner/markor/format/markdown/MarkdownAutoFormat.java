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

public class MarkdownAutoFormat implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            if (end - start == 1
                    && start < source.length()
                    && dstart <= dest.length()) {
                char newChar = source.charAt(start);

                if (newChar == '\n') {
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
        if (istart > -1 && istart < dest.length() - 1) {
            int iend;

            for (iend = ++istart; iend < dest.length() - 1; ++iend) {
                char c = dest.charAt(iend);
                if (c != ' ' && c != '\t') {
                    break;
                }
            }

            if (iend < dest.length() - 1) {
                // This is for any line that is not the first line in a file
                Matcher listMatcher = MarkdownHighlighterPattern.LIST_UNORDERED.pattern.matcher(dest.toString().substring(iend, dend));
                if (listMatcher.find()) {
                    return dest.toString().substring(istart, iend) + listMatcher.group() + " ";
                } else {
                    Matcher m = MarkdownHighlighterPattern.LIST_ORDERED.pattern.matcher(dest.toString().substring(iend, dend));
                    if (m.find()) {
                        return dest.subSequence(istart, iend) + addNumericListItemIfNeeded(m.group(1));
                    } else {
                        return "";
                    }
                }
            } else {
                return "";
            }
        } else if (istart > -1) {
            return "";
        } else if (dest.length() > 1) {
            Matcher listMatcher = MarkdownHighlighterPattern.LIST_UNORDERED.pattern.matcher(dest.toString());
            if (listMatcher.find()) {
                return Character.toString(dest.charAt(0)) + " ";
            } else {
                Matcher m = MarkdownHighlighterPattern.LIST_ORDERED.pattern.matcher(dest.toString());
                if (m.find()) {
                    return addNumericListItemIfNeeded(m.group(1));
                } else {
                    return "";
                }
            }
        } else {
            return "";
        }
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
