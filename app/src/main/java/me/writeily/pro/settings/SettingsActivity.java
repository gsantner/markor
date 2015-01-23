package me.writeily.pro.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import me.writeily.pro.R;
import me.writeily.pro.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class SettingsActivity extends ActionBarActivity implements SettingsFragment.WriteilySettingsListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Context context = getApplicationContext();
        String theme = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_theme_key), "");

        if (theme.equals(context.getString(R.string.theme_dark))) {
            setTheme(R.style.AppTheme);
        } else {
            // uses light theme
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
                    .replace(R.id.frame, new SettingsFragment())
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
}
