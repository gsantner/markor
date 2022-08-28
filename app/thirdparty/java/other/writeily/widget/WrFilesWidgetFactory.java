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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserFragment;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserListAdapter;

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
        updateFiles();
    }

    private void updateFiles() {
        _widgetFilesList.clear();
        final File dir = WrWidgetConfigure.getWidgetDirectory(_context, _appWidgetId);

        if (dir.equals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_RECENTS)) {
            _widgetFilesList.addAll(Arrays.asList(FilesystemViewerCreator.strlistToArray(AppSettings.get().getRecentDocuments())));
        } else if (dir.equals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_POPULAR)) {
            _widgetFilesList.addAll(Arrays.asList(FilesystemViewerCreator.strlistToArray(AppSettings.get().getPopularDocuments())));
        } else if (dir.exists() && dir.canRead()) {
            final File[] all = dir.listFiles(file -> !file.isDirectory() && TextFormat.isTextFile(file));
            _widgetFilesList.addAll(all != null ? Arrays.asList(all) : Collections.emptyList());
            GsFileBrowserFragment.sortFolder(_widgetFilesList); // Sort only if actual folder
        }
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
            final Intent fillInIntent = new Intent().putExtra(Document.EXTRA_PATH, file);
            rowView.setTextViewText(R.id.widget_note_title, file.getName());
            rowView.setOnClickFillInIntent(R.id.widget_note_title, fillInIntent);
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
