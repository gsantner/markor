/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.DocumentIO;

import java.io.File;

public class MarkorWidgetProvider extends AppWidgetProvider {
    public static final String WIDGET_PATH = "WIDGET_PATH";

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    "" + appWidgetIds[i], Context.MODE_PRIVATE);
            String directory = sharedPreferences.getString(WIDGET_PATH, AppSettings.get().getNotebookDirectoryAsStr());
            Intent newDocumentIntent = new Intent(context, DocumentActivity.class)
                    .putExtra(DocumentIO.EXTRA_PATH, new File(directory))
                    .putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, true);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, newDocumentIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_new_note, pendingIntent);

            Intent goToMain = new Intent(context, MainActivity.class);
            PendingIntent goToMainPendingIntent = PendingIntent.getActivity(context, 0, goToMain, 0);
            views.setOnClickPendingIntent(R.id.widget_header, goToMainPendingIntent);

            // ListView
            Intent notesListIntent = new Intent(context, FilesWidgetService.class);
            notesListIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            notesListIntent.putExtra(DocumentIO.EXTRA_PATH, new File(directory));
            notesListIntent.putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, true);
            notesListIntent.setData(Uri.parse(notesListIntent.toUri(Intent.URI_INTENT_SCHEME)));

            views.setEmptyView(R.id.widget_list_container, R.id.widget_empty_hint);
            views.setRemoteAdapter(R.id.widget_notes_list, notesListIntent);

            Intent openNoteIntent = new Intent(context, DocumentActivity.class).setAction(Intent.ACTION_EDIT);
            PendingIntent openNotePendingIntent = PendingIntent.getActivity(context, 0,
                    openNoteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_notes_list, openNotePendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
