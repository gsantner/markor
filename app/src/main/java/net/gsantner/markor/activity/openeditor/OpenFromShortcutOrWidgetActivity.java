package net.gsantner.markor.activity.openeditor;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import net.gsantner.markor.activity.DocumentActivity;

/**
 * This Activity exists solely to launch activities with the correct intent
 * it is necessary as widget and shortcut intents do not respect MultipleTask etc
 */
public class OpenFromShortcutOrWidgetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        DocumentActivity.launch(this, getIntent());
    }
}