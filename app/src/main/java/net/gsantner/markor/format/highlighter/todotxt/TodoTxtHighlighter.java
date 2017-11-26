/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.todotxt;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.format.highlighter.Highlighter;
import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.format.highlighter.general.HorizontalLineBackgroundParagraphSpan;
import net.gsantner.markor.util.AppSettings;

public class TodoTxtHighlighter extends Highlighter {
    private final TodoTxtHighlighterColors colors;
    public final String fontType;
    public final Integer fontSize;

    public TodoTxtHighlighter() {
        colors = new TodoTxtHighlighterColorsNeutral();
        fontType = AppSettings.get().getFontFamily();
        fontSize = AppSettings.get().getFontSize();
    }

    @Override
    protected Editable run(final HighlightingEditor editor, final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.CONTEXT.getPattern(), colors.getContextColor());
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.CATEGORY.getPattern(), colors.getCategoryColor());
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.KEYVALUE.getPattern(), Typeface.ITALIC);

            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(), colors.getLinkColor());
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(),0.7f);
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(), Typeface.ITALIC);

            // Priorities
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_ANY.getPattern(), Typeface.BOLD);
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_A.getPattern(), colors.getPriorityColor(1));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_B.getPattern(), colors.getPriorityColor(2));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_C.getPattern(), colors.getPriorityColor(3));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_D.getPattern(), colors.getPriorityColor(4));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_E.getPattern(), colors.getPriorityColor(5));
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_F.getPattern(), colors.getPriorityColor(6));

            // Date
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.DATE.getPattern(), colors.getDateColor());
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.DATE.getPattern(),0.8f);

            // Paragraph divider
            createLineBackgroundSpanForMatches(editable, TodoTxtHighlighterPattern.LINE_OF_TEXT.getPattern(),
                    (matcher, iM) -> new HorizontalLineBackgroundParagraphSpan(editor.getCurrentTextColor()));

            // Do this at the end
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.DONE.getPattern(), colors.getDoneColor());
            createSpanWithStrikeThroughForMatches(editable, TodoTxtHighlighterPattern.DONE.getPattern());

        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new TodoTxtAutoFormat();
    }

}

