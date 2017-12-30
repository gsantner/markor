/*
 * Copyright (c) 2017-2018 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.todotxt;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.format.highlighter.Highlighter;
import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.format.highlighter.general.FirstLineTopPaddedParagraphSpan;
import net.gsantner.markor.format.highlighter.general.HorizontalLineBackgroundParagraphSpan;
import net.gsantner.markor.util.AppSettings;

public class TodoTxtHighlighter extends Highlighter {
    private final TodoTxtHighlighterColors colors;
    public final String fontType;
    public final Integer fontSize;

    public TodoTxtHighlighter() {
        colors = new TodoTxtHighlighterColors();
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

            _profiler.start(true, "Todo.Txt Highlighting");
            _profiler.restart("Paragraph top padding");
            createParagraphStyleSpanForMatches(editable, TodoTxtHighlighterPattern.LINE_OF_TEXT.getPattern(),
                    (matcher, iM) -> new FirstLineTopPaddedParagraphSpan(2f));


            _profiler.restart("Context");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.CONTEXT.getPattern(), colors.getContextColor());
            _profiler.restart("Category");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PROJECT.getPattern(), colors.getCategoryColor());
            _profiler.restart("KeyValue");
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.PATTERN_KEY_VALUE.getPattern(), Typeface.ITALIC);

            _profiler.restart("Link Color");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(), colors.getLinkColor());
            _profiler.restart("Link Size");
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(), 0.7f);
            _profiler.restart("Link Italic");
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.LINK.getPattern(), Typeface.ITALIC);

            // Priorities
            _profiler.restart("Priority Bold");
            createStyleSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_ANY.getPattern(), Typeface.BOLD);
            _profiler.restart("Priority A");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_A.getPattern(), colors.getPriorityColor(1));
            _profiler.restart("Priority B");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_B.getPattern(), colors.getPriorityColor(2));
            _profiler.restart("Priority C");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_C.getPattern(), colors.getPriorityColor(3));
            _profiler.restart("Priority D");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_D.getPattern(), colors.getPriorityColor(4));
            _profiler.restart("Priority E");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_E.getPattern(), colors.getPriorityColor(5));
            _profiler.restart("Priority F");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.PRIORITY_F.getPattern(), colors.getPriorityColor(6));

            // Date: Match Creation date before completition date
            _profiler.restart("Date Color");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.DATE.getPattern(), colors.getDateColor());
            //createColorSpanForMatches(editable, TodoTxtHighlighterPattern.CREATION_DATE.getPattern(), 0xff00ff00);
            //createColorSpanForMatches(editable, TodoTxtHighlighterPattern.COMPLETION_DATE.getPattern(), 0xff0000ff);


            // Paragraph divider
            _profiler.restart("Paragraph divider");
            createParagraphStyleSpanForMatches(editable, TodoTxtHighlighterPattern.LINE_OF_TEXT.getPattern(),
                    (matcher, iM) -> new HorizontalLineBackgroundParagraphSpan(editor.getCurrentTextColor(), 0.8f, editor.getTextSize() / 2f));

            // Strike out done tasks (apply no other to-do.txt span format afterwards)
            _profiler.restart("Done BgColor");
            createColorSpanForMatches(editable, TodoTxtHighlighterPattern.DONE.getPattern(), colors.getDoneColor());
            _profiler.restart("done Strike");
            createSpanWithStrikeThroughForMatches(editable, TodoTxtHighlighterPattern.DONE.getPattern());

            // Fix for paragraph padding and horizontal rule
            /*
            nprofiler.restart("Single line fix 1");
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINESTART.getPattern(), 0.8f);
            nprofiler.restart("Single line fix 2");
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINESTART.getPattern(), 1.2f);*/
            _profiler.restart("Single line fix 1");
            createRelativeSizeSpanForMatches(editable, TodoTxtHighlighterPattern.LINESTART.getPattern(), 1.00001f);
            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new TodoTxtAutoFormat();
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return new AppSettings(context).getHighlightingDelayTodoTxt();
    }

}

