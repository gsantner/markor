package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import net.gsantner.markor.R;
import net.gsantner.markor.dialog.FilesystemDialog;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;

public class SettingsActivity extends AppCompatActivity implements MarkorSettingsListener {

    SettingsFragment settingsFragment;

    private final BroadcastReceiver fsSelectFolderBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.FILESYSTEM_SELECT_FOLDER_TAG)) {
                AppSettings.get().setSaveDirectory(intent.getStringExtra(Constants.EXTRA_FILEPATH));
                settingsFragment.updateSummaries();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = getApplicationContext();
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        settingsFragment = new SettingsFragment();
        if (AppSettings.get().isDarkThemeEnabled()) {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings__activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Show main settings page or about screen?
        boolean showAbout = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_SHOW_ABOUT, false);

        if (!showAbout) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.main__activity__fragment_placeholder, settingsFragment)
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
        MarkorSettingsListener mCallback;
        ListPreference pinPreference;
        Context context;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("app");
            addPreferencesFromResource(R.xml.preferences);

            context = getActivity().getApplicationContext();
            pinPreference = (ListPreference) findPreference(getString(R.string.pref_key__lock_type));

            // Listen for Pin Preference change
            pinPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    int lockType = Integer.valueOf((String) o);
                    if (lockType == Integer.valueOf(getString(R.string.pref_value__lock__none))) {
                        AppSettings appSettings = AppSettings.get();
                        appSettings.setLockAuthPinOrPassword("");
                        appSettings.setLockType(lockType);
                        pinPreference.setSummary(getString(R.string.lock_type__none));
                        return true;
                    } else if (lockType == Integer.valueOf(getString(R.string.pref_value__lock__pin))) {
                        Intent pinIntent = new Intent(context, PinActivity.class);
                        pinIntent.setAction(Constants.SET_PIN_ACTION);
                        startActivityForResult(pinIntent, Constants.SET_PIN_REQUEST_CODE);
                    } else if (lockType == Integer.valueOf(getString(R.string.pref_value__lock__password))) {
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
            final Preference rootDir = findPreference(getString(R.string.pref_key__save_directory));
            rootDir.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (PermissionChecker.doIfPermissionGranted(getActivity()) && PermissionChecker.mkSaveDir(getActivity())) {
                        FragmentManager fragManager = ((AppCompatActivity)getActivity()).getSupportFragmentManager();

                        Bundle args = new Bundle();
                        args.putString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY, Constants.FILESYSTEM_SELECT_FOLDER_ACCESS_TYPE);
                        FilesystemDialog filesystemDialog = new FilesystemDialog();
                        filesystemDialog.setArguments(args);
                        filesystemDialog.show(fragManager, Constants.FILESYSTEM_SELECT_FOLDER_TAG);
                    }
                    return true;
                }
            });
            updateSummaries();
        }

        public void updateSummaries() {
            Preference pref = findPreference(getString(R.string.pref_key__save_directory));
            pref.setSummary(AppSettings.get().getSaveDirectory());

            int currentLockType = AppSettings.get().getLockType();
            pinPreference.setSummary(getResources().getStringArray(R.array.pref_arrdisp__lock_type)[currentLockType]);

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == Constants.SET_PIN_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    updateSummaries();
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            AppCompatActivity activity = (AppCompatActivity) mCallback;

            if (activity.getString(R.string.pref_key__app_theme).equals(key)) {
                mCallback.onThemeChanged();
            }
            if (activity.getString(R.string.pref_key__language).equals(key)) {
                AppSettings.get().setRecreateMainRequired(true);
            }
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            // Make sure the container has implemented the callback interface
            try {
                mCallback = (MarkorSettingsListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + "must implement OnThemeChangedListener");
            }
        }
    }
}

// Needed for callback to container activity
interface MarkorSettingsListener {
    public void onThemeChanged();
}