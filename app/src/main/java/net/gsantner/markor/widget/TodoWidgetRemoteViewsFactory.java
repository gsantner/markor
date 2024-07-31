package net.gsantner.markor.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TodoWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context _context;
    private final AppSettings _appSettings;
    private final List<String> _lines;

    public TodoWidgetRemoteViewsFactory(Context context, Intent intent) {
        _context = context;
        _appSettings = ApplicationObject.settings();
        _lines = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        _lines.clear();
        File todoFile = _appSettings.getTodoFile();
        _lines.addAll(readFileContent(todoFile));
    }

    @Override
    public void onDestroy() {
        _lines.clear();
    }

    @Override
    public int getCount() {
        return _lines.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(_context.getPackageName(), R.layout.todo_widget_list_item);
        views.setTextViewText(R.id.todo_widget_item_text, _lines.get(position));
        views.setInt(R.id.todo_widget_item_text, "setTextColor", _appSettings.getEditorForegroundColor());

        final Intent fillInIntent = new Intent();
        views.setOnClickFillInIntent(R.id.todo_widget_item_text, fillInIntent);

        return views;
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

    private List<String> readFileContent(File file) {
        List<String> content = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            content.add("Error reading file");
        }
        return content;
    }
}
