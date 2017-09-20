/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.gsantner.markor.model.Constants;

public class WidgetConfigure extends AppCompatActivity {

    int mAppWidgetId;

    final BroadcastReceiver fsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.FILESYSTEM_SELECT_FOLDER_TAG)) {
                String directory = intent.getStringExtra(Constants.EXTRA_FILEPATH);
                complete(directory);

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        android.support.v4.app.FragmentManager fragManager = getSupportFragmentManager();

        IntentFilter ifilterSwitchedFolderFilder = new IntentFilter();
        ifilterSwitchedFolderFilder.addAction(Constants.FILESYSTEM_SELECT_FOLDER_TAG);
        registerReceiver(fsBroadcastReceiver, ifilterSwitchedFolderFilder);
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

            Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MarkorWidgetProvider.class);
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
