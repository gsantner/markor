package me.writeily.widget;

import android.app.Activity;
import android.app.FragmentManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import me.writeily.dialog.FilesystemDialog;
import me.writeily.model.Constants;

public class WidgetConfigure extends Activity {

    int mAppWidgetId;

    final BroadcastReceiver fsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.FILESYSTEM_SELECT_FOLDER_TAG)) {
                String directory = intent.getStringExtra(Constants.FILESYSTEM_FILE_NAME);
                complete(directory);

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        FragmentManager fragManager = getFragmentManager();

        IntentFilter ifilterSwitchedFolderFilder = new IntentFilter();
        ifilterSwitchedFolderFilder.addAction(Constants.FILESYSTEM_SELECT_FOLDER_TAG);
        registerReceiver(fsBroadcastReceiver, ifilterSwitchedFolderFilder);

        Bundle args = new Bundle();
        args.putString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY, Constants.FILESYSTEM_SELECT_FOLDER_FOR_WIDGET_ACCESS_TYPE);
        FilesystemDialog filesystemDialog = new FilesystemDialog();
        filesystemDialog.setArguments(args);
        filesystemDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        filesystemDialog.show(fragManager, Constants.FILESYSTEM_SELECT_FOLDER_TAG);

        super.onCreate(savedInstanceState);

    }

    public final void complete(String directory) {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            Context context = getApplicationContext();

            // Store widget filter
            SharedPreferences preferences = context.getSharedPreferences("" + mAppWidgetId, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Constants.WIDGET_PATH, directory);
            editor.apply();

            Intent resultValue = new Intent(context, FilesWidgetService.class);
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);

            Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, WriteilyWidgetProvider.class);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId});
            sendBroadcast(i);

            finish();
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(fsBroadcastReceiver);
        super.onPause();
    }
}
