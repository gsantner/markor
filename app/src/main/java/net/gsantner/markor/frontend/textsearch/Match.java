package net.gsantner.markor.frontend.textsearch;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatDelegate;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;

public class Match {
    public static final @ColorInt int MATCH_COLOR = 0x80FFAF00;
    public static final @ColorInt int ACTIVE_MATCH_COLOR = 0x70FF0000;
    public static final @ColorInt int MATCH_COLOR_DARK = 0x90FFAA00;
    public static final @ColorInt int ACTIVE_MATCH_COLOR_DARK = 0x90FF0000;

    public SyntaxHighlighterBase.SpanGroup spanGroup;
    private SyntaxHighlighterBase.HighlightSpan span;

    public Match() {
        spanGroup = SyntaxHighlighterBase.createBackgroundHighlight(0, 0, 0);
        span = (SyntaxHighlighterBase.HighlightSpan) spanGroup.span;
    }

    public void useMatchColor() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            span.setBackColor(MATCH_COLOR_DARK);
        } else {
            span.setBackColor(MATCH_COLOR);
        }
    }

    public void useActiveMatchColor() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            span.setBackColor(ACTIVE_MATCH_COLOR_DARK);
        } else {
            span.setBackColor(ACTIVE_MATCH_COLOR);
        }
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
