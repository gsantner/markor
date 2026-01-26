package net.gsantner.markor.frontend.search;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class Match implements Cloneable {
    public static final @ColorInt int MATCH_COLOR = 0x60FF0000;
    public static final @ColorInt int ACTIVE_MATCH_COLOR = 0x60FFA500;
    public @ColorInt int color;

    private int start;
    private int end;

    @NonNull
    @Override
    protected Match clone() throws CloneNotSupportedException {
        return (Match) super.clone();
    }

    public void applyMatchColor() {
        color = MATCH_COLOR;
    }

    public void applyActiveMatchColor() {
        color = ACTIVE_MATCH_COLOR;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getLength() {
        return this.end - this.start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void offsetStart(int offset) {
        this.start += offset;
    }

    public void offsetEnd(int offset) {
        this.end += offset;
    }
}
