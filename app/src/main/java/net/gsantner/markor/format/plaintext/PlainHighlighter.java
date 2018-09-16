/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.plaintext;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;

public class PlainHighlighter extends Highlighter {

    public PlainHighlighter() {
    }

    @Override
    protected Editable run(final HighlightingEditor editor, final Editable editable) {
        try {
            clearSpans(editable);
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
        return 99999;
    }
}

