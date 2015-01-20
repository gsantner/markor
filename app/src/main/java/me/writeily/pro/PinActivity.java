package me.writeily.pro;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import me.writeily.pro.model.Constants;

/**
 * Created by jeff on 2014-08-20.
 */
public class PinActivity extends ActionBarActivity {

    private Context context;
    private String pin;

    private EditText pin1;
    private EditText pin2;
    private EditText pin3;
    private EditText pin4;

    private boolean isSettingUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get the Intent (to check if coming from Settings)
        String action = getIntent().getAction();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_nonelevated);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Get the pin a user may have set
        pin = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.USER_PIN_KEY, "");

        if (action.equalsIgnoreCase(Constants.SET_PIN_ACTION)) {
            isSettingUp = true;
        } else {
            checkIfPinRequired();
        }

        setContentView(R.layout.activity_pin);
        context = getApplicationContext();

        // Find pin EditTexts
        pin1 = (EditText) findViewById(R.id.pin1);
        pin2 = (EditText) findViewById(R.id.pin2);
        pin3 = (EditText) findViewById(R.id.pin3);
        pin4 = (EditText) findViewById(R.id.pin4);

        attachPinListeners();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (isSettingUp) {
            setResult(RESULT_CANCELED);
        }

        super.onBackPressed();
    }

    private void attachPinListeners() {
        // Pin 1
        pin1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                checkPin();

                if (!editable.toString().isEmpty()) {
                    pin2.requestFocus();
                }
            }
        });

        // Pin 2
        pin2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                checkPin();

                if (!editable.toString().isEmpty()) {
                    pin3.requestFocus();
                }
            }
        });

        // Pin 3
        pin3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void afterTextChanged(Editable editable) {
                checkPin();

                if (!editable.toString().isEmpty()) {
                    pin4.requestFocus();
                }
            }
        });

        // Pin 4
        pin4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkPin();
            }
        });
    }

    /**
     * Checks and validates the pin that was entered with the pin in the SharedPreferences.
     * This process runs every time one of the four pin EditText fields is modified.
     * Once the entered pin matches the pin the user set, then redirect to the main activity.
     */
    private void checkPin() {
        String enteredPin = pin1.getText().toString() + pin2.getText().toString() + pin3.getText().toString() + pin4.getText().toString();

        if (isSettingUp) {
            // Check if the pin has 4 digits
            if (enteredPin.length() == 4) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString(Constants.USER_PIN_KEY, enteredPin).apply();

                setResult(RESULT_OK);
                finish();
            }
        } else {
            // Check if we can unlock the app
            if (enteredPin.equalsIgnoreCase(pin)) {
                startMain();
            } else {
                if (enteredPin.length() == 4) {
                    Toast.makeText(context, getString(R.string.incorrect_pin_text), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Checks if the user enabled pin protection. If they didn't, redirect to the Main Activity.
     */
    private void checkIfPinRequired() {
        boolean pinRequired = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_pin_key), false);

        if (!pinRequired) {
            startMain();
        } else {
            // Check if user set up a pin but didn't enter one, in this case, set pin required to false
            if (pin.length() < 4) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putBoolean(getString(R.string.pref_pin_key), false).apply();
                startMain();
            }
        }
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
