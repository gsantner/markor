/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;

public class ListHandler implements TextWatcher {
    private boolean triggerReorder = false;
    private Integer beforeLineEnd = null;
    private boolean alreadyRunning = false; // Prevent this instance from triggering itself
    private Pair<Integer, Integer> _deleteRegion;

    private final AutoTextFormatter.FormatPatterns _prefixPatterns;

    public ListHandler(final AutoTextFormatter.FormatPatterns prefixPatterns) {
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

            final AutoTextFormatter.OrderedListLine oMatch = new AutoTextFormatter.OrderedListLine(s, start, _prefixPatterns);
            final AutoTextFormatter.UnOrderedOrCheckListLine uMatch = new AutoTextFormatter.UnOrderedOrCheckListLine(s, start, _prefixPatterns);

            if (oMatch.isOrderedList && beforeLineEnd == oMatch.groupEnd) {
                _deleteRegion = Pair.create(oMatch.lineStart, oMatch.lineEnd + 1);
            } else if (uMatch.isUnorderedOrCheckList && beforeLineEnd == uMatch.groupEnd) {
                _deleteRegion = Pair.create(oMatch.lineStart, oMatch.lineEnd + 1);
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
            if (_deleteRegion != null) {
                e.delete(_deleteRegion.first, _deleteRegion.second);
                _deleteRegion = null;
            }
            if (triggerReorder) {
                AutoTextFormatter.renumberOrderedList(e, _prefixPatterns);
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
        beforeLineEnd = TextViewUtils.getLineEnd(s, start);
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

