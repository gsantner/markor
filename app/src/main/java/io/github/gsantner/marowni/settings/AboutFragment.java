package io.github.gsantner.marowni.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import io.github.gsantner.marowni.R;

public class AboutFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_screen);
    }

}
