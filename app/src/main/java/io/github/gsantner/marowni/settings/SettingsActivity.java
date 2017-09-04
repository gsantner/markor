package io.github.gsantner.marowni.settings;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import io.github.gsantner.marowni.R;
import io.github.gsantner.marowni.activity.AlphanumericPinActivity;
import io.github.gsantner.marowni.activity.PinActivity;
import io.github.gsantner.marowni.dialog.FilesystemDialog;
import io.github.gsantner.marowni.model.Constants;
import io.github.gsantner.marowni.util.AppSettings;

public class SettingsActivity extends AppCompatActivity implements MarowniSettingsListener {

    SettingsFragment settingsFragment;

    private final BroadcastReceiver fsSelectFolderBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.FILESYSTEM_SELECT_FOLDER_TAG)) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putString(getString(R.string.pref_root_directory), intent.getStringExtra(Constants.FILESYSTEM_FILE_NAME));
                editor.apply();
                settingsFragment.updateRootDirSummary();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = getApplicationContext();
        settingsFragment = new SettingsFragment();
        if (AppSettings.get().isDarkTheme()) {
            setTheme(R.style.AppTheme_Dark);
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

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        MarowniSettingsListener mCallback;
        ListPreference pinPreference;
        Context context;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("app");
            addPreferencesFromResource(R.xml.preferences);

            context = getActivity().getApplicationContext();
            pinPreference = (ListPreference) findPreference(getString(R.string.pref_lock_type_key));
            updateLockSummary();

            // Listen for Pin Preference change
            pinPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String lockType = (String) o;
                    if (lockType == null || lockType.equals("") || getString(R.string.pref_no_lock_value).equals(lockType)) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                        editor.putString(Constants.USER_PIN_KEY, "").apply();
                        editor.putString(getString(R.string.pref_lock_type_key), getString(R.string.pref_no_lock_value)).apply();
                        pinPreference.setSummary(PreferenceManager.getDefaultSharedPreferences(context)
                                .getString(getString(R.string.pref_no_lock_value), getString(R.string.pref_no_lock)));
                        return true;
                    } else if (getString(R.string.pref_pin_lock_value).equals(lockType)) {
                        Intent pinIntent = new Intent(context, PinActivity.class);
                        pinIntent.setAction(Constants.SET_PIN_ACTION);
                        startActivityForResult(pinIntent, Constants.SET_PIN_REQUEST_CODE);
                    } else if (getString(R.string.pref_alpha_pin_lock_value).equals(lockType)) {
                        Intent pinIntent = new Intent(context, AlphanumericPinActivity.class);
                        pinIntent.setAction(Constants.SET_PIN_ACTION);
                        startActivityForResult(pinIntent, Constants.SET_PIN_REQUEST_CODE);
                    }
                    return false;
                }
            });

            // Register PreferenceChangeListener
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            setUpStorageDirPreference();

        }

        private void setUpStorageDirPreference() {
            final Preference rootDir = (Preference) findPreference(getString(R.string.pref_root_directory));
            rootDir.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FragmentManager fragManager = getFragmentManager();

                    Bundle args = new Bundle();
                    args.putString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY, Constants.FILESYSTEM_SELECT_FOLDER_ACCESS_TYPE);
                    FilesystemDialog filesystemDialog = new FilesystemDialog();
                    filesystemDialog.setArguments(args);
                    filesystemDialog.show(fragManager, Constants.FILESYSTEM_SELECT_FOLDER_TAG);
                    return true;
                }
            });
            updateRootDirSummary();
        }

        public void updateRootDirSummary() {
            Preference rootDir = findPreference(getString(R.string.pref_root_directory));
            ;
            rootDir.setSummary(PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.pref_root_directory), Constants.DEFAULT_WRITEILY_STORAGE_FOLDER));
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == Constants.SET_PIN_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    updateLockSummary();
                }
            }
        }

        private void updateLockSummary() {
            Integer currentLockType = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.pref_lock_type_key), "0"));
            pinPreference.setSummary(getResources().getStringArray(R.array.possibleLocksStrings)[currentLockType]);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            AppCompatActivity activity = (AppCompatActivity) mCallback;

            if (activity.getString(R.string.pref_theme_key).equals(key)) {
                mCallback.onThemeChanged();
            }
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            // Make sure the container has implemented the callback interface
            try {
                mCallback = (MarowniSettingsListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + "must implement OnThemeChangedListener");
            }
        }
    }
}

// Needed for callback to container activity
interface MarowniSettingsListener {
    public void onThemeChanged();
}