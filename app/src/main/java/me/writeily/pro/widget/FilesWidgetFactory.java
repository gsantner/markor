package me.writeily.pro.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.File;
import java.io.FileFilter;

import me.writeily.pro.R;
import me.writeily.pro.model.Constants;

/**
 * Created by jeff on 15-04-21.
 */
public class FilesWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private File[] widgetFilesList;
    private int appWidgetId;

    public FilesWidgetFactory(Context context, Intent intent) {
        this.context = context;
        widgetFilesList = new File[0];
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    private void updateFiles() {
        File dir = new File(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_root_directory),Constants.DEFAULT_WRITEILY_STORAGE_FOLDER));
        this.widgetFilesList = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isDirectory();
            }
        });
    }

    @Override
    public void onCreate() {
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        updateFiles();
    }

    @Override
    public void onDestroy() {
        widgetFilesList = new File[0];
    }

    @Override
    public int getCount() {
        return widgetFilesList.length;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        File file = widgetFilesList[position];
        Intent fillInIntent = new Intent().putExtra(Constants.NOTE_KEY,file);
        RemoteViews rowView = new RemoteViews(context.getPackageName(), R.layout.widget_file_item);
        rowView.setTextViewText(R.id.widget_note_title, Constants.MD_EXTENSION.matcher(file.getName()).replaceAll(""));
        rowView.setOnClickFillInIntent(R.id.widget_note_title, fillInIntent);
        return rowView;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
