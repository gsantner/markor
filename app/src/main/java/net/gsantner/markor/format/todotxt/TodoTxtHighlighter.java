/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;

import net.gsantner.markor.format.general.FirstLineTopPaddedParagraphSpan;
import net.gsantner.markor.format.general.HorizontalLineBackgroundParagraphSpan;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

public class TodoTxtHighlighter extends Highlighter {
    private final TodoTxtHighlighterColors colors;

    public TodoTxtHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
        colors = new TodoTxtHighlighterColors();
    }

    @Override
    protected Editable run(final Editable editable) {

        try {
            if (editable.length() == 0) {
                return editable;
            }

            SpannableString editString = new SpannableString(editable.toString());

            _profiler.start(true, "Todo.Txt Highlighting");
            generalHighlightRun(editString);
            _profiler.restart("Paragraph top padding");
            createParagraphStyleSpanForMatches(editString, TodoTxtHighlighterPattern.LINE_OF_TEXT.getPattern(),
                    (matcher, iM) -> new FirstLineTopPaddedParagraphSpan(2f));


            _profiler.restart("Context");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.CONTEXT.getPattern(), colors.getContextColor());
            _profiler.restart("Category");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.PROJECT.getPattern(), colors.getCategoryColor());
            _profiler.restart("KeyValue");
            createStyleSpanForMatches(editString, TodoTxtHighlighterPattern.PATTERN_KEY_VALUE.getPattern(), Typeface.ITALIC);

            // Priorities
            _profiler.restart("Priority Bold");
            createStyleSpanForMatches(editString, TodoTxtHighlighterPattern.PRIORITY_ANY.getPattern(), Typeface.BOLD);
            _profiler.restart("Priority A");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.PRIORITY_A.getPattern(), colors.getPriorityColor(1));
            _profiler.restart("Priority B");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.PRIORITY_B.getPattern(), colors.getPriorityColor(2));
            _profiler.restart("Priority C");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.PRIORITY_C.getPattern(), colors.getPriorityColor(3));
            _profiler.restart("Priority D");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.PRIORITY_D.getPattern(), colors.getPriorityColor(4));
            _profiler.restart("Priority E");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.PRIORITY_E.getPattern(), colors.getPriorityColor(5));
            _profiler.restart("Priority F");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.PRIORITY_F.getPattern(), colors.getPriorityColor(6));

            // Date: Match Creation date before completition date
            _profiler.restart("Date Color");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.DATE.getPattern(), colors.getDateColor());
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.DUE_DATE.getPattern(), colors.getPriorityColor(1), 1);
            //createColorSpanForMatches(editString, TodoTxtHighlighterPattern.CREATION_DATE.getPattern(), 0xff00ff00);
            //createColorSpanForMatches(editString, TodoTxtHighlighterPattern.COMPLETION_DATE.getPattern(), 0xff0000ff);


            // Paragraph divider
            _profiler.restart("Paragraph divider");
            createParagraphStyleSpanForMatches(editString, TodoTxtHighlighterPattern.LINE_OF_TEXT.getPattern(),
                    (matcher, iM) -> new HorizontalLineBackgroundParagraphSpan(_hlEditor.getCurrentTextColor(), 0.8f, _hlEditor.getTextSize() / 2f));

            // Strike out done tasks (apply no other to-do.txt span format afterwards)
            _profiler.restart("Done BgColor");
            createColorSpanForMatches(editString, TodoTxtHighlighterPattern.DONE.getPattern(), colors.getDoneColor());
            _profiler.restart("done Strike");
            createSpanWithStrikeThroughForMatches(editString, TodoTxtHighlighterPattern.DONE.getPattern());

            // Fix for paragraph padding and horizontal rule
            /*
            nprofiler.restart("Single line fix 1");
            createRelativeSizeSpanForMatches(editString, TodoTxtHighlighterPattern.LINESTART.getPattern(), 0.8f);
            nprofiler.restart("Single line fix 2");
            createRelativeSizeSpanForMatches(editString, TodoTxtHighlighterPattern.LINESTART.getPattern(), 1.2f);*/
            _profiler.restart("Single line fix 1");
            //createRelativeSizeSpanForMatches(editString, TodoTxtHighlighterPattern.LINESTART.getPattern(), 1.00001f);
            _profiler.end();
            _profiler.printProfilingGroup();

            editable.replace(0, editable.length(), editString);
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

