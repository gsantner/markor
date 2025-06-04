package net.gsantner.markor.frontend.search;

public class Selection {
    public static final int BACKGROUND_COLOR = 0x500000FF;
    private BackgroundColorSpan backgroundColorSpan;

    private int startIndex;
    private int endIndex;

    public BackgroundColorSpan createBackgroundColorSpan() {
        this.backgroundColorSpan = new BackgroundColorSpan(BACKGROUND_COLOR);
        return this.backgroundColorSpan;
    }

    public BackgroundColorSpan getBackgroundColorSpan() {
        return this.backgroundColorSpan;
    }

    public void reset() {
        startIndex = 0;
        endIndex = 0;
        backgroundColorSpan = null;
    }

    public static class BackgroundColorSpan extends android.text.style.BackgroundColorSpan {
        public BackgroundColorSpan(int color) {
            super(color);
        }
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
