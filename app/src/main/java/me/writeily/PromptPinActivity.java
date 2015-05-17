package me.writeily;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by jeff on 2014-08-20.
 */
public class PromptPinActivity extends ActionBarActivity {

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
        String[] stringArray = getResources().getStringArray(R.array.possibleLocksValues);
        String lockType = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_lock_type_key), "");
        if (lockType == null || lockType.equals("") || stringArray[0].equals(lockType)) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
            this.finish();
        } else if (stringArray[1].equals(lockType)) {
            Intent pinIntent = new Intent(this, PinActivity.class);
            startActivity(pinIntent);
            this.finish();
        } else if (stringArray[2].equals(lockType)) {
            Intent pinIntent = new Intent(this, AlphanumericPinActivity.class);
            startActivity(pinIntent);
            this.finish();
        }
    }
}
