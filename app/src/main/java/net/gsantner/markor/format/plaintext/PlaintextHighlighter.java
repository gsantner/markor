/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.plaintext;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;

public class PlaintextHighlighter extends Highlighter {
    public PlaintextHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
    }

    @Override
    protected Editable run(final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            _profiler.start(true, "Plaintext Highlighting");
            generalHighlightRun(editable);
            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return AUTOFORMATTER_NONE;
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return LONG_HIGHLIGHTING_DELAY;
    }

}

