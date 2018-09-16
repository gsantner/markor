package net.gsantner.markor.activity.openeditor;

import android.os.Bundle;
import android.support.annotation.Nullable;

import net.gsantner.markor.util.AppSettings;

public class OpenEditorQuickNoteActivity extends OpenEditorActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openEditorForFile(new AppSettings(getApplicationContext()).getQuickNoteFile());
    }
}
