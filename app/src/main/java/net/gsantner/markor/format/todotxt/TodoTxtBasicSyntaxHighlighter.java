package net.gsantner.markor.format.todotxt;

import android.graphics.Typeface;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

public class TodoTxtBasicSyntaxHighlighter extends SyntaxHighlighterBase {

    private final static int COLOR_CATEGORY = 0xffef6C00;
    private final static int COLOR_CONTEXT = 0xff88b04b;

    private final static int COLOR_PRIORITY_A = 0xffEF2929;
    private final static int COLOR_PRIORITY_B = 0xffF57900;
    private final static int COLOR_PRIORITY_C = 0xff73D216;
    private final static int COLOR_PRIORITY_D = 0xff0099CC;
    private final static int COLOR_PRIORITY_E = 0xffEDD400;
    private final static int COLOR_PRIORITY_F = 0xff888A85;

    private final static int COLOR_DONE_DARK = 0x999d9d9d;
    private final static int COLOR_DONE_LIGHT = 0x993d3d3d;
    private final static int COLOR_DATE_DARK = COLOR_DONE_DARK;
    private final static int COLOR_DATE_LIGHT = 0xcc6d6d6d;

    public TodoTxtBasicSyntaxHighlighter(final AppSettings as) {
        super(as);
    }

    @Override
    public void generateSpans() {
        createSmallBlueLinkSpans();
        createColorSpanForMatches(TodoTxtTask.PATTERN_CONTEXTS, COLOR_CONTEXT);
        createColorSpanForMatches(TodoTxtTask.PATTERN_PROJECTS, COLOR_CATEGORY);
        createStyleSpanForMatches(TodoTxtTask.PATTERN_KEY_VALUE_PAIRS, Typeface.ITALIC);

        // Priorities
        createSpanForMatches(TodoTxtTask.PATTERN_PRIORITY_A, new HighlightSpan().setForeColor(COLOR_PRIORITY_A).setBold(true));
        createSpanForMatches(TodoTxtTask.PATTERN_PRIORITY_B, new HighlightSpan().setForeColor(COLOR_PRIORITY_B).setBold(true));
        createSpanForMatches(TodoTxtTask.PATTERN_PRIORITY_C, new HighlightSpan().setForeColor(COLOR_PRIORITY_C).setBold(true));
        createSpanForMatches(TodoTxtTask.PATTERN_PRIORITY_D, new HighlightSpan().setForeColor(COLOR_PRIORITY_D).setBold(true));
        createSpanForMatches(TodoTxtTask.PATTERN_PRIORITY_E, new HighlightSpan().setForeColor(COLOR_PRIORITY_E).setBold(true));
        createSpanForMatches(TodoTxtTask.PATTERN_PRIORITY_F, new HighlightSpan().setForeColor(COLOR_PRIORITY_F).setBold(true));
        createStyleSpanForMatches(TodoTxtTask.PATTERN_PRIORITY_G_TO_Z, Typeface.BOLD);

        createColorSpanForMatches(TodoTxtTask.PATTERN_CREATION_DATE, _isDarkMode ? COLOR_DATE_DARK : COLOR_DATE_LIGHT, 1);
        createColorSpanForMatches(TodoTxtTask.PATTERN_DUE_DATE, COLOR_PRIORITY_A, 2, 3);

        // Strike out done tasks
        // Note - as we now sort by start, projects, contexts, tags and due date will be highlighted for done tasks
        createSpanForMatches(TodoTxtTask.PATTERN_DONE, new HighlightSpan().setForeColor(_isDarkMode ? COLOR_DONE_DARK : COLOR_DONE_LIGHT).setStrike(true));
    }
}