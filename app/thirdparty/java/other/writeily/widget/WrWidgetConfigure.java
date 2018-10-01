/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.ui.FilesystemDialogData;

import java.io.File;

public class WrWidgetConfigure extends AppCompatActivity {
    private int _appWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            _appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        showSelectionDialog();
    }

    private void showSelectionDialog() {
        PermissionChecker permc = new PermissionChecker(this);
        if (permc.mkdirIfStoragePermissionGranted()) {
            FragmentManager fragManager = getSupportFragmentManager();
            FilesystemDialogCreator.showFolderDialog(new FilesystemDialogData.SelectionListenerAdapter() {
                @Override
                public void onFsSelected(String request, File file) {
                    complete(file.getAbsolutePath());
                }

                @Override
                public void onFsNothingSelected(String request) {
                    finish();
                }

                @Override
                public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                    opt.titleText = R.string.select_folder;
                    opt.rootFolder = AppSettings.get().getNotebookDirectory();
                }
            }, fragManager, this);
        }

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (new PermissionChecker(this).checkPermissionResult(requestCode, permissions, grantResults)) {
            showSelectionDialog();
        } else {
            finish();
        }
    }

    public final void complete(String directory) {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            _appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            Context context = getApplicationContext();

            // Store widget filter
            SharedPreferences preferences = context.getSharedPreferences("" + _appWidgetId, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(WrMarkorWidgetProvider.WIDGET_PATH, directory);
            editor.apply();

            Intent resultValue = new Intent(context, WrFilesWidgetService.class);
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, _appWidgetId);
            setResult(RESULT_OK, resultValue);

            Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, WrMarkorWidgetProvider.class);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{_appWidgetId});
            sendBroadcast(i);

            finish();
        }
    }
}
