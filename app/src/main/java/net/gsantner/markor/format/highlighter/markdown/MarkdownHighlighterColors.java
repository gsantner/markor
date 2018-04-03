/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.markdown;

// Neutral means good readability in both, light and dark theme
@SuppressWarnings({"FieldCanBeLocal", "SpellCheckingInspection", "WeakerAccess"})
public class MarkdownHighlighterColors {
    private final int COLOR_HEADER = 0xffef6D00;
    private final int COLOR_LINK = 0xff1ea3fe;
    private final int COLOR_LIST = 0xffdaa521;
    private final int COLOR_QUOTE = 0xff88b04c;
    private final int COLOR_DOUBLESPACE = 0xffe0e1e0;

    public int getHeaderColor() {
        return COLOR_HEADER;
    }

    public int getLinkColor() {
        return COLOR_LINK;
    }

    public int getListColor() {
        return COLOR_LIST;
    }

    public int getDoublespaceColor() {
        return COLOR_DOUBLESPACE;
    }

    public int getQuotationColor() {
        return COLOR_QUOTE;
    }

}
