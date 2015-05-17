package me.writeily.editor;

class MyHighlighterColorsNeutral implements HighlighterColors {

    private final int COLOR_HEADER = 0xffef6C00;
    private final int COLOR_LINK = 0xff1ea3fd;
    private final int COLOR_LIST = COLOR_HEADER;

    @Override
    public int getHeaderColor() {
        return COLOR_HEADER;
    }

    @Override
    public int getLinkColor() {
        return COLOR_LINK;
    }

    @Override
    public int getListColor() {
        return COLOR_LIST;
    }
}
