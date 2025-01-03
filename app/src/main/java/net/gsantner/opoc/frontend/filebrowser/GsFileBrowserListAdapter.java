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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;
import android.util.Log;
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
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess", "unused"})
public class GsFileBrowserListAdapter extends RecyclerView.Adapter<GsFileBrowserListAdapter.FilesystemViewerViewHolder> implements Filterable, View.OnClickListener, View.OnLongClickListener, FilenameFilter {
    //########################
    //## Static
    //########################
    public static final File VIRTUAL_STORAGE_ROOT = new File("/storage/");
    public static final File VIRTUAL_STORAGE_EMULATED = new File(VIRTUAL_STORAGE_ROOT, "emulated");
    public static final File VIRTUAL_STORAGE_RECENTS = new File(VIRTUAL_STORAGE_ROOT, "Recent");
    public static final File VIRTUAL_STORAGE_FAVOURITE = new File(VIRTUAL_STORAGE_ROOT, "Favourites");
    public static final File VIRTUAL_STORAGE_POPULAR = new File(VIRTUAL_STORAGE_ROOT, "Popular");
    public static final File VIRTUAL_STORAGE_APP_DATA_PRIVATE = new File(VIRTUAL_STORAGE_ROOT, "AppData (data partition)");
    public static final String EXTRA_CURRENT_FOLDER = "EXTRA_CURRENT_FOLDER";
    public static final String EXTRA_DOPT = "EXTRA_DOPT";
    public static final String EXTRA_RECYCLER_SCROLL_STATE = "EXTRA_RECYCLER_SCROLL_STATE";
    public static final String EXTRA_REQ_FOLDER = "EXTRA_REQ_FOLDER";

    private static final File GO_BACK_SIGNIFIER = new File("__GO_BACK__");
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
    private RecyclerView _recyclerView;
    private LinearLayoutManager _layoutManager;
    private final Map<File, File> _virtualMapping = new LinkedHashMap<>();
    private final Map<File, Integer> _fileIdMap = new HashMap<>();
    private final Map<File, Parcelable> _folderScrollMap = new HashMap<>();
    private final Stack<File> _backStack = new Stack<>();
    private final int _userId = getUserId();
    private long _prevModSum = 0;

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
        final GsContextUtils cu = GsContextUtils.instance;

        _virtualMapping.clear();
        _virtualMapping.put(VIRTUAL_STORAGE_EMULATED, VIRTUAL_STORAGE_EMULATED);

        final File appDataFolder = _context.getFilesDir();
        if (appDataFolder.exists() || appDataFolder.mkdir()) {
            _virtualMapping.put(VIRTUAL_STORAGE_APP_DATA_PRIVATE, appDataFolder);
        }

        for (final File file : ContextCompat.getExternalFilesDirs(_context, null)) {
            if (file != null) {
                final File parent = file.getParentFile();
                if (parent != null) {
                    final String name = parent.toString().replace("/", "-").substring(1);
                    final File remap = new File(VIRTUAL_STORAGE_ROOT, "AppData (" + name + ")");
                    _virtualMapping.put(remap, file);
                }
            }
        }

        _virtualMapping.putAll(_dopt.storageMaps);

        _virtualMapping.put(VIRTUAL_STORAGE_RECENTS, VIRTUAL_STORAGE_RECENTS);
        _virtualMapping.put(VIRTUAL_STORAGE_POPULAR, VIRTUAL_STORAGE_POPULAR);
        _virtualMapping.put(VIRTUAL_STORAGE_FAVOURITE, VIRTUAL_STORAGE_FAVOURITE);
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

        final File file = GsCollectionUtils.getOrDefault(_virtualMapping, displayFile, displayFile);

        final boolean isGoUp = displayFile.equals(_goUpFile);
        final boolean isVirtual = _virtualMapping.containsKey(displayFile);
        final boolean isSelected = _currentSelection.contains(displayFile);
        final boolean isFavourite = _dopt.favouriteFiles != null && _dopt.favouriteFiles.contains(displayFile);
        final boolean isPopular = _dopt.popularFiles != null && _dopt.popularFiles.contains(displayFile);
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

        if (!isSelected && isFavourite) {
            holder.image.setColorFilter(0xFFE3B51B);
        }

        // Some extras
        if (_dopt.itemSidePadding > 0) {
            int dp = (int) (_dopt.itemSidePadding * _context.getResources().getDisplayMetrics().density);
            holder.itemRoot.setPadding(dp, holder.itemRoot.getPaddingTop(), dp, holder.itemRoot.getPaddingBottom());
        }

        final int descriptionRes = isSelected ? _dopt.contentDescriptionSelected : (displayFile.isDirectory() ? _dopt.contentDescriptionFolder : _dopt.contentDescriptionFile);
        holder.itemRoot.setContentDescription((descriptionRes != 0 ? (_context.getString(descriptionRes) + " ") : "") + titleText + " " + holder.description.getText().toString());
        holder.image.setOnLongClickListener(view -> {
            Toast.makeText(_context, displayFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return true;
        });

        holder.itemRoot.setTag(new TagContainer(displayFile, position));
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
        _layoutManager = (LinearLayoutManager) view.getLayoutManager();
        reloadCurrentFolder();
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
                        if (!toggleSelection(data) && data.file.isDirectory()) {
                            loadFolder(data.file, null);
                        }
                    } else {
                        // No pre-selection
                        if (data.file.isDirectory() || _virtualMapping.containsKey(data.file)) {
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

    public void toggleSelectionAll() {
        for (int i = 0; i < _adapterDataFiltered.size(); i++) {
            final TagContainer data = new TagContainer(_adapterDataFiltered.get(i), i);
            toggleSelection(data);
        }
    }

    public void selectAll() {
        for (int i = 0; i < _adapterDataFiltered.size(); i++) {
            final TagContainer data = new TagContainer(_adapterDataFiltered.get(i), i);
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
        if (data.file != null && _currentFolder != null && !data.file.equals(_goUpFile)) {
            if (_currentSelection.contains(data.file)) {
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

    public boolean goBack() {
        if (!_backStack.isEmpty()) {
            File show = _currentFolder;
            if (VIRTUAL_STORAGE_ROOT.equals(_backStack.peek())) {
                show = GsCollectionUtils.reverseSearch(_virtualMapping, _currentFolder);
            }
            loadFolder(GO_BACK_SIGNIFIER, show);
            return true;
        }
        return false;
    }

    private @Nullable File getCurrentParent() {
        if (_currentFolder == null) {
            return null;
        }

        final File parent = _currentFolder.getParentFile();
        if ((parent != null && parent.canWrite()) || GsFileUtils.isChild(VIRTUAL_STORAGE_ROOT, parent)) {
            return parent;
        }

        if (VIRTUAL_STORAGE_ROOT.equals(parent) || _virtualMapping.containsValue(_currentFolder)) {
            return VIRTUAL_STORAGE_ROOT;
        }

        return null;
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

    private void doAfterChange(final GsCallback.a0 callback) {
        _recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                _recyclerView.removeOnLayoutChangeListener(this);
                callback.callback();
            }
        });
    }

    private void postScrollToAndFlash(final File file) {
        if (_recyclerView != null && file != null) {
            _recyclerView.post(() -> scrollToAndFlash(file));
        }
    }

    /**
     * Scroll to a file in current folder and flash
     *
     * @param file File to blink
     */
    public boolean scrollToAndFlash(final File file) {
        final int pos = _adapterDataFiltered.indexOf(file);
        if (pos >= 0 && _layoutManager != null) {
            _layoutManager.scrollToPosition(pos);
            _recyclerView.post(() ->
                    _recyclerView.postDelayed(() -> {
                        final RecyclerView.ViewHolder holder = _recyclerView.findViewHolderForLayoutPosition(pos);
                        if (holder != null) {
                            GsContextUtils.blinkView(holder.itemView);
                        }
                    }, 400));
            return true;
        }
        return false;
    }

    private static final ExecutorService executorService = new ThreadPoolExecutor(0, 3, 60, TimeUnit.SECONDS, new SynchronousQueue<>());

    private void loadFolder(final File folder, final File show) {
        if (folder == null || _recyclerView == null) {
            return;
        }

        final boolean folderChanged = !folder.equals(_currentFolder);

        if (folderChanged && _currentFolder != null && _layoutManager != null) {
            _folderScrollMap.put(_currentFolder, _layoutManager.onSaveInstanceState());
        }

        if (GO_BACK_SIGNIFIER == folder) {
            _currentFolder = _backStack.pop();
        } else {
            if (folderChanged && _currentFolder != null) {
                _backStack.push(_currentFolder);
            }
            _currentFolder = GsCollectionUtils.getOrDefault(_virtualMapping, folder, folder);
        }

        if (_currentFolder != null) {
            final File toShow = show == null ? _fileToShowAfterNextLoad : show;
            _fileToShowAfterNextLoad = null;

            try {
                executorService.execute(() -> _loadFolder(folderChanged, toShow));
            } catch (RejectedExecutionException err) { // during exit
                Log.d(GsFileBrowserListAdapter.class.getName(), err.toString());
            }
        }
    }

    // This function is not called on the main thread
    private synchronized void _loadFolder(final boolean folderChanged, final @Nullable File toShow) {

        final List<File> newData = new ArrayList<>();

        // Make sure /storage/emulated/0 is browsable, even though filesystem says it's not accessible
        if (_currentFolder.equals(new File("/"))) {
            newData.add(VIRTUAL_STORAGE_ROOT);
        } else if (_currentFolder.equals(VIRTUAL_STORAGE_ROOT)) {
            newData.addAll(_virtualMapping.keySet());

            // SD Card and other external storage directories that are also not listable
            for (final Pair<File, String> p : GsContextUtils.instance.getAppDataPublicDirs(_context, false, true, false)) {
                File f = p.first;
                while (f.getParentFile() != null && !f.getParentFile().getName().equals("storage")) {
                    f = f.getParentFile();
                }
                newData.add(f);
            }
        } else if (_currentFolder.equals(VIRTUAL_STORAGE_EMULATED)) {
            newData.add(new File(_currentFolder, "" + _userId));
        } else if (_currentFolder.equals(VIRTUAL_STORAGE_RECENTS)) {
            newData.addAll(_dopt.recentFiles);
        } else if (_currentFolder.equals(VIRTUAL_STORAGE_POPULAR)) {
            newData.addAll(_dopt.popularFiles);
        } else if (_currentFolder.equals(VIRTUAL_STORAGE_FAVOURITE)) {
            newData.addAll(_dopt.favouriteFiles);
        }

        if (_currentFolder.isDirectory() && _currentFolder.canRead()) {
            GsCollectionUtils.addAll(newData, _currentFolder.listFiles(GsFileBrowserListAdapter.this));
        }

        GsCollectionUtils.deduplicate(newData);

        // Don't sort recent or virtual root items - use the default order
        if (!Arrays.asList(VIRTUAL_STORAGE_RECENTS, VIRTUAL_STORAGE_ROOT).contains(_currentFolder)) {
            GsFileUtils.sortFiles(newData, _dopt.sortByType, _dopt.sortFolderFirst, _dopt.sortReverse);
        }

        // Testing if modtimes have changed (modtimes generally only increase)
        final long modSum = GsCollectionUtils.accumulate(newData, (f, s) -> s + f.lastModified(), 0L);
        final boolean modSumChanged = modSum != _prevModSum;

        final File goUp = getCurrentParent();

        if (folderChanged || modSumChanged || !newData.equals(_adapterData)) {
            final ArrayList<File> filteredData = new ArrayList<>();
            _filter._filter(newData, filteredData);

            _recyclerView.post(() -> {
                // Modify all these values in the UI thread
                _goUpFile = goUp;
                _adapterData.clear();
                _adapterDataFiltered.clear();
                if (_goUpFile != null) {
                    _adapterData.add(_goUpFile);
                    _adapterDataFiltered.add(_goUpFile);
                }
                _adapterData.addAll(newData);
                _adapterDataFiltered.addAll(filteredData);
                _currentSelection.retainAll(_adapterDataFiltered);
                _prevModSum = modSum;

                if (folderChanged) {
                    _fileIdMap.clear();
                }

                // TODO - add logic to notify the changed bits
                notifyDataSetChanged();

                if (folderChanged) {
                    _recyclerView.post(() -> {
                        if (_layoutManager != null) {
                            _layoutManager.onRestoreInstanceState(_folderScrollMap.remove(_currentFolder));
                        }

                        postScrollToAndFlash(toShow);
                    });
                } else {
                    postScrollToAndFlash(toShow);
                }

                if (_dopt.listener != null) {
                    _dopt.listener.onFsViewerDoUiUpdate(GsFileBrowserListAdapter.this);
                }
            });
        } else {
            postScrollToAndFlash(toShow);
        }
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

    //########################
    //##
    //## StringFilter
    //##
    //########################
    private static class StringFilter extends Filter {
        private final GsFileBrowserListAdapter _adapter;
        private final List<File> _filteredList;
        public String _lastFilter = "";

        private StringFilter(final GsFileBrowserListAdapter adapter) {
            super();
            _adapter = adapter;
            _filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults results = new FilterResults();

            _lastFilter = constraint.toString().toLowerCase().trim();
            _filter(_adapter._adapterData, _filteredList);

            results.values = _filteredList;
            results.count = _filteredList.size();
            return results;
        }

        public void _filter(final List<File> all, final List<File> filtered) {
            filtered.clear();
            if (_lastFilter.isEmpty()) {
                filtered.addAll(all);
            } else {
                for (final File file : all) {
                    if (file.getName().toLowerCase().contains(_lastFilter)) {
                        filtered.add(file);
                    }
                }
            }
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

    public static boolean isVirtualFolder(final File file) {
        return VIRTUAL_STORAGE_RECENTS.equals(file) ||
                VIRTUAL_STORAGE_FAVOURITE.equals(file) ||
                VIRTUAL_STORAGE_POPULAR.equals(file) ||
                VIRTUAL_STORAGE_APP_DATA_PRIVATE.equals(file) ||
                VIRTUAL_STORAGE_EMULATED.equals(file) ||
                (file != null && VIRTUAL_STORAGE_ROOT.equals(file.getParentFile()) && !file.exists());
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
}
