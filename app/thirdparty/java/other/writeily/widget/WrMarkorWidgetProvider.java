/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2019 Gregor Santner
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
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.DocumentIO;

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
        // Handling widget color scheme
        handleWidgetScheme(
                context,
                new RemoteViews(context.getPackageName(), R.layout.widget_layout),
                new AppSettings(context).isDarkThemeEnabled());

        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    "" + appWidgetId, Context.MODE_PRIVATE);
            String directory = sharedPreferences.getString(WIDGET_PATH, AppSettings.get().getNotebookDirectoryAsStr());

            File directoryF = new File(directory);
            views.setTextViewText(R.id.widget_header_title, directoryF.getName());

            // ~~~Create new File~~~ Share empty text into markor, easier to access from widget than new file dialog
            Intent newDocumentIntent = new Intent(context, DocumentActivity.class)
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, "");
            views.setOnClickPendingIntent(R.id.widget_new_note, PendingIntent.getActivity(context, 0, newDocumentIntent, 0));

            // Open Markor
            Intent goToMain = new Intent(context, MainActivity.class);
            views.setOnClickPendingIntent(R.id.widget_header, PendingIntent.getActivity(context, 0, goToMain, 0));


            // Open To-do
            AppSettings appSettings = new AppSettings(context);
            Intent openTodo = new Intent(context, DocumentActivity.class)
                    .setAction(Intent.ACTION_CALL_BUTTON)
                    .putExtra(DocumentIO.EXTRA_PATH, appSettings.getTodoFile())
                    .putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
            views.setOnClickPendingIntent(R.id.widget_todo, PendingIntent.getActivity(context, 0, openTodo, 0));

            // Open QuickNote
            Intent openQuickNote = new Intent(context, DocumentActivity.class)
                    .setAction(Intent.ACTION_ANSWER)
                    .putExtra(DocumentIO.EXTRA_PATH, appSettings.getQuickNoteFile())
                    .putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
            views.setOnClickPendingIntent(R.id.widget_quicknote, PendingIntent.getActivity(context, 0, openQuickNote, 0));

            // Open LinkBox
            Intent openLinkBox = new Intent(context, DocumentActivity.class)
                    .setAction(Intent.ACTION_NEW_OUTGOING_CALL)
                    .putExtra(DocumentIO.EXTRA_PATH, appSettings.getTodoFile())
                    .putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
            views.setOnClickPendingIntent(R.id.widget_linkbox, PendingIntent.getActivity(context, 0, openLinkBox, 0));


            // ListView
            Intent notesListIntent = new Intent(context, WrFilesWidgetService.class);
            notesListIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            notesListIntent.putExtra(DocumentIO.EXTRA_PATH, directoryF);
            notesListIntent.putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, true);
            notesListIntent.setData(Uri.parse(notesListIntent.toUri(Intent.URI_INTENT_SCHEME)));

            views.setEmptyView(R.id.widget_list_container, R.id.widget_empty_hint);
            views.setRemoteAdapter(R.id.widget_notes_list, notesListIntent);

            Intent openNoteIntent = new Intent(context, DocumentActivity.class);
            PendingIntent openNotePendingIntent = PendingIntent.getActivity(context, 0,
                    openNoteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.widget_notes_list, openNotePendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static void handleWidgetScheme(Context context, RemoteViews remoteViews, Boolean enabled) {
        if (!enabled) {
            remoteViews.setInt(R.id.widget_notes_list, "setBackgroundColor", context.getResources().getColor(R.color.dark__background));
            remoteViews.setTextColor(R.id.widget_note_title, context.getResources().getColor(R.color.dark__primary_text));
        } else {
            remoteViews.setInt(R.id.widget_notes_list, "setBackgroundColor", context.getResources().getColor(R.color.light__background));
            remoteViews.setTextColor(R.id.widget_note_title, context.getResources().getColor(R.color.light__primary_text));
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(new ComponentName(context.getPackageName(), WrMarkorWidgetProvider.class.getName()), remoteViews);
    }
}
