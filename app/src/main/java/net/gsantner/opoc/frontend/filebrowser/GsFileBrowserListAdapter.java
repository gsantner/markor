/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend.filebrowser;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;
import android.util.Pair;
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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.markor.R;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "unused"})
public class GsFileBrowserListAdapter extends RecyclerView.Adapter<GsFileBrowserListAdapter.FilesystemViewerViewHolder> implements Filterable, View.OnClickListener, View.OnLongClickListener {
    //########################
    //## Static
    //########################
    public static final File VIRTUAL_STORAGE_ROOT = new File("/storage/");
    public static final File VIRTUAL_STORAGE_SYSTEM = new File(VIRTUAL_STORAGE_ROOT, "System");
    public static final File VIRTUAL_STORAGE_EMULATED = new File(VIRTUAL_STORAGE_SYSTEM, "emulated");
    public static final File VIRTUAL_STORAGE_RECENTS = new File(VIRTUAL_STORAGE_ROOT, "Recent");
    public static final File VIRTUAL_STORAGE_FAVOURITE = new File(VIRTUAL_STORAGE_ROOT, "Favourites");
    public static final File VIRTUAL_STORAGE_POPULAR = new File(VIRTUAL_STORAGE_ROOT, "Popular");
    public static final File VIRTUAL_STORAGE_APP_DATA_PRIVATE = new File(VIRTUAL_STORAGE_SYSTEM, "AppData (private)");
    public static final String EXTRA_CURRENT_FOLDER = "EXTRA_CURRENT_FOLDER";
    public static final String EXTRA_DOPT = "EXTRA_DOPT";
    public static final String EXTRA_RECYCLER_SCROLL_STATE = "EXTRA_RECYCLER_SCROLL_STATE";
    public static final String EXTRA_REQ_FOLDER = "EXTRA_REQ_FOLDER";
    public static final int FAVOURITE_COLOR = 0xFFE3B51B;

    private static final File GO_BACK_SIGNIFIER = new File("__GO_BACK__");
    private static final File STORAGE_EMULATED = new File(VIRTUAL_STORAGE_ROOT, "emulated");
    private static final StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();

    //########################
    //## Members
    //########################
    private final GsFileBrowserOptions.Options _dopt;
    private final List<File> _adapterData; // List of current folder
    private final List<File> _adapterDataFiltered; // Filtered list of current folder
    private final Set<File> _currentSelection;
    private File _fileToShowAfterNextLoad;
    private File _currentFolder;
    private File _goUpFile;
    private final Context _context;
    private final StringFilter _filter;
    private final ThreadPoolExecutor _executorService = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private RecyclerView _recyclerView;
    private LinearLayoutManager _layoutManager;
    private volatile Map<File, File> _virtualMapping = new LinkedHashMap<>();
    private final Map<File, Integer> _fileIdMap = new HashMap<>();
    private final Map<File, Parcelable> _folderScrollMap = new HashMap<>();
    private final Stack<File> _backStack = new Stack<>();
    private final int _userId = getUserId();
    private volatile int _loadGeneration = 0;
    private long _prevModSum = 0;
    private static final int FOLDER_OBSERVER_MASK =
            FileObserver.CREATE | FileObserver.DELETE | FileObserver.MOVED_FROM
                    | FileObserver.MOVED_TO | FileObserver.MODIFY;
    private FileObserver _folderObserver;
    private final Runnable _folderReloadDebounced = TextViewUtils.makeDebounced(300, this::reloadCurrentFolder);

    //########################
    //## Methods
    //########################
    public GsFileBrowserListAdapter(GsFileBrowserOptions.Options options, Context context) {
        _dopt = options;
        _adapterData = new ArrayList<>();
        _adapterDataFiltered = new ArrayList<>();
        _currentSelection = new HashSet<>();
        _context = context;
        GsContextUtils.instance.setAppLocale(_context, Locale.getDefault());

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

        updateVirtualFolders();
        _filter = new StringFilter(this);
    }

    public void updateVirtualFolders() {
        final Map<File, File> virtualMapping = new LinkedHashMap<>();
        virtualMapping.put(VIRTUAL_STORAGE_SYSTEM, VIRTUAL_STORAGE_SYSTEM);
        virtualMapping.put(VIRTUAL_STORAGE_EMULATED, STORAGE_EMULATED);

        final File appDataFolder = _context.getFilesDir();
        if (appDataFolder.exists() || appDataFolder.mkdir()) {
            virtualMapping.put(VIRTUAL_STORAGE_APP_DATA_PRIVATE, appDataFolder);
        }

        final File[] externals = ContextCompat.getExternalFilesDirs(_context, null);
        for (int i = 0; i < externals.length; i++) {
            final File file = externals[i];
            if (file != null && file.getParentFile() != null) {
                final File remap = new File(VIRTUAL_STORAGE_SYSTEM, "AppData (external-" + i + ")");
                virtualMapping.put(remap, file);
            }
        }

        virtualMapping.put(VIRTUAL_STORAGE_RECENTS, VIRTUAL_STORAGE_RECENTS);
        virtualMapping.put(VIRTUAL_STORAGE_POPULAR, VIRTUAL_STORAGE_POPULAR);
        virtualMapping.put(VIRTUAL_STORAGE_FAVOURITE, VIRTUAL_STORAGE_FAVOURITE);
        virtualMapping.putAll(_dopt.storageMaps);

        _virtualMapping = virtualMapping;
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
        final File displayFile = _adapterDataFiltered.get(position);

        if (displayFile == null) {
            holder.title.setText("????");
            return;
        }

        final File file = resolveVirtualFile(displayFile);

        final boolean isGoUp = displayFile.equals(_goUpFile);
        final boolean isVirtual = _virtualMapping.containsKey(displayFile);
        final boolean isSelected = _currentSelection.contains(displayFile);
        final boolean isFavourite = _dopt.favouriteFiles != null && _dopt.favouriteFiles.contains(displayFile);
        final boolean isFile = displayFile.isFile();

        String titleText = displayFile.getName();
        if (isCurrentFolderVirtual() && "index.html".equals(titleText)) {
            final String currentFolderName = _currentFolder != null ? _currentFolder.getName() : "";
            titleText += " [" + currentFolderName + "]";
        }

        // Set title
        holder.title.setText(isGoUp ? ".." : titleText, TextView.BufferType.SPANNABLE);
        holder.title.setTextColor(ContextCompat.getColor(_context, _dopt.primaryTextColor));

        if (!isFileWriteable(displayFile, isGoUp) && !isVirtual && holder.title.length() > 0) {
            try {
                ((Spannable) holder.title.getText()).setSpan(STRIKE_THROUGH_SPAN, 0, holder.title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (Exception ignored) {
            }
        }

        // Set description
        if (!_dopt.descModtimeInsteadOfParent || isGoUp) {
            holder.description.setText(file.getAbsolutePath());
        } else {
            holder.description.setText(formatFileDescription(file, _dopt.descriptionFormat));
        }
        holder.description.setTextColor(ContextCompat.getColor(_context, _dopt.secondaryTextColor));

        // Set icon
        if (isSelected) {
            holder.image.setImageResource(_dopt.selectedItemImage);
        } else if (_dopt.iconMaps != null && _dopt.iconMaps.containsKey(displayFile)) {
            holder.image.setImageResource(_dopt.iconMaps.get(displayFile));
        } else {
            holder.image.setImageResource(isFile ? _dopt.fileImage : _dopt.folderImage);
        }

        holder.image.setColorFilter(ContextCompat.getColor(
                        _context,
                        isSelected ? _dopt.accentColor : isFile ? _dopt.fileColor : _dopt.folderColor),
                android.graphics.PorterDuff.Mode.SRC_ATOP
        );

        if (!isSelected && !isGoUp && isFavourite) {
            holder.image.setColorFilter(FAVOURITE_COLOR);
        }

        // Some extras
        if (_dopt.itemSidePadding > 0) {
            int dp = (int) (_dopt.itemSidePadding * _context.getResources().getDisplayMetrics().density);
            holder.itemRoot.setPadding(dp, holder.itemRoot.getPaddingTop(), dp, holder.itemRoot.getPaddingBottom());
        }

        final int descriptionRes = isSelected ? _dopt.contentDescriptionSelected : (isFile ? _dopt.contentDescriptionFile : _dopt.contentDescriptionFolder);
        holder.itemRoot.setContentDescription((descriptionRes != 0 ? (_context.getString(descriptionRes) + " ") : "") + titleText + " " + holder.description.getText().toString());
        holder.image.setOnLongClickListener(view -> {
            Toast.makeText(_context, displayFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return true;
        });

        holder.itemRoot.setTag(new TagContainer(displayFile, position));
        holder.itemRoot.setOnClickListener(this);
        holder.itemRoot.setOnLongClickListener(this);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull final RecyclerView view) {
        super.onAttachedToRecyclerView(view);
        _recyclerView = view;
        _layoutManager = (LinearLayoutManager) view.getLayoutManager();
        rebindFolderObserver();
        reloadCurrentFolder();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull final RecyclerView view) {
        _loadGeneration++;
        _executorService.getQueue().clear();
        stopFolderObserver();
        _layoutManager = null;
        _recyclerView = null;
        super.onDetachedFromRecyclerView(view);
    }

    private void rebindFolderObserver() {
        stopFolderObserver();
        final File folder = _currentFolder;
        if (folder == null || !folder.isDirectory() || !folder.canRead()) {
            return;
        }
        _folderObserver = new FileObserver(folder.getAbsolutePath(), FOLDER_OBSERVER_MASK) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                if (path == null) {
                    return;
                }
                _folderReloadDebounced.run();
            }
        };
        _folderObserver.startWatching();
    }

    private void stopFolderObserver() {
        if (_folderObserver != null) {
            _folderObserver.stopWatching();
            _folderObserver = null;
        }
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
                outState.putParcelable(EXTRA_RECYCLER_SCROLL_STATE, _layoutManager.onSaveInstanceState());
            }
        }
    }

    public void restoreSavedInstanceState(final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        if (_dopt != null && _dopt.listener != null) {
            _dopt.listener.onFsViewerConfig(_dopt);
        }

        if (savedInstanceState.containsKey(EXTRA_CURRENT_FOLDER)) {
            final String path = savedInstanceState.getString(EXTRA_CURRENT_FOLDER);
            if (path != null) {
                final File f = new File(path);
                final boolean isVirtualDirectory = _virtualMapping.containsKey(f);

                if (f.isDirectory() || isVirtualDirectory) {
                    loadFolder(f, null);
                }
            }
        }

        if (savedInstanceState.containsKey(EXTRA_RECYCLER_SCROLL_STATE) && _layoutManager != null) {
            _recyclerView.postDelayed(() -> _layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(EXTRA_RECYCLER_SCROLL_STATE)), 200);
        }
    }

    public void reloadCurrentFolder() {
        if (_currentFolder != null) {
            loadFolder(_currentFolder, null);
        } else if (_dopt.startFolder != null) {
            loadFolder(_dopt.startFolder, null);
        } else {
            loadFolder(_dopt.rootFolder, null);
        }
    }

    public void setCurrentFolder(final File folder) {
        loadFolder(folder, GsFileUtils.isChild(_currentFolder, folder) ? folder : null);
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
        return _filter;
    }

    public boolean isCurrentFolderWriteable() {
        return canWrite(_currentFolder);
    }

    @Override
    @SuppressWarnings("UnnecessaryReturnStatement")
    public void onClick(View view) {
        final TagContainer data = (TagContainer) view.getTag();

        if (!_currentSelection.isEmpty()) {
            // Blink in multi-select
            GsContextUtils.blinkView(view);
        }

        switch (view.getId()) {
            case R.id.opoc_filesystem_item__root: {
                // A own item was clicked
                if (data.file != null) {
                    if (areItemsSelected()) {
                        // There are 1 or more items selected yet
                        if (!toggleSelection(data) && isDirectory(data.file)) {
                            loadFolder(data.file, null);
                        }
                    } else {
                        // No pre-selection
                        if (isDirectory(data.file)) {
                            loadFolder(data.file, data.file.equals(_goUpFile) ? _currentFolder : null);
                        } else if (data.file.isFile()) {
                            _dopt.listener.onFsViewerSelected(_dopt.requestId, data.file, null);
                        }
                    }
                }
                return;
            }
            case R.id.ui__filesystem_dialog__home: {
                loadFolder(_dopt.rootFolder, _currentFolder);
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

    public void selectAll() {
        boolean changed = false;
        for (final File file : _adapterDataFiltered) {
            if (!_virtualMapping.containsKey(file) && canSelect(file)) {
                changed |= _currentSelection.add(file);
            }
        }
        notifySelectionChanged(changed);
    }

    public void unselectAll() {
        final boolean changed = !_currentSelection.isEmpty();
        _currentSelection.clear();
        notifySelectionChanged(changed);
    }

    public boolean areItemsSelected() {
        return !_currentSelection.isEmpty();
    }

    public Set<File> getCurrentSelection() {
        return _currentSelection;
    }

    public boolean isFilesOnlySelected() {
        for (File f : _currentSelection) {
            if (GsFileUtils.isDirectory(f)) {
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
            if (_currentSelection.contains(data.file)) {
                // Single selection
                _currentSelection.remove(data.file);
                clickHandled = true;
            } else if (canSelect(data.file)) {
                _currentSelection.add(data.file);
                clickHandled = true;
            }
        }

        if (clickHandled) {
            notifyItemChanged(data.position);
            _dopt.listener.onFsViewerDoUiUpdate(this);
        }

        return clickHandled;
    }

    private boolean canSelect(final File file) {
        if (file == null || !_dopt.doSelectMultiple || file.equals(_goUpFile)) {
            return false;
        }
        if (_virtualMapping.containsKey(file)) {
            return _dopt.doSelectFolder && _dopt.selectVirtualCollections && isVirtualCollection(file);
        }
        final boolean isDirectory = file.isDirectory();
        return _dopt.doSelectFile && !isDirectory || _dopt.doSelectFolder && isDirectory;
    }

    private void notifySelectionChanged(final boolean changed) {
        if (changed) {
            notifyItemRangeChanged(0, getItemCount());
            _dopt.listener.onFsViewerDoUiUpdate(this);
        }
    }

    private boolean isDirectory(final File file) {
        return file != null && (_virtualMapping.containsKey(file) || file.isDirectory());
    }

    public boolean goBack() {
        if (!_backStack.isEmpty()) {
            File show = _currentFolder;
            if (VIRTUAL_STORAGE_ROOT.equals(_backStack.peek()) || VIRTUAL_STORAGE_SYSTEM.equals(_backStack.peek())) {
                show = GsCollectionUtils.reverseSearch(_virtualMapping, _currentFolder);
            }
            loadFolder(GO_BACK_SIGNIFIER, show);
            return true;
        }
        return false;
    }

    private @Nullable File getCurrentParent(final File folder) {
        if (folder == null || folder.getParentFile() == null || VIRTUAL_STORAGE_ROOT.equals(folder)) {
            return null;
        }

        final File virtualAlias = GsCollectionUtils.reverseSearch(_virtualMapping, folder);
        if (virtualAlias != null && (VIRTUAL_STORAGE_SYSTEM.equals(virtualAlias) || VIRTUAL_STORAGE_SYSTEM.equals(virtualAlias.getParentFile()))) {
            return virtualAlias.getParentFile();
        }

        final File ancestor = writableAncestor(folder);
        if (ancestor != null) {
            return ancestor;
        }

        return GsFileUtils.isChild(VIRTUAL_STORAGE_ROOT, folder) || isInVirtualMappingTree(folder) ? VIRTUAL_STORAGE_ROOT : null;
    }

    private @Nullable File writableAncestor(final File file) {
        File ancestor = file.getParentFile();
        while (ancestor != null && !canWrite(ancestor)) {
            ancestor = ancestor.getParentFile();
        }
        return ancestor;
    }

    private boolean isInVirtualMappingTree(final File file) {
        for (final File mapped : _virtualMapping.values()) {
            if (file.equals(mapped)
                    || GsFileUtils.isChild(file, mapped)
                    || GsFileUtils.isChild(mapped, file)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onLongClick(final View view) {
        GsContextUtils.blinkView(view);
        if (view.getId() == R.id.opoc_filesystem_item__root) {
            final TagContainer data = (TagContainer) view.getTag();
            toggleSelection(data);
            _dopt.listener.onFsViewerItemLongPressed(data.file, _dopt.doSelectMultiple);
            return true;
        }
        return false;
    }

    public File createDirectoryHere(final CharSequence name) {
        if (name == null || _currentFolder == null || !_currentFolder.canWrite()) {
            return null;
        }

        final String trimmed = name.toString().trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            final File file = new File(_currentFolder, trimmed);
            if (file.exists() || file.mkdir()) {
                loadFolder(_currentFolder, file);
                return file;
            }
        } catch (SecurityException ignored) {
        }

        Toast.makeText(_context, R.string.file_does_not_exist_and_cant_be_created, Toast.LENGTH_LONG).show();
        return null;
    }

    // Switch to folder and show the file
    public void showFile(final File file) {
        if (file == null || !file.exists() || _recyclerView == null) {
            return;
        }

        final File dir = file.getParentFile();
        if (dir != null) {
            loadFolder(dir, file);
        }
    }

    private void postScrollToAndFlash(final File file, final int loadGeneration) {
        final RecyclerView recyclerView = _recyclerView;
        if (recyclerView != null && file != null) {
            recyclerView.post(() -> {
                if (loadGeneration == _loadGeneration) {
                    scrollToAndFlash(file, loadGeneration);
                }
            });
        }
    }

    /**
     * Scroll to a file in current folder and flash
     *
     * @param file File to blink
     */
    public boolean scrollToAndFlash(final File file) {
        return scrollToAndFlash(file, _loadGeneration);
    }

    private boolean scrollToAndFlash(final File file, final int loadGeneration) {
        final int pos = _adapterDataFiltered.indexOf(file);
        final LinearLayoutManager layoutManager = _layoutManager;
        final RecyclerView recyclerView = _recyclerView;
        if (pos >= 0 && layoutManager != null && recyclerView != null) {
            layoutManager.scrollToPosition(pos);
            recyclerView.post(() ->
                    recyclerView.postDelayed(() -> {
                        if (loadGeneration != _loadGeneration || recyclerView != _recyclerView ||
                            pos >= _adapterDataFiltered.size() || !file.equals(_adapterDataFiltered.get(pos))) {
                            return;
                        }
                        final RecyclerView.ViewHolder holder = recyclerView.findViewHolderForLayoutPosition(pos);
                        if (holder != null) {
                            GsContextUtils.blinkView2(holder.itemView);
                            holder.itemView.requestFocus();
                        }
                    }, 400));
            return true;
        }
        return false;
    }

    private void loadFolder(final File folder, final File show) {
        if (folder == null || _recyclerView == null) {
            return;
        }

        final boolean folderChanged = !folder.equals(_currentFolder);

        if (folderChanged && _currentFolder != null && _layoutManager != null) {
            _folderScrollMap.put(_currentFolder, _layoutManager.onSaveInstanceState());
        }

        // Update current folder
        if (GO_BACK_SIGNIFIER == folder) {
            _currentFolder = _backStack.pop();
        } else {
            if (folderChanged && _currentFolder != null) {
                _backStack.push(_currentFolder);
            }
            _currentFolder = resolveVirtualFile(folder);
        }

        if (folderChanged) {
            _currentSelection.clear();
            rebindFolderObserver();
        }

        final File currentFolder = _currentFolder;
        final int loadGeneration = ++_loadGeneration;

        _dopt.listener.onFsViewerFolderLoad(currentFolder);
        if (loadGeneration != _loadGeneration) {
            return;
        }

        if (VIRTUAL_STORAGE_ROOT.equals(currentFolder)) {
            updateVirtualFolders();
        }

        if (currentFolder != null) {
            final File toShow = show == null ? _fileToShowAfterNextLoad : show;
            _fileToShowAfterNextLoad = null;
            final RecyclerView recyclerView = _recyclerView;
            _executorService.getQueue().clear();
            _executorService.execute(() -> _loadFolder(currentFolder, folderChanged, toShow, loadGeneration, recyclerView));
        }
    }

    // This function is not called on the main thread
    private void _loadFolder(final File folder, final boolean folderChanged, final @Nullable File toShow,
                             final int loadGeneration, final RecyclerView recyclerView) {
        if (loadGeneration != _loadGeneration) {
            return;
        }

        final List<File> newData = new ArrayList<>();

        // Make sure /storage/emulated/0 is browsable, even though filesystem says it's not accessible
        if (folder.equals(new File("/"))) {
            newData.add(VIRTUAL_STORAGE_ROOT);
        } else if (folder.equals(VIRTUAL_STORAGE_ROOT) || folder.equals(VIRTUAL_STORAGE_SYSTEM)) {
            addVirtualChildren(newData, folder);

            // SD Card and other external storage directories that are also not listable
            if (folder.equals(VIRTUAL_STORAGE_ROOT)) {
                for (final Pair<File, String> p : GsContextUtils.instance.getAppDataPublicDirs(_context, false, true, false)) {
                    File f = p.first;
                    while (f.getParentFile() != null && !f.getParentFile().getName().equals("storage")) {
                        f = f.getParentFile();
                    }
                    if (!STORAGE_EMULATED.equals(f)) {
                        newData.add(f);
                    }
                }
            }
        } else if (folder.equals(STORAGE_EMULATED)) {
            newData.add(new File(folder, "" + _userId));
        } else if (folder.equals(VIRTUAL_STORAGE_RECENTS)) {
            newData.addAll(_dopt.recentFiles);
        } else if (folder.equals(VIRTUAL_STORAGE_POPULAR)) {
            newData.addAll(_dopt.popularFiles);
        } else if (folder.equals(VIRTUAL_STORAGE_FAVOURITE)) {
            newData.addAll(_dopt.favouriteFiles);
        }

        if (!VIRTUAL_STORAGE_ROOT.equals(folder) &&
            !VIRTUAL_STORAGE_SYSTEM.equals(folder) &&
            folder.isDirectory() &&
            folder.canRead()) {
            GsCollectionUtils.addAll(newData, folder.listFiles());
        }

        GsCollectionUtils.keepIf(newData, this::accept);
        GsCollectionUtils.deduplicate(newData);

        // Don't sort recent or virtual container items - use the default order
        if (isFolderSortable(folder)) {
            GsFileUtils.sortFiles(newData, _dopt.sortOrder);
        }

        // Testing if modtimes have changed (modtimes generally only increase)
        final long modSum = GsCollectionUtils.accumulate(newData, (f, s) -> s + f.lastModified(), 0L);
        final boolean modSumChanged = modSum != _prevModSum;

        final File goUp = getCurrentParent(folder);
        final ArrayList<File> adapterData = new ArrayList<>();
        if (goUp != null) {
            adapterData.add(goUp);
        }
        adapterData.addAll(newData);

        if (folderChanged || modSumChanged || !adapterData.equals(_adapterData)) {
            final String filterQuery = _filter.getQuery();
            final ArrayList<File> filteredData = new ArrayList<>();
            _filter.filter(adapterData, filteredData, filterQuery, goUp);

            recyclerView.post(() -> {
                if (loadGeneration != _loadGeneration) {
                    return;
                }

                // Modify all these values in the UI thread
                _goUpFile = goUp;
                _adapterData.clear();
                _adapterDataFiltered.clear();
                _adapterData.addAll(adapterData);
                if (filterQuery.equals(_filter.getQuery())) {
                    _adapterDataFiltered.addAll(filteredData);
                } else {
                    _filter.filter(adapterData, _adapterDataFiltered, _filter.getQuery(), goUp);
                }
                _currentSelection.retainAll(_adapterDataFiltered);
                _prevModSum = modSum;
                _filter.updateSource(loadGeneration, _adapterData, _goUpFile);

                if (folderChanged) {
                    _fileIdMap.clear();
                }

                // TODO - add logic to notify the changed bits
                notifyDataSetChanged();

                if (folderChanged) {
                    recyclerView.post(() -> {
                        if (loadGeneration != _loadGeneration) {
                            return;
                        }
                        if (_layoutManager != null) {
                            _layoutManager.onRestoreInstanceState(_folderScrollMap.remove(folder));
                        }

                        postScrollToAndFlash(toShow, loadGeneration);
                    });
                } else {
                    postScrollToAndFlash(toShow, loadGeneration);
                }

                if (_dopt.listener != null) {
                    _dopt.listener.onFsViewerDoUiUpdate(GsFileBrowserListAdapter.this);
                }
            });
        } else {
            recyclerView.post(() -> {
                if (loadGeneration != _loadGeneration) {
                    return;
                }
                final ArrayList<File> filteredData = new ArrayList<>();
                _filter.filter(_adapterData, filteredData, _filter.getQuery(), _goUpFile);
                if (!filteredData.equals(_adapterDataFiltered)) {
                    _adapterDataFiltered.clear();
                    _adapterDataFiltered.addAll(filteredData);
                    notifyDataSetChanged();
                }
                _filter.updateSource(loadGeneration, _adapterData, _goUpFile);
                postScrollToAndFlash(toShow, loadGeneration);
            });
        }
    }

    private void addVirtualChildren(final List<File> files, final File parent) {
        for (final File file : _virtualMapping.keySet()) {
            if (parent.equals(file.getParentFile())) {
                files.add(file);
            }
        }
    }

    public boolean canWrite(final File file) {
        return canWrite(file, _dopt.mountedStorageFolder);
    }

    public static boolean canWrite(final File file, final File mountedStorageFolder) {
        return file != null && (file.canWrite() || file.equals(mountedStorageFolder) || GsFileUtils.isChild(mountedStorageFolder, file));
    }

    public boolean accept(File file) {
        file = resolveVirtualFile(file);
        final boolean isDirectory = GsFileUtils.isDirectory(file);
        final File parent = file.getParentFile();
        final String name = file.getName().toLowerCase();
        final boolean filterYes = isDirectory || _dopt.fileOverallFilter == null || _dopt.fileOverallFilter.callback(_context, file);
        final boolean dotYes = _dopt.sortOrder.showDotFiles || !name.startsWith(".") && !isAccessoryFolder(parent, name, file);
        final boolean selFileYes = _dopt.doSelectFile || isDirectory;
        return filterYes && dotYes && selFileYes;
    }

    public boolean accept(final File dir, final String filename) {
        return accept(new File(dir, filename));
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

    //########################
    //##
    //## StringFilter
    //##
    //########################
    private static class StringFilter extends Filter {
        private final GsFileBrowserListAdapter _adapter;
        private volatile FilterData _source = new FilterData(0, "", null, new ArrayList<>());
        private volatile String _lastFilter = "";

        private StringFilter(final GsFileBrowserListAdapter adapter) {
            super();
            _adapter = adapter;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults results = new FilterResults();
            final String query = constraint.toString().toLowerCase(Locale.getDefault()).trim();
            final ArrayList<File> filtered = new ArrayList<>();

            _lastFilter = query;
            final FilterData source = _source;
            filter(source.files, filtered, query, source.goUpFile);

            results.values = new FilterData(source.loadGeneration, query, source.goUpFile, filtered);
            results.count = filtered.size();
            return results;
        }

        private String getQuery() {
            return _lastFilter;
        }

        private void updateSource(final int loadGeneration, final List<File> files, final File goUpFile) {
            _source = new FilterData(loadGeneration, "", goUpFile, new ArrayList<>(files));
        }

        private void filter(final List<File> all, final List<File> filtered, final String query, final File alwaysInclude) {
            filtered.clear();
            if (query.isEmpty()) {
                filtered.addAll(all);
            } else {
                for (final File file : all) {
                    if (file.equals(alwaysInclude) || file.getName().toLowerCase(Locale.getDefault()).contains(query)) {
                        filtered.add(file);
                    }
                }
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            final FilterData result = (FilterData) results.values;
            if (result.loadGeneration != _adapter._loadGeneration || !result.query.equals(_lastFilter)) {
                return;
            }
            _adapter._adapterDataFiltered.clear();
            _adapter._adapterDataFiltered.addAll(result.files);
            _adapter.notifyDataSetChanged();
        }

        private static class FilterData {
            final int loadGeneration;
            final String query;
            final File goUpFile;
            final List<File> files;

            FilterData(final int loadGeneration, final String query, final File goUpFile, final List<File> files) {
                this.loadGeneration = loadGeneration;
                this.query = query;
                this.goUpFile = goUpFile;
                this.files = files;
            }
        }
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    public static class FilesystemViewerViewHolder extends RecyclerView.ViewHolder {
        //########################
        //## UI Binding
        //########################
        final LinearLayout itemRoot;
        final ImageView image;
        final TextView title;
        final TextView description;

        //########################
        //## Methods
        //########################
        FilesystemViewerViewHolder(final View row) {
            super(row);
            itemRoot = row.findViewById(R.id.opoc_filesystem_item__root);
            image = row.findViewById(R.id.opoc_filesystem_item__image);
            title = row.findViewById(R.id.opoc_filesystem_item__title);
            description = row.findViewById(R.id.opoc_filesystem_item__description);
        }
    }

    public boolean isCurrentFolderVirtual() {
        return isVirtualFolder(_currentFolder);
    }

    // Is the folder a virtual folder - does it contain links or other special items
    public static boolean isVirtualFolder(final File file) {
        return VIRTUAL_STORAGE_SYSTEM.equals(file) || VIRTUAL_STORAGE_ROOT.equals(file) || isVirtualCollection(file);
    }

    private static boolean isVirtualCollection(final File file) {
        return VIRTUAL_STORAGE_RECENTS.equals(file) ||
               VIRTUAL_STORAGE_FAVOURITE.equals(file) ||
               VIRTUAL_STORAGE_POPULAR.equals(file);
    }

    public void showFileAfterNextLoad(final File file) {
        _fileToShowAfterNextLoad = file;
    }

    private int getUserId() {
        try {
            final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            final String[] parts = path.split("/");
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public boolean isCurrentFolderSortable() {
        return isFolderSortable(_currentFolder);
    }

    private boolean isFolderSortable(final File folder) {
        return folder != null &&
               !VIRTUAL_STORAGE_ROOT.equals(folder) &&
               !VIRTUAL_STORAGE_SYSTEM.equals(folder) &&
               !VIRTUAL_STORAGE_RECENTS.equals(folder);
    }

    public File resolveVirtualFile(final File file) {
        return GsCollectionUtils.getOrDefault(_virtualMapping, file, file);
    }
}
