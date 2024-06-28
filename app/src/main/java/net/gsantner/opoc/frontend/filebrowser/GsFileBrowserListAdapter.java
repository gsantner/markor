/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend.filebrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.markor.R;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "unused"})
public class GsFileBrowserListAdapter extends RecyclerView.Adapter<GsFileBrowserListAdapter.FilesystemViewerViewHolder> implements Filterable, View.OnClickListener, View.OnLongClickListener, FilenameFilter {
    //########################
    //## Static
    //########################
    public static final File VIRTUAL_STORAGE_ROOT = new File("/storage/");
    public static final File VIRTUAL_STORAGE_RECENTS = new File(VIRTUAL_STORAGE_ROOT, "Recent");
    public static final File VIRTUAL_STORAGE_FAVOURITE = new File(VIRTUAL_STORAGE_ROOT, "Favourites");
    public static final File VIRTUAL_STORAGE_POPULAR = new File(VIRTUAL_STORAGE_ROOT, "Popular");
    public static final File VIRTUAL_STORAGE_APP_DATA_PRIVATE = new File(VIRTUAL_STORAGE_ROOT, "appdata-private");
    private static final StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();
    public static final String EXTRA_CURRENT_FOLDER = "EXTRA_CURRENT_FOLDER";
    public static final String EXTRA_DOPT = "EXTRA_DOPT";
    public static final String EXTRA_RECYCLER_SCROLL_STATE = "EXTRA_RECYCLER_SCROLL_STATE";
    public static final String EXTRA_REQ_FOLDER = "EXTRA_REQ_FOLDER";

    //########################
    //## Members
    //########################
    private final GsFileBrowserOptions.Options _dopt;
    private final List<File> _adapterData; // List of current folder
    private final List<File> _adapterDataFiltered; // Filtered list of current folder
    private final Set<File> _currentSelection;
    private File _currentFolder;
    private final Context _context;
    private StringFilter _filter;
    private RecyclerView _recyclerView;
    private final SharedPreferences _prefApp;
    private final HashMap<File, File> _virtualMapping = new HashMap<>();
    private final Map<File, Integer> _fileIdMap = new HashMap<>();

    //########################
    //## Methods
    //########################
    public GsFileBrowserListAdapter(GsFileBrowserOptions.Options options, Context context) {
        _dopt = options;
        _adapterData = new ArrayList<>();
        _adapterDataFiltered = new ArrayList<>();
        _currentSelection = new HashSet<>();
        _context = context;
        _prefApp = _context.getSharedPreferences("app", Context.MODE_PRIVATE);

        // Prevents view flicker - https://stackoverflow.com/a/32488059
        setHasStableIds(true);

        GsContextUtils cu = GsContextUtils.instance;
        if (_dopt.primaryColor == 0) {
            _dopt.primaryColor = cu.getResId(context, GsContextUtils.ResType.COLOR, "primary");
        }
        if (_dopt.accentColor == 0) {
            _dopt.accentColor = cu.getResId(context, GsContextUtils.ResType.COLOR, "accent");
        }
        if (_dopt.primaryTextColor == 0) {
            _dopt.primaryTextColor = cu.getResId(context, GsContextUtils.ResType.COLOR, "primary_text");
        }
        if (_dopt.secondaryTextColor == 0) {
            _dopt.secondaryTextColor = cu.getResId(context, GsContextUtils.ResType.COLOR, "secondary_text");
        }
        if (_dopt.titleTextColor == 0) {
            _dopt.titleTextColor = _dopt.primaryTextColor;
        }
        if (_dopt.fileColor == 0) {
            _dopt.fileColor = cu.getResId(context, GsContextUtils.ResType.COLOR, "file");
        }
        if (_dopt.folderColor == 0) {
            _dopt.folderColor = cu.getResId(context, GsContextUtils.ResType.COLOR, "folder");
        }

        loadFolder(_dopt.startFolder != null ? _dopt.startFolder : _dopt.rootFolder);
    }

    @NonNull
    @Override
    public FilesystemViewerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.opoc_filesystem_item, parent, false);
        return new FilesystemViewerViewHolder(v);
    }

    public boolean isCurrentFolderEmpty() {
        return _adapterData.size() < 2;
    }

    public boolean isFileWriteable(File file, boolean isGoUp) {
        return file != null && (canWrite(file) || isGoUp || _virtualMapping.containsKey(file));
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onBindViewHolder(@NonNull FilesystemViewerViewHolder holder, int position) {
        File file_pre = _adapterDataFiltered.get(position);
        if (file_pre == null) {
            holder.title.setText("????");
            return;
        }
        GsContextUtils.instance.setAppLocale(_context, Locale.getDefault());
        final File file_pre_Parent = file_pre.getParentFile() == null ? new File("/") : file_pre.getParentFile();
        final String filename = file_pre.getName();
        if (_virtualMapping.containsKey(file_pre)) {
            file_pre = _virtualMapping.get(file_pre);
        }
        final File file = file_pre;
        final File fileParent = file.getParentFile() == null ? new File("/") : file.getParentFile();
        final File descriptionFile = file.equals(_currentFolder.getParentFile()) ? file : fileParent;
        final boolean isGoUp = file.equals(_currentFolder.getParentFile());
        final boolean isSelected = _currentSelection.contains(file);
        final boolean isFavourite = _dopt.favouriteFiles != null && _dopt.favouriteFiles.contains(file);
        final boolean isPopular = _dopt.popularFiles != null && _dopt.popularFiles.contains(file);
        final int descriptionRes = isSelected ? _dopt.contentDescriptionSelected : (file.isDirectory() ? _dopt.contentDescriptionFolder : _dopt.contentDescriptionFile);
        String titleText = filename;
        if (isCurrentFolderVirtual() && "index.html".equals(filename)) {
            titleText += " [" + fileParent.getName() + "]";
        }

        holder.title.setText(isGoUp ? ".." : titleText, TextView.BufferType.SPANNABLE);
        holder.title.setTextColor(ContextCompat.getColor(_context, _dopt.primaryTextColor));
        if (!isFileWriteable(file, isGoUp) && holder.title.length() > 0) {
            try {
                ((Spannable) holder.title.getText()).setSpan(STRIKE_THROUGH_SPAN, 0, holder.title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (Exception ignored) {
            }
        }

        holder.description.setTextColor(ContextCompat.getColor(_context, _dopt.secondaryTextColor));

        holder.image.postDelayed(() -> {
            holder.image.setImageResource(isSelected ? _dopt.selectedItemImage : (!file.isFile() ? _dopt.folderImage : _dopt.fileImage));
            holder.description.setText(!_dopt.descModtimeInsteadOfParent || holder.title.getText().toString().equals("..")
                    ? descriptionFile.getAbsolutePath() : formatFileDescription(file, _prefApp.getString("pref_key__file_description_format", "")));
        }, 60);
        holder.image.setColorFilter(ContextCompat.getColor(_context,
                        isSelected ? _dopt.accentColor : (!file.isFile() ? _dopt.folderColor : _dopt.fileColor)),
                android.graphics.PorterDuff.Mode.SRC_ATOP);
        if (!isSelected && isFavourite) {
            holder.image.setColorFilter(0xFFE3B51B);
        }

        if (_dopt.itemSidePadding > 0) {
            int dp = (int) (_dopt.itemSidePadding * _context.getResources().getDisplayMetrics().density);
            holder.itemRoot.setPadding(dp, holder.itemRoot.getPaddingTop(), dp, holder.itemRoot.getPaddingBottom());
        }

        holder.itemRoot.setContentDescription((descriptionRes != 0 ? (_context.getString(descriptionRes) + " ") : "") + holder.title.getText().toString() + " " + holder.description.getText().toString());
        holder.image.setOnLongClickListener(view -> {
            Toast.makeText(_context, file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return true;
        });
        holder.itemRoot.setTag(new TagContainer(file, position));
        holder.itemRoot.setOnClickListener(this);
        holder.itemRoot.setOnLongClickListener(this);

        final Drawable drawable = holder.itemView.getBackground();
        if (drawable != null && ((ColorDrawable) drawable).getColor() == Color.LTGRAY) {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT); // Clear highlight
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull final RecyclerView view) {
        super.onAttachedToRecyclerView(view);
        _recyclerView = view;
    }

    public String formatFileDescription(final File file, String format) {
        if (TextUtils.isEmpty(format)) {
            return DateUtils.formatDateTime(_context, file.lastModified(), (DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE));
        } else {
            format = format.replaceAll("FS(?=([^']*'[^']*')*[^']*$)", '\'' + GsFileUtils.getHumanReadableByteCountSI(file.length()) + '\'');
            return new SimpleDateFormat(format, Locale.getDefault()).format(file.lastModified());
        }
    }

    public void saveInstanceState(final @NonNull Bundle outState) {
        if (_currentFolder != null) {
            outState.putSerializable(EXTRA_CURRENT_FOLDER, _currentFolder.getAbsolutePath());
        }

        if (_recyclerView != null) {
            if (_recyclerView.getLayoutManager() != null) {
                outState.putParcelable(EXTRA_RECYCLER_SCROLL_STATE, _recyclerView.getLayoutManager().onSaveInstanceState());
            }
        }
    }

    public void restoreSavedInstanceState(final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        if (savedInstanceState.containsKey(EXTRA_CURRENT_FOLDER)) {
            final String path = savedInstanceState.getString(EXTRA_CURRENT_FOLDER);
            if (path != null) {
                final File f = new File(path);
                final boolean isVirtualDirectory = _virtualMapping.containsKey(f) || isVirtualStorage(f);

                if (isVirtualDirectory && _dopt != null && _dopt.listener != null) {
                    _dopt.listener.onFsViewerConfig(_dopt);
                }
                if (f.isDirectory() || isVirtualDirectory) {
                    loadFolder(f);
                }
            }
        }

        if (savedInstanceState.containsKey(EXTRA_RECYCLER_SCROLL_STATE) && _recyclerView.getLayoutManager() != null) {
            _recyclerView.postDelayed(() -> _recyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable(EXTRA_RECYCLER_SCROLL_STATE)), 200);
        }
    }

    public void reloadCurrentFolder() {
        loadFolder(_currentFolder);
    }

    public void setCurrentFolder(final File folder) {
        if (folder != null && !folder.equals(_currentFolder)) {
            loadFolder(folder);
        }
    }

    public void reconfigure() {
        if (_dopt.listener != null) {
            _dopt.listener.onFsViewerConfig(_dopt);
            reloadCurrentFolder();
        }
    }

    public boolean isCurrentFolderVirtual() {
        return isVirtualFolder(_currentFolder);
    }

    public static class TagContainer {
        public final File file;
        public final int position;

        public TagContainer(File file_, int position_) {
            file = file_;
            position = position_;
        }
    }

    // Prevents view flicker - https://stackoverflow.com/a/32488059
    @Override
    public long getItemId(final int position) {
        final File f = _adapterDataFiltered.get(position);
        final Integer key = _fileIdMap.get(f);
        if (key == null) {
            final int newId = _fileIdMap.size();
            _fileIdMap.put(f, newId);
            return newId;
        } else {
            return key;
        }
    }

    public File getCurrentFolder() {
        return _currentFolder;
    }

    @Override
    public int getItemCount() {
        return _adapterDataFiltered.size();
    }

    @Override
    public Filter getFilter() {
        if (_filter == null) {
            _filter = new StringFilter(this, _adapterData);
        }
        return _filter;
    }

    public boolean isCurrentFolderWriteable() {
        return canWrite(_currentFolder);
    }

    private int getPathLevel(String path) {
        final int end = path.lastIndexOf('/');
        int level = 0;
        for (int i = 0; i <= end; i++) {
            if (path.charAt(i) == '/') {
                level++;
            }
        }
        return level;
    }

    private void saveItemState(final TagContainer data) {
        if (data != null) {
            final int currentItemLevel = getPathLevel(data.file.getAbsolutePath());
            final int currentFolderLevel = getPathLevel(_currentFolder.getAbsolutePath());

            if (currentItemLevel > currentFolderLevel) {
                final RecyclerView.LayoutManager layoutManager = _recyclerView.getLayoutManager();
            }
        }
    }

    @Override
    @SuppressWarnings("UnnecessaryReturnStatement")
    public void onClick(View view) {
        final TagContainer data = (TagContainer) view.getTag();
        if (_currentSelection.size() == 0) {
            saveItemState(data);
        }

        switch (view.getId()) {
            case R.id.opoc_filesystem_item__root: {
                // A own item was clicked
                if (data.file != null) {
                    File file = data.file;
                    if (_virtualMapping.containsKey(file)) {
                        file = _virtualMapping.get(data.file);
                    }
                    if (areItemsSelected()) {
                        // There are 1 or more items selected yet
                        if (!toggleSelection(data) && file != null && file.isDirectory()) {
                            loadFolder(file);
                        }
                    } else if (file != null) {
                        // No pre-selection
                        if (file.isDirectory()) {
                            loadFolder(file);
                        } else if (file.isFile()) {
                            _dopt.listener.onFsViewerSelected(_dopt.requestId, file, null);
                        } else if (isVirtualStorage(file)) {
                            loadFolder(file);
                        }
                    }
                }
                return;
            }
            case R.id.ui__filesystem_dialog__home: {
                loadFolder(_dopt.rootFolder);
                return;
            }
            case R.id.ui__filesystem_dialog__button_ok: {
                if (_dopt.doSelectMultiple && areItemsSelected()) {
                    _dopt.listener.onFsViewerMultiSelected(_dopt.requestId, _currentSelection.toArray(new File[0]));
                } else {
                    _dopt.listener.onFsViewerSelected(_dopt.requestId, _currentFolder, null);
                }
                return;
            }
        }
    }

    public void toggleSelectionAll() {
        for (int i = 0; i < _adapterDataFiltered.size(); i++) {
            TagContainer data = new TagContainer(_adapterDataFiltered.get(i), i);
            toggleSelection(data);
        }
    }

    public void selectAll() {
        for (int i = 0; i < _adapterDataFiltered.size(); i++) {
            TagContainer data = new TagContainer(_adapterDataFiltered.get(i), i);
            if (!_currentSelection.contains(data.file)) {
                if (data.file.isDirectory() && getCurrentFolder().getParentFile() != null && getCurrentFolder().getParentFile().equals(data.file)) {
                    continue;
                }
                _currentSelection.add(data.file);
                notifyItemChanged(data.position);
            }
        }
        _dopt.listener.onFsViewerDoUiUpdate(this);
    }

    public void unselectAll() {
        for (int i = 0; i < _adapterDataFiltered.size(); i++) {
            final TagContainer data = new TagContainer(_adapterDataFiltered.get(i), i);
            if (_currentSelection.contains(data.file)) {
                _currentSelection.remove(data.file);
                notifyItemChanged(data.position);
            }
        }
        _dopt.listener.onFsViewerDoUiUpdate(this);
    }

    public boolean areItemsSelected() {
        return !_currentSelection.isEmpty();
    }

    public Set<File> getCurrentSelection() {
        return _currentSelection;
    }

    public boolean isFilesOnlySelected() {
        for (File f : _currentSelection) {
            if (f.isDirectory()) {
                return false;
            }
        }
        return true;
    }

    public boolean toggleSelection(final TagContainer data) {
        if (data == null) {
            return false;
        }

        boolean clickHandled = false;
        if (data.file != null && _currentFolder != null) {
            if (data.file.isDirectory() && _currentFolder.getParentFile() != null && _currentFolder.getParentFile().equals(data.file)) {
                // goUp
                clickHandled = true;
            } else if (_currentSelection.contains(data.file)) {
                // Single selection
                _currentSelection.remove(data.file);
                clickHandled = true;
            } else if (_dopt.doSelectMultiple) {
                // Multi selection
                if (_dopt.doSelectFile && !data.file.isDirectory()) {
                    // Multi selection - file
                    _currentSelection.add(data.file);
                    clickHandled = true;
                }
                if (_dopt.doSelectFolder && data.file.isDirectory()) {
                    // Multi selection - folder
                    _currentSelection.add(data.file);
                    clickHandled = true;
                }
            }
        }

        notifyItemChanged(data.position);
        _dopt.listener.onFsViewerDoUiUpdate(this);

        return clickHandled;
    }

    public boolean goUp() {
        if (canGoUp()) {
            final String absolutePath = _currentFolder.getAbsolutePath();
            if (_currentFolder != null && _currentFolder.getParentFile() != null && !_currentFolder.getParentFile().getAbsolutePath().equals(absolutePath)) {
                unselectAll();
                loadFolder(_currentFolder.getParentFile());
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean canGoUp() {
        return canGoUp(_currentFolder);
    }

    public boolean canGoUp(File currentFolder) {
        File parentFolder = _currentFolder != null ? _currentFolder.getParentFile() : null;
        return parentFolder != null && (!_dopt.mustStartWithRootFolder || parentFolder.getAbsolutePath().startsWith(_dopt.rootFolder.getAbsolutePath()));
    }

    @Override
    public boolean onLongClick(final View view) {
        if (view.getId() == R.id.opoc_filesystem_item__root) {
            final TagContainer data = (TagContainer) view.getTag();
            toggleSelection(data);
            _dopt.listener.onFsViewerItemLongPressed(data.file, _dopt.doSelectMultiple);
            return true;
        }
        return false;
    }

    public File createDirectoryHere(final CharSequence name, final boolean show) {
        if (name == null || _currentFolder == null || !_currentFolder.canWrite()) {
            return null;
        }

        final String trimmed = name.toString().trim();

        if (trimmed.length() == 0) {
            return null;
        }

        try {
            final File file = new File(_currentFolder, trimmed);
            if (file.exists() || file.mkdir()) {
                if (show) {
                    showFile(file);
                } else {
                    reloadCurrentFolder();
                }
                return file;
            }
        } catch (SecurityException ignored) {
        }

        Toast.makeText(_context, R.string.file_does_not_exist_and_cant_be_created, Toast.LENGTH_LONG).show();
        return null;
    }

    public void doAfterChange(final GsCallback.a0 callback) {
        final long init = System.currentTimeMillis();
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                // Ignore if the load takes too long
                if ((System.currentTimeMillis() - init) < 2000) {
                    _recyclerView.post(callback::callback);
                }
                unregisterAdapterDataObserver(this);
            }
        });
    }

    // Switch to folder and show the file
    public void showFile(final File file) {
        if (file == null || !file.exists() || _recyclerView == null) {
            return;
        }

        final File dir = file.getParentFile();
        if (dir == null) {
            return;
        }

        if (getFilePosition(file) < 0) {
            doAfterChange(() -> showAndFlash(file));
            loadFolder(dir); // Will reload folder if necessary
        } else {
            showAndFlash(file);
        }
    }

    /**
     * Show a file in the current folder and blink it
     *
     * @param file File to blink
     */
    private void showAndFlash(final File file) {
        final int pos = getFilePosition(file);
        final LinearLayoutManager manager = (LinearLayoutManager) _recyclerView.getLayoutManager();

        if (pos >= 0 && manager != null) {

            // Scroll to position if needed
            final int firstVisible = manager.findFirstCompletelyVisibleItemPosition();
            final int lastVisible = manager.findLastCompletelyVisibleItemPosition();
            if (pos < firstVisible || pos > lastVisible) {
                manager.scrollToPositionWithOffset(pos, 1);
            }

            _recyclerView.postDelayed(() -> {
                final RecyclerView.ViewHolder holder = _recyclerView.findViewHolderForLayoutPosition(pos);
                if (holder != null) {
                    GsContextUtils.blinkView(holder.itemView);
                }
            }, 100);
        }
    }

    // Get the position of a file in the current view
    // -1 if file is not a child of the current directory
    public int getFilePosition(final File file) {
        if (file != null) {
            for (int i = 0; i < _adapterDataFiltered.size(); i++) {
                if (_adapterDataFiltered.get(i).equals(file)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static final ExecutorService executorService = new ThreadPoolExecutor(0, 3, 60, TimeUnit.SECONDS, new SynchronousQueue<>());

    // Stop blinking if we are currently blinking
    private void stopBlinking() {
        if (_recyclerView != null) {
            for (int i = 0; i < _recyclerView.getChildCount(); i++) {
                GsContextUtils.stopBlinking(_recyclerView.getChildAt(i));
            }
        }
    }

    private void loadFolder(final File folder) {
        stopBlinking();
        executorService.execute(() -> {
            synchronized (LOAD_FOLDER_SYNC_OBJECT) {

                if (_dopt.refresh != null) {
                    _dopt.refresh.callback();
                }

                final File prevFolder = _currentFolder;
                _currentFolder = folder;
                _virtualMapping.clear();
                final List<File> newData = new ArrayList<>();

                if (folder.equals(VIRTUAL_STORAGE_ROOT)) {
                    // Scan for /storage/emulated/{0,1,2,..}
                    for (int i = 0; i < 10; i++) {
                        final File file = new File("/storage/emulated/" + i);
                        if (canWrite(file)) {
                            File remap = new File(folder, "emulated-" + i);
                            _virtualMapping.put(remap, file);
                            newData.add(remap);
                        } else {
                            break;
                        }
                    }

                    if (_dopt.recentFiles != null) {
                        _virtualMapping.put(VIRTUAL_STORAGE_RECENTS, VIRTUAL_STORAGE_RECENTS);
                        newData.add(VIRTUAL_STORAGE_RECENTS);
                    }
                    if (_dopt.popularFiles != null) {
                        _virtualMapping.put(VIRTUAL_STORAGE_POPULAR, VIRTUAL_STORAGE_POPULAR);
                        newData.add(VIRTUAL_STORAGE_POPULAR);
                    }
                    if (_dopt.favouriteFiles != null) {
                        _virtualMapping.put(VIRTUAL_STORAGE_FAVOURITE, VIRTUAL_STORAGE_FAVOURITE);
                        newData.add(VIRTUAL_STORAGE_FAVOURITE);
                    }
                    File appDataFolder = _context.getFilesDir();
                    if (appDataFolder.exists() || (!appDataFolder.exists() && appDataFolder.mkdir())) {
                        _virtualMapping.put(VIRTUAL_STORAGE_APP_DATA_PRIVATE, appDataFolder);
                        newData.add(VIRTUAL_STORAGE_APP_DATA_PRIVATE);
                    }
                } else if (_currentFolder.isDirectory()) {
                    GsCollectionUtils.addAll(newData, _currentFolder.listFiles(GsFileBrowserListAdapter.this));
                } else if (_currentFolder.equals(VIRTUAL_STORAGE_RECENTS)) {
                    newData.addAll(_dopt.recentFiles);
                } else if (_currentFolder.equals(VIRTUAL_STORAGE_POPULAR)) {
                    newData.addAll(_dopt.popularFiles);
                } else if (_currentFolder.equals(VIRTUAL_STORAGE_FAVOURITE)) {
                    newData.addAll(_dopt.favouriteFiles);
                } else if (folder.getAbsolutePath().equals("/storage/emulated")) {
                    newData.add(new File(folder, "0"));
                } else if (folder.getAbsolutePath().equals("/")) {
                    newData.add(new File(folder, "storage"));
                } else if (folder.equals(_context.getFilesDir().getParentFile())) {
                    // Private AppStorage: Allow to access to files directory only
                    // (don't allow access to internals like shared_preferences & databases)
                    newData.add(new File(folder, "files"));
                }

                for (final File externalFileDir : ContextCompat.getExternalFilesDirs(_context, null)) {
                    for (int i = 0; i < newData.size(); i++) {
                        final File file = newData.get(i);
                        if (!canWrite(file) && !file.getAbsolutePath().equals("/") && externalFileDir != null && externalFileDir.getAbsolutePath().startsWith(file.getAbsolutePath())) {
                            final int depth = GsTextUtils.countChars(file.getAbsolutePath(), '/')[0];
                            if (depth < 3) {
                                final File parent = file.getParentFile();
                                if (parent != null) {
                                    final File remap = new File(parent.getAbsolutePath(), "appdata-public (" + file.getName() + ")");
                                    _virtualMapping.put(remap, new File(externalFileDir.getAbsolutePath()));
                                    newData.add(remap);
                                }
                            }
                        }
                    }
                }

                // Don't sort recent items - use the default order
                if (!_currentFolder.equals(VIRTUAL_STORAGE_RECENTS)) {
                    GsFileUtils.sortFiles(newData, _dopt.sortByType, _dopt.sortFolderFirst, _dopt.sortReverse);
                }

                if (canGoUp(_currentFolder)) {
                    newData.add(0, _currentFolder.equals(new File("/storage/emulated/0")) ? new File("/storage/emulated") : _currentFolder.getParentFile());
                }

                if (!newData.equals(_adapterData)) {
                    _adapterData.clear();
                    _adapterData.addAll(newData);
                    _currentSelection.retainAll(_adapterData);
                    _filter.filter(_filter._lastFilter);

                    if (!_currentFolder.equals(prevFolder)) {
                        _fileIdMap.clear();
                    }

                    _recyclerView.post(() -> {
                        // Must be called from UI thread
                        // TODO - add logic to notify the changed bits
                        notifyDataSetChanged();

                        if (prevFolder != null && _currentFolder.equals(prevFolder.getParentFile())) {
                            doAfterChange(() -> showAndFlash(prevFolder));
                        }

                        if (_dopt.listener != null) {
                            _dopt.listener.onFsViewerDoUiUpdate(GsFileBrowserListAdapter.this);
                        }
                    });
                }
            }
        });
    }

    private boolean canWrite(File file) {
        if (file != null) {
            return file.canWrite() || (_dopt.mountedStorageFolder != null && file.getAbsolutePath().startsWith(_dopt.mountedStorageFolder.getAbsolutePath()));
        }
        return false;
    }

    // listFiles(FilenameFilter)
    @Override
    public boolean accept(File dir, String filename) {
        final File f = new File(dir, filename);
        final boolean filterYes = f.isDirectory() || _dopt.fileOverallFilter == null || _dopt.fileOverallFilter.callback(_context, f);
        final boolean dotYes = _dopt.filterShowDotFiles || !filename.startsWith(".") && !isAccessoryFolder(dir, filename, f);
        final boolean selFileYes = _dopt.doSelectFile || f.isDirectory();
        return filterYes && dotYes && selFileYes;
    }

    private boolean isAccessoryFolder(File dir, String filename, File file) {
        return file.isDirectory() &&
                ((filename.endsWith("_files") && new File(dir, filename.replaceFirst("_files$", ".html")).isFile()) ||
                        (filename.endsWith(".assets") && new File(dir, filename.replaceFirst("\\.assets$", ".md")).isFile()));
    }

    public GsFileBrowserOptions.Options getFsOptions() {
        return _dopt;
    }

    public boolean isCurrentFolderHome() {
        return _currentFolder != null && _dopt.rootFolder != null && _dopt.rootFolder.getAbsolutePath().equals(_currentFolder.getAbsolutePath());
    }

    public static boolean isVirtualStorage(File file) {
        return VIRTUAL_STORAGE_FAVOURITE.equals(file) ||
                VIRTUAL_STORAGE_APP_DATA_PRIVATE.equals(file) ||
                VIRTUAL_STORAGE_POPULAR.equals(file) ||
                VIRTUAL_STORAGE_RECENTS.equals(file);
    }

    private final static Object LOAD_FOLDER_SYNC_OBJECT = new Object();


    //########################
    //##
    //## StringFilter
    //##
    //########################
    private static class StringFilter extends Filter {
        private final GsFileBrowserListAdapter _adapter;
        private final List<File> _originalList;
        private final List<File> _filteredList;
        public CharSequence _lastFilter = "";

        private StringFilter(GsFileBrowserListAdapter adapter, List<File> adapterData) {
            super();
            _adapter = adapter;
            _originalList = adapterData;
            _filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults results = new FilterResults();
            constraint = constraint.toString().toLowerCase(Locale.getDefault()).trim();
            _filteredList.clear();

            if (constraint.length() == 0) {
                _filteredList.addAll(_originalList);
            } else {
                for (File file : _originalList) {
                    if (file.getName().toLowerCase(Locale.getDefault()).contains(constraint)) {
                        _filteredList.add(file);
                    }
                }
            }

            _lastFilter = constraint;
            results.values = _filteredList;
            results.count = _filteredList.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            _adapter._adapterDataFiltered.clear();
            _adapter._adapterDataFiltered.addAll((ArrayList<File>) results.values);
            _adapter.notifyDataSetChanged();
        }
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    static class FilesystemViewerViewHolder extends RecyclerView.ViewHolder {
        //########################
        //## UI Binding
        //########################
        LinearLayout itemRoot;
        ImageView image;
        TextView title;
        TextView description;

        //########################
        //## Methods
        //########################
        FilesystemViewerViewHolder(View row) {
            super(row);
            itemRoot = row.findViewById(R.id.opoc_filesystem_item__root);
            image = row.findViewById(R.id.opoc_filesystem_item__image);
            title = row.findViewById(R.id.opoc_filesystem_item__title);
            description = row.findViewById(R.id.opoc_filesystem_item__description);
        }
    }

    public static boolean isVirtualFolder(final File file) {
        return file != null && (
                file.equals(VIRTUAL_STORAGE_APP_DATA_PRIVATE) ||
                        file.equals(VIRTUAL_STORAGE_FAVOURITE) ||
                        file.equals(VIRTUAL_STORAGE_POPULAR) ||
                        file.equals(VIRTUAL_STORAGE_RECENTS) ||
                        file.equals(new File("/")) ||
                        file.equals(new File("/storage")) ||
                        file.equals(new File("/storage/self")) ||
                        file.equals(new File("/storage/emulated"))
        );
    }
}
