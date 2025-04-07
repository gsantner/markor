package net.gsantner.markor.frontend.search;

public class Occurrence {
    public static final int BACKGROUND_COLOR = 0x80FF0000;
    public static final int SPECIAL_BACKGROUND_COLOR = 0x80FFA500;
    private BackgroundColorSpan backgroundColorSpan;

    public int startIndex;
    public int endIndex;
    public int length;

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
}
