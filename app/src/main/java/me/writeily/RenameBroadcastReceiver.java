package me.writeily;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.File;

import me.writeily.model.Constants;

class RenameBroadcastReceiver extends BroadcastReceiver {

    private NotesFragment notesFragment;

    public RenameBroadcastReceiver(NotesFragment notesFragment) {
        super();
        this.notesFragment = notesFragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.RENAME_DIALOG_TAG)) {
            String newName = intent.getStringExtra(Constants.RENAME_NEW_NAME);
            File sourceFile = new File(intent.getStringExtra(Constants.SOURCE_FILE));
            File targetFile = new File(sourceFile.getParent(), newName);

            if (targetFile.exists()) {
                Toast.makeText(context, context.getString(R.string.rename_error_target_already_exists), Toast.LENGTH_LONG).show();
                notesFragment.finishActionMode();
                return;
            }

            if (sourceFile.renameTo(targetFile)) {
                Toast.makeText(context, context.getString(R.string.rename_success), Toast.LENGTH_LONG).show();
                notesFragment.listFilesInDirectory(notesFragment.getCurrentDir());
            } else {
                Toast.makeText(context, context.getString(R.string.rename_fail), Toast.LENGTH_LONG).show();
            }
            notesFragment.finishActionMode();
        }
    }
}
