/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Spannable;
import android.text.style.LineBackgroundSpan;
import android.text.style.LineHeightSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.regex.Pattern;

public class TodoTxtSyntaxHighlighter extends TodoTxtBasicSyntaxHighlighter {

    private final static Pattern LINE_OF_TEXT = Pattern.compile("(?m)(.*)?");
    private ParagraphDividerSpan _paragraphSpan;

    public TodoTxtSyntaxHighlighter(final AppSettings as) {
        super(as);
    }

    @Override
    public SyntaxHighlighterBase configure(Paint paint) {
        super.configure(paint);

        _delay = _appSettings.getHighlightingDelayTodoTxt();
        _paragraphSpan = new ParagraphDividerSpan(paint, _textColor);

        return this;
    }

    @Override
    public void generateSpans() {

        // Single span for the whole text - highly performant
        addSpanGroup(_paragraphSpan, 0, _spannable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        super.generateSpans();
    }

    // Adds spacing and divider line between paragraphs
    public static class ParagraphDividerSpan implements LineBackgroundSpan, LineHeightSpan, StaticSpan {
        private final @ColorInt int _lineColor;
        private final int _top, _ascent, _descent, _bottom, _offset;

        public ParagraphDividerSpan(final Paint paint, final @ColorInt int lineColor) {
            final Paint.FontMetricsInt fm = paint.getFontMetricsInt();
            _lineColor = lineColor;
            _offset = Math.abs(fm.ascent) / 2;
            _top = fm.top;
            _ascent = fm.ascent;
            _descent = fm.descent;
            _bottom = fm.bottom;
        }

        @Override
        public void drawBackground(@NonNull Canvas canvas, @NonNull Paint paint, int left, int right, int top, int baseline, int bottom, @NonNull CharSequence text, int start, int end, int lineNumber) {
            if (end > 0 && text.charAt(end - 1) == '\n') {
                paint.setColor(_lineColor);
                paint.setStrokeWidth(0);
                canvas.drawLine(left, bottom, right, bottom, paint);
            }
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
            fm.top = _top;
            fm.ascent = _ascent;
            fm.descent = _descent;
            fm.bottom = _bottom;

            if (start > 0 && text.charAt(start - 1) == '\n') {
                fm.top = _top - _offset;
                fm.ascent = _ascent - _offset;
            }

            if (end > 0 && text.charAt(end - 1) == '\n') {
                fm.descent = _descent + _offset;
                fm.bottom = _bottom + _offset;
            }
        }
    }
}

