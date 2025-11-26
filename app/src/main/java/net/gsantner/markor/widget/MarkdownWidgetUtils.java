package net.gsantner.markor.widget;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small Markdown -> Spannable converter for widget usage.
 * Supports:
 *  - headings: '# ' (H1) and '## ' (H2)
 *  - inline bold: **bold** or __bold__
 *  - simple lists: lines starting with "- ", "* " or "1. "
 *
 * Simple, fast, few allocations. Not a full Markdown implementation.
 */
public final class MarkdownWidgetUtils {

    private static final Pattern INLINE_BOLD = Pattern.compile("(\\*\\*|__)(.+?)\\1");

    private MarkdownWidgetUtils() {
    }

    public static CharSequence toSpannable(final String text) {
        if (text == null) {
            return "";
        }

        // Split into lines and handle line-level transformations (headings / lists).
        String[] lines = text.split("\\r?\\n", -1);
        SpannableStringBuilder sb = new SpannableStringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            int start = sb.length();

            // Heading detection (#, ##)
            if (line.startsWith("# ")) {
                String content = line.substring(2).trim();
                sb.append(content);
                int end = sb.length();
                // Bold + larger size
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(1.35f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.startsWith("## ")) {
                String content = line.substring(3).trim();
                sb.append(content);
                int end = sb.length();
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(1.15f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.matches("^\\s*([-\\*]\\s+|\\d+\\.\\s+).*")) {
                // list item: remove marker, prepend bullet char
                String content = line.replaceFirst("^\\s*([-\\*]\\s+|\\d+\\.\\s+)", "").trim();
                sb.append("• ").append(content);
                // optionally apply small relative size or bolding to marker; we keep it simple
            } else {
                sb.append(line);
            }

            // Add newline except after last line (preserve original structure)
            if (i < lines.length - 1) {
                sb.append('\n');
            }
        }

        // Now handle inline bold (**) across the whole builder; because we change content while replacing,
        // loop until no matches to avoid index issues.
        boolean found;
        do {
            found = false;
            Matcher m = INLINE_BOLD.matcher(sb);
            if (m.find()) {
                found = true;
                int matchStart = m.start();
                int matchEnd = m.end();
                String marker = m.group(1);
                String inner = m.group(2);

                // Replace whole matched range with just the inner text, then apply StyleSpan to the correct range.
                sb.replace(matchStart, matchEnd, inner);
                int innerStart = matchStart;
                int innerEnd = innerStart + inner.length();
                sb.setSpan(new StyleSpan(Typeface.BOLD), innerStart, innerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } while (found);

        return sb;
    }
}
