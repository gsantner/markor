package net.gsantner.markor.activity.openeditor;

import android.content.Intent;
import android.os.Bundle;

import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.MarkorBaseActivity;
import net.gsantner.markor.util.MarkorContextUtils;

import java.io.File;

/**
 * This Activity exists solely to launch DocumentActivity with the correct intent
 * it is necessary as widget and shortcut intents do not respect MultipleTask etc
 */
public class OpenFromShortcutOrWidgetActivity extends MarkorBaseActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launchActivityAndFinish(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        launchActivityAndFinish(intent);
    }

    private void launchActivityAndFinish(Intent intent) {
        final File file = MarkorContextUtils.getIntentFile(intent, null);
        if (file != null) {
            DocumentActivity.launch(this, file, null, null);
        }
        finish();
    }
}