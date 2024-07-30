package net.gsantner.markor.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;

public class TodoWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.todo_widget_layout);

            final Intent intent = new Intent(context, TodoWidgetService.class);
            views.setRemoteAdapter(R.id.todo_widget_list_view, intent);
            views.setEmptyView(R.id.todo_widget_list_view, R.id.todo_widget_empty_view);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static void updateTodoWidget() {
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
