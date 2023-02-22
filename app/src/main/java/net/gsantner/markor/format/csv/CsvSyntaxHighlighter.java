/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import android.graphics.Color;
import android.util.Log;

import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.model.AppSettings;

public class CsvSyntaxHighlighter extends MarkdownSyntaxHighlighter {
    private static final int[] COLORS_FOR_WHITE_BACKGROUND = {Color.RED,Color.BLUE,Color.GREEN, Color.MAGENTA, Color.CYAN};
    public static final String TAG = CsvSyntaxHighlighter.class.getSimpleName();
    public static boolean DEBUG_COLORING = true;

    public CsvSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    @Override
    protected void generateSpans() {
        super.generateSpans();

        CsvMatcher matcher = new CsvMatcher(this._spannable);
        // todo: get colors from resources
        createSpanForColumns(matcher, COLORS_FOR_WHITE_BACKGROUND);
    }

    protected final void createSpanForColumns(CsvMatcher matcher, int[] colors) {
        int colNumner = -1;
        int from = matcher.getStartOfCol();
        int to;
        while (from < _spannable.length()) {
            if (matcher.isEndOfRow()) colNumner = -1; // -1 == skip coloring
            to = matcher.nextDelimiterPos(from);

            createSpanForColumn(from, to, colNumner >= 0 ? colors[colNumner] : Color.BLACK, colNumner);
            colNumner++;
            if (colNumner >= colors.length) {
                colNumner = 0;
            }
            from = to+1;
        };
    }

    protected void createSpanForColumn(int from, int to, int color, int colNumner) {
        if (DEBUG_COLORING) {
            Log.d(TAG, "#" + colNumner +
                    "(" + from +
                    "," + to +
                    "," + color +
                    ") = " + _spannable.subSequence(from, to));

        }
        if (colNumner >= 0 && Math.abs(to - from) > 0) {
            HighlightSpan span = new HighlightSpan().setForeColor(color);

            if (span != null) {
                addSpanGroup(span, from, to);
            }
        }
    }
}
