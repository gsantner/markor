/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.editor.highlighter.markdown;

public class MyHighlighterColorsNeutral implements HighlighterColors {

    private final int COLOR_HEADER = 0xffef6C00;
    private final int COLOR_LINK = 0xff1ea3fd;
    private final int COLOR_LIST = 0xffdaa520;
    private final int COLOR_QUOTE = 0xff88b04b;

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

    @Override
    public int getQuotationColor() {
        return COLOR_QUOTE;
    }

}
