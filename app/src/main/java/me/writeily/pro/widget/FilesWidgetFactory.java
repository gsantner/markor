package me.writeily.pro.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.File;
import java.util.ArrayList;

import me.writeily.pro.R;
import me.writeily.pro.adapter.NotesAdapter;
import me.writeily.pro.model.Constants;
import me.writeily.pro.model.WriteilySingleton;

/**
 * Created by jeff on 15-04-21.
 */
public class FilesWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private ArrayList<File> widgetFilesList;
    private int appWidgetId;

    public FilesWidgetFactory(Context context, Intent intent) {
        this.context = context;
        widgetFilesList = new ArrayList<File>();
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    private void updateFiles() {
        ArrayList<File> newFilesList = new ArrayList<File>();
        File dir = new File(PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_root_directory),Constants.DEFAULT_WRITEILY_STORAGE_FOLDER));

        try {
            // Load from SD card
            newFilesList = WriteilySingleton.getInstance().addFilesFromDirectory(dir, newFilesList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d("WOQEUWQPOIEWQ", newFilesList.toString());

        this.widgetFilesList = newFilesList;
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
        widgetFilesList.clear();
    }

    @Override
    public int getCount() {
        return widgetFilesList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rowView = new RemoteViews(context.getPackageName(), R.layout.file_item);
        rowView.setTextViewText(R.id.note_title, widgetFilesList.get(position).getName());

        Intent intent = new Intent();
        rowView.setOnClickFillInIntent(android.R.id.text1, intent);

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
