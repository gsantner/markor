package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * This Activity exists solely to launch DocumentActivity with the correct intent
 * it is necessary as widget and shortcut intents do not respect MultipleTask etc
 */
public class DocumentRelayActivity extends Activity {

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
        DocumentActivity.launch(this, null, null, null, newIntent);
        finish();
    }
}