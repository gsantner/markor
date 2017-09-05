package net.gsantner.markor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;

public class PromptPinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the Intent (to check if coming from Settings)
        String action = getIntent().getAction();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_nonelevated);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Get the pin a user may have set
        int lockType = AppSettings.get().getLockType();
        if (lockType == Integer.valueOf(getString(R.string.pref_value__lock__none))) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
            this.finish();
        } else if (lockType == Integer.valueOf(getString(R.string.pref_value__lock__pin))) {
            Intent pinIntent = new Intent(this, PinActivity.class);
            startActivity(pinIntent);
            this.finish();
        } else if (lockType == Integer.valueOf(getString(R.string.pref_value__lock__password))) {
            Intent pinIntent = new Intent(this, AlphanumericPinActivity.class);
            startActivity(pinIntent);
            this.finish();
        }
    }
}
