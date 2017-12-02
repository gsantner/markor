/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.todotxt;

public interface TodoTxtHighlighterColors {

    int getContextColor();

    int getLinkColor();

    int getCategoryColor();

    int getPriorityColor(int priority);

    int getDoneColor();

    int getDateColor();
}
