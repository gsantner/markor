package net.gsantner.markor.activity.LaunchActivity;

import android.content.Intent;
import android.os.Bundle;

import net.gsantner.markor.activity.MarkorBaseActivity;

import java.io.File;

public abstract class OpenSpecialBase extends MarkorBaseActivity {

    public abstract File getSpecialFile();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        OpenerActivity.launch(this, getSpecialFile(), null, null);
    }

    @Override
    public void onNewIntent(final Intent intent) {
        OpenerActivity.launch(this, getSpecialFile(), null, null);
    }
}
