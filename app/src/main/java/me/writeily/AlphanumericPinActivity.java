package me.writeily;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import me.writeily.model.Constants;

/**
 * Created by jeff on 2014-08-20.
 */
public class AlphanumericPinActivity extends ActionBarActivity {

    private Context context;
    private String pin;

    private EditText pinView;

    private boolean isSettingUp = false;

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
        pin = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.USER_PIN_KEY, "");

        if (Constants.SET_PIN_ACTION.equalsIgnoreCase(action)) {
            isSettingUp = true;
        }

        setContentView(R.layout.activity_alphanumeric_pin);
        context = getApplicationContext();

        // Find pin EditTexts
        pinView = (EditText) findViewById(R.id.passcode);
        pinView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                checkPin();
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isSettingUp) {
            setResult(RESULT_CANCELED);
        }

        super.onBackPressed();
    }

    /**
     * Checks and validates the pin that was entered with the pin in the SharedPreferences.
     * This process runs every time one of the four pin EditText fields is modified.
     * Once the entered pin matches the pin the user set, then redirect to the main activity.
     */
    private void checkPin() {
        String enteredPin = pinView.getText().toString();

        if (isSettingUp) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString(getString(R.string.pref_lock_type_key), getString(R.string.pref_alpha_pin_lock_value)).apply();
            editor.putString(Constants.USER_PIN_KEY, enteredPin).apply();

            setResult(RESULT_OK);
            finish();
        } else {
            // Check if we can unlock the app
            if (enteredPin.equalsIgnoreCase(pin)) {
                startMain();
            } else {
                Toast.makeText(context, getString(R.string.incorrect_pin_text), Toast.LENGTH_SHORT).show();
                resetPin();
            }
        }
    }

    private void resetPin() {
        pinView.setText("");
        pinView.requestFocus();
    }

    /**
     * Start the main activity.
     */
    private void startMain() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        this.finish();
        startActivity(mainIntent);
    }
}
