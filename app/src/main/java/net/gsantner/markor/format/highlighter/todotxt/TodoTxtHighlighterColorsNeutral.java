/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.todotxt;

// Neutral means good readability in both, light and dark theme
@SuppressWarnings("FieldCanBeLocal")
public class TodoTxtHighlighterColorsNeutral implements TodoTxtHighlighterColors {

    private final int COLOR_CATEGORY = 0xffef6C00;
    private final int COLOR_CONTEXT = 0xff88b04b;
    private final int COLOR_LINK = 0xff1ea3fd;

    @Override
    public int getContextColor() {
        return COLOR_CONTEXT;
    }

    @Override
    public int getLinkColor() {
        return COLOR_LINK;
    }

    @Override
    public int getPriorityColor(int priority) {
        switch (priority) {
            case 1:
                return 0xffEF2929;
            case 2:
                return 0xffF57900;
            case 3:
                return 0xff73D216;
            case 4:
                return 0xff0099CC;
            case 5:
                return 0xffEDD400;
            default:
            case 6:
                return 0xff888A85;
        }
    }

    @Override
    public int getDoneColor() {
        return 0x993d3d3d;
    }

    @Override
    public int getDateColor() {
        return 0xcc4d4d4d;
    }

    @Override
    public int getCategoryColor() {
        return COLOR_CATEGORY;
    }
}
