package net.gsantner.markor.frontend.textsearch;

import androidx.appcompat.app.AppCompatDelegate;

public class Selection {
    public static final int COLOR = 0x605050FF;
    public static final int COLOR_DARK = 0x609090FF;

    private int start; // Start index
    private int end; // End index

    public int getColor() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            return COLOR_DARK;
        } else {
            return COLOR;
        }
    }

    public void reset() {
        start = 0;
        end = 0;
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

    public boolean isSelected() {
        return this.start != this.end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}
