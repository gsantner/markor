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
 * Add dependencies:
    implementation "com.android.support:preference-v7:${version_library_appcompat}"
    implementation "com.android.support:preference-v14:${version_library_appcompat}"

 * Apply to activity using setTheme(), add to styles.xml/theme:
        <item name="preferenceTheme">@style/PreferenceThemeOverlay.v14.Material</item>
 * OR
    <style name="AppTheme" ...
        <item name="preferenceTheme">@style/AppTheme.PreferenceTheme</item>
    </style>
    <style name="AppTheme.PreferenceTheme" parent="PreferenceThemeOverlay.v14.Material">
      <item name="preferenceCategoryStyle">@style/AppTheme.PreferenceTheme.CategoryStyle</item>
    </style>
    <style name="AppTheme.PreferenceTheme.CategoryStyle" parent="Preference.Category">
        <item name="android:layout">@layout/opoc_pref_category_text</item>
    </style>

 * Layout file:
    <?xml version="1.0" encoding="utf-8"?>
    <TextView xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@android:id/title"
        style="?android:attr/listSeparatorTextViewStyle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAllCaps="false"
        android:textColor="@color/colorAccent" />


 */
package net.gsantner.opoc.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.support.v4.app.Fragment;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Baseclass to use as preference fragment (with support libraries)
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public abstract class GsPreferenceFragmentCompat<AS extends SharedPreferencesPropertyBackend> extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private static final int DEFAULT_ICON_TINT_DELAY = 200;
    protected boolean _isDividerVisible = false;

    //
    // Abstract
    //

    @XmlRes
    public abstract int getPreferenceResourceForInflation();

    public abstract String getFragmentTag();

    protected abstract AS getAppSettings(Context context);

    //
    // Virtual
    //

    public Boolean onPreferenceClicked(Preference preference, String key, int keyResId) {
        return null;
    }

    public String getSharedPreferencesName() {
        return "app";
    }

    protected void afterOnCreate(Bundle savedInstances, Context context) {

    }

    public synchronized void doUpdatePreferences() {
    }

    protected void onPreferenceScreenChanged(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
    }

    public Integer getIconTintColor() {
        return _defaultIconTintColor;
    }

    public String getTitle() {
        return null;
    }

    //
    //
    //

    private final Set<String> _registeredPrefs = new HashSet<>();
    private final List<PreferenceScreen> _prefScreenBackstack = new ArrayList<>();
    protected AS _appSettings;
    protected int _defaultIconTintColor;
    protected ContextUtils _cu;

    @Override
    @Deprecated
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Activity activity = getActivity();
        _appSettings = getAppSettings(activity);
        _cu = new ContextUtils(activity);
        getPreferenceManager().setSharedPreferencesName(getSharedPreferencesName());
        addPreferencesFromResource(getPreferenceResourceForInflation());

        if (activity != null && activity.getTheme() != null) {
            TypedArray array = activity.getTheme().obtainStyledAttributes(new int[]{android.R.attr.colorBackground});
            int bgcolor = array.getColor(0, 0xFFFFFFFF);
            _defaultIconTintColor = _cu.shouldColorOnTopBeLight(bgcolor) ? Color.WHITE : Color.BLACK;
        }

        // on bottom
        afterOnCreate(savedInstanceState, activity);
    }

    public final Callback.a1<PreferenceFragmentCompat> updatePreferenceIcons = (frag) -> {
        try {
            View view = frag.getView();
            final Integer color = getIconTintColor();
            if (view != null && color != null) {
                Runnable r = () -> tintAllPrefIcons(frag, color);
                for (long delayFactor : new int[]{1, 10, 50, 100, 500}) {
                    view.postDelayed(r, delayFactor * DEFAULT_ICON_TINT_DELAY);
                }
            }
        } catch (Exception ignored) {
        }
    };

    public void tintAllPrefIcons(PreferenceFragmentCompat preferenceFragment, @ColorInt int iconColor) {
        tintPrefIconsRecursive(getPreferenceScreen(), iconColor);
    }

    private void tintPrefIconsRecursive(PreferenceGroup prefGroup, @ColorInt int iconColor) {
        if (prefGroup != null && isAdded()) {
            int prefCount = prefGroup.getPreferenceCount();
            for (int i = 0; i < prefCount; i++) {
                Preference pref = prefGroup.getPreference(i);
                if (pref != null) {
                    if (isAllowedToTint(pref)) {
                        pref.setIcon(_cu.tintDrawable(pref.getIcon(), iconColor));
                    }
                    if (pref instanceof PreferenceGroup) {
                        tintPrefIconsRecursive((PreferenceGroup) pref, iconColor);
                    }
                }
            }
        }
    }

    protected boolean isAllowedToTint(Preference pref) {
        return true;
    }

    /**
     * Try to fetch string resource id from key
     * This only works if the key is only defined once and value=key
     */
    protected int keyToStringResId(Preference preference) {
        if (preference != null && !TextUtils.isEmpty(preference.getKey())) {
            return _cu.getResId(ContextUtils.ResType.STRING, preference.getKey());
        }
        return 0;
    }

    /**
     * Try to fetch string resource id from key
     * This only works if the key is only defined once and value=key
     */
    protected int keyToStringResId(String keyAsString) {
        return _cu.getResId(ContextUtils.ResType.STRING, keyAsString);
    }


    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePreferenceIcons.callback(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            view.postDelayed(() -> {
                ViewGroup.LayoutParams lpg = view.getLayoutParams();
                if (lpg instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) lpg;
                    lp.rightMargin = lp.leftMargin = (int) _cu.convertDpToPx(16);
                    view.setLayoutParams(lp);
                } else if (lpg instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) lpg;
                    lp.rightMargin = lp.leftMargin = (int) _cu.convertDpToPx(16);
                    view.setLayoutParams(lp);
                } else if (lpg instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) lpg;
                    lp.rightMargin = lp.leftMargin = (int) _cu.convertDpToPx(16);
                    view.setLayoutParams(lp);
                }
            }, 10);
        }
    }

    private synchronized void updatePreferenceChangedListeners(boolean shouldListen) {
        String tprefname = getSharedPreferencesName();
        if (shouldListen && tprefname != null && !_registeredPrefs.contains(tprefname)) {
            SharedPreferences preferences = _appSettings.getContext().getSharedPreferences(tprefname, Context.MODE_PRIVATE);
            _appSettings.registerPreferenceChangedListener(preferences, this);
            _registeredPrefs.add(tprefname);
        } else if (!shouldListen) {
            for (String prefname : _registeredPrefs) {
                SharedPreferences preferences = _appSettings.getContext().getSharedPreferences(tprefname, Context.MODE_PRIVATE);
                _appSettings.unregisterPreferenceChangedListener(preferences, this);
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceChangedListeners(true);
        doUpdatePreferences(); // Invoked later
        onPreferenceScreenChangedPriv(this, getPreferenceScreen());
    }

    @Override
    public void onPause() {
        super.onPause();
        updatePreferenceChangedListeners(false);
    }

    @Override
    @Deprecated
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (isAdded()) {
            onPreferenceChanged(sharedPreferences, key);
            doUpdatePreferences();
        }
    }

    protected void onPreferenceChanged(SharedPreferences prefs, String key) {
        // Wait some ms to be sure the pref objects have changed it's internal values
        // and the new values are ready to be read ;)
        Runnable r = this::doUpdatePreferences;
        if (getView() != null) {
            getView().postDelayed(r, 350);
        } else {
            r.run();
        }
    }

    @Override
    @Deprecated
    public boolean onPreferenceTreeClick(Preference preference) {
        if (isAdded()) {
            String key = preference.hasKey() ? preference.getKey() : "";
            int keyResId = keyToStringResId(preference);
            Boolean ret = onPreferenceClicked(preference, key, keyResId);
            if (ret != null) {
                return ret;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }


    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    @Override
    public void onStop() {
        _prefScreenBackstack.clear();
        super.onStop();
    }

    @Deprecated
    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        _prefScreenBackstack.add(getPreferenceScreen());
        preferenceFragmentCompat.setPreferenceScreen(preferenceScreen);
        updatePreferenceIcons.callback(this);
        onPreferenceScreenChangedPriv(preferenceFragmentCompat, preferenceScreen);
        return true;
    }

    protected void updateSummary(@StringRes int keyResId, CharSequence summary) {
        updatePreference(keyResId, null, null, summary, null);
    }

    /**
     * Finds a {@link Preference} based on its key res id.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link DialogPreference} with the key, or null.
     * @see android.support.v7.preference.PreferenceGroup#findPreference(CharSequence)
     */
    public DialogPreference setDialogMessage(@StringRes int key, CharSequence message) {
        Preference p = findPreference(key);
        if (p instanceof DialogPreference) {
            ((DialogPreference) p).setDialogMessage(message);
            return (DialogPreference) p;
        }
        return null;
    }

    /**
     * Finds a {@link Preference} based on its key res id.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link Preference} with the key, or null.
     * @see android.support.v7.preference.PreferenceGroup#findPreference(CharSequence)
     */
    public Preference findPreference(@StringRes int key) {
        return isAdded() ? findPreference(getString(key)) : null;
    }

    /**
     * Finds a {@link Preference} based on its key res id.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link Preference} with the key, or null.
     * @see android.support.v7.preference.PreferenceGroup#findPreference(CharSequence)
     */
    public Preference setPreferenceVisible(@StringRes int key, boolean visible) {
        Preference pref;
        if ((pref = findPreference(key)) != null) {
            pref.setVisible(visible);
        }
        return pref;
    }

    @Nullable
    @SuppressWarnings("SameParameterValue")
    protected Preference updatePreference(@StringRes int keyResId, @DrawableRes Integer iconRes, CharSequence title, CharSequence summary, Boolean visible) {
        Preference pref = findPreference(getString(keyResId));
        if (pref != null) {
            if (summary != null) {
                pref.setSummary(summary);
            }
            if (title != null) {
                pref.setTitle(title);
            }
            if (iconRes != null && iconRes != 0) {
                if (isAllowedToTint(pref)) {
                    pref.setIcon(_cu.tintDrawable(iconRes, getIconTintColor()));
                } else {
                    pref.setIcon(iconRes);
                }
            }
            if (visible != null) {
                pref.setVisible(visible);
            }
        }
        return pref;
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

    public boolean canGoBack() {
        return !_prefScreenBackstack.isEmpty();
    }

    public void goBack() {
        if (canGoBack()) {
            PreferenceScreen screen = _prefScreenBackstack.remove(_prefScreenBackstack.size() - 1);
            if (screen != null) {
                setPreferenceScreen(screen);
                onPreferenceScreenChangedPriv(this, screen);
            }
        }
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

    private void onPreferenceScreenChangedPriv(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        setDividerVisibility(isDividerVisible());
        onPreferenceScreenChanged(preferenceFragmentCompat, preferenceScreen);
        updatePreferenceChangedListeners(true);
        doUpdatePreferences();
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

    /**
     * Append a pref to given {@code target}. If target is null, the current screen is taken
     * The pref icon is tint according to color
     *
     * @param pref   Preference to add
     * @param target The target to add the pref to, or null for current screen
     * @return true if successfully added
     */
    protected boolean appendPreference(Preference pref, @Nullable PreferenceGroup target) {
        if (target == null) {
            if ((target = getPreferenceScreen()) == null) {
                return false;
            }
        }
        if (getIconTintColor() != null && pref.getIcon() != null && isAllowedToTint(pref)) {
            pref.setIcon(_cu.tintDrawable(pref.getIcon(), getIconTintColor()));
        }
        return target.addPreference(pref);
    }


    //###############################
    //### Divider
    ////###############################


    public boolean isDividerVisible() {
        return _isDividerVisible;
    }

    public void setDividerVisibility(boolean visible) {
        _isDividerVisible = visible;
        RecyclerView recyclerView = getListView();
        if (visible) {
            recyclerView.addItemDecoration(new DividerDecoration(getContext(), getDividerColor(), _flatPosIsPreferenceCategoryCallback));
        } else if (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecoration(recyclerView.getItemDecorationAt(0));
        }
    }

    public Integer getDividerColor() {
        ActivityUtils au = new ActivityUtils(getActivity());
        try {
            return Color.parseColor(au.shouldColorOnTopBeLight(au.getActivityBackgroundColor()) ? "#3d3d3d" : "#d1d1d1");
        } catch (Exception ignored) {
            return null;
        } finally {
            au.freeContextRef();
        }
    }

    Callback.b1<Integer> _flatPosIsPreferenceCategoryCallback = position -> {
        int flatPos = 0;
        PreferenceGroup prefGroup = getPreferenceScreen();
        if (prefGroup != null) {
            int prefCount = prefGroup.getPreferenceCount();
            for (int i = 0; i < prefCount; i++) {
                Preference pref = prefGroup.getPreference(i);
                if (pref != null) {
                    if (pref instanceof PreferenceCategory) {
                        PreferenceGroup prefSubGroup = ((PreferenceGroup) pref);
                        for (int j = 0; j < prefSubGroup.getPreferenceCount(); j++) {
                            flatPos++;
                            if (flatPos == position) {
                                return !(prefSubGroup.getPreference(j) instanceof PreferenceCategory);
                            }
                        }
                    } else if (flatPos == position) {
                        return true;
                    }
                }
                flatPos++;
            }
        }
        return false;
    };

    /**
     * Divider for preferences
     */
    public static class DividerDecoration extends RecyclerView.ItemDecoration {
        private Callback.b1<Integer> _isCategoryAtFlatpositionCallback;
        private final Paint _paint;
        private int _heightDp;

        public DividerDecoration(Context context, @Nullable Callback.b1<Integer> isCategoryAtFlatpos) {
            this(context, null, 1f, isCategoryAtFlatpos);
        }

        // b8b8b8          = default divider color
        // d1d1d1 / 3d3d3d = color for light / dark mode
        public DividerDecoration(Context context, @Nullable @ColorInt Integer color, @Nullable Callback.b1<Integer> isCategoryAtFlatpos) {
            this(context, color, 1f, isCategoryAtFlatpos);
        }

        public DividerDecoration(Context context, @Nullable @ColorInt Integer color, float heightDp, @Nullable Callback.b1<Integer> isCategoryAtFlatpos) {
            if (color == null) {
                color = Color.parseColor("#b8b8b8");
            }
            _isCategoryAtFlatpositionCallback = isCategoryAtFlatpos;
            _paint = new Paint();
            _paint.setStyle(Paint.Style.FILL);
            _paint.setColor(color);
            _heightDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, context.getResources().getDisplayMetrics());
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int viewType = 0;
            try {
                viewType = parent.getAdapter() != null ? parent.getAdapter().getItemViewType(position) : 0;
            } catch (NullPointerException ignored) {
            }
            if (viewType != 1) {
                outRect.set(0, 0, 0, _heightDp);
            } else {
                outRect.setEmpty();
            }
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                View view = parent.getChildAt(i);
                int position = parent.getChildAdapterPosition(view);
                if (_isCategoryAtFlatpositionCallback == null || _isCategoryAtFlatpositionCallback.callback(position)) {
                    c.drawRect(view.getLeft(), view.getBottom(), view.getRight(), view.getBottom() + _heightDp, _paint);
                }
            }
        }
    }
}
