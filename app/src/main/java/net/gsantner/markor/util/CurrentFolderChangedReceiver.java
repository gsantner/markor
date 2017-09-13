package net.gsantner.markor.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;

import java.io.File;

public class CurrentFolderChangedReceiver extends BroadcastReceiver {

    private MainActivity context;

    public CurrentFolderChangedReceiver(MainActivity activity) {
        this.context = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(AppCast.CURRENT_FOLDER_CHANGED.ACTION)) {
            File directory = new File(intent.getStringExtra(AppCast.CURRENT_FOLDER_CHANGED.EXTRA_PATH));
            File rootDir = new File(intent.getStringExtra(AppCast.CURRENT_FOLDER_CHANGED.EXTRA_ROOT_FOLDERPATH));
            toggleBreadcrumbsVisibility(directory, rootDir);
        }
    }

    private void toggleBreadcrumbsVisibility(File currentDir, File rootDir) {
        TextView breadcrumbs = context.findViewById(R.id.main__activity__breadcrumbs);
        if (currentDir.equals(rootDir)) {
            breadcrumbs.setVisibility(View.GONE);
        } else {
            breadcrumbs.setText(backButtonText(currentDir, rootDir));
            breadcrumbs.setVisibility(View.VISIBLE);
        }

    }

    private String backButtonText(File currentDir, File rootDir) {
        if (currentDir.getParentFile().equals(rootDir)) {
            return " > " + currentDir.getName();
        } else {
            return "... > " + currentDir.getParentFile().getName() + " > " + currentDir.getName();
        }
    }
}
