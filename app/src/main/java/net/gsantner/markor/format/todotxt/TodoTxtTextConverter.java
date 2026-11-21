/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.webkit.WebView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.text.TextUtilsCompat;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsContextUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class TodoTxtTextConverter extends TextConverterBase {

    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: " + TOKEN_FONT + "' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";
    public static final Pattern TODOTXT_FILE_PATTERN = Pattern.compile("(?i)(^todo[-.]?.*)|(.*[-.]todo\\.((txt)|(text))$)");
    private static final String PREVIEW_ASSETS = "<meta name='viewport' content='width=device-width, initial-scale=1'>"
            + "<link rel='stylesheet' href='file:///android_asset/todotxt/preview.css'>"
            + "<script defer src='file:///android_asset/todotxt/preview.js'></script>";
    private static final Pattern WORD = Pattern.compile("\\S+");


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean lightMode, boolean lineNum, File file) {
        String converted = "", onLoadJs = "", head = "";
        converted = HTML100_BODY_PRE_BEGIN
                + parse(TextUtilsCompat.htmlEncode(markup))
                + HTML101_BODY_PRE_END;
        return putContentIntoTemplate(context, converted, lightMode, file, onLoadJs, head);
    }

    private String parse(String str) {
        str = str.replace("\n", "</br><span style='margin-bottom=20px;'/><hr/><span style='margin-bottom=20px;'/>");
        return str;
    }

    void showInteractivePreview(final Document document, final Activity activity, final WebView webView, final String text, final String revision) {
        final boolean dark = GsContextUtils.instance.isDarkModeEnabled(activity);
        final String filter = escapedString(activity, R.string.filter);
        final String rowsMarker = "<!-- " + revision + " -->";
        final String content = "<section id='markor-todo' data-revision='" + revision + "'>"
                + "<div class='todo-controls'>"
                + "<input type='search' id='todo-filter' placeholder='" + filter + "' aria-label='" + filter + "'>"
                + "<select id='todo-status' aria-label='" + escapedString(activity, R.string.todo_filter_status) + "'>"
                + "<option value='all'>" + escapedString(activity, R.string.todo_filter_all) + "</option>"
                + "<option value='open'>" + escapedString(activity, R.string.todo_filter_open) + "</option>"
                + "<option value='done'>" + escapedString(activity, R.string.completed) + "</option>"
                + "</select>"
                + "<button type='button' id='todo-sort'>" + escapedString(activity, R.string.todo_sort_file) + "</button>"
                + "</div><div id='todo-rows' role='list'>" + rowsMarker + "</div>"
                + "<p id='todo-empty' role='status' hidden>" + escapedString(activity, R.string.no_results) + "</p></section>";
        final String html = putContentIntoTemplate(activity, content, false, document.file, "", PREVIEW_ASSETS + previewColors(activity, dark))
                .replace(rowsMarker, renderRows(activity, text));
        loadHtmlInWebView(document, activity, webView, html);
    }

    private static String previewColors(final Activity activity, final boolean dark) {
        final boolean black = AppSettings.get(activity).getAppThemeName().contains("black");
        final int background = black ? Color.BLACK : ContextCompat.getColor(activity, R.color.background);
        final int controls = dark ? ColorUtils.blendARGB(background, Color.WHITE, 0.06f) : Color.WHITE;
        final int foreground = ColorUtils.compositeColors(ContextCompat.getColor(activity, R.color.primary_text), background);
        final int muted = ColorUtils.compositeColors(ContextCompat.getColor(activity, R.color.secondary_text), background);
        return CSS_S + ":root{color-scheme:" + (dark ? "dark" : "light")
                + ";--todo-background:" + GsTextUtils.colorToHexString(background)
                + ";--todo-control:" + GsTextUtils.colorToHexString(controls)
                + ";--todo-foreground:" + GsTextUtils.colorToHexString(foreground)
                + ";--todo-muted:" + GsTextUtils.colorToHexString(muted)
                + ";--todo-divider:" + GsTextUtils.colorToHexString(ContextCompat.getColor(activity, R.color.light_border))
                + ";--todo-accent:" + TOKEN_ACCENT_COLOR + "}" + CSS_E;
    }

    static String renderRows(final Context context, final CharSequence text) {
        final StringBuilder html = new StringBuilder();
        GsTextUtils.forEachline(text, (line, start, end) -> {
            final int taskEnd = end > start && text.charAt(end - 1) == '\r' ? end - 1 : end;
            html.append(renderRow(context, text.subSequence(start, taskEnd).toString(), line));
            return true;
        });
        return html.toString();
    }

    static String renderRow(final Context context, final String line, final int index) {
        if (line.trim().isEmpty()) {
            return "";
        }
        final TodoTxtTask task = new TodoTxtTask(line);
        final boolean done = task.isDone();
        final String checkboxLabel = TextUtilsCompat.htmlEncode(context.getString(R.string.todo_toggle_task, line));
        final String editLabel = TextUtilsCompat.htmlEncode(context.getString(R.string.todo_edit_task, line));
        return "<div class='todo-row" + (done ? " todo-done" : "") + "' role='listitem' data-line='" + index + "'>"
                + "<label class='todo-check'><input type='checkbox' aria-label='" + checkboxLabel + "'" + (done ? " checked" : "") + "></label>"
                + "<button type='button' class='todo-text' aria-label='" + editLabel + "' dir='auto'>"
                + highlight(task) + "</button></div>";
    }

    private static String highlight(final TodoTxtTask task) {
        final String line = task.getLine();
        final Matcher word = WORD.matcher(line);
        final Matcher date = TodoTxtTask.PATTERN_CREATION_DATE.matcher(line);
        final int dateStart = date.find() ? date.start(1) : -1;
        int end = task.isDone() && line.length() > 2 ? 2 : 0;
        word.region(end, line.length());
        final StringBuilder html = new StringBuilder();
        while (word.find()) {
            html.append(TextUtilsCompat.htmlEncode(line.substring(end, word.start())));
            final String token = word.group();
            String style = "";
            if (word.start() == 0 && task.getPriority() != TodoTxtTask.PRIORITY_NONE) {
                style = "todo-priority priority-" + task.getPriority();
            } else if (word.start() == dateStart) {
                style = "todo-date";
            } else if (TodoTxtTask.PATTERN_PROJECTS.matcher(token).matches()) {
                style = "todo-project";
            } else if (TodoTxtTask.PATTERN_CONTEXTS.matcher(token).matches()) {
                style = "todo-context";
            } else if (token.indexOf(':') >= 0) {
                if (TodoTxtTask.PATTERN_DUE_DATE.matcher(token).matches()) {
                    style = "todo-due todo-tag";
                } else if (TodoTxtTask.PATTERN_KEY_VALUE_PAIRS.matcher(token).matches()) {
                    style = "todo-tag";
                }
            }
            if (style.isEmpty()) {
                html.append(TextUtilsCompat.htmlEncode(token));
            } else {
                html.append("<span class='").append(style).append("'>").append(TextUtilsCompat.htmlEncode(token)).append("</span>");
            }
            end = word.end();
        }
        return html.append(TextUtilsCompat.htmlEncode(line.substring(end))).toString();
    }

    private static String escapedString(final Context context, final int key) {
        return TextUtilsCompat.htmlEncode(context.getString(key));
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }

    @Override
    protected boolean isFileOutOfThisFormat(final File file, final String name, final String ext) {
        return name.equals("todo.txt") ||
                (TODOTXT_FILE_PATTERN.matcher(name).matches() && (name.endsWith(".txt") || name.endsWith(".text")));
    }
}
