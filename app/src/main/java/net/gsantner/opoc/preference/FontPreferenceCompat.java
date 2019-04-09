/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
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
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.ListPreference;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.MetricAffectingSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link ListPreference} that displays a list of fonts to select from
 * This list contains fonts that are bundled with android
 * <p>
 * Apply to TextView:
 * setTypeface(Typeface.create(settings.getFontFamilyAsString(), Typeface.NORMAL));
 */
@SuppressWarnings({"unused", "SpellCheckingInspection", "WeakerAccess"})
public class FontPreferenceCompat extends ListPreference {
    public static File additionalyCheckedFolder = null;
    public static final FilenameFilter FONT_FILENAME_FILTER = (file, s) -> s.toLowerCase().endsWith(".ttf") || s.toLowerCase().endsWith(".otf");
    private final static String ANDROID_ASSET_DIR = "/android_asset/";
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

        for (File file : getAdditionalFonts()) {
            _fontNames = appendToArray(_fontNames, file.getName().replace(".ttf", "").replace(".TTF", ""));
            _fontValues = appendToArray(_fontValues, file.getAbsolutePath());
        }

        Spannable[] fontText = new Spannable[_fontNames.length];
        for (int i = 0; i < _fontNames.length; i++) {
            fontText[i] = new SpannableString(_fontNames[i] + "\n" + _fontValues[i]);
            fontText[i].setSpan(new TypefaceObjectSpan(typeface(getContext(), _fontValues[i], null)), 0, _fontNames[i].length(), 0);
            fontText[i].setSpan(new RelativeSizeSpan(0.7f), _fontNames[i].length() + 1, fontText[i].length(), 0);

        }
        setDefaultValue(_defaultValue);
        setEntries(fontText);
        setEntryValues(_fontValues);
    }

    public static Typeface typeface(Context context, String familyOrFilepath, Integer typefaceStyle) {
        if (typefaceStyle == null) {
            typefaceStyle = Typeface.NORMAL;
        }
        if (!familyOrFilepath.startsWith("/")) {
            return Typeface.create(familyOrFilepath, typefaceStyle);
        } else {
            try {
                if (familyOrFilepath.startsWith(ANDROID_ASSET_DIR)) {
                    return Typeface.createFromAsset(context.getAssets(), familyOrFilepath.substring(ANDROID_ASSET_DIR.length()));

                } else {
                    return Typeface.createFromFile(familyOrFilepath);
                }
            } catch (RuntimeException exception) {
                return typeface(context, "sans-serif-regular", typefaceStyle);
            }
        }
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


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public List<File> getAdditionalFonts() {
        final ArrayList<File> additionalFonts = new ArrayList<>();

        // Bundled fonts
        try {
            //noinspection ConstantConditions
            for (String filename : getContext().getAssets().list("fonts")) {
                additionalFonts.add(new File(ANDROID_ASSET_DIR + "fonts", filename));
            }
        } catch (Exception ignored) {
        }

        // Directories that are additionally checked out for fonts
        final List<File> checkedDirs = new ArrayList<>(Arrays.asList(
                new File(getContext().getFilesDir(), ".app/fonts"),
                new File(getContext().getFilesDir(), ".app/Fonts"),
                additionalyCheckedFolder,
                new File(Environment.getExternalStorageDirectory(), "fonts"),
                new File(Environment.getExternalStorageDirectory(), "Fonts")
        ));

        // Also check external storage directories, at the respective root and data directory
        for (File externalFileDir : ContextCompat.getExternalFilesDirs(getContext(), null)) {
            if (externalFileDir == null || externalFileDir.getAbsolutePath() == null) {
                continue;
            }
            checkedDirs.add(new File(externalFileDir.getAbsolutePath().replaceFirst("/Android/data/.*$", "/fonts")));
            checkedDirs.add(new File(externalFileDir.getAbsolutePath().replaceFirst("/Android/data/.*$", "/Fonts")));
            checkedDirs.add(new File(externalFileDir.getAbsolutePath(), "/fonts"));
            checkedDirs.add(new File(externalFileDir.getAbsolutePath(), "/Fonts"));
        }
        // Check all directories for fonts
        for (File checkedDir : checkedDirs) {
            if (checkedDir != null && checkedDir.exists()) {
                File[] checkedDirFiles = checkedDir.listFiles(FONT_FILENAME_FILTER);
                if (checkedDirFiles != null) {
                    for (File font : checkedDirFiles) {
                        if (!additionalFonts.contains(new File(font.getAbsolutePath().replace("/Fonts/", "/fonts/")))) {
                            additionalFonts.add(font);
                        }
                    }
                }
            }
        }

        return additionalFonts;
    }

    private static String[] appendToArray(String[] arr, String append) {
        List<String> arro = new ArrayList<>(Arrays.asList(arr));
        arro.add(append);
        return arro.toArray(new String[arr.length + 1]);
    }


    public class TypefaceObjectSpan extends MetricAffectingSpan {
        private final Typeface _typeface;

        public TypefaceObjectSpan(final Typeface typeface) {
            _typeface = typeface;
        }

        @Override
        public void updateDrawState(final TextPaint drawState) {
            apply(drawState);
        }

        @Override
        public void updateMeasureState(final TextPaint paint) {
            apply(paint);
        }

        private void apply(final Paint paint) {
            final Typeface oldTypeface = paint.getTypeface();
            final int oldStyle = oldTypeface != null ? oldTypeface.getStyle() : 0;
            final int fakeStyle = oldStyle & ~_typeface.getStyle();

            if ((fakeStyle & Typeface.BOLD) != 0) {
                paint.setFakeBoldText(true);
            }

            if ((fakeStyle & Typeface.ITALIC) != 0) {
                paint.setTextSkewX(-0.25f);
            }

            paint.setTypeface(_typeface);
        }
    }
}
