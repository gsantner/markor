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
import android.text.TextWatcher;
import android.util.Pair;

import net.gsantner.opoc.util.StringUtils;

public class ListHandler implements TextWatcher {
    private int reorderPosition;
    private boolean triggerReorder = false;
    private Integer beforeLineEnd = null;
    private boolean alreadyRunning = false; // Prevent this instance from triggering itself
    private Pair<Integer, Integer> _deleteRegion;

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

            final AutoFormatter.OrderedListLine oMatch = new AutoFormatter.OrderedListLine(s, start, _prefixPatterns);
            final AutoFormatter.UnOrderedOrCheckListLine uMatch = new AutoFormatter.UnOrderedOrCheckListLine(s, start, _prefixPatterns);

            if (oMatch.isOrderedList && beforeLineEnd == oMatch.groupEnd) {
                _deleteRegion = Pair.create(oMatch.lineStart, oMatch.lineEnd + 1);
            } else if (uMatch.isUnorderedOrCheckList && beforeLineEnd == uMatch.groupEnd) {
                _deleteRegion = Pair.create(oMatch.lineStart, oMatch.lineEnd + 1);
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
            if (_deleteRegion != null) {
                e.delete(_deleteRegion.first, _deleteRegion.second);
                _deleteRegion = null;
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

