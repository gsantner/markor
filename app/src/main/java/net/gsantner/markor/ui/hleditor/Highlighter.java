/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui.hleditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.LineHeightSpan;
import android.text.style.ParagraphStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.util.Patterns;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.format.general.ColorUnderlineSpan;
import net.gsantner.markor.format.general.HexColorCodeUnderlineSpan;
import net.gsantner.markor.format.plaintext.PlaintextHighlighter;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.NanoProfiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "FieldCanBeLocal", "unused"})
public abstract class Highlighter {
    protected final static int LONG_HIGHLIGHTING_DELAY = 2400;
    protected final static InputFilter AUTOFORMATTER_NONE = (charSequence, i, i1, spanned, i2, i3) -> null;
    protected float _highlightingFactorBasedOnFilesize = 1f;

    protected final NanoProfiler _profiler = new NanoProfiler().setEnabled(BuildConfig.IS_TEST_BUILD || MainActivity.IS_DEBUG_ENABLED);

    protected abstract Spannable run(final Spannable spannable);

    public abstract InputFilter getAutoFormatter();

    protected static Highlighter getDefaultHighlighter(HighlightingEditor hlEditor, Document document) {
        return new PlaintextHighlighter(hlEditor, document);
    }

    public abstract int getHighlightingDelay(Context context);

    //
    // Instance
    //
    protected final HighlightingEditor _hlEditor;
    protected final AppSettings _appSettings;

    protected final int _preCalcTabWidth;
    protected boolean _highlightLinks = true;
    protected final boolean _highlightHexcolor;
    protected final Document _document;
    private TextWatcher _modifier = null;

    public Highlighter(HighlightingEditor editor, Document document) {
        _hlEditor = editor;
        _appSettings = new AppSettings(_hlEditor.getContext().getApplicationContext());

        _preCalcTabWidth = (int) (_appSettings.getTabWidth() <= 1 ? -1 : editor.getPaint().measureText(" ") * _appSettings.getTabWidth());
        _highlightHexcolor = _appSettings.isHighlightingHexColorEnabled();
        _document = document;
    }

    public float getHighlightingFactorBasedOnFilesize() {
        return _highlightingFactorBasedOnFilesize;
    }

    public void generalHighlightRun(final Spannable spannable) {
        final String text = spannable.toString();
        _highlightingFactorBasedOnFilesize = Math.max(1, Math.min(Math.max(text.length() - 9000, 10000) / 10000, 4));
        _profiler.restart("General Highlighter");
        if (_preCalcTabWidth > 0) {
            _profiler.restart("Tabulator width");
            createReplacementSpanForMatches(spannable, Pattern.compile("\t"), _preCalcTabWidth);
        }
        if (_highlightLinks && (text.contains("http://") || text.contains("https://"))) {
            _profiler.restart("Link Color");
            createColorSpanForMatches(spannable, Patterns.WEB_URL, 0xff1ea3fd);
            _profiler.restart("Link Size");
            createRelativeSizeSpanForMatches(spannable, Patterns.WEB_URL, 0.85f);
            _profiler.restart("Link Italic");
            createStyleSpanForMatches(spannable, Patterns.WEB_URL, Typeface.ITALIC);
        }
        if (_highlightHexcolor) {
            _profiler.restart("RGB Color underline");
            createColoredUnderlineSpanForMatches(spannable, HexColorCodeUnderlineSpan.PATTERN, new HexColorCodeUnderlineSpan(), 1);
        }
    }

    public AppSettings getAppSettings() {
        return _appSettings;
    }

    public TextWatcher getTextModifier() {
        return _modifier;
    }

    protected void setTextModifier(TextWatcher modifier) {
        _modifier = modifier;
    }

    //
    // Clear spans
    //

    public static void clearSpans(Spannable spannable) {
        clearCharacterSpanType(spannable, TextAppearanceSpan.class);
        clearCharacterSpanType(spannable, ForegroundColorSpan.class);
        clearCharacterSpanType(spannable, BackgroundColorSpan.class);
        clearCharacterSpanType(spannable, StrikethroughSpan.class);
        clearCharacterSpanType(spannable, RelativeSizeSpan.class);
        clearCharacterSpanType(spannable, StyleSpan.class);
        clearCharacterSpanType(spannable, ColorUnderlineSpan.class);
        clearParagraphSpanType(spannable, LineBackgroundSpan.class);
        clearParagraphSpanType(spannable, LineHeightSpan.class);
    }

    private static <T extends CharacterStyle> void clearCharacterSpanType(Spannable spannable, Class<T> spanType) {
        CharacterStyle[] spans = spannable.getSpans(0, spannable.length(), spanType);

        for (int n = spans.length; n-- > 0; ) {
            spannable.removeSpan(spans[n]);
        }
    }

    private static <T extends ParagraphStyle> void clearParagraphSpanType(Spannable spannable, Class<T> spanType) {
        ParagraphStyle[] spans = spannable.getSpans(0, spannable.length(), spanType);

        for (int n = spans.length; n-- > 0; ) {
            spannable.removeSpan(spans[n]);
        }
    }

    private boolean _isFirstHighlighting = false;

    protected boolean isFirstHighlighting() {
        boolean f = _isFirstHighlighting;
        _isFirstHighlighting = false;
        return f;
    }

    //
    // Create spans
    //

    /**
     * Create Span for isMatching in ParcelableSpan's. Note that this will highlight the full matched pattern
     * (including optionals) if no group parameters are given.
     *
     * @param spannable     Text spannable
     * @param pattern       The pattern to match
     * @param creator       A ParcelableSpanCreator for ParcelableSpan
     * @param groupsToMatch (optional) groups to be matched, indexes start at 1.
     */
    protected static void createSpanForMatches(final Spannable spannable, final Pattern pattern, final SpanCreator.ParcelableSpanCreator creator, int... groupsToMatch) {
        if (groupsToMatch == null || groupsToMatch.length < 1) {
            groupsToMatch = new int[]{0};
        }
        int i = 0;
        for (Matcher m = pattern.matcher(spannable); m.find(); i++) {
            ParcelableSpan span = creator.create(m, i);
            if (span != null) {
                for (int g : groupsToMatch) {
                    if (g == 0 || g <= m.groupCount()) {
                        spannable.setSpan(span, m.start(g), m.end(g), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }


    /**
     * Create Span for isMatching in paragraph's. Note that this will highlight the full matched pattern
     * (including optionals) if no group parameters are given.
     *
     * @param spannable     Text spannable
     * @param pattern       The pattern to match
     * @param creator       A ParcelableSpanCreator for ParcelableSpan
     * @param groupsToMatch (optional) groups to be matched, indexes start at 1.
     */
    protected static void createSpanForMatchesP(final Spannable spannable, final Pattern pattern, final SpanCreator.ParagraphStyleCreator creator, int... groupsToMatch) {
        if (groupsToMatch == null || groupsToMatch.length < 1) {
            groupsToMatch = new int[]{0};
        }
        int i = 0;
        for (Matcher m = pattern.matcher(spannable); m.find(); i++) {
            ParagraphStyle span = creator.create(m, i);
            if (span != null) {
                for (int g : groupsToMatch) {
                    if (g == 0 || g <= m.groupCount()) {
                        spannable.setSpan(span, m.start(g), m.end(g), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }

    /**
     * Create Span for isMatching in paragraph's. Note that this will highlight the full matched pattern
     * (including optionals) if no group parameters are given.
     *
     * @param spannable     Text spannable
     * @param pattern       The pattern to match
     * @param creator       A ParcelableSpanCreator for ParcelableSpan
     * @param groupsToMatch (optional) groups to be matched, indexes start at 1.
     */
    protected static void createSpanForMatchesM(final Spannable spannable, final Pattern pattern, final SpanCreator creator, int... groupsToMatch) {
        if (groupsToMatch == null || groupsToMatch.length < 1) {
            groupsToMatch = new int[]{0};
        }
        int i = 0;
        for (Matcher m = pattern.matcher(spannable); m.find(); i++) {
            Object span = creator.create(m, i);
            if (span != null) {
                for (int g : groupsToMatch) {
                    if (g == 0 || g <= m.groupCount()) {
                        spannable.setSpan(span, m.start(g), m.end(g), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }

    protected static void createStyleSpanForMatches(final Spannable spannable, final Pattern pattern, final int style, int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new StyleSpan(style));
    }

    protected static void createColorSpanForMatches(final Spannable spannable, final Pattern pattern, final int color, int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new ForegroundColorSpan(color), groupsToMatch);
    }

    protected static void createColorBackgroundSpan(Spannable spannable, final Pattern pattern, final int color, int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new BackgroundColorSpan(color), groupsToMatch);
    }

    protected static void createSpanWithStrikeThroughForMatches(Spannable spannable, final Pattern pattern, int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new StrikethroughSpan(), groupsToMatch);
    }

    protected static void createTypefaceSpanForMatches(Spannable spannable, Pattern pattern, final String typeface, int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new TypefaceSpan(typeface), groupsToMatch);
    }

    protected static void createRelativeSizeSpanForMatches(Spannable spannable, final Pattern pattern, float relativeSize, int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new RelativeSizeSpan(relativeSize), groupsToMatch);
    }

    protected static void createReplacementSpanForMatches(final Spannable spannable, final Pattern pattern, final int charWidth, int... groupsToMatch) {
        createSpanForMatchesM(spannable, pattern, (matcher, iM) -> new ReplacementSpan() {
            @Override
            public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
                return charWidth;
            }

            @Override
            public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
            }
        }, groupsToMatch);
    }

    protected static void createMonospaceSpanForMatches(Spannable spannable, final Pattern pattern, int... groupsToMatch) {
        createTypefaceSpanForMatches(spannable, pattern, "monospace", groupsToMatch);
    }

    protected static void createColoredUnderlineSpanForMatches(Spannable spannable, final Pattern pattern, @ColorInt int color, int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new ColorUnderlineSpan(color, null), groupsToMatch);
    }

    protected static void createColoredUnderlineSpanForMatches(Spannable spannable, final Pattern pattern, final SpanCreator.ParcelableSpanCreator creator, int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, creator, groupsToMatch);
    }

    protected static void createParagraphStyleSpanForMatches(Spannable spannable, final Pattern pattern, final SpanCreator.ParagraphStyleCreator creator, int... groupsToMatch) {
        createSpanForMatchesP(spannable, pattern, creator, groupsToMatch);
    }

    protected static void createSuperscriptStyleSpanForMatches(Spannable spannable, final Pattern pattern, final int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new SuperscriptSpan(), groupsToMatch);
    }

    protected static void createSubscriptStyleSpanForMatches(Spannable spannable, final Pattern pattern, final int... groupsToMatch) {
        createSpanForMatches(spannable, pattern, (matcher, iM) -> new SubscriptSpan(), groupsToMatch);
    }

}
