/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui.hleditor;

import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;

import net.gsantner.markor.format.markdown.MarkdownHighlighterPattern;
import net.gsantner.opoc.util.StringUtils;

import java.util.regex.Matcher;

public class ListHandler implements TextWatcher {

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        // Detects if enter pressed on empty list (correctly handles indent) and marks line for deletion.
        if (count > 0 && start > -1 && start < s.length() && s.charAt(start) == '\n') {

            int iStart = StringUtils.getLineStart(s, start);
            int iEnd = StringUtils.getNextNonWhitespace(s, iStart);

            String previousLine = s.subSequence(iEnd, start).toString();
            Spannable sSpan = (Spannable) s;

            Matcher uMatch = MarkdownHighlighterPattern.LIST_UNORDERED.pattern.matcher(previousLine);
            if (uMatch.find() && previousLine.equals(uMatch.group() + " ")) {
                sSpan.setSpan(this, iStart, start + 1, Spanned.SPAN_COMPOSING);
            } else {
                Matcher oMatch = MarkdownHighlighterPattern.LIST_ORDERED.pattern.matcher(previousLine);
                if (oMatch.find() && previousLine.equals(oMatch.group(1) + ". ")) {
                    sSpan.setSpan(this, iStart, start + 1, Spanned.SPAN_COMPOSING);
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable e) {
        // Deletes spans marked for deletion
        Spannable eSpan = (Spannable) e;
        for (Object span : eSpan.getSpans(0, e.length(), this.getClass())) {
            if ((eSpan.getSpanFlags(span) & Spanned.SPAN_COMPOSING) != 0) {
                e.delete(eSpan.getSpanStart(span), eSpan.getSpanEnd(span));
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Not used
    }
}

