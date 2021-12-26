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
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.InputFilter;
import android.text.Spannable;
import android.util.Patterns;

import net.gsantner.markor.App;
import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
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

    private final static Pattern LINK = Patterns.WEB_URL;
    private final static Pattern NEWLINE_CHARACTER = Pattern.compile("(\\n|^)");
    private final static Pattern LINESTART = Pattern.compile("(?m)^.");
    private final static Pattern LINE_OF_TEXT = Pattern.compile("(?m)(.*)?");

    private final static int COLOR_CATEGORY = 0xffef6C00;
    private final static int COLOR_CONTEXT = 0xff88b04b;

    private final static int COLOR_PRIORITY_A = 0xffEF2929;
    private final static int COLOR_PRIORITY_B = 0xffF57900;
    private final static int COLOR_PRIORITY_C = 0xff73D216;
    private final static int COLOR_PRIORITY_D = 0xff0099CC;
    private final static int COLOR_PRIORITY_E = 0xffEDD400;
    private final static int COLOR_PRIORITY_F = 0xff888A85;


    public TodoTxtHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
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

            basicTodoTxtHighlights(spannable, false,  _profiler);

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

    public static Spannable basicTodoTxtHighlights(final Spannable spannable, final boolean clear, NanoProfiler profiler) {
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
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_CONTEXTS, COLOR_CONTEXT);
            profiler.restart("Category");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PROJECTS, COLOR_CATEGORY);
            profiler.restart("KeyValue");
            createStyleSpanForMatches(spannable, TodoTxtTask.PATTERN_KEY_VALUE_PAIRS, Typeface.ITALIC);

            // Priorities
            profiler.restart("Priority Bold");
            createStyleSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_ANY, Typeface.BOLD);
            profiler.restart("Priority A");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_A, COLOR_PRIORITY_A);
            profiler.restart("Priority B");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_B, COLOR_PRIORITY_B);
            profiler.restart("Priority C");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_C, COLOR_PRIORITY_C);
            profiler.restart("Priority D");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_D, COLOR_PRIORITY_D);
            profiler.restart("Priority E");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_E, COLOR_PRIORITY_E);
            profiler.restart("Priority F");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_PRIORITY_F, COLOR_PRIORITY_F);

            profiler.restart("Date Color");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_CREATION_DATE, App.getResouces().getColor(R.color.todo_txt__date), 1);
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_DUE_DATE, COLOR_PRIORITY_A, 2, 3);

            // Strike out done tasks (apply no other to-do.txt span format afterwards)
            profiler.restart("Done BgColor");
            createColorSpanForMatches(spannable, TodoTxtTask.PATTERN_DONE, App.getResouces().getColor(R.color.todo_txt__done));
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

