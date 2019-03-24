/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.Spanned;
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

import net.gsantner.markor.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings({"WeakerAccess", "unused"})
public class FilesystemDialogAdapter extends RecyclerView.Adapter<FilesystemDialogAdapter.UiFilesystemDialogViewHolder> implements Filterable, View.OnClickListener, View.OnLongClickListener, Comparator<File>, FilenameFilter {
    //########################
    //## Static
    //########################
    public static final File VIRTUAL_STORAGE_RECENTS = new File("/storage/recent-files");
    public static final File VIRTUAL_STORAGE_POPULAR = new File("/storage/popular-files");
    public static final File VIRTUAL_STORAGE_APP_DATA_PRIVATE = new File("/storage/appdata-private");
    private static final StrikethroughSpan STRIKE_THROUGH_SPAN = new StrikethroughSpan();
    private static final String EXTRA_CURRENT_FOLDER = "EXTRA_CURRENT_FOLDER";
    private static final String EXTRA_RECYCLER_SCROLL_STATE = "EXTRA_RECYCLER_SCROLL_STATE";

    //########################
    //## Members
    //########################
    private final FilesystemDialogData.Options _dopt;
    private final List<File> _adapterData; // List of current folder
    private final List<File> _adapterDataFiltered; // Filtered list of current folder
    private final Set<File> _currentSelection;
    private File _currentFolder;
    private final Context _context;
    private StringFilter _filter;
    private boolean _wasInit;
    private final HashMap<File, File> _virtualMapping = new HashMap<>();
    private final RecyclerView _recyclerView;

    //########################
    //## Methods
    //########################

    public FilesystemDialogAdapter(FilesystemDialogData.Options options, Context context) {
        this(options, context, null);
    }

    public FilesystemDialogAdapter(FilesystemDialogData.Options options, Context context, RecyclerView recyclerView) {
        _dopt = options;
        _adapterData = new ArrayList<>();
        _adapterDataFiltered = new ArrayList<>();
        _currentSelection = new HashSet<>();
        _context = context.getApplicationContext();
        loadFolder(options.rootFolder);
        _recyclerView = recyclerView;
    }


    @NonNull
    @Override
    public UiFilesystemDialogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.opoc_filesystem_item, parent, false);
        _wasInit = true;
        return new UiFilesystemDialogViewHolder(v);
    }

    public boolean isFileWriteable(File file, boolean isGoUp) {
        return file != null && (file.canWrite() || isGoUp || _virtualMapping.keySet().contains(file));
    }

    @Override
    public void onBindViewHolder(@NonNull UiFilesystemDialogViewHolder holder, int position) {
        File file_pre = _adapterDataFiltered.get(position);
        File file_pre_Parent = file_pre.getParentFile() == null ? new File("/") : file_pre.getParentFile();
        String filename = file_pre.getName();
        if (_virtualMapping.keySet().contains(file_pre)) {
            file_pre = _virtualMapping.get(file_pre);
        }
        final File file = file_pre;
        final File fileParent = file.getParentFile() == null ? new File("/") : file.getParentFile();

        boolean isGoUp = file.equals(_currentFolder.getParentFile());
        holder.title.setText(isGoUp ? ".." : filename, TextView.BufferType.SPANNABLE);
        holder.title.setTextColor(ContextCompat.getColor(_context, _dopt.primaryTextColor));
        if (!isFileWriteable(file, isGoUp) && holder.title.length() > 0) {
            ((Spannable) holder.title.getText()).setSpan(STRIKE_THROUGH_SPAN, 0, holder.title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        File descriptionFile = fileParent.equals(_currentFolder) ? fileParent : file;
        holder.description.setText(!_dopt.descModtimeInsteadOfParent ? descriptionFile.getAbsolutePath() : DateUtils.formatDateTime(_context, descriptionFile.lastModified(), (DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE)));
        holder.description.setTextColor(ContextCompat.getColor(_context, _dopt.secondaryTextColor));

        holder.image.setImageResource(file.isDirectory() ? _dopt.folderImage : _dopt.fileImage);
        if (_currentSelection.contains(file)) {
            holder.image.setImageResource(_dopt.selectedItemImage);
        }
        holder.image.setColorFilter(ContextCompat.getColor(_context,
                _currentSelection.contains(file) ? _dopt.accentColor : _dopt.secondaryTextColor),
                android.graphics.PorterDuff.Mode.SRC_ATOP);

        if (_dopt.itemSidePadding > 0) {
            int dp = (int) (_dopt.itemSidePadding * _context.getResources().getDisplayMetrics().density);
            holder.itemRoot.setPadding(dp, holder.itemRoot.getPaddingTop(), dp, holder.itemRoot.getPaddingBottom());
        }

        //holder.itemRoot.setBackgroundColor(ContextCompat.getColor(_context,
        //        _currentSelection.contains(file) ? _dopt.primaryColor : _dopt.backgroundColor));
        holder.image.setOnLongClickListener(view -> {
            Toast.makeText(_context, file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return true;
        });
        holder.itemRoot.setTag(new TagContainer(file, position));
        holder.itemRoot.setOnClickListener(this);
        holder.itemRoot.setOnLongClickListener(this);
    }

    public Bundle saveInstanceState(Bundle outState) {
        outState = outState == null ? new Bundle() : outState;
        if (_currentFolder != null) {
            outState.putString(EXTRA_CURRENT_FOLDER, _currentFolder.getAbsolutePath());
        }

        if (_recyclerView != null) {
            if (_recyclerView.getLayoutManager() != null) {
                outState.putParcelable(EXTRA_RECYCLER_SCROLL_STATE, _recyclerView.getLayoutManager().onSaveInstanceState());
            }
        }
        return outState;
    }

    public void restoreSavedInstanceState(Bundle savedInstanceStateArg) {
        final Bundle savedInstanceState = savedInstanceStateArg == null ? new Bundle() : savedInstanceStateArg;
        File f;

        if (savedInstanceState.containsKey(EXTRA_CURRENT_FOLDER) && (f = new File(savedInstanceState.getString(EXTRA_CURRENT_FOLDER))).isDirectory()) {
            loadFolder(f);
        }

        if (savedInstanceState.containsKey(EXTRA_RECYCLER_SCROLL_STATE) && _recyclerView.getLayoutManager() != null) {
            _recyclerView.postDelayed(() -> {
                _recyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable(EXTRA_RECYCLER_SCROLL_STATE));
            }, 200);
        }

    }

    public void reloadCurrentFolder() {
        restoreSavedInstanceState(saveInstanceState(null));
    }

    public void setCurrentFolder(File folder, boolean reload) {
        _currentFolder = folder;
        if (reload) {
            reloadCurrentFolder();
        }
    }

    public class TagContainer {
        public final File file;
        public final int position;

        public TagContainer(File file_, int position_) {
            file = file_;
            position = position_;
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

    @Override
    @SuppressWarnings("UnnecessaryReturnStatement")
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ui__filesystem_item__root: {
                // A own item was clicked
                TagContainer data = (TagContainer) view.getTag();
                if (data != null && data.file != null) {
                    File file = data.file;
                    if (_virtualMapping.keySet().contains(file)) {
                        file = _virtualMapping.get(data.file);
                    }
                    if (areItemsSelected()) {
                        // There are 1 or more items selected yet
                        if (!toggleSelection(data) && file.isDirectory()) {
                            loadFolder(file);
                        }
                    } else {
                        // No pre-selection
                        if (file.isDirectory()) {
                            loadFolder(file);
                        } else if (file.isFile()) {
                            _dopt.listener.onFsSelected(_dopt.requestId, file);
                        } else if (file.equals(VIRTUAL_STORAGE_POPULAR) || file.equals(VIRTUAL_STORAGE_RECENTS) || file.equals(VIRTUAL_STORAGE_APP_DATA_PRIVATE)) {
                            loadFolder(file);
                        }
                    }
                }
                return;
            }
            case R.id.ui__filesystem_dialog__home: {
                _currentSelection.clear();
                loadFolder(_dopt.rootFolder);
                return;
            }
            case R.id.ui__filesystem_dialog__button_ok: {
                if (_dopt.doSelectMultiple && areItemsSelected()) {
                    _dopt.listener.onFsMultiSelected(_dopt.requestId,
                            _currentSelection.toArray(new File[_currentSelection.size()]));
                } else if (_dopt.doSelectFolder && (_currentFolder.exists() || _currentFolder.equals(VIRTUAL_STORAGE_RECENTS) || _currentFolder.equals(VIRTUAL_STORAGE_POPULAR) || _currentFolder.equals(VIRTUAL_STORAGE_APP_DATA_PRIVATE))) {
                    _dopt.listener.onFsSelected(_dopt.requestId, _currentFolder);
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

    public boolean areItemsSelected() {
        return !_currentSelection.isEmpty();
    }

    public boolean toggleSelection(TagContainer data) {
        boolean ret = false;
        if (_currentSelection.contains(data.file)) {
            _currentSelection.remove(data.file);
            ret = true;
        } else if (_dopt.doSelectMultiple) {
            if (_dopt.doSelectFile && !data.file.isDirectory()) {
                _currentSelection.add(data.file);
                ret = true;
            }
            if (_dopt.doSelectFolder && data.file.isDirectory()) {
                _currentSelection.add(data.file);
                ret = true;
            }
        }
        notifyItemChanged(data.position);
        _dopt.listener.onFsDoUiUpdate(this);
        return ret;
    }

    public boolean goUp() {
        if (canGoUp()) {
            if (_currentFolder != null && _currentFolder.getAbsolutePath() != null && _currentFolder.getParentFile() != null && !_currentFolder.getParentFile().getAbsolutePath().equals(_currentFolder.getAbsolutePath())) {
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
        File parentFolder = _currentFolder.getParentFile();
        return parentFolder != null && (!_dopt.mustStartWithRootFolder || parentFolder.getAbsolutePath().startsWith(_dopt.rootFolder.getAbsolutePath()));
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.ui__filesystem_item__root: {
                TagContainer data = (TagContainer) view.getTag();
                toggleSelection(data);
                _dopt.listener.onFsLongPressed(data.file, _dopt.doSelectMultiple);
                return true;
            }
        }
        return false;
    }

    private final static Object LOAD_FOLDER_SYNC_OBJECT = new Object();

    public void loadFolder(final File folder) {
        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                synchronized (LOAD_FOLDER_SYNC_OBJECT) {
                    _currentFolder = folder;
                    _adapterData.clear();
                    _virtualMapping.clear();
                    File file;
                    File[] files = null;

                    if (_currentFolder.isDirectory()) {
                        files = _currentFolder.listFiles(FilesystemDialogAdapter.this);
                    } else if (_currentFolder.equals(VIRTUAL_STORAGE_RECENTS)) {
                        files = _dopt.recentFiles;
                    } else if (_currentFolder.equals(VIRTUAL_STORAGE_POPULAR)) {
                        files = _dopt.popularFiles;
                    }
                    files = (files == null ? new File[0] : files);

                    Collections.addAll(_adapterData, files);

                    if (folder.getAbsolutePath().equals("/storage/emulated")) {
                        _adapterData.add(new File(folder, "0"));
                    }

                    if (folder.getAbsolutePath().equals("/")) {
                        _adapterData.add(new File(folder, "storage"));
                    }

                    if (folder.getAbsolutePath().equals("/storage")) {
                        // Scan for /storage/emulated/{0,1,2,..}
                        for (int i = 0; i < 10; i++) {
                            file = new File("/storage/emulated/" + i);
                            if (file.canWrite()) {
                                File remap = new File(folder, "emulated-" + i);
                                _virtualMapping.put(remap, file);
                                _adapterData.add(remap);
                            } else {
                                break;
                            }
                        }

                        if (_dopt.recentFiles != null) {
                            _virtualMapping.put(VIRTUAL_STORAGE_RECENTS, VIRTUAL_STORAGE_RECENTS);
                            _adapterData.add(VIRTUAL_STORAGE_RECENTS);
                        }
                        if (_dopt.popularFiles != null) {
                            _virtualMapping.put(VIRTUAL_STORAGE_POPULAR, VIRTUAL_STORAGE_POPULAR);
                            _adapterData.add(VIRTUAL_STORAGE_POPULAR);
                        }
                        File appDataFolder = _context.getFilesDir();
                        if (appDataFolder.exists() || (!appDataFolder.exists() && appDataFolder.mkdir())) {
                            _virtualMapping.put(VIRTUAL_STORAGE_APP_DATA_PRIVATE, appDataFolder);
                            _adapterData.add(VIRTUAL_STORAGE_APP_DATA_PRIVATE);
                        }
                    }

                    for (File externalFileDir : ContextCompat.getExternalFilesDirs(_context, null)) {
                        for (int i = 0; i < _adapterData.size(); i++) {
                            file = _adapterData.get(i);
                            if (!file.canWrite() && !file.getAbsolutePath().equals("/") && externalFileDir != null && externalFileDir.getAbsolutePath().startsWith(file.getAbsolutePath())) {
                                int c = 0;
                                for (char ch : file.getAbsolutePath().toCharArray()) {
                                    if (ch == '/') {
                                        c++;
                                    }
                                }
                                if (c < 3) {
                                    File remap = new File(file.getParentFile().getAbsolutePath(), "appdata-public (" + file.getName() + ")");
                                    _virtualMapping.put(remap, new File(externalFileDir.getAbsolutePath()));
                                    _adapterData.add(remap);
                                }
                            }
                        }
                    }

                    Collections.sort(_adapterData, FilesystemDialogAdapter.this);

                    if (canGoUp(_currentFolder)) {
                        _adapterData.add(0, _currentFolder.equals(new File("/storage/emulated/0")) ? new File("/storage/emulated") : _currentFolder.getParentFile());
                    }

                    handler.post(() -> {
                        _filter.filter(_filter._lastFilter);
                        notifyDataSetChanged();
                        if (_dopt.listener != null) {
                            _dopt.listener.onFsDoUiUpdate(FilesystemDialogAdapter.this);
                        }
                    });
                }
            }
        }.start();
    }

    // listFiles(FilenameFilter)
    @Override
    public boolean accept(File dir, String filename) {
        File f = new File(dir, filename);
        Boolean yes = _dopt.fileOverallFilter == null ? null : _dopt.fileOverallFilter.apply(f);
        yes = yes == null || yes;
        return f.isDirectory() || (!f.isDirectory() && _dopt.doSelectFile && yes);
    }

    // Sort adapterData
    @Override
    public int compare(File o1, File o2) {
        if (o1 == null || o2 == null) {
            return 0;
        }
        if (o1.isDirectory() && _dopt.folderFirst)
            return o2.isDirectory() ? o1.getName().toLowerCase(Locale.getDefault()).compareTo(o2.getName().toLowerCase(Locale.getDefault())) : -1;
        else if (!o2.canWrite())
            return -1;
        else if (o2.isDirectory() && _dopt.folderFirst)
            return 1;
        else if (_dopt.fileComparable != null) {
            int v = _dopt.fileComparable.compare(o1, o2);
            if (v != 0) {
                return v;
            }
        }
        return o1.getName().toLowerCase(Locale.getDefault()).compareTo(o2.getName().toLowerCase(Locale.getDefault()));
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
        private FilesystemDialogAdapter _adapter;
        private final List<File> _originalList;
        private final List<File> _filteredList;
        public CharSequence _lastFilter = "";

        private StringFilter(FilesystemDialogAdapter adapter, List<File> adapterData) {
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
    static class UiFilesystemDialogViewHolder extends RecyclerView.ViewHolder {
        //########################
        //## UI Binding
        //########################
        @BindView(R.id.ui__filesystem_item__root)
        LinearLayout itemRoot;
        @BindView(R.id.ui__filesystem_item__image)
        ImageView image;
        @BindView(R.id.ui__filesystem_item__title)
        TextView title;
        @BindView(R.id.ui__filesystem_item__description)
        TextView description;

        //########################
        //## Methods
        //########################
        UiFilesystemDialogViewHolder(View row) {
            super(row);
            ButterKnife.bind(this, row);
        }
    }
}