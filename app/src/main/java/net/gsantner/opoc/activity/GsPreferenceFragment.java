package net.gsantner.opoc.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.text.TextUtils;
import android.view.View;

import net.gsantner.opoc.util.AppSettingsBase;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

/**
 * Baseclass for (non-appcompat) PreferenceFragment
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class GsPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int DEFAULT_ICON_TINT_DELAY = 200;

    //
    // Abstract
    //

    @XmlRes
    public abstract int getPreferenceResourceForInflation();

    public abstract String getFragmentTag();

    protected abstract AppSettingsBase getAppSettings(Context context);

    //
    // Virtual
    //

    public Boolean onPreferenceClicked(PreferenceScreen screen, Preference preference) {
        return null;
    }

    protected void onPreferenceChanged(SharedPreferences prefs, String key) {

    }

    public String getSharedPreferencesName() {
        return "app";
    }

    protected void afterOnCreate(Bundle savedInstances, Context context) {

    }

    public void updateSummaries() {

    }

    public Integer getIconTintColor() {
        return null;
    }

    public String getTitle() {
        return null;
    }

    //
    //
    //

    private AppSettingsBase _asb;
    protected ContextUtils _cu;

    @Deprecated
    public void onCreate(Bundle savedInstances) {
        super.onCreate(savedInstances);
        _asb = getAppSettings(getActivity());
        _cu = new ContextUtils(getActivity());
        getPreferenceManager().setSharedPreferencesName(getSharedPreferencesName());
        addPreferencesFromResource(getPreferenceResourceForInflation());
        afterOnCreate(savedInstances, getActivity());
    }

    public final Callback.a1<PreferenceFragment> updatePreferenceIcons = (frag) -> {
        try {
            View view = frag.getView();
            final Integer color = getIconTintColor();
            if (view != null && color != null) {
                Runnable r = () -> tintAllPrefIcons(frag, color);
                int d = DEFAULT_ICON_TINT_DELAY;
                int[] delays = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                        new int[]{d} : new int[]{d, d * 10, d * 50, d * 100};
                for (int delay : delays) {
                    view.postDelayed(r, delay);
                }
            }
        } catch (Exception ignored) {
        }
    };

    public void tintAllPrefIcons(PreferenceFragment preferenceFragment, @ColorInt int iconColor) {
        for (String prefKey : preferenceFragment.getPreferenceManager().getSharedPreferences().getAll().keySet()) {
            Preference pref = preferenceFragment.findPreference(prefKey);
            if (pref != null) {
                pref.setIcon(_cu.tintDrawable(pref.getIcon(), iconColor));
            }
        }
    }


    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePreferenceIcons.callback(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        _asb.registerPreferenceChangedListener(this);
        updateSummaries();
    }

    @Override
    public void onPause() {
        super.onPause();
        _asb.unregisterPreferenceChangedListener(this);
    }

    @Override
    @Deprecated
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (isAdded()) {
            onPreferenceChanged(sharedPreferences, key);
        }
    }


    @Override
    @Deprecated
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (isAdded() && preference.hasKey()) {
            Boolean ret = onPreferenceClicked(screen, preference);
            if (ret != null) {
                return ret;
            }
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    protected void updateSummary(@StringRes int keyResId, String summary) {
        updateSummary(keyResId, 0, summary);
    }

    protected void updateSummary(@StringRes int keyResId, @DrawableRes int iconRes, String summary) {
        Preference pref = findPreference(getString(keyResId));
        if (pref != null) {
            pref.setSummary(summary);
            if (iconRes != 0) {
                pref.setIcon(_cu.tintDrawable(iconRes, getIconTintColor()));
            }
        }
    }

    protected void removePreference(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        PreferenceGroup parent = getPreferenceParent(getPreferenceScreen(), preference);
        if (parent == null) {
            return;
        }
        parent.removePreference(preference);
    }

    protected PreferenceGroup getPreferenceParent(PreferenceGroup prefGroup, Preference pref) {
        for (int i = 0; i < prefGroup.getPreferenceCount(); ++i) {
            Preference prefChild = prefGroup.getPreference(i);
            if (prefChild == pref) {
                return prefGroup;
            }
            if (prefChild instanceof PreferenceGroup) {
                PreferenceGroup childGroup = (PreferenceGroup) prefChild;
                PreferenceGroup result = getPreferenceParent(childGroup, pref);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Is key equal
     *
     * @param pref     A preference
     * @param resIdKey the resource id of the string
     * @return if equals
     */
    public boolean eq(@Nullable Preference pref, @StringRes int resIdKey) {
        return pref != null && getString(resIdKey).equals(pref.getKey());
    }


    /**
     * Is key equal
     *
     * @param key      the key
     * @param resIdKey the resource id of the string
     * @return if equals
     */
    public boolean eq(@Nullable String key, @StringRes int resIdKey) {
        return getString(resIdKey).equals(key);
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(getTitle());
    }

    public String getTitleOrDefault(String defaultTitle) {
        return hasTitle() ? getTitle() : defaultTitle;
    }

    protected void restartActivity() {
        Activity activity;
        if (isAdded() && (activity = getActivity()) != null) {
            Intent intent = getActivity().getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            activity.overridePendingTransition(0, 0);
            activity.finish();
            activity.overridePendingTransition(0, 0);
            startActivity(intent);
        }
    }
}
