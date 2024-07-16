/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/

package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.format.GsSimpleMarkdownParser;
import net.gsantner.opoc.frontend.base.GsPreferenceFragmentBase;

import java.io.IOException;
import java.util.Locale;

public class MoreInfoFragment extends GsPreferenceFragmentBase<AppSettings> {
    public static final String TAG = "MoreInfoFragment";

    public static MoreInfoFragment newInstance() {
        return new MoreInfoFragment();
    }

    @Override
    public int getPreferenceResourceForInflation() {
        return R.xml.prefactions__more_information;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    protected AppSettings getAppSettings(Context context) {
        return ApplicationObject.settings();
    }

    @Override
    public boolean isDividerVisible() {
        return true;
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "ConstantIfStatement", "StatementWithEmptyBody"})
    public Boolean onPreferenceClicked(Preference preference, String key, int keyResId) {
        Activity activity = getActivity();
        if (isAdded() && preference.hasKey()) {
            switch (keyResId) {
                case R.string.pref_key__more_info__app: {
                    _cu.openWebpageInExternalBrowser(getContext(), getString(R.string.app_web_url));
                    return true;
                }
                case R.string.pref_key__more_info__settings: {
                    _cu.animateToActivity(activity, SettingsActivity.class, false, 124);
                    return true;
                }
                case R.string.pref_key__more_info__rate_app: {
                    _cu.showGooglePlayEntryForThisApp(getContext());
                    return true;
                }

                case R.string.pref_key__more_info__join_community: {
                    _cu.openWebpageInExternalBrowser(getContext(), getString(R.string.app_community_url));
                    return true;
                }
                case R.string.pref_key__more_info__help: {
                    _cu.openWebpageInExternalBrowser(getContext(),
                            String.format("https://github.com/gsantner/%s#readme", getString(R.string.app_name_real).toLowerCase()));
                    return true;
                }
                case R.string.pref_key__more_info__bug_reports: {
                    _cu.openWebpageInExternalBrowser(getContext(), getString(R.string.app_bugreport_url));
                    return true;
                }
                case R.string.pref_key__more_info__translate: {
                    _cu.openWebpageInExternalBrowser(getContext(), getString(R.string.app_translate_url));
                    return true;
                }
                case R.string.pref_key__more_info__source_code: {
                    _cu.openWebpageInExternalBrowser(getContext(), getString(R.string.app_source_code_url));
                    return true;
                }
                case R.string.pref_key__more_info__project_license: {
                    _cu.showDialogWithRawFileInWebView(getActivity(), "license.txt", R.string.project_license);
                    return true;
                }
                case R.string.pref_key__more_info__open_source_licenses: {
                    try {
                        _cu.showDialogWithHtmlTextView(getActivity(), R.string.licenses, new GsSimpleMarkdownParser().parse(
                                getResources().openRawResource(R.raw.licenses_3rd_party),
                                "", GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW).getHtml());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                case R.string.pref_key__more_info__contributors_public_info: {
                    try {
                        _cu.showDialogWithHtmlTextView(getActivity(), R.string.contributors, new GsSimpleMarkdownParser().parse(
                                getResources().openRawResource(R.raw.contributors),
                                "", GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW).getHtml());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                case R.string.pref_key__more_info__copy_build_information: {
                    _cu.setClipboard(getContext(), preference.getSummary());
                    GsSimpleMarkdownParser smp = new GsSimpleMarkdownParser();
                    try {
                        String html = smp.parse(getResources().openRawResource(R.raw.changelog), "", GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW).getHtml();
                        _cu.showDialogWithHtmlTextView(getActivity(), R.string.changelog, html);
                    } catch (Exception ex) {

                    }
                    return true;
                }
            }
        }
        return null;
    }

    @Override
    protected boolean isAllowedToTint(Preference pref) {
        return !getString(R.string.pref_key__more_info__app).equals(pref.getKey());
    }

    @Override
    public synchronized void doUpdatePreferences() {
        super.doUpdatePreferences();
        Context context = getContext();
        if (context == null) {
            return;
        }
        Locale locale = Locale.getDefault();
        String tmp;
        Preference pref;
        updateSummary(R.string.pref_key__more_info__project_license, getString(R.string.app_license_name));

        // Basic app info
        if ((pref = findPreference(R.string.pref_key__more_info__app)) != null && pref.getSummary() == null) {
            pref.setIcon(R.drawable.ic_launcher);
            pref.setSummary(String.format(locale, "%s\nVersion v%s (%d)", _cu.getAppIdFlavorSpecific(context), _cu.getAppVersionName(context), _cu.bcint(context, "VERSION_CODE", 0)));
        }

        // Extract some build information and publish in summary
        if ((pref = findPreference(R.string.pref_key__more_info__copy_build_information)) != null && pref.getSummary() == null) {
            String summary = String.format(locale, "\n<b>Package:</b> %s\n<b>Version:</b> v%s (%d)", _cu.getAppIdFlavorSpecific(context), _cu.getAppVersionName(context), _cu.bcint(context, "VERSION_CODE", 0));
            summary += (tmp = _cu.bcstr(context, "FLAVOR", "")).isEmpty() ? "" : ("\n<b>Flavor:</b> " + tmp.replace("flavor", ""));
            summary += (tmp = _cu.bcstr(context, "BUILD_TYPE", "")).isEmpty() ? "" : (" (" + tmp + ")");
            summary += (tmp = _cu.bcstr(context, "BUILD_DATE", "")).isEmpty() ? "" : ("\n<b>Build date:</b> " + tmp);
            summary += (tmp = _cu.getAppInstallationSource(context)).isEmpty() ? "" : ("\n<b>ISource:</b> " + tmp);
            summary += (tmp = _cu.bcstr(context, "GITHASH", "")).isEmpty() ? "" : ("\n<b>VCS Hash:</b> " + tmp);
            summary += (tmp = _cu.bcstr(context, "GITMSG", "")).isEmpty() ? "" : ("\n<b>VCS Msg:</b> " + tmp);
            pref.setSummary(_cu.htmlToSpanned(summary.trim().replace("\n", "<br/>")));
        }

        // Extract project team from raw ressource, where 1 person = 4 lines
        // 1) Name/Title, 2) Description/Summary, 3) Link/View-Intent, 4) Empty line
        if ((pref = findPreference(R.string.pref_key__more_info__project_team)) != null && ((PreferenceGroup) pref).getPreferenceCount() == 0) {
            String[] data = (_cu.readTextfileFromRawRes(context, R.raw.project_team, "", "").trim() + "\n\n").split("\n");
            for (int i = 0; i + 2 < data.length; i += 4) {
                Preference person = new Preference(context);
                person.setTitle(data[i]);
                person.setSummary(data[i + 1]);
                person.setIcon(R.drawable.ic_person_black_24dp);
                try {
                    Uri uri = Uri.parse(data[i + 2]);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    person.setIntent(intent);
                } catch (Exception ignored) {
                }
                appendPreference(person, (PreferenceGroup) pref);
            }
        }

        if ((pref = findPreference(R.string.pref_key__more_info__help)) != null) {
            pref.setTitle(getString(R.string.help) + " / FAQ");
        }
    }
}
