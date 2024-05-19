/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gsantner.markor.format.general.ColorUnderlineSpan;
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SyntaxHighlighterBase {

    protected final static int LONG_HIGHLIGHTING_DELAY = 2400;

    private static final Pattern PATTERN_TAB = Pattern.compile("\t");

    /**
     * Url pattern with required http/https protocol. Case-sensitive.
     */
    public static final Pattern URL = Pattern.compile("\\bhttps?://(?:(?:[-;:&=+$,\\w]+@)?[A-Za-z0-9.-]+|(?:www\\.|[-;:&=+$,\\w]+@)[A-Za-z0-9.-]+)(?:/[+~%/.\\w_-]*\\??[-+=&;%@.\\w_]*#?[.!/\\\\\\w]*)?");

    protected static SyntaxHighlighterBase getDefaultHighlighter(final AppSettings as) {
        return new PlaintextSyntaxHighlighter(as);
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
    protected int _tabSize = 1;
    protected boolean _isDarkMode = false;
    protected int _textColor = Color.BLACK;
    protected String _fontFamily = "";

    public SyntaxHighlighterBase configure() {
        return configure(null);
    }

    /**
     * Configure this highlighter. Call before doing any highlighting
     * This is separate from the constructor as we may want to reconfigure after font size etc change
     *
     * @param paint Optional paint to pass in - used for text parameters
     * @return Highlighter
     */
    public SyntaxHighlighterBase configure(@Nullable final Paint paint) {
        _isDarkMode = GsContextUtils.instance.isDarkModeEnabled(_appSettings.getContext());
        _fontFamily = _appSettings.getFontFamily();
        _textColor = _appSettings.getEditorForegroundColor();
        if (paint != null) {
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
        final boolean isStatic;

        SpanGroup(Object o, int s, int e) {
            span = o;
            start = s;
            end = e;
            isStatic = o instanceof UpdateLayout;
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
    private final NavigableSet<Integer> _appliedDynamic;
    private boolean _staticApplied = false;

    protected Spannable _spannable;
    protected final AppSettings _appSettings;

    public SyntaxHighlighterBase(final AppSettings as) {
        _appSettings = as;
        _groups = new ArrayList<>();
        _appliedDynamic = new TreeSet<>();

        _layoutUpdater = new ForceUpdateLayout();
    }

    // ---------------------------------------------------------------------------------------------

    public SyntaxHighlighterBase clearAll() {
        return clearDynamic().clearStatic();
    }

    /**
     * Removes all dynamic spans applied by this highlighter to the currently set spannable
     *
     * @return this
     */
    public synchronized SyntaxHighlighterBase clearDynamic() {
        if (_spannable == null) {
            return this;
        }

        final Iterator<Integer> it = _appliedDynamic.descendingIterator();
        while (it.hasNext()) {
            _spannable.removeSpan(_groups.get(it.next()).span);
        }
        _appliedDynamic.clear();

        return this;
    }

    /**
     * Removes all static spans applied by this highlighter to the currently set spannable
     *
     * @return this
     */
    public synchronized SyntaxHighlighterBase clearStatic() {
        if (_spannable == null) {
            return this;
        }

        for (int i = _groups.size() - 1; i >= 0; i--) {
            final SpanGroup group = _groups.get(i);
            if (group.isStatic) {
                _spannable.removeSpan(group.span);
            }
        }

        _staticApplied = false;

        return this;
    }

    /**
     * Change the currently attached spannable.
     * Caller is responsible for clearing spans attached to existing spannable
     *
     * @param spannable Spannable to work on
     * @return this
     */
    public synchronized SyntaxHighlighterBase setSpannable(@Nullable final Spannable spannable) {
        if (spannable != _spannable) {
            _groups.clear();
            _appliedDynamic.clear();
            _spannable = spannable;
        }

        return this;
    }

    // Get currently attached spannable
    public Spannable getSpannable() {
        return _spannable;
    }

    public boolean hasSpans() {
        return _spannable != null && _groups.size() > 0;
    }

    /**
     * Helper to change spans in 'onTextChanged'
     */
    public SyntaxHighlighterBase fixup(final int start, final int before, final int count) {
        return fixup(start + before, count - before);
    }

    // Adjust all spans after a change in the text

    /**
     * Adjust all currently computed spans. Use to adjust spans after text edited.
     *
     * @param after Apply to spans with region starting after 'after'
     * @param delta Apply to
     * @return this
     */
    public synchronized SyntaxHighlighterBase fixup(final int after, final int delta) {
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

    public SyntaxHighlighterBase applyAll() {
        return applyDynamic().applyStatic();
    }

    public SyntaxHighlighterBase applyDynamic() {
        return applyDynamic(new int[]{0, _spannable.length()});
    }

    /**
     * Apply spans which intersect region [start, end)
     *
     * @return this
     */
    public synchronized SyntaxHighlighterBase applyDynamic(final int[] range) {
        if (_spannable == null) {
            return this;
        }

        final int length = _spannable.length();
        if (!TextViewUtils.checkRange(length, range)) {
            return this;
        }

        for (int i = 0; i < _groups.size(); i++) {
            final SpanGroup group = _groups.get(i);

            if (group.isStatic) {
                continue;
            }

            if (group.start >= range[1]) {
                // As we are sorted on start, we can break out after the first group.start > end
                break;
            }

            final boolean valid = group.start >= 0 && group.end > range[0] && group.end <= length;
            if (valid && !_appliedDynamic.contains(i)) {
                _spannable.setSpan(group.span, group.start, group.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                _appliedDynamic.add(i);
            }
        }

        return this;
    }

    public synchronized SyntaxHighlighterBase applyStatic() {
        if (_spannable == null || _staticApplied) {
            return this;
        }

        for (int i = 0; i < _groups.size(); i++) {
            final SpanGroup group = _groups.get(i);
            if (group.isStatic) {
                _spannable.setSpan(group.span, group.start, group.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        _staticApplied = true;

        return this;
    }

    public final SyntaxHighlighterBase reflow() {
        return reflow(new int[]{0, _spannable.length()});
    }

    // Reflow selected region's lines
    public final synchronized SyntaxHighlighterBase reflow(final int[] range) {
        if (TextViewUtils.checkRange(_spannable, range)) {
            _spannable.setSpan(_layoutUpdater, range[0], range[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            _spannable.removeSpan(_layoutUpdater);
        }
        return this;
    }

    /**
     * Recompute all spans. References to existing spans will be lost.
     * Caller is responsible for calling 'clear()' before this, if necessary
     *
     * @return this
     */
    public synchronized final SyntaxHighlighterBase recompute() {
        _groups.clear();
        _appliedDynamic.clear();
        _staticApplied = false;

        if (TextUtils.isEmpty(_spannable)) {
            return this;
        }

        // Highlighting cannot generate exceptions!
        try {
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
        if (end > start && span != null) {
            _groups.add(new SpanGroup(span, start, end));
        }
    }

    protected final void createSpanForMatches(final Pattern pattern, GsCallback.r1<Object, Matcher> creator, int... groupsToMatch) {
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

    protected final void createColoredUnderlineSpanForMatches(final Pattern pattern, final GsCallback.r1<Object, Matcher> creator, int... groupsToMatch) {
        createSpanForMatches(pattern, creator, groupsToMatch);
    }

    protected final void createSuperscriptStyleSpanForMatches(final Pattern pattern, final int... groupsToMatch) {
        createSpanForMatches(pattern, matcher -> new SuperscriptSpan(), groupsToMatch);
    }

    protected final void createSubscriptStyleSpanForMatches(final Pattern pattern, final int... groupsToMatch) {
        createSpanForMatches(pattern, matcher -> new SubscriptSpan(), groupsToMatch);
    }

    protected final void createTabSpans(final int tabWidth) {
        if (tabWidth > 0) {
            createReplacementSpanForMatches(PATTERN_TAB, tabWidth);
        }
    }

    protected final void createSmallBlueLinkSpans() {
        createSpanForMatches(URL, new HighlightSpan().setForeColor(0xff1ea3fd).setItalic(true).setTextScale(0.85f));
    }

    protected final void createUnderlineHexColorsSpans() {
        createColoredUnderlineSpanForMatches(ColorUnderlineSpan.HEX_CODE_UNDERLINE_PATTERN, m -> new ColorUnderlineSpan(m.group(1), 3f), 1);
    }

    // We _do not_ implement UpdateLayout or Parcelable for performance reasons
    public static class HighlightSpan extends CharacterStyle implements UpdateAppearance, GsCallback.r1<Object, Matcher> {

        public Boolean bold = null;
        public Boolean italic = null;
        public Boolean underline = null;
        public Boolean strikethrough = null;
        public Float textScale = null;
        public @ColorInt
        Integer foregroundColor = null;
        public @ColorInt
        Integer backgroundColor = null;

        // Setters. Use null (default) to indicate "don't change this value"
        public HighlightSpan setForeColor(@ColorInt Integer color) {
            foregroundColor = color;
            return this;
        }

        public HighlightSpan setBackColor(@ColorInt Integer color) {
            backgroundColor = color;
            return this;
        }

        public HighlightSpan setTextScale(Float scale) {
            textScale = scale;
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

            if (foregroundColor != null) {
                tp.setColor(foregroundColor);
            }

            if (backgroundColor != null) {
                tp.bgColor = backgroundColor;
            }

            if (textScale != null) {
                tp.setTextSize(tp.getTextSize() * textScale);
            }
        }

        // The class is a callback which returns a clone of itself
        // So we can construct an instance and use it as a span generator
        @Override
        public HighlightSpan callback(Matcher m) {
            return new HighlightSpan()
                    .setForeColor(foregroundColor)
                    .setBackColor(backgroundColor)
                    .setBold(bold)
                    .setItalic(italic)
                    .setUnderline(underline)
                    .setStrike(strikethrough)
                    .setTextScale(textScale);
        }
    }
}