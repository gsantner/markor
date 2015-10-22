package me.writeily.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import me.writeily.R;
import me.writeily.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class SettingsActivity extends ActionBarActivity implements SettingsFragment.WriteilySettingsListener {

    SettingsFragment settingsFragment;

    private final BroadcastReceiver fsSelectFolderBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.FILESYSTEM_SELECT_FOLDER_TAG)) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putString(getString(R.string.pref_root_directory),intent.getStringExtra(Constants.FILESYSTEM_FILE_NAME));
                editor.apply();
                settingsFragment.updateRootDirSummary();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Context context = getApplicationContext();
        String theme = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_theme_key), "");
        settingsFragment = new SettingsFragment();

        if (theme.equals(context.getString(R.string.theme_dark))) {
            setTheme(R.style.AppTheme);
        } else {
            // uses light theme
            setTheme(R.style.AppTheme_Light);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Show main settings page or about screen?
        boolean showAbout = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_SHOW_ABOUT, false);

        if (!showAbout) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.frame, settingsFragment)
                    .commit();
        } else {
            setTitle(R.string.pref_about_dialog_title);
            getFragmentManager().beginTransaction()
                    .replace(R.id.frame, new AboutFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onThemeChanged() {
        // Restart settings activity to reflect theme changes
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        finish();

        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    @Override
    public void onAboutClicked() {
        Intent intent = getIntent();
        intent.putExtra(Constants.INTENT_EXTRA_SHOW_ABOUT, true);
        startActivity(intent);
    }


    @Override
    public void onResume() {
        IntentFilter ifilterFsDialog = new IntentFilter();
        ifilterFsDialog.addAction(Constants.FILESYSTEM_SELECT_FOLDER_TAG);
        registerReceiver(fsSelectFolderBroadcastReceiver, ifilterFsDialog);
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceiver(fsSelectFolderBroadcastReceiver);
        super.onPause();
    }
}
