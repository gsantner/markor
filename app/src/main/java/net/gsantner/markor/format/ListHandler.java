/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;

import net.gsantner.opoc.util.StringUtils;

public class ListHandler implements TextWatcher {
    private int reorderPosition;
    private boolean triggerReorder = false;
    private Integer beforeLineEnd = null;
    private boolean alreadyRunning = false; // Prevent this instance from triggering itself

    private final AutoFormatter.PrefixPatterns _prefixPatterns;

    public ListHandler(final AutoFormatter.PrefixPatterns prefixPatterns) {
        super();
        _prefixPatterns = prefixPatterns;
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        if (alreadyRunning) {
            return;
        }

        triggerReorder = triggerReorder || containsNewline(s, start, count);

        // Detects if enter pressed on empty list (correctly handles indent) and marks line for deletion.
        if (beforeLineEnd != null && count > 0 && start > -1 && start < s.length() && s.charAt(start) == '\n') {

            final Spannable sSpan = (Spannable) s;

            final AutoFormatter.OrderedListLine oMatch = new AutoFormatter.OrderedListLine(s, start, _prefixPatterns);
            final AutoFormatter.UnOrderedOrCheckListLine uMatch = new AutoFormatter.UnOrderedOrCheckListLine(s, start, _prefixPatterns);

            if (oMatch.isOrderedList && beforeLineEnd == oMatch.groupEnd) {
                sSpan.setSpan(this, oMatch.lineStart, oMatch.lineEnd + 1, Spanned.SPAN_COMPOSING);
            } else if (uMatch.isUnorderedOrCheckList && beforeLineEnd == uMatch.groupEnd) {
                sSpan.setSpan(this, uMatch.lineStart, uMatch.lineEnd + 1, Spanned.SPAN_COMPOSING);
            } else {
                reorderPosition = start;
            }
        }
        beforeLineEnd = null;
    }

    @Override
    public void afterTextChanged(final Editable e) {
        if (alreadyRunning) {
            return;
        }
        try {
            alreadyRunning = true;
            // Deletes spans marked for deletion
            for (final Object span : e.getSpans(0, e.length(), this.getClass())) {
                if ((e.getSpanFlags(span) & Spanned.SPAN_COMPOSING) != 0) {
                    e.delete(e.getSpanStart(span), e.getSpanEnd(span));
                }
            }
            if (triggerReorder && reorderPosition > 0 && reorderPosition < e.length()) {
                AutoFormatter.renumberOrderedList(e, reorderPosition, _prefixPatterns);
            }
        } finally {
            alreadyRunning = false;
        }
    }

    @Override
    public void beforeTextChanged(final CharSequence s, int start, final int count, final int after) {
        if (alreadyRunning) {
            return;
        }

        triggerReorder = containsNewline(s, start, count);
        reorderPosition = start;
        beforeLineEnd = StringUtils.getLineEnd(s, start);
    }

    private boolean containsNewline(final CharSequence s, final int start, final int count) {
        final int end = start + count;
        for (int i = start; i < end; i++) {
            if (s.charAt(i) == '\n') {
                return true;
            }
        }
        return false;
    }
}

