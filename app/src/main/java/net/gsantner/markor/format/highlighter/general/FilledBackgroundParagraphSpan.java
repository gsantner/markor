/*
 * Copyright (c) 2017-2018 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.general;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ParagraphStyle;

import net.gsantner.markor.format.highlighter.ParagraphStyleCreator;

import java.util.regex.Matcher;

public class FilledBackgroundParagraphSpan extends BackgroundParagraphSpan {
    private final int _color;

    public FilledBackgroundParagraphSpan(int color) {
        _color = color;
    }


    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum, boolean isFirstLineOfParagraph) {

        p.setColor(_color);
        c.drawRect(left, top, right, bottom, p);
    }

    //
    //
    //

    public static class EverySecondLineSpanCreatorP implements ParagraphStyleCreator {
        private int _color;

        public EverySecondLineSpanCreatorP(int color) {
            _color = color;
        }

        @Override
        public ParagraphStyle create(Matcher matcher, int iM) {
            return (iM == 0 || iM % 2 == 1)
                    ? null : new FilledBackgroundParagraphSpan(_color
            );
        }
    }
}
