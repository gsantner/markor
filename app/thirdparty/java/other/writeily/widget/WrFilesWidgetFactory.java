/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2024 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserListAdapter;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WrFilesWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context _context;
    private final List<File> _widgetFilesList;
    private final int _appWidgetId;

    public WrFilesWidgetFactory(final Context context, final Intent intent) {
        _context = context;
        _appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        _widgetFilesList = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {

        _widgetFilesList.clear();
        final File dir = WrWidgetConfigure.getWidgetDirectory(_context, _appWidgetId);
        final AppSettings as = ApplicationObject.settings();

        if (dir.equals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_RECENTS)) {
            _widgetFilesList.addAll(ApplicationObject.settings().getRecentFiles());
        } else if (dir.equals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_POPULAR)) {
            _widgetFilesList.addAll(ApplicationObject.settings().getPopularFiles());
        } else if (dir.equals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_FAVOURITE)) {
            _widgetFilesList.addAll(ApplicationObject.settings().getFavouriteFiles());
        } else if (dir.exists() && dir.canRead()) {
            final boolean showDot = as.isFileBrowserFilterShowDotFiles();
            final File[] all = dir.listFiles(file -> showDot || !file.getName().startsWith("."));
            _widgetFilesList.addAll(all != null ? Arrays.asList(all) : Collections.emptyList());
        }
        GsFileUtils.sortFiles(_widgetFilesList, as.getFileBrowserSortByType(), as.isFileBrowserSortFolderFirst(), as.isFileBrowserSortReverse());
    }

    @Override
    public void onDestroy() {
        _widgetFilesList.clear();
    }

    @Override
    public int getCount() {
        return _widgetFilesList.size();
    }

    @Override
    public RemoteViews getViewAt(final int position) {
        final RemoteViews rowView = new RemoteViews(_context.getPackageName(), R.layout.widget_file_item);
        rowView.setTextViewText(R.id.widget_note_title, "???");
        if (position < _widgetFilesList.size()) {
            final File file = _widgetFilesList.get(position);
            final Intent fillInIntent = new Intent().putExtra(Document.EXTRA_FILE, file);
            rowView.setTextViewText(R.id.widget_note_title, file.getName());
            rowView.setOnClickFillInIntent(R.id.widget_note_title, fillInIntent);
            final int icon = file.isDirectory() ? R.drawable.ic_folder_gray_24dp : R.drawable.ic_file_gray_24dp;
            rowView.setTextViewCompoundDrawables(R.id.widget_note_title, icon, 0, 0, 0);
        }
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
