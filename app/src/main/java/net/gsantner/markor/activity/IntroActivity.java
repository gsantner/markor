package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.opoc.util.GsContextUtils;

public class IntroActivity extends AppIntro {
    private static final String PREF_KEY_WAS_SHOWN = IntroActivity.class.getCanonicalName() + "was_shown";
    public static final int REQ_CODE_APPINTRO = 61234;

    public static boolean optStart(final Activity activeActivity) {
        final boolean firstStart = isFirstStart(activeActivity);
        if (firstStart) {
            activeActivity.startActivityForResult(new Intent(activeActivity, IntroActivity.class), REQ_CODE_APPINTRO);
        }
        return firstStart;
    }

    public static boolean isFirstStart(final Context context) {
        final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return !getPrefs.getBoolean(PREF_KEY_WAS_SHOWN, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GsContextUtils.instance.setAppLanguage(this, ApplicationObject.settings().getLanguage());

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest
        addSlide(AppIntroFragment.createInstance(getString(R.string.main_view), getString(R.string.notebook_is_the_home_of_your_files), R.drawable.screen1_main_view, R.color.primary));
        addSlide(AppIntroFragment.createInstance(getString(R.string.view), "", R.drawable.screen3_view, R.color.primary));
        addSlide(AppIntroFragment.createInstance(getString(R.string.share) + " -> " + getString(R.string.app_name), "", R.drawable.screen4_share_into, R.color.primary));
        addSlide(AppIntroFragment.createInstance(getString(R.string.todo), getString(R.string.todo_is_the_easiest_way_), R.drawable.ic_launcher_todo, R.color.primary));
        addSlide(AppIntroFragment.createInstance(getString(R.string.quicknote), getString(R.string.quicknote_is_the_fastest_option_to_write_down_notes), R.drawable.ic_launcher_quicknote, R.color.primary));

        // Permissions -- takes a permission and slide number
        setSkipButtonEnabled(false);
        setSwipeLock(false);
        setSeparatorColor(Color.DKGRAY);
        setIndicatorColor(GsContextUtils.instance.rcolor(this, R.color.accent), Color.LTGRAY);
    }

    @Override
    protected void onSkipPressed(@Nullable Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    protected void onNextPressed(@Nullable Fragment currentFragment) {
        super.onNextPressed(currentFragment);
    }

    @Override
    protected void onDonePressed(@Nullable Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(PREF_KEY_WAS_SHOWN, true).apply();
        finish();
    }

    @Override
    protected void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
    }
}
