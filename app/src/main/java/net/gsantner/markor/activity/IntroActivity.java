package net.gsantner.markor.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.opoc.util.PermissionChecker;

public class IntroActivity extends AppIntro {
    private static final String PREF_KEY_WAS_SHOWN = IntroActivity.class.getCanonicalName() + "was_shown";

    public static void optStart(Activity activeActivity) {
        SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(activeActivity.getBaseContext());
        boolean wasShownYet = getPrefs.getBoolean(PREF_KEY_WAS_SHOWN, false);
        if (!wasShownYet) {
            activeActivity.startActivity(new Intent(activeActivity, IntroActivity.class));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new ContextUtils(this).setAppLanguage(AppSettings.get().getLanguage());

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest
        addSlide(AppIntroFragment.newInstance(getString(R.string.main_view), getString(R.string.notebook_is_the_home_of_your_files), R.drawable.screen1_main_view, ContextCompat.getColor(getApplicationContext(), R.color.primary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.editor), getString(R.string.error_need_storage_permission_to_save_documents), R.drawable.screen2_editor, ContextCompat.getColor(getApplicationContext(), R.color.primary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.view), "", R.drawable.screen3_view, ContextCompat.getColor(getApplicationContext(), R.color.primary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.share) + " -> " + getString(R.string.app_name), "", R.drawable.screen4_share_into, ContextCompat.getColor(getApplicationContext(), R.color.primary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.todo), getString(R.string.todo_is_the_easiest_way_), R.drawable.ic_launcher_todo, ContextCompat.getColor(getApplicationContext(), R.color.primary)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.quicknote), getString(R.string.quicknote_is_the_fastest_option_to_write_down_notes), R.drawable.ic_launcher_quicknote, ContextCompat.getColor(getApplicationContext(), R.color.primary)));

        // Permissions -- takes a permission and slide number
        askForPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, getSlides().size());
        showSkipButton(false);
        setNextPageSwipeLock(false);
        setSwipeLock(false);
    }

    @Override
    public void onSkipPressed() {
        if (new PermissionChecker(this).doIfExtStoragePermissionGranted()) {
            finish();
        }
        if (getPager().getCurrentItem() < getSlides().size()) {
            for (int i = getPager().getCurrentItem(); i < getSlides().size(); i++) {
                getPager().goToNextSlide();
            }
        }
    }

    @Override
    public void onNextPressed() {
    }

    @Override
    public void onDonePressed() {
        PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putBoolean(PREF_KEY_WAS_SHOWN, true).apply();
        finish();
    }

    @Override
    public void onSlideChanged() {
        if (getPager().getCurrentItem() == 2) {
            new PermissionChecker(this).doIfExtStoragePermissionGranted();
        }
    }
}
