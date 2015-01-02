package me.writeily.writeilypro.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import me.writeily.writeilypro.PinActivity;
import me.writeily.writeilypro.R;
import me.writeily.writeilypro.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class SettingsFragment extends PreferenceFragment {

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
}
