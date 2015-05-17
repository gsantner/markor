package me.writeily.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.File;
import java.io.FileFilter;

import me.writeily.R;
import me.writeily.model.Constants;

public class FilesWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private File[] widgetFilesList = new File[0];
    private int appWidgetId;
    private File dir;

    public FilesWidgetFactory(Context context, Intent intent) {

        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        this.dir = new File(intent.getStringExtra(Constants.FOLDER_NAME));
    }

    @Override
    public void onCreate() {
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        updateFiles();
    }

    private void updateFiles() {
        this.widgetFilesList = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isDirectory();
            }
        });
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
