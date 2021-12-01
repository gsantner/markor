package net.gsantner.markor.activity.openeditor;

import android.content.Intent;
import android.os.Bundle;

import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.MarkorBaseActivity;

/**
 * This Activity exists solely to launch DocumentActivity with the correct intent
 * it is necessary as widget and shortcut intents do not respect MultipleTask etc
 */
public class OpenEditorFromShortcutOrWidgetActivity extends MarkorBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launchDocumentActivityAndFinish(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        launchDocumentActivityAndFinish(intent);
    }

    private void launchDocumentActivityAndFinish(Intent intent) {
        Intent newIntent = new Intent(intent);
        newIntent.setClass(this, DocumentActivity.class);
        DocumentActivity.launch(this, null, null, null, newIntent, null);
        finish();
    }
}