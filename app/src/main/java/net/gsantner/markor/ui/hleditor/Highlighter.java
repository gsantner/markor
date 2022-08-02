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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UpdateAppearance;
import android.text.style.UpdateLayout;
import android.util.Log;
import android.util.Patterns;

import net.gsantner.markor.format.general.ColorUnderlineSpan;
import net.gsantner.markor.format.plaintext.PlaintextHighlighter;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Highlighter {

    protected final static int LONG_HIGHLIGHTING_DELAY = 2400;

    public static final Pattern HEX_CODE_UNDERLINE_PATTERN = Pattern.compile("(?:\\s|[\";,:'*]|^)(#[A-Fa-f0-9]{6,8})+(?:\\s|[\";,:'*]|$)");
    private static final Pattern PATTERN_TAB = Pattern.compile("\t");

    protected static Highlighter getDefaultHighlighter(final AppSettings as) {
        return new PlaintextHighlighter(as);
    }

    // Functions for derived classes to implement
    // ---------------------------------------------------------------------------------------------

    // Derived classes should override this to generate all spans
    // All exceptions will be caught and handled
    protected abstract void generateSpans();

    public int getHighlightingDelay() {
        return _delay;
    }

    // Configuration - to be set on a call to configure
    protected int _delay = LONG_HIGHLIGHTING_DELAY;
    protected float _textSize = 1;
    protected int _tabSize = 1;
    protected boolean _isDarkMode = false;
    protected int _textColor = Color.BLACK;
    protected String  _fontFamily = "";

    public Highlighter configure() {
        return configure(null);
    }

    /**
     * Configure this highlighter. Call before doing any highlighting
     * This is separate from the constructor as we may want to reconfigure after font size etc change
     *
     * @param paint Optional paint to pass in - used for text parameters
     * @return Highlighter
     */
    public Highlighter configure(@Nullable final Paint paint) {
        _isDarkMode = _appSettings.isDarkThemeEnabled();
        _fontFamily = _appSettings.getFontFamily();
        _textColor = _appSettings.getEditorForegroundColor();
        if (paint != null) {
            _textSize = paint.getTextSize();
            _tabSize = (int) (_appSettings.getTabWidth() * paint.measureText(" "));
        }
        return this;
    }

    // Instance
    // ---------------------------------------------------------------------------------------------

    /**
     * A class representing any span
     */
    public static class SpanGroup implements Comparable<SpanGroup> {
        int start, end;
        final Object span;

        SpanGroup(Object o, int s, int e) {
            span = o;
            start = s;
            end = e;
        }

        @Override
        public int compareTo(final SpanGroup o) {
            return start - o.start;
        }
    }

    private static class ForceUpdateLayout implements UpdateLayout {
        // Empty class - just implements UpdateLayout
    }

    private final ForceUpdateLayout _layoutUpdater;

    private final List<SpanGroup> _groups;
    private final List<Integer> _applied;

    protected Spannable _spannable;
    protected final AppSettings _appSettings;

    public Highlighter(final AppSettings as) {
        _appSettings = as;
        _groups = new ArrayList<>();
        _applied = new ArrayList<>();

        _layoutUpdater = new ForceUpdateLayout();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Removes all spans applied by this highlighter to the currently set spannable
     * @return this
     */
    public synchronized Highlighter clear() {
        if (_spannable != null) {
            for (int i = _applied.size() - 1; i >= 0; i--) {
                // Reverse order to align with TextView internals
                _spannable.removeSpan(_groups.get(_applied.get(i)).span);
            }
            _applied.clear();
        }
        return this;
    }

    /**
     * Change the currently attached spannable.
     * Caller is responsible for clearing spans attached to existing spannable
     * @param spannable Spannable to work on
     * @return this
     */
    public synchronized Highlighter setSpannable(@Nullable final Spannable spannable) {
        if (spannable != _spannable) {
            _groups.clear();
            _applied.clear();
            _spannable = spannable;
        }

        return this;
    }

    /**
     * Helper to change spans in 'onTextChanged'
     */
    public Highlighter fixup(final int start, final int before, final int count) {
        return fixup(start + before, count - before);
    }

    // Adjust all spans after a change in the text

    /**
     * Adjust all currently computed spans. Use to adjust spans after text edited.
     * @param after Apply to spans with region starting after 'after'
     * @param delta Apply to
     * @return this
     */
    public synchronized Highlighter fixup(final int after, final int delta) {
        for (int i = _groups.size() - 1; i >= 0; i--) {
            final SpanGroup group = _groups.get(i);
            // Very simple fixup. If the group is entirely after 'after', adjust it's region
            if (group.start <= after) {
                // We iterate backwards. As groups are sorted, if start is before after, can break out
                break;
            } else {
                group.start += delta;
                group.end += delta;
            }
        }
        return this;
    }

    // Get currently attached spannable
    public Spannable getSpannable() {
        return _spannable;
    }

    // Region specified as array
    public Highlighter apply(final int[] region) {
        return apply(region[0], region[1]);
    }

    // Apply all spans
    public Highlighter apply() {
        return apply(0, -1);
    }

    public boolean isApplied(final int index) {
        // _applied is an ordered list of int, we can very efficiently search it
        return !_applied.isEmpty()
                && index <= _applied.get(_applied.size() - 1) // In the common case, we will hit this
                && index >= _applied.get(0)
                && Collections.binarySearch(_applied, index) >= 0;
    }

    /**
     * Apply spans which intersect region [start, end)
     * @return this
     */
    public synchronized Highlighter apply(int start, int end) {
        if (_spannable == null) {
            return this;
        }

        final boolean sortRequired = !_applied.isEmpty();
        final int length = _spannable.length();

        start = Math.max(0, start);
        end = Math.min(end < 0 ? length : end, length);

        if (start >= end) {
            return this;
        }

        for (int i = 0; i < _groups.size(); i++) {
            final SpanGroup group = _groups.get(i);

            if (group.start > end) {
                // As we are sorted on start, we can break out after the first group.start > end
                break;
            }

            final boolean intersecting = group.start < end && group.end > start;
            final boolean valid = group.start >= 0 && group.end <= length;
            if (intersecting && valid && !isApplied(i)) {
                _spannable.setSpan(group.span, group.start, group.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                _applied.add(i);
            }

        }

        if (sortRequired) {
            // Sort the list of applied spans if required
            Collections.sort(_applied);
        }

        return this;
    }

    public final Highlighter reflow(final int[] region) {
        if (region != null && region.length >= 2) {
            reflow(region[0], region[1]);
        }
        return this;
    }

    // Update whole layout
    public final Highlighter reflow() {
        return reflow(0, -1);
    }

    // Reflow highlighted region (to prevent tearing etc)
    public synchronized final Highlighter reflow(int start, int end) {
        if (_spannable != null) {
            final int length = _spannable.length();
            start = Math.max(0, start);
            end = Math.min(end < 0 ? length : end, length);
            _spannable.setSpan(_layoutUpdater, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return this;
    }

    /**
     * Recompute all spans. References to existing spans will be lost.
     * Caller is responsible for calling 'clear()' before this, if necessary
     * @return this
     */
    public synchronized final Highlighter recompute() {
        _groups.clear();
        _applied.clear();

        if (TextUtils.isEmpty(_spannable)) {
            return this;
        }

        // Highlighting cannot generate exceptions!
        try
        {
            generateSpans();
            Collections.sort(_groups); // Dramatically improves performance
        } catch (Exception ex) {
            Log.w(getClass().getName(), ex);
        } catch (Error er) {
            Log.w(getClass().getName(), er);
        }

        return this;
    }

    //
    // Helpers for creating spans
    //

    protected final void addSpanGroup(final Object span, final int start, final int end) {
        _groups.add(new SpanGroup(span, start, end));
    }

    protected final void createSpanForMatches(final Pattern pattern, Callback.r1<Object, Matcher> creator, int... groupsToMatch) {
        if (groupsToMatch == null || groupsToMatch.length < 1) {
            groupsToMatch = new int[]{0};
        }
        final Matcher m = pattern.matcher(_spannable);

        while (m.find()) {
            final Object span = creator.callback(m);
            if (span != null) {
                for (final int g : groupsToMatch) {
                    final int start = m.start(g);
                    final int end = m.end(g);
                    if ((g == 0 || g <= m.groupCount()) && Math.abs(end - start) > 0) {
                        addSpanGroup(span, start, end);
                    }
                }
            }
        }
    }

    protected final void createStyleSpanForMatches(final Pattern pattern, final int style, int... groupsToMatch) {
        createSpanForMatches(pattern, new HighlightSpan().setTypeface(style), groupsToMatch);
    }

    protected final void createColorSpanForMatches(final Pattern pattern, final int color, int... groupsToMatch) {
         createSpanForMatches(pattern, new HighlightSpan().setForeColor(color), groupsToMatch);
    }

    protected final void createColorBackgroundSpan(final Pattern pattern, final int color, int... groupsToMatch) {
         createSpanForMatches(pattern, new HighlightSpan().setBackColor(color), groupsToMatch);
    }

    protected final void createStrikeThroughSpanForMatches(final Pattern pattern, int... groupsToMatch) {
         createSpanForMatches(pattern, new HighlightSpan().setStrike(true), groupsToMatch);
    }

    protected final void createTypefaceSpanForMatches(Pattern pattern, final String typeface, int... groupsToMatch) {
         createSpanForMatches(pattern, matcher -> new TypefaceSpan(typeface), groupsToMatch);
    }

    protected final void createRelativeSizeSpanForMatches(final Pattern pattern, float relativeSize, int... groupsToMatch) {
         createSpanForMatches(pattern, matcher -> new RelativeSizeSpan(relativeSize), groupsToMatch);
    }

    protected final void createReplacementSpanForMatches(final Pattern pattern, final int charWidth, int... groupsToMatch) {
         createSpanForMatches(pattern, matcher -> new ReplacementSpan() {
            @Override
            public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
                 return charWidth;
            }

            @Override
            public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
            }
        }, groupsToMatch);
    }

    protected final void createMonospaceSpanForMatches(final Pattern pattern, int... groupsToMatch) {
         createTypefaceSpanForMatches(pattern, "monospace", groupsToMatch);
    }

    protected final void createColoredUnderlineSpanForMatches(final Pattern pattern, @ColorInt int color, int... groupsToMatch) {
         createSpanForMatches(pattern, matcher -> new ColorUnderlineSpan(color, null), groupsToMatch);
    }

    protected final void createColoredUnderlineSpanForMatches(final Pattern pattern, final Callback.r1<Object, Matcher> creator, int... groupsToMatch) {
         createSpanForMatches(pattern, creator, groupsToMatch);
    }

    protected final void createSuperscriptStyleSpanForMatches(final Pattern pattern, final int... groupsToMatch) {
         createSpanForMatches(pattern, matcher -> new SuperscriptSpan(), groupsToMatch);
    }

    protected final void createSubscriptStyleSpanForMatches(final Pattern pattern, final int... groupsToMatch) {
         createSpanForMatches(pattern, matcher -> new SubscriptSpan(), groupsToMatch);
    }

    protected final void createTabSpans(final int tabWidth)  {
        if (tabWidth > 0) {
            createReplacementSpanForMatches(PATTERN_TAB, tabWidth);
        }
    }

    protected final void createHighlightLinksSpans() {
        createSpanForMatches(Patterns.WEB_URL, new HighlightSpan().setForeColor(0xff1ea3fd).setItalic(true).setTextSize(_textSize * 0.85f));
    }

    protected final void createUnderlineHexColorsSpans() {
        createColoredUnderlineSpanForMatches(HEX_CODE_UNDERLINE_PATTERN, m -> new ColorUnderlineSpan(Color.parseColor(m.group(1)), 3f), 1);
    }

    // We _do not_ implement UpdateLayout or Parcelable for performance reasons
    public static class HighlightSpan extends CharacterStyle implements UpdateAppearance, Callback.r1<Object, Matcher> {

        public Boolean bold = null;
        public Boolean italic = null;
        public Boolean underline = null;
        public Boolean strikethrough = null;
        public Float textSize = null;
        public @ColorInt Integer foregroundColor = null;
        public @ColorInt Integer backgroundColor = null;

        // Setters. Use null (default) to indicate "don't change this value"
        public HighlightSpan setForeColor(@ColorInt Integer color) {
            foregroundColor = color;
            return this;
        }

        public HighlightSpan setBackColor(@ColorInt Integer color) {
            backgroundColor = color;
            return this;
        }

        public HighlightSpan setTextSize(Float size) {
            textSize = size;
            return this;
        }

        public HighlightSpan setStrike(Boolean val) {
            strikethrough = val;
            return this;
        }

        public HighlightSpan setUnderline(Boolean val) {
            underline = val;
            return this;
        }

        public HighlightSpan setBold(Boolean val) {
            bold = val;
            return this;
        }

        public HighlightSpan setItalic(Boolean val) {
            italic = val;
            return this;
        }

        public HighlightSpan setTypeface(final int tf) {
            return setBold((tf & Typeface.BOLD) != 0).setItalic((tf & Typeface.ITALIC) != 0);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            if (bold != null) {
                tp.setFakeBoldText(bold);
            }

            if (strikethrough != null) {
                tp.setStrikeThruText(strikethrough);
            }

            if (underline != null) {
                tp.setUnderlineText(underline);
            }

            if (italic != null && italic) {
                tp.setTextSkewX(-0.25f); // This is what android uses
            }

            if (foregroundColor != null)  {
                tp.setColor(foregroundColor);
            }

            if (backgroundColor != null) {
                tp.bgColor = backgroundColor;
            }

            if (textSize != null) {
                tp.setTextSize(textSize);
            }
        }


        @Override
        public HighlightSpan callback(Matcher m) {
            // Return a copy
            return new HighlightSpan()
                    .setForeColor(foregroundColor)
                    .setBackColor(backgroundColor)
                    .setBold(bold)
                    .setItalic(italic)
                    .setUnderline(underline)
                    .setStrike(strikethrough)
                    .setTextSize(textSize);
        }
    }
}