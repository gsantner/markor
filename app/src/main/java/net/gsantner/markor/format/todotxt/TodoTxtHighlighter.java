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
import android.text.InputFilter;
import android.text.Spannable;
import android.util.Patterns;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.format.general.FirstLineTopPaddedParagraphSpan;
import net.gsantner.markor.format.general.HorizontalLineBackgroundParagraphSpan;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.NanoProfiler;

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

            basicTodoTxtHighlights(spannable, false, colors, _appSettings.isDarkThemeEnabled(), _profiler);

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

    public static Spannable basicTodoTxtHighlights(
            final Spannable spannable,
            final boolean clear,
            final TodoTxtHighlighterColors colors,
            final boolean isDarkBg,
            NanoProfiler profiler
    ) {
        try {
            if (clear) {
                clearSpans(spannable);
            }

            if (spannable.length() == 0) {
                return spannable;
            }

            if (profiler == null) {
                profiler = new NanoProfiler().setEnabled(BuildConfig.IS_TEST_BUILD || MainActivity.IS_DEBUG_ENABLED);
            }

            profiler.restart("Context");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_CONTEXTS, colors.getContextColor());
            profiler.restart("Category");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PROJECTS, colors.getCategoryColor());
            profiler.restart("KeyValue");
            createStyleSpanForMatches(spannable, TodoTxtTask.PATTERN_KEY_VALUE_PAIRS, Typeface.ITALIC);

            // Priorities
            profiler.restart("Priority Bold");
            createStyleSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_ANY, Typeface.BOLD);
            profiler.restart("Priority A");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_A, colors.getPriorityColor(1));
            profiler.restart("Priority B");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_B, colors.getPriorityColor(2));
            profiler.restart("Priority C");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_C, colors.getPriorityColor(3));
            profiler.restart("Priority D");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_D, colors.getPriorityColor(4));
            profiler.restart("Priority E");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_E, colors.getPriorityColor(5));
            profiler.restart("Priority F");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_F, colors.getPriorityColor(6));

            // Date: Match Creation date before completition date
            profiler.restart("Date Color");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_DATE, colors.getDateColor(isDarkBg));
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_DUE_DATE, colors.getPriorityColor(1), 2, 3);

            // Strike out done tasks (apply no other to-do.txt span format afterwards)
            profiler.restart("Done BgColor");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_DONE, colors.getDoneColor(isDarkBg));
            profiler.restart("done Strike");
            createSpanWithStrikeThroughForMatches(spannable, TodoTxtTask.PATTERN_DONE);

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

