/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.text.style.LineBackgroundSpan;
import android.text.style.LineHeightSpan;
import android.text.style.UpdateAppearance;

import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Pattern;

public class TodoTxtHighlighter extends BasicTodoTxtHighlighter {

    private final static Pattern LINE_OF_TEXT = Pattern.compile("(?m)(.*)?");

    public TodoTxtHighlighter(final AppSettings as) {
        super(as);
    }

    @Override
    public Highlighter configure(Paint paint) {
        _delay = _appSettings.getHighlightingDelayTodoTxt();
        return super.configure(paint);
    }

    @Override
    public void generateSpans() {

        super.generateSpans();

        // Paragraph space and divider half way up the space
        createSpanForMatches(LINE_OF_TEXT, matcher -> new ParagraphDividerSpan(_textColor, _textSize));
    }

    // Adds spacing and divider line between paragraphs
    // Should implement UpdateLayout, but we rely on the Highlighter to do this
    public static class ParagraphDividerSpan implements LineBackgroundSpan, LineHeightSpan, ShiftText, UpdateAppearance {
        private final int _lineColor;
        private final float _spacing;
        private Integer _origAscent = null;

        public ParagraphDividerSpan(@ColorInt int lineColor, float spacing) {
            _lineColor = lineColor;
            _spacing = spacing;
        }

        @Override
        public void drawBackground(@NonNull Canvas canvas, @NonNull Paint paint, int left, int right, int top, int baseline, int bottom, @NonNull CharSequence text, int start, int end, int lineNumber) {
            if (start > 0 && text.charAt(start - 1) == '\n') {
                paint.setColor(_lineColor);
                paint.setStrokeWidth(0);
                canvas.drawLine(left, top + _spacing / 2, right, top +  _spacing / 2, paint);
            }
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
            if (_origAscent == null) {
                _origAscent = fm.ascent;
            }
            boolean isFirstLineInParagraph = start > 0 && text.charAt(start - 1) == '\n';
            fm.ascent = (isFirstLineInParagraph) ? fm.ascent - (int) _spacing : _origAscent;
        }

        @Override
        public float yShift(final CharSequence text, final int start, final int end, final int index) {
            return index >= start ? _spacing : 0;
        }
    }
}

