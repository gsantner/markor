/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.github.io> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */

/*
 * A ListPreference that displays a list of available languages
 * Requires:
 *     The BuildConfig field "APPLICATION_LANGUAGES" which is a array of all available languages
 *     opoc/Helpers
 * BuildConfig field can be defined by using the method below

    buildConfigField("String[]", "APPLICATION_LANGUAGES", '{' + getUsedAndroidLanguages().collect {"\"${it}\""}.join(",")  + '}')

    String[] getUsedAndroidLanguages(){
        Set<String> langs = new HashSet<>();
        String[] resFolders = new File("app/src/main/res").list()
        for(resFolder in resFolders){
            if (resFolder.startsWith("values-")){
                String[] files = new File("app/src/main/res/"+resFolder).list();
                for (file in files){
                    if (file.startsWith("strings") && file.endsWith(".xml")){
                        langs.add(resFolder.replace("values-",""))
                        break;
                    }
                }
            }
        }
        return langs.toArray(new String[langs.size()])
    }

 * Define element in Preferences-XML:
    <!--suppress AndroidDomInspection -->
    <io.github.gsantner.opoc.ui.LanguagePreference
        android:icon="@drawable/ic_language_black_24dp"
        android:defaultValue=""
        android:key="@string/pref_key__language"
        android:summary="@string/pref_desc__language"
        android:title="@string/pref_title__language"/>
 */
package io.github.gsantner.opoc.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.github.gsantner.opoc.util.Helpers;

/**
 * A {@link android.preference.ListPreference} that displays a list of languages to select from
 */
@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class LanguagePreference extends ListPreference {
    public LanguagePreference(Context context) {
        super(context);
        init(context, null);
    }

    public LanguagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LanguagePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LanguagePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Override
    protected boolean callChangeListener(Object newValue) {
        if (newValue instanceof String) {
            // Does not apply to existing UI, use recreate()
            new Helpers(getContext()).setAppLanguage((String) newValue);
        }
        return super.callChangeListener(newValue);
    }

    private void init(Context context, AttributeSet attrs) {
        // Fetch readable details
        Helpers helpers = new Helpers(context);
        List<String> languages = new ArrayList<>();
        Object bcof = helpers.getBuildConfigValue("APPLICATION_LANGUAGES");
        if (bcof instanceof String[]) {
            for (String langId : (String[]) bcof) {
                Locale locale = helpers.getLocaleByAndroidCode(langId);
                languages.add(summarizeLocale(locale) + ";" + langId);
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
        entries[0] = "System";
        entryval[0] = "";
        entries[1] = "English";
        entryval[1] = "en";

        setEntries(entries);
        setEntryValues(entryval);
    }

    // Concat english and localized language name
    // Append country if country specific (e.g. Portuguese Brazil)
    private String summarizeLocale(Locale locale) {
        String country = locale.getDisplayCountry(locale);
        String language = locale.getDisplayLanguage(locale);
        return locale.getDisplayLanguage(Locale.ENGLISH)
                + " (" + language.substring(0, 1).toUpperCase() + language.substring(1)
                + ((!country.isEmpty() && !country.toLowerCase().equals(language.toLowerCase())) ? (", " + country) : "")
                + ")";
    }

    // Add current language to summary
    @Override
    public CharSequence getSummary() {
        Locale locale = new Helpers(getContext()).getLocaleByAndroidCode(getValue());
        return super.getSummary() + "\n\n" + summarizeLocale(locale);
    }
}
