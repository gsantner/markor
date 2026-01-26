package net.gsantner.markor.frontend.textsearch;

public class Selection {
    public final int color = 0x500000FF;

    private int startIndex;
    private int endIndex;

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
