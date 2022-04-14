/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2022 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import net.gsantner.markor.App;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.activity.openeditor.OpenEditorFromShortcutOrWidgetActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;

public class WrMarkorWidgetProvider extends AppWidgetProvider {

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        int requestCode = 1;
        final AppSettings appSettings = new AppSettings(context);

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (final int appWidgetId : appWidgetIds) {

            final File directoryF = WrWidgetConfigure.getWidgetDirectory(context, appWidgetId);

            // Get the layout for the App Widget and attach an on-click listener to the button
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            views.setTextViewText(R.id.widget_header_title, FileUtils.extractTitle(directoryF));

            // ~~~Create new File~~~ Share empty text into markor, easier to access from widget than new file dialog
            final Intent openShare = new Intent(context, OpenEditorFromShortcutOrWidgetActivity.class)
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Document.EXTRA_PATH, directoryF)
                    .putExtra(Intent.EXTRA_TEXT, "");
            views.setOnClickPendingIntent(R.id.widget_new_note, PendingIntent.getActivity(context, requestCode++, openShare, PendingIntent.FLAG_UPDATE_CURRENT));

            // Open Folder
            final Intent goToFolder = new Intent(context, MainActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(Document.EXTRA_PATH, directoryF);
            views.setOnClickPendingIntent(R.id.widget_header, PendingIntent.getActivity(context, requestCode++, goToFolder, PendingIntent.FLAG_UPDATE_CURRENT));

            // Open To-do
            final Intent openTodo = new Intent(context, OpenEditorFromShortcutOrWidgetActivity.class)
                    .setAction(Intent.ACTION_EDIT)
                    .putExtra(Document.EXTRA_PATH, appSettings.getTodoFile());
            views.setOnClickPendingIntent(R.id.widget_todo, PendingIntent.getActivity(context, requestCode++, openTodo, PendingIntent.FLAG_UPDATE_CURRENT));

            // Open QuickNote
            final Intent openQuickNote = new Intent(context, OpenEditorFromShortcutOrWidgetActivity.class)
                    .setAction(Intent.ACTION_EDIT)
                    .putExtra(Document.EXTRA_PATH, appSettings.getQuickNoteFile());
            views.setOnClickPendingIntent(R.id.widget_quicknote, PendingIntent.getActivity(context, requestCode++, openQuickNote, PendingIntent.FLAG_UPDATE_CURRENT));

            // Open Notebook
            final Intent goHome = new Intent(context, MainActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(Document.EXTRA_PATH, appSettings.getNotebookDirectory());
            views.setOnClickPendingIntent(R.id.widget_main, PendingIntent.getActivity(context, requestCode++, goHome, PendingIntent.FLAG_UPDATE_CURRENT));

            // ListView
            final Intent notesListIntent = new Intent(context, WrFilesWidgetService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .putExtra(Document.EXTRA_PATH, directoryF);
            notesListIntent.setData(Uri.parse(notesListIntent.toUri(Intent.URI_INTENT_SCHEME)));

            views.setEmptyView(R.id.widget_list_container, R.id.widget_empty_hint);
            views.setRemoteAdapter(R.id.widget_notes_list, notesListIntent);

            final Intent openNoteIntent = new Intent(context, OpenEditorFromShortcutOrWidgetActivity.class);
            final PendingIntent openNotePendingIntent = PendingIntent.getActivity(context, requestCode++, openNoteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_notes_list, openNotePendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    // Update all widget lists and shortcuts for all widgets
    public static void updateLauncherWidgets() {
        final Context context = App.get().getApplicationContext();
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final ComponentName comp = new ComponentName(context, WrMarkorWidgetProvider.class);
        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(comp);

        // Update List
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_notes_list);

        // Trigger remote views update
        context.sendBroadcast(new Intent(context, WrMarkorWidgetProvider.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
    }
}
