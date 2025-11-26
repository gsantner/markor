package net.gsantner.markor.widget;

import android.os.Build;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;


import java.util.Collections;

/**
 * Small Markdown -> Spannable converter for widget usage.
 * Supports:
 * - headings: '# ' (H1) and '## ' (H2)
 * - inline bold: **bold** or __bold__
 * - simple lists: lines starting with "- ", "* " or "1. "
 * <p>
 * Simple, fast, few allocations. Not a full Markdown implementation.
 */
public final class MarkdownWidgetUtils {

    /**
     * Converts markdown → HTML using flexmark → Spanned → SpannableStringBuilder.
     * Safe for RemoteViews (bold, italic, lists, headings, etc.).
     */
    public static SpannableStringBuilder fromFlexmark(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return new SpannableStringBuilder("");
        }

        Parser parser = Parser.builder()
                .extensions(Collections.singletonList(TaskListExtension.create()))
                .build();
        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(Collections.singletonList(TaskListExtension.create()))
                .build();

        String html = renderer.render(parser.parse(markdown));

        Spanned spanned;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            spanned = Html.fromHtml(html);
        }

        // Flexmark produces <p>…</p> blocks, so Html.fromHtml adds newlines.
        // Trim trailing newlines to match old behavior.
        SpannableStringBuilder out = new SpannableStringBuilder(spanned);
        trimTrailingNewlines(out);

        return out;
    }


    /**
     * Helper to remove excessive newlines from Html.fromHtml output
     */
    private static void trimTrailingNewlines(SpannableStringBuilder sb) {
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.delete(sb.length() - 1, sb.length());
        }
    }

}
