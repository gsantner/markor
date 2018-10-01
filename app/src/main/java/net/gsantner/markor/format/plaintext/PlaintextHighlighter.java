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
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.format.todotxt.TodoTxtHighlighterColors;
import net.gsantner.markor.format.todotxt.TodoTxtHighlighterPattern;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

public class PlaintextHighlighter extends Highlighter {
    private final TodoTxtHighlighterColors colors;
    public final String fontType;
    public final Integer fontSize;

    public PlaintextHighlighter() {
        colors = new TodoTxtHighlighterColors();
        fontType = AppSettings.get().getFontFamily();
        fontSize = AppSettings.get().getFontSize();
    }

    @Override
    protected Editable run(final HighlightingEditor editor, final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            _profiler.start(true, "Plaintext Highlighting");

            _profiler.restart("Link Color");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(), colors.getLinkColor());
            _profiler.restart("Link Size");
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(), 0.7f);
            _profiler.restart("Link Italic");
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(), Typeface.ITALIC);


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
        return 2500;
    }

}

