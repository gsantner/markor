package me.writeily.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import me.writeily.R;

/**
 * Created by Minty123 on 2015-01-15.
 */
public class AboutFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_screen);
    }

}
