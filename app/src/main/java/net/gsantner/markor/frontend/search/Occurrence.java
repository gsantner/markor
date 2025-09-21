package net.gsantner.markor.frontend.search;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class Occurrence implements Cloneable {
    public static final @ColorInt int NORMAL_HIGHLIGHT_COLOR = 0x60FF0000;
    public static final @ColorInt int FOCUSED_HIGHLIGHT_COLOR = 0x60FFA500;
    public @ColorInt int color = 0;

    private int start;
    private int end;

    @NonNull
    @Override
    protected Occurrence clone() throws CloneNotSupportedException {
        return (Occurrence) super.clone();
    }

    public void applyNormalColor() {
        color = NORMAL_HIGHLIGHT_COLOR;
    }

    public void applyFocusedColor() {
        color = FOCUSED_HIGHLIGHT_COLOR;
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
