package net.gsantner.markor.frontend.textsearch;

import androidx.annotation.ColorInt;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;

public class Match {
    public static final @ColorInt int MATCH_COLOR = 0x60FF0000;
    public static final @ColorInt int ACTIVE_MATCH_COLOR = 0x60FFA500;

    public SyntaxHighlighterBase.SpanGroup spanGroup;
    private SyntaxHighlighterBase.HighlightSpan span;

    public Match() {
        spanGroup = SyntaxHighlighterBase.createBackgroundHighlight(0, 0, 0);
        span = (SyntaxHighlighterBase.HighlightSpan) spanGroup.span;
    }

    public void useMatchColor() {
        span.setBackColor(MATCH_COLOR);
    }

    public void useActiveMatchColor() {
        span.setBackColor(ACTIVE_MATCH_COLOR);
    }

    public int getStart() {
        return spanGroup.start;
    }

    public int getEnd() {
        return spanGroup.end;
    }

    public int getLength() {
        return spanGroup.end - spanGroup.start;
    }

    public void setStart(int start) {
        spanGroup.start = start;
    }

    public void setEnd(int end) {
        spanGroup.end = end;
    }

    public void shiftStart(int offset) {
        spanGroup.start += offset;
    }

    public void shiftEnd(int offset) {
        spanGroup.end += offset;
    }
}
