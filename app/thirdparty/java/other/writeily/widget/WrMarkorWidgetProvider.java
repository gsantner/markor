/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2021 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.activity.openeditor.OpenEditorFromShortcutOrWidgetActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;

import java.io.File;

public class WrMarkorWidgetProvider extends AppWidgetProvider {
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

        int requestCode = 0;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (final int appWidgetId : appWidgetIds) {
            // Get the layout for the App Widget and attach an on-click listener to the button
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            final SharedPreferences sharedPreferences = context.getSharedPreferences("" + appWidgetId, Context.MODE_PRIVATE);
            final String directory = sharedPreferences.getString(WIDGET_PATH, AppSettings.get().getNotebookDirectoryAsStr());

            final File directoryF = new File(directory);
            views.setTextViewText(R.id.widget_header_title, directoryF.getName());

            final AppSettings appSettings = new AppSettings(context);

            // ~~~Create new File~~~ Share empty text into markor, easier to access from widget than new file dialog
            final Intent openShare = new Intent(context, OpenEditorFromShortcutOrWidgetActivity.class)
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Document.EXTRA_PATH, directoryF)
                    .putExtra(Document.EXTRA_PATH_IS_FOLDER, true)
                    .putExtra(Intent.EXTRA_TEXT, "");
            views.setOnClickPendingIntent(R.id.widget_new_note, PendingIntent.getActivity(context, requestCode++, openShare, PendingIntent.FLAG_UPDATE_CURRENT));

            // Open Folder
            final Intent goToFolder = new Intent(context, MainActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(Document.EXTRA_PATH, directoryF)
                    .putExtra(Document.EXTRA_PATH_IS_FOLDER, true);
            views.setOnClickPendingIntent(R.id.widget_header, PendingIntent.getActivity(context, requestCode++, goToFolder, PendingIntent.FLAG_UPDATE_CURRENT));

            // Open To-do
            final Intent openTodo = new Intent(context, OpenEditorFromShortcutOrWidgetActivity.class)
                    .setAction(Intent.ACTION_EDIT)
                    .putExtra(Document.EXTRA_PATH, appSettings.getTodoFile())
                    .putExtra(Document.EXTRA_PATH_IS_FOLDER, false);
            views.setOnClickPendingIntent(R.id.widget_todo, PendingIntent.getActivity(context, requestCode++, openTodo, PendingIntent.FLAG_UPDATE_CURRENT));

            // Open QuickNote
            final Intent openQuickNote = new Intent(context, OpenEditorFromShortcutOrWidgetActivity.class)
                    .setAction(Intent.ACTION_EDIT)
                    .putExtra(Document.EXTRA_PATH, appSettings.getQuickNoteFile())
                    .putExtra(Document.EXTRA_PATH_IS_FOLDER, false);
            views.setOnClickPendingIntent(R.id.widget_quicknote, PendingIntent.getActivity(context, requestCode++, openQuickNote, PendingIntent.FLAG_UPDATE_CURRENT));

            // Open Notebook
            final Intent goHome = new Intent(context, MainActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .putExtra(Document.EXTRA_PATH, appSettings.getNotebookDirectory())
                    .putExtra(Document.EXTRA_PATH_IS_FOLDER, true);
            views.setOnClickPendingIntent(R.id.widget_main, PendingIntent.getActivity(context, requestCode++, goHome, PendingIntent.FLAG_UPDATE_CURRENT));

            // ListView
            final Intent notesListIntent = new Intent(context, WrFilesWidgetService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .putExtra(Document.EXTRA_PATH, directoryF)
                    .putExtra(Document.EXTRA_PATH_IS_FOLDER, true);
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
}
