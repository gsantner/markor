package net.gsantner.markor.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;

public class PinActivity extends AppCompatActivity {
    private String pin;

    private EditText pin1;
    private EditText pin2;
    private EditText pin3;
    private EditText pin4;

    private boolean isSettingUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
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

        setContentView(R.layout.pin__activity);

        // Find pin EditTexts
        pin1 = (EditText) findViewById(R.id.pin1);
        pin2 = (EditText) findViewById(R.id.pin2);
        pin3 = (EditText) findViewById(R.id.pin3);
        pin4 = (EditText) findViewById(R.id.pin4);

        attachPinListeners();
        attachPinKeyListeners();
    }

    @Override
    public void onBackPressed() {
        if (isSettingUp) {
            setResult(RESULT_CANCELED);
        }

        super.onBackPressed();
    }

    private void attachPinKeyListeners() {
        pin1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    pin1.setText("");
                }
            }
        });

        pin2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    pin2.setText("");
                }
            }
        });

        pin3.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    pin3.setText("");
                }
            }
        });

        pin4.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    pin4.setText("");
                }
            }
        });
    }

    private void attachPinListeners() {
        // Pin 1
        pin1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

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
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

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
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

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
                AppSettings appSettings = AppSettings.get();
                appSettings.setLockType(getString(R.string.pref_value__lock__pin));
                appSettings.setLockAuthPinOrPassword(enteredPin);
                setResult(RESULT_OK);
                finish();
            }
        } else {
            // Check if we can unlock the app
            if (enteredPin.equalsIgnoreCase(pin)) {
                startMain();
            } else {
                if (enteredPin.length() == 4) {
                    Toast.makeText(this, getString(R.string.incorrect_pin_text), Toast.LENGTH_SHORT).show();
                    resetPin();
                }
            }
        }
    }

    private void resetPin() {
        pin1.setText("");
        pin2.setText("");
        pin3.setText("");
        pin4.setText("");
        pin1.requestFocus();
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
