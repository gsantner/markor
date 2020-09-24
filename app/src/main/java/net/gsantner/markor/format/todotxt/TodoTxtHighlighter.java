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
import android.text.Spannable;
import android.text.InputFilter;
import android.util.Patterns;

import net.gsantner.markor.format.general.FirstLineTopPaddedParagraphSpan;
import net.gsantner.markor.format.general.HorizontalLineBackgroundParagraphSpan;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Pattern;

public class TodoTxtHighlighter extends Highlighter {
    private final TodoTxtHighlighterColors colors;

    private final Pattern LINK = Patterns.WEB_URL;
    private final Pattern NEWLINE_CHARACTER = Pattern.compile("(\\n|^)");
    private final Pattern LINESTART = Pattern.compile("(?m)^.");
    private final Pattern LINE_OF_TEXT = Pattern.compile("(?m)(.*)?");

    public TodoTxtHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
        colors = new TodoTxtHighlighterColors();
    }

    @Override
    protected Spannable run(final Spannable spannable) {
        try {
            clearSpans(spannable);

            if (spannable.length() == 0) {
                return spannable;
            }

            _profiler.start(true, "Todo.Txt Highlighting");
            generalHighlightRun(spannable);
            _profiler.restart("Paragraph top padding");
            createParagraphStyleSpanForMatches(spannable, LINE_OF_TEXT,
                    (matcher, iM) -> new FirstLineTopPaddedParagraphSpan(2f));

            basicHighlights(spannable, false);

            // Paragraph divider
            _profiler.restart("Paragraph divider");
            createParagraphStyleSpanForMatches(spannable, LINE_OF_TEXT,
                    (matcher, iM) -> new HorizontalLineBackgroundParagraphSpan(_hlEditor.getCurrentTextColor(), 0.8f, _hlEditor.getTextSize() / 2f));

            // Fix for paragraph padding and horizontal rule
            /*
            nprofiler.restart("Single line fix 1");
            createRelativeSizeSpanForMatches(spannable, LINESTART, 0.8f);
            nprofiler.restart("Single line fix 2");
            createRelativeSizeSpanForMatches(spannable, LINESTART, 1.2f);*/
            _profiler.restart("Single line fix 1");
            createRelativeSizeSpanForMatches(spannable, LINESTART, 1.00001f);
            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return spannable;
    }

    public Spannable basicHighlights(final Spannable spannable, final boolean clear) {
        try {
            if (clear) {
                clearSpans(spannable);
            }

            if (spannable.length() == 0) {
                return spannable;
            }

            final boolean isDarkBg = _appSettings.isDarkThemeEnabled();

            _profiler.restart("Context");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_CONTEXTS, colors.getContextColor());
            _profiler.restart("Category");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PROJECTS, colors.getCategoryColor());
            _profiler.restart("KeyValue");
            createStyleSpanForMatches(spannable, TodoTxtTask.PATTERN_KEY_VALUE_PAIRS, Typeface.ITALIC);

            // Priorities
            _profiler.restart("Priority Bold");
            createStyleSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_ANY, Typeface.BOLD);
            _profiler.restart("Priority A");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_A, colors.getPriorityColor(1));
            _profiler.restart("Priority B");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_B, colors.getPriorityColor(2));
            _profiler.restart("Priority C");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_C, colors.getPriorityColor(3));
            _profiler.restart("Priority D");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_D, colors.getPriorityColor(4));
            _profiler.restart("Priority E");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_E, colors.getPriorityColor(5));
            _profiler.restart("Priority F");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_F, colors.getPriorityColor(6));

            // Date: Match Creation date before completition date
            _profiler.restart("Date Color");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_DATE, colors.getDateColor(isDarkBg));
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_DUE_DATE, colors.getPriorityColor(1), 2, 3);
            //createColorSpanForMatches(spannable, TodoTxtTask.CREATION_DATE, 0xff00ff00);
            //createColorSpanForMatches(spannable, TodoTxtTask.COMPLETION_DATE, 0xff0000ff);

            // Strike out done tasks (apply no other to-do.txt span format afterwards)
            _profiler.restart("Done BgColor");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_DONE, colors.getDoneColor(isDarkBg));
            _profiler.restart("done Strike");
            createSpanWithStrikeThroughForMatches(spannable, TodoTxtTask.PATTERN_DONE);

            // Fix for paragraph padding and horizontal rule
            /*
            nprofiler.restart("Single line fix 1");
            createRelativeSizeSpanForMatches(spannable, LINESTART, 0.8f);
            nprofiler.restart("Single line fix 2");
            createRelativeSizeSpanForMatches(spannable, LINESTART, 1.2f);*/
            _profiler.restart("Single line fix 1");
            createRelativeSizeSpanForMatches(spannable, LINESTART, 1.00001f);
            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return spannable;
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

