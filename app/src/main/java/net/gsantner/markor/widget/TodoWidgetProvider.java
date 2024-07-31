package net.gsantner.markor.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.openeditor.OpenFromShortcutOrWidgetActivity;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;

public class TodoWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        int requestCode = 1;
        final AppSettings appSettings = ApplicationObject.settings();

        final int staticFlags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
        final int mutableFlags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);

        for (int appWidgetId : appWidgetIds) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.todo_widget_layout);

            final Intent intent = new Intent(context, TodoWidgetService.class);
            views.setRemoteAdapter(R.id.todo_widget_list_view, intent);
            views.setEmptyView(R.id.todo_widget_list_view, R.id.todo_widget_empty_view);
            views.setInt(R.id.todo_widget_list_view, "setBackgroundColor", appSettings.getEditorBackgroundColor());

            final Intent openTodo = new Intent(context, OpenFromShortcutOrWidgetActivity.class)
                    .setAction(Intent.ACTION_EDIT)
                    .putExtra(Document.EXTRA_FILE, appSettings.getTodoFile());
            views.setPendingIntentTemplate(R.id.todo_widget_list_view, PendingIntent.getActivity(context, requestCode++, openTodo, mutableFlags));
            views.setOnClickPendingIntent(R.id.todo_widget_container, PendingIntent.getActivity(context, requestCode++, openTodo, staticFlags));

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    // Update all widget lists and shortcuts for all widgets
    public static void updateTodoWidgets() {
        final Context context = ApplicationObject.get().getApplicationContext();
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager == null) {
            // The device does not support widgets.
            return;
        }
        final ComponentName comp = new ComponentName(context, TodoWidgetProvider.class);
        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(comp);

        // Update List
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.todo_widget_list_view);

        // Trigger remote views update
        context.sendBroadcast(new Intent(context, TodoWidgetProvider.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds));
    }
}
