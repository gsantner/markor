package net.gsantner.markor.frontend.search;

public class Occurrence {
    public static final int BACKGROUND_COLOR = 0x90FF0000;
    public static final int SPECIAL_BACKGROUND_COLOR = 0x90FFA500;
    private BackgroundColorSpan backgroundColorSpan;

    private int startIndex;
    private int endIndex;

    public BackgroundColorSpan createBackgroundColorSpan() {
        this.backgroundColorSpan = new BackgroundColorSpan(BACKGROUND_COLOR);
        return this.backgroundColorSpan;
    }

    public BackgroundColorSpan createSpecialBackgroundColorSpan() {
        this.backgroundColorSpan = new BackgroundColorSpan(SPECIAL_BACKGROUND_COLOR);
        return this.backgroundColorSpan;
    }

    public BackgroundColorSpan getBackgroundColorSpan() {
        return this.backgroundColorSpan;
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

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public void offsetStartIndex(int offset) {
        this.startIndex += offset;
    }

    public void offsetEndIndex(int offset) {
        this.endIndex += offset;
    }
}
