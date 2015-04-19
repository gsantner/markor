package me.writeily.pro.settings;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

import me.writeily.pro.PinActivity;
import me.writeily.pro.R;
import me.writeily.pro.dialog.FilesystemDialog;
import me.writeily.pro.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    WriteilySettingsListener mCallback;
    CheckBoxPreference pinPreference;
    Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        context = getActivity().getApplicationContext();
        pinPreference = (CheckBoxPreference) findPreference(getString(R.string.pref_pin_key));

        // Listen for Pin Preference change
        pinPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (((Boolean) o).booleanValue()) {
                    Intent pinIntent = new Intent(context, PinActivity.class);
                    pinIntent.setAction(Constants.SET_PIN_ACTION);
                    startActivityForResult(pinIntent, Constants.SET_PIN_REQUEST_CODE);
                } else {
                    // Reset pin
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    editor.putString(Constants.USER_PIN_KEY, "").apply();
                }
                return true;
            }
        });

        // Register PreferenceChangeListener
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        // Workaround for About-Screen
        Preference aboutScreen = (Preference) findPreference(getString(R.string.pref_about_key));
        aboutScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mCallback == null) {
                    return false;
                }

                mCallback.onAboutClicked();
                return true;
            }
        });

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
        Preference rootDir = findPreference(getString(R.string.pref_root_directory));;
        rootDir.setSummary(PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.pref_root_directory), Constants.DEFAULT_WRITEILY_STORAGE_FOLDER));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.SET_PIN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                pinPreference.setChecked(true);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                pinPreference.setChecked(false);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        ActionBarActivity activity = (ActionBarActivity) mCallback;

        if (activity.getString(R.string.pref_theme_key).equals(key)) {
            mCallback.onThemeChanged();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure the container has implemented the callback interface
        try {
            mCallback = (WriteilySettingsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + "must implement OnThemeChangedListener");
        }
    }

    // Needed for callback to container activity
    public interface WriteilySettingsListener {
        public void onThemeChanged();

        public void onAboutClicked();
    }
}
