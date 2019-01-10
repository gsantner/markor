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
 * Requires:
      The BuildConfig field "APPLICATION_LANGUAGES" which is a array of all available languages
      opoc/ContextUtils
 * BuildConfig field can be defined by using the method below

buildConfigField "String[]", "APPLICATION_LANGUAGES", "${getUsedAndroidLanguages()}"

@SuppressWarnings(["UnnecessaryQualifiedReference", "SpellCheckingInspection", "GroovyUnusedDeclaration"])
// Returns used android languages as a buildConfig array: {'de', 'it', ..}"
static String getUsedAndroidLanguages() {
    Set<String> langs = new HashSet<>()
    new File('.').eachFileRecurse(groovy.io.FileType.DIRECTORIES) {
        final foldername = it.name
        if (foldername.startsWith('values-') && !it.canonicalPath.contains("build" + File.separator + "intermediates")) {
            new File(it.toString()).eachFileRecurse(groovy.io.FileType.FILES) {
                if (it.name.toLowerCase().endsWith(".xml") && it.getCanonicalFile().getText('UTF-8').contains("<string")) {
                    langs.add(foldername.replace("values-", ""))
                }
            }
        }
    }
    return '{' + langs.collect { "\"${it}\"" }.join(",") + '}'
}

 * Summary: Change language of this app. Restart app for changes to take effect

 * Define element in Preferences-XML:
    <net.gsantner.opoc.preference.LanguagePreferenceCompat
        android:icon="@drawable/ic_language_black_24dp"
        android:key="@string/pref_key__language"
        android:summary="@string/pref_desc__language"
        android:title="@string/pref_title__language"/>
 */
package net.gsantner.opoc.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.os.ConfigurationCompat;
import android.support.v7.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A {@link android.preference.ListPreference} that displays a list of languages to select from
 */
@SuppressWarnings({"unused", "SpellCheckingInspection", "WeakerAccess"})
public class LanguagePreferenceCompat extends ListPreference {
    private static final String SYSTEM_LANGUAGE_CODE = "";

    // The language of res/values/ -> (usually English)
    public String _systemLanguageName = "System";
    public String _defaultLanguageCode = "en";

    public LanguagePreferenceCompat(Context context) {
        super(context);
        loadLangs(context, null);
    }

    public LanguagePreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadLangs(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LanguagePreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        loadLangs(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LanguagePreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        loadLangs(context, attrs);
    }

    @Override
    public boolean callChangeListener(Object newValue) {
        if (newValue instanceof String) {
            // Does not apply to existing UI, use recreate()
            new ContextUtils(getContext()).setAppLanguage((String) newValue);
        }
        return super.callChangeListener(newValue);
    }


    private void loadLangs(Context context) {
        loadLangs(context, null);
    }

    private void loadLangs(Context context, @Nullable AttributeSet attrs) {
        setDefaultValue(SYSTEM_LANGUAGE_CODE);

        // Fetch readable details
        ContextUtils contextUtils = new ContextUtils(context);
        List<String> languages = new ArrayList<>();
        Object bcof = contextUtils.getBuildConfigValue("DETECTED_ANDROID_LOCALES");
        if (bcof instanceof String[]) {
            for (String langId : (String[]) bcof) {
                Locale locale = contextUtils.getLocaleByAndroidCode(langId);
                languages.add(summarizeLocale(locale, langId) + ";" + langId);
            }
        }

        // Sort languages naturally
        Collections.sort(languages);

        // Show in UI
        String[] entries = new String[languages.size() + 2];
        String[] entryval = new String[languages.size() + 2];
        for (int i = 0; i < languages.size(); i++) {
            entries[i + 2] = languages.get(i).split(";")[0];
            entryval[i + 2] = languages.get(i).split(";")[1];
        }
        entryval[0] = SYSTEM_LANGUAGE_CODE;
        entries[0] = _systemLanguageName + " » " + summarizeLocale(ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0), "");
        entryval[1] = _defaultLanguageCode;
        entries[1] = summarizeLocale(contextUtils.getLocaleByAndroidCode(_defaultLanguageCode), _defaultLanguageCode);

        setEntries(entries);
        setEntryValues(entryval);
    }

    // Concat english and localized language name
    // Append country if country specific (e.g. Portuguese Brazil)
    private String summarizeLocale(final Locale locale, final String localeAndroidCode) {
        String country = locale.getDisplayCountry(locale);
        String language = locale.getDisplayLanguage(locale);
        String ret = locale.getDisplayLanguage(Locale.ENGLISH)
                + " (" + language.substring(0, 1).toUpperCase(Locale.getDefault()) + language.substring(1)
                + ((!country.isEmpty() && !country.toLowerCase(Locale.getDefault()).equals(language.toLowerCase(Locale.getDefault()))) ? (", " + country) : "")
                + ")";

        if (localeAndroidCode.equals("zh-rCN")) {
            ret = ret.substring(0, ret.indexOf(" ") + 1) + "Simplified" + ret.substring(ret.indexOf(" "));
        } else if (localeAndroidCode.equals("zh-rTW")) {
            ret = ret.substring(0, ret.indexOf(" ") + 1) + "Traditional" + ret.substring(ret.indexOf(" "));
        } else if (localeAndroidCode.equals("sr-rRS")) {
            ret = ret.substring(0, ret.indexOf(" ") + 1) + "Latin" + ret.substring(ret.indexOf(" "));
        } else if (localeAndroidCode.startsWith("sr")) {
            ret = ret.substring(0, ret.indexOf(" ") + 1) + "Cyrillic" + ret.substring(ret.indexOf(" "));
        } else if (localeAndroidCode.equals("fil")) {
            ret = ret.substring(0, ret.indexOf("(") + 1) + "Philippines)";
        }

        return ret;
    }

    // Add current language to summary
    @Override
    public CharSequence getSummary() {
        Locale locale = new ContextUtils(getContext()).getLocaleByAndroidCode(getValue());
        String prefix = TextUtils.isEmpty(super.getSummary())
                ? "" : super.getSummary() + "\n\n";
        return prefix + summarizeLocale(locale, getValue());
    }

    public String getSystemLanguageName() {
        return _systemLanguageName;
    }

    public void setSystemLanguageName(String systemLanguageName) {
        _systemLanguageName = systemLanguageName;
        loadLangs(getContext());
    }

    public String getDefaultLanguageCode() {
        return _defaultLanguageCode;
    }

    public void setDefaultLanguageCode(String defaultLanguageCode) {
        _defaultLanguageCode = defaultLanguageCode;
        loadLangs(getContext());
    }
}
