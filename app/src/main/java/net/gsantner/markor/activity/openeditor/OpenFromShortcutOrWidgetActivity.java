package net.gsantner.markor.activity.openeditor;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.MarkorBaseActivity;

/**
 * This Activity exists solely to launch DocumentActivity with the correct intent
 * it is necessary as widget and shortcut intents do not respect MultipleTask etc
 */
public class OpenFromShortcutOrWidgetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launchActivityAndFinish(getIntent());
    }

    private void launchActivityAndFinish(Intent intent) {
        finish();
        DocumentActivity.launch(this, intent);
    }
}