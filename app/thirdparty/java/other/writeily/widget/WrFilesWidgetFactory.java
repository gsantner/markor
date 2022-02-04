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
import net.gsantner.opoc.ui.FilesystemViewerAdapter;
import net.gsantner.opoc.ui.FilesystemViewerFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class WrFilesWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context _context;
    private File[] _widgetFilesList = new File[0];
    private final int _appWidgetId;
    private final File _dir;

    public WrFilesWidgetFactory(final Context context, final Intent intent) {
        _context = context;
        _appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        _dir = (File) intent.getSerializableExtra(Document.EXTRA_PATH);
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
        _widgetFilesList = (_dir == null) ? new File[0] : _dir.listFiles(file -> !file.isDirectory() && TextFormat.isTextFile(file));
        if (_dir != null && _dir.equals(FilesystemViewerAdapter.VIRTUAL_STORAGE_RECENTS)) {
            _widgetFilesList = FilesystemViewerCreator.strlistToArray(AppSettings.get().getRecentDocuments());
        }
        if (_dir != null && _dir.equals(FilesystemViewerAdapter.VIRTUAL_STORAGE_POPULAR)) {
            _widgetFilesList = FilesystemViewerCreator.strlistToArray(AppSettings.get().getPopularDocuments());
        }
        final ArrayList<File> files = new ArrayList<>(Arrays.asList(_widgetFilesList != null ? _widgetFilesList : new File[0]));

        //noinspection StatementWithEmptyBody
        if (_dir != null && (_dir.equals(FilesystemViewerAdapter.VIRTUAL_STORAGE_RECENTS) || _dir.equals(FilesystemViewerAdapter.VIRTUAL_STORAGE_POPULAR))) {
            // nothing to do
        } else {
            FilesystemViewerFragment.sortFolder(files);
        }
        _widgetFilesList = files.toArray(new File[files.size()]);
    }

    @Override
    public void onDestroy() {
        _widgetFilesList = new File[0];
    }

    @Override
    public int getCount() {
        return _widgetFilesList == null ? 0 : _widgetFilesList.length;
    }

    @Override
    public RemoteViews getViewAt(final int position) {
        final RemoteViews rowView = new RemoteViews(_context.getPackageName(), R.layout.widget_file_item);
        rowView.setTextViewText(R.id.widget_note_title, "???");
        if (position < _widgetFilesList.length) {
            final File file = _widgetFilesList[position];
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
