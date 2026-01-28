package net.gsantner.markor.frontend.textsearch;

import androidx.appcompat.app.AppCompatDelegate;

public class Selection {
    public static final int COLOR = 0x605050FF;
    public static final int COLOR_DARK = 0x609090FF;

    private int startIndex;
    private int endIndex;

    public int getColor() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            return COLOR_DARK;
        } else {
            return COLOR;
        }
    }

    public void reset() {
        startIndex = 0;
        endIndex = 0;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getLength() {
        return this.endIndex - this.startIndex;
    }

    public boolean isSelected() {
        return this.startIndex != this.endIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
}
