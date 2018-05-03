/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

/*
 * Define element in Preferences-XML:
    <net.gsantner.opoc.preference.FontPreferenceCompat
        android:icon="@drawable/ic_title_black_24dp"
        android:defaultValue="@string/default_font_family"
        android:key="@string/pref_key__font_family"
        android:title="@string/pref_title__font_choice" />
 */
package net.gsantner.opoc.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.preference.ListPreference;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;

/**
 * A {@link ListPreference} that displays a list of fonts to select from
 * This list contains fonts that are bundled with android
 * <p>
 * Apply to TextView:
 * setTypeface(Typeface.create(settings.getFontFamilyAsString(), Typeface.NORMAL));
 */
@SuppressWarnings({"unused", "SpellCheckingInspection", "WeakerAccess"})
public class FontPreferenceCompat extends ListPreference {
    private String _defaultValue;
    private String[] _fontNames = {
            "Roboto Regular", "Roboto Light", "Roboto Bold", "Roboto Medium",
            "Monospace", "Noto Serif", "Cutive Mono", "Roboto Condensed", "Roboto Thin",
            "Roboto Black", "Coming Soon", "Carrois Gothic", "Dancing Script"
    };
    private String[] _fontValues = {
            "sans-serif-regular", "sans-serif-light", "sans-serif-bold", "sans-serif-medium",
            "monospace", "serif", "serif-monospace", "sans-serif-condensed", "sans-serif-thin",
            "sans-serif-black", "casual", "sans-serif-smallcaps", "cursive"
    };


    public FontPreferenceCompat(Context context) {
        super(context);
        loadFonts(context, null);
    }

    public FontPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadFonts(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FontPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        loadFonts(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FontPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        loadFonts(context, attrs);
    }

    private void loadFonts(Context context) {
        loadFonts(context, null);
    }

    private void loadFonts(Context context, @Nullable AttributeSet attrs) {
        _defaultValue = _fontValues[0];
        if (attrs != null) {
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                String attrName = attrs.getAttributeName(i);
                String attrValue = attrs.getAttributeValue(i);
                if (attrName.equalsIgnoreCase("defaultValue")) {
                    if (attrValue.startsWith("@")) {
                        int resId = Integer.valueOf(attrValue.substring(1));
                        attrValue = getContext().getString(resId);
                    }
                    _defaultValue = attrValue;
                    break;
                }
            }
        }

        Spannable[] fontText = new Spannable[_fontNames.length];
        for (int i = 0; i < _fontNames.length; i++) {
            fontText[i] = new SpannableString(_fontNames[i] + "\n" + _fontValues[i]);
            fontText[i].setSpan(new TypefaceSpan(_fontValues[i]), 0, _fontNames[i].length(), 0);
            fontText[i].setSpan(new RelativeSizeSpan(0.7f), _fontNames[i].length() + 1, fontText[i].length(), 0);

        }
        setDefaultValue(_defaultValue);
        setEntries(fontText);
        setEntryValues(_fontValues);
    }

    @Override
    public CharSequence getSummary() {
        String prefix = TextUtils.isEmpty(super.getSummary())
                ? "" : super.getSummary() + "\n\n";
        String fontText = TextUtils.isEmpty(getValue()) ? _defaultValue : getValue();
        for (int i = 0; i < _fontValues.length; i++) {
            if (_fontValues[i].equals(fontText)) {
                fontText = _fontNames[i] + " (" + fontText + ")";
                break;
            }
        }
        fontText = fontText.replace("★", "");
        return prefix + fontText;
    }

    public String[] getFontNames() {
        return _fontNames;
    }

    public void setFontNames(String[] fontNames) {
        _fontNames = fontNames;
    }

    public String[] getFontValues() {
        return _fontValues;
    }

    public void setFontValues(String[] fontValues) {
        _fontValues = fontValues;
    }
}
