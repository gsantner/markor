package net.gsantner.markor.activity.LaunchActivity;

import android.content.Intent;
import android.os.Bundle;

import net.gsantner.markor.activity.MarkorBaseActivity;

public class LaunchSpecialShareInto extends MarkorBaseActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        LaunchActivity.launch(this, getIntent().setAction(Intent.ACTION_SEND));
    }

    @Override
    public void onNewIntent(final Intent intent) {
        LaunchActivity.launch(this, intent.setAction(Intent.ACTION_SEND));
    }
}
