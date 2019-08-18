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
import android.text.Editable;
import android.text.InputFilter;
import android.text.ParcelableSpan;
import android.text.Spannable;
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

    protected abstract Editable run(final Editable editable);

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

    public void generalHighlightRun(final Editable editable) {
        final String text = editable.toString();
        _highlightingFactorBasedOnFilesize = Math.max(1, Math.min(Math.max(text.length() - 9000, 10000) / 10000, 4));
        _profiler.restart("General Highlighter");
        if (_preCalcTabWidth > 0) {
            _profiler.restart("Tabulator width");
            createReplacementSpanForMatches(editable, Pattern.compile("\t"), _preCalcTabWidth);
        }
        if (_highlightLinks && (text.contains("http://") || text.contains("https://"))) {
            _profiler.restart("Link Color");
            createColorSpanForMatches(editable, Patterns.WEB_URL, 0xff1ea3fd);
            _profiler.restart("Link Size");
            createRelativeSizeSpanForMatches(editable, Patterns.WEB_URL, 0.7f);
            _profiler.restart("Link Italic");
            createStyleSpanForMatches(editable, Patterns.WEB_URL, Typeface.ITALIC);
        }
        if (_highlightHexcolor) {
            _profiler.restart("RGB Color underline");
            createColoredUnderlineSpanForMatches(editable, HexColorCodeUnderlineSpan.PATTERN, new HexColorCodeUnderlineSpan(), 1);
        }
    }

    protected String getFilepath() {
        if (_document != null && _document.getFile() != null) {
            return _document.getFile().getAbsolutePath();
        }
        return "";
    }


    //
    // Clear spans
    //

    protected void clearSpans(Editable editable) {
        clearCharacterSpanType(editable, TextAppearanceSpan.class);
        clearCharacterSpanType(editable, ForegroundColorSpan.class);
        clearCharacterSpanType(editable, BackgroundColorSpan.class);
        clearCharacterSpanType(editable, StrikethroughSpan.class);
        clearCharacterSpanType(editable, RelativeSizeSpan.class);
        clearCharacterSpanType(editable, StyleSpan.class);
        clearCharacterSpanType(editable, ColorUnderlineSpan.class);
        clearParagraphSpanType(editable, LineBackgroundSpan.class);
        clearParagraphSpanType(editable, LineHeightSpan.class);
    }

    private <T extends CharacterStyle> void clearCharacterSpanType(Editable editable, Class<T> spanType) {
        CharacterStyle[] spans = editable.getSpans(0, editable.length(), spanType);

        for (int n = spans.length; n-- > 0; ) {
            editable.removeSpan(spans[n]);
        }
    }

    private <T extends ParagraphStyle> void clearParagraphSpanType(Editable editable, Class<T> spanType) {
        ParagraphStyle[] spans = editable.getSpans(0, editable.length(), spanType);

        for (int n = spans.length; n-- > 0; ) {
            editable.removeSpan(spans[n]);
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
     * @param editable      Text editable
     * @param pattern       The pattern to match
     * @param creator       A ParcelableSpanCreator for ParcelableSpan
     * @param groupsToMatch (optional) groups to be matched, indexes start at 1.
     */
    protected void createSpanForMatches(final Editable editable, final Pattern pattern, final SpanCreator.ParcelableSpanCreator creator, int... groupsToMatch) {
        if (groupsToMatch == null || groupsToMatch.length < 1) {
            groupsToMatch = new int[]{0};
        }
        int i = 0;
        for (Matcher m = pattern.matcher(editable); m.find(); i++) {
            ParcelableSpan span = creator.create(m, i);
            if (span != null) {
                for (int g : groupsToMatch) {
                    if (g == 0 || g <= m.groupCount()) {
                        editable.setSpan(span, m.start(g), m.end(g), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }


    /**
     * Create Span for isMatching in paragraph's. Note that this will highlight the full matched pattern
     * (including optionals) if no group parameters are given.
     *
     * @param editable      Text editable
     * @param pattern       The pattern to match
     * @param creator       A ParcelableSpanCreator for ParcelableSpan
     * @param groupsToMatch (optional) groups to be matched, indexes start at 1.
     */
    protected void createSpanForMatchesP(final Editable editable, final Pattern pattern, final SpanCreator.ParagraphStyleCreator creator, int... groupsToMatch) {
        if (groupsToMatch == null || groupsToMatch.length < 1) {
            groupsToMatch = new int[]{0};
        }
        int i = 0;
        for (Matcher m = pattern.matcher(editable); m.find(); i++) {
            ParagraphStyle span = creator.create(m, i);
            if (span != null) {
                for (int g : groupsToMatch) {
                    if (g == 0 || g <= m.groupCount()) {
                        editable.setSpan(span, m.start(g), m.end(g), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }

    /**
     * Create Span for isMatching in paragraph's. Note that this will highlight the full matched pattern
     * (including optionals) if no group parameters are given.
     *
     * @param editable      Text editable
     * @param pattern       The pattern to match
     * @param creator       A ParcelableSpanCreator for ParcelableSpan
     * @param groupsToMatch (optional) groups to be matched, indexes start at 1.
     */
    protected void createSpanForMatchesM(final Editable editable, final Pattern pattern, final SpanCreator creator, int... groupsToMatch) {
        if (groupsToMatch == null || groupsToMatch.length < 1) {
            groupsToMatch = new int[]{0};
        }
        int i = 0;
        for (Matcher m = pattern.matcher(editable); m.find(); i++) {
            Object span = creator.create(m, i);
            if (span != null) {
                for (int g : groupsToMatch) {
                    if (g == 0 || g <= m.groupCount()) {
                        editable.setSpan(span, m.start(g), m.end(g), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
    }

    protected void createStyleSpanForMatches(final Editable editable, final Pattern pattern, final int style, int... groupsToMatch) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new StyleSpan(style));
    }

    protected void createColorSpanForMatches(final Editable editable, final Pattern pattern, final int color, int... groupsToMatch) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new ForegroundColorSpan(color), groupsToMatch);
    }

    protected void createColorBackgroundSpan(Editable editable, final Pattern pattern, final int color, int... groupsToMatch) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new BackgroundColorSpan(color), groupsToMatch);
    }

    protected void createSpanWithStrikeThroughForMatches(Editable editable, final Pattern pattern, int... groupsToMatch) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new StrikethroughSpan(), groupsToMatch);
    }

    protected void createTypefaceSpanForMatches(Editable editable, Pattern pattern, final String typeface, int... groupsToMatch) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new TypefaceSpan(typeface), groupsToMatch);
    }

    protected void createRelativeSizeSpanForMatches(Editable editable, final Pattern pattern, float relativeSize, int... groupsToMatch) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new RelativeSizeSpan(relativeSize), groupsToMatch);
    }

    protected void createReplacementSpanForMatches(final Editable editable, final Pattern pattern, final int charWidth, int... groupsToMatch) {
        createSpanForMatchesM(editable, pattern, (matcher, iM) -> new ReplacementSpan() {
            @Override
            public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
                return charWidth;
            }

            @Override
            public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
            }
        }, groupsToMatch);
    }

    protected void createMonospaceSpanForMatches(Editable editable, final Pattern pattern, int... groupsToMatch) {
        createTypefaceSpanForMatches(editable, pattern, "monospace", groupsToMatch);
    }

    protected void createColoredUnderlineSpanForMatches(Editable editable, final Pattern pattern, @ColorInt int color, int... groupsToMatch) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new ColorUnderlineSpan(color, null), groupsToMatch);
    }

    protected void createColoredUnderlineSpanForMatches(Editable editable, final Pattern pattern, final SpanCreator.ParcelableSpanCreator creator, int... groupsToMatch) {
        createSpanForMatches(editable, pattern, creator, groupsToMatch);
    }

    protected void createParagraphStyleSpanForMatches(Editable editable, final Pattern pattern, final SpanCreator.ParagraphStyleCreator creator, int... groupsToMatch) {
        createSpanForMatchesP(editable, pattern, creator, groupsToMatch);
    }
}
