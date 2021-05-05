package net.gsantner.opoc.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")

public class SearchEngine {
    public static boolean isSearchExecuting = false;
    public static Activity activity;
    public final static Pattern[] defaultIgnoredDirs = {
            Pattern.compile("^\\.git$"),
            Pattern.compile(".*[Tt]humb.*")
    };
    public final static Pattern[] defaultIgnoredFiles = {

    };

    public static class Config {
        private final List<Pattern> _ignoredRegexDirs;
        private final List<String> _ignoredExactDirs;
        private final List<Pattern> _ignoredRegexFiles;
        private final List<String> _ignoredExactFiles;
        private final List<Pattern> _searchByContentExtensions;

        public final boolean _isRegexQuery;
        public final boolean _isCaseSensitiveQuery;
        public final String _query;
        public final File _rootSearchDir;
        public final boolean _isSearchInContent;
        public final boolean _isShowResultOnCancel;
        public final Integer _maxSearchDepth;
        public final List<String> _ignoredDirectories;
        public final List<String> _ignoredFiles;
        public final boolean _isOnlyFirstContentMatch;
        public final boolean _isShowMatchPreview;

        public Config(final File rootSearchDir, String query, final boolean isShowResultOnCancel, final Integer maxSearchDepth, final List<String> ignoredDirectories, final List<String> ignoredFiles, final boolean isRegexQuery, final boolean isCaseSensitiveQuery, final boolean isSearchInContent, final boolean isOnlyFirstContentMatch, final boolean isShowMatchPreview, final List<String> searchByContentExtensions) {
            _rootSearchDir = rootSearchDir;
            _isSearchInContent = isSearchInContent;
            _isOnlyFirstContentMatch = isOnlyFirstContentMatch;
            _isShowMatchPreview = isShowMatchPreview;
            _isShowResultOnCancel = isShowResultOnCancel;
            _maxSearchDepth = maxSearchDepth;
            _ignoredDirectories = ignoredDirectories;
            _ignoredFiles = ignoredFiles;
            _isRegexQuery = isRegexQuery;
            _isCaseSensitiveQuery = isCaseSensitiveQuery;

            _ignoredExactDirs = new ArrayList<String>();
            _ignoredRegexDirs = new ArrayList<Pattern>();
            splitRegexExactFiles(_ignoredDirectories, _ignoredExactDirs, _ignoredRegexDirs);

            _ignoredExactFiles = new ArrayList<String>();
            _ignoredRegexFiles = new ArrayList<Pattern>();
            splitRegexExactFiles(_ignoredFiles, _ignoredExactFiles, _ignoredRegexFiles);

            _searchByContentExtensions = new ArrayList<Pattern>();
            splitExtensions(searchByContentExtensions, _searchByContentExtensions);


            query = isRegexQuery ? query.replaceAll("(?<![.])[*]", ".*") : query;
            _query = _isCaseSensitiveQuery ? query : query.toLowerCase();
        }


        private void splitRegexExactFiles(List<String> list, List<String> exactList, List<Pattern> regexList) {
            for (int i = 0; i < list.size(); i++) {
                String pattern = list.get(i);
                if (pattern.isEmpty()) {
                    continue;
                }

                if (pattern.startsWith("\"")) {
                    pattern = pattern.replace("\"", "");
                    if (pattern.isEmpty()) {
                        continue;
                    }
                    exactList.add(pattern);
                } else {
                    pattern = pattern.replaceAll("(?<![.])[*]", ".*");
                    try {
                        regexList.add(Pattern.compile(pattern));
                    } catch (Exception ex) {
                        String errorMessage = String.format(SearchEngine.activity.getString(R.string.regex_can_not_compile), pattern);
                        Toast.makeText(SearchEngine.activity, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }

        private void splitExtensions(List<String> list, List<Pattern> patterns) {
            for (int i = 0; i < list.size(); i++) {
                String line = list.get(i);
                if (StringUtils.isNullOrWhitespace(line)) {
                    continue;
                }

                line = line.replaceAll("(?<![.])[*]", ".*");
                if (!line.contains("*") && !line.contains("$") && !line.contains("^") && !line.contains("?") && !line.contains("\\") && !line.contains("|")) {
                    line = String.format(".*%s$", line);
                }
                try {
                    patterns.add(Pattern.compile(line));
                } catch (Exception ex) {
                    String errorMessage = String.format(SearchEngine.activity.getString(R.string.regex_can_not_compile), line);
                    Toast.makeText(SearchEngine.activity, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    public static class FitFile {
        private final String _path;
        private boolean _isDirectory;
        private final List<ContentMatchUnit> _contentMatches;

        public FitFile(final String path, final boolean isDirectory) {
            _path = path;
            _isDirectory = isDirectory;
            _contentMatches = new ArrayList<ContentMatchUnit>();
        }

        public FitFile(final String path, final boolean isDirectory, List<ContentMatchUnit> contentMatches) {
            this(path, isDirectory);
            addContentMatches(contentMatches);
        }

        private final void addContentMatch(final ContentMatchUnit lineNumber) {
            _contentMatches.add(lineNumber);
        }

        private final void addContentMatches(final List<ContentMatchUnit> lineNumbers) {
            _contentMatches.addAll(lineNumbers);
        }

        public final String getPath() {
            return _path;
        }

        public final List<ContentMatchUnit> getContentMatches() {
            return Collections.unmodifiableList(_contentMatches);
        }

        public final boolean isDirectory() {
            return _isDirectory;
        }

        public static class ContentMatchUnit {
            private final int _lineNumber;
            private final String _previewMatch;

            public ContentMatchUnit(final int lineNumber) {
                this(lineNumber, "");
            }

            public ContentMatchUnit(final int lineNumber, final String previewMatch) {
                _lineNumber = lineNumber;
                _previewMatch = previewMatch;
            }

            public final int getLineNumber() {
                return _lineNumber;
            }

            public final String getPreviewMatch() {
                return _previewMatch;
            }
        }
    }


    public static SearchEngine.QueueSearchFilesTask queueFileSearch(Activity activity, SearchEngine.Config config, Callback.a1<List<FitFile>> callback) {
        SearchEngine.activity = activity;
        SearchEngine.isSearchExecuting = true;
        SearchEngine.QueueSearchFilesTask task = new SearchEngine.QueueSearchFilesTask(config, callback);
        task.execute();

        return task;
    }


    public static class QueueSearchFilesTask extends AsyncTask<Void, Integer, List<FitFile>> {
        private SearchEngine.Config _config;
        private final Callback.a1<List<FitFile>> _callback;
        private final Pattern _regex;

        private Snackbar _snackBar;
        private Integer _countCheckedFiles;
        private Integer _currentQueueLength;
        private boolean _isCanceled;
        private Integer _currentSearchDepth;
        private List<FitFile> _result;

        public QueueSearchFilesTask(final SearchEngine.Config config, final Callback.a1<List<FitFile>> callback) {
            _config = config;
            _callback = callback;

            Pattern pattern = null;
            if (_config._isRegexQuery) {
                try {
                    pattern = Pattern.compile(_config._query);
                } catch (Exception ex) {
                    String errorMessage = String.format(SearchEngine.activity.getString(R.string.regex_can_not_compile), _config._query);
                    Toast.makeText(SearchEngine.activity, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
            _regex = pattern;

            _countCheckedFiles = 0;
            _isCanceled = false;
            _currentSearchDepth = 0;
            _currentQueueLength = 1;

            _result = new ArrayList<>();
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (_config._isRegexQuery && _regex == null) {
                cancel(true);
                return;
            }

            bindSnackBar(_config._query);
        }


        public void bindSnackBar(String text) {
            if (!SearchEngine.isSearchExecuting) {
                return;
            }

            try {
                View view = SearchEngine.activity.findViewById(android.R.id.content);
                _snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (SearchEngine.isSearchExecuting) {
                                    bindSnackBar(text);
                                }
                            }
                        });
                _snackBar.setAction(android.R.string.cancel, (v) -> {
                    _snackBar.dismiss();
                    preCancel();
                });
                _snackBar.show();
            } catch (Exception ignored) {
            }
        }

        @Override
        protected List<FitFile> doInBackground(Void... voidp) {
            Queue<File> mainQueue = new LinkedList<File>();
            mainQueue.add(_config._rootSearchDir);

            while (!mainQueue.isEmpty() && !isCancelled() && !_isCanceled) {
                File currentDirectory = mainQueue.remove();

                if (!currentDirectory.canRead() || currentDirectory.isFile()) {
                    continue;
                }

                _currentSearchDepth = getDirectoryDepth(_config._rootSearchDir, currentDirectory);
                if (_currentSearchDepth > _config._maxSearchDepth) {
                    break;
                }
                _currentQueueLength = mainQueue.size() + 1;
                publishProgress(_currentQueueLength, _currentSearchDepth, _result.size(), _countCheckedFiles);

                mainQueue.addAll(currentDirectoryHandler(currentDirectory));
            }

            if (_isCanceled && _result.size() == 0) {
                cancel(true);
            }

            return _result;
        }


        private Queue<File> currentDirectoryHandler(File currentDir) {
            Queue<File> subQueue = new LinkedList<File>();

            try {
                if (!currentDir.canRead() || currentDir.isFile()) {
                    return subQueue;
                }

                File[] subDirsOrFiles = currentDir.listFiles();
                for (int i = 0; i < subDirsOrFiles.length && !isCancelled() && !_isCanceled; i++) {
                    _countCheckedFiles++;
                    File subDirOrFile = subDirsOrFiles[i];

                    if (!subDirOrFile.canRead()) {
                        continue;
                    }

                    if (subDirOrFile.isDirectory()) {
                        File directory = subDirOrFile;
                        if (isFolderIgnored(directory) || isFileContainSymbolicLinks(directory, currentDir)) {
                            continue;
                        }
                        subQueue.add(directory);
                    } else {
                        File file = subDirOrFile;
                        if (isFileIgnored(file)) {
                            continue;
                        }

                        if (_config._isSearchInContent) {
                            if (isSearchByContentIgnoredFor(file)) {
                                continue;
                            }
                            List<FitFile.ContentMatchUnit> contentMatches = getContentMatches(file, _config._isOnlyFirstContentMatch);

                            if (contentMatches.size() == 0) {
                                continue;
                            }

                            String path = file.getCanonicalPath().replace(_config._rootSearchDir.getCanonicalPath() + "/", "");
                            _result.add(new FitFile(path, false, contentMatches));
                        }
                    }

                    if (!_config._isSearchInContent) {
                        getFileIfNameMatches(subDirOrFile);
                    }

                    publishProgress(_currentQueueLength + subQueue.size(), _currentSearchDepth, _result.size(), _countCheckedFiles);
                }
            } catch (Exception ignored) {
            }

            return subQueue;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            Integer queueLength = values[0];
            Integer queueDepth = values[1];
            Integer filesFound = values[2];
            Integer countCheckedFiles = values[3];
            String snackBarText = "f:" + filesFound + " qu:" + queueLength + "|" + queueDepth + " c:" + countCheckedFiles + "\n" + _config._query;
            if (_snackBar != null) {
                _snackBar.setText(snackBarText);
            }
        }


        @Override
        protected void onPostExecute(List<FitFile> ret) {
            super.onPostExecute(ret);
            SearchEngine.isSearchExecuting = false;
            if (_snackBar != null) {
                _snackBar.dismiss();
            }
            if (_callback != null) {
                try {
                    _callback.callback(ret);
                } catch (Exception ignored) {
                }
            }
        }


        @Override
        protected void onCancelled() {
            super.onCancelled();
            SearchEngine.isSearchExecuting = false;
        }


        private boolean isFileContainSymbolicLinks(File file, File expectedParentDir) {
            try {
                File realParentDir = file.getCanonicalFile().getParentFile();
                if (realParentDir != null && expectedParentDir.getCanonicalPath().equals(realParentDir.getCanonicalPath())) {
                    return false;
                }
            } catch (Exception ignored) {
            }

            return true;
        }


        private void getFileIfNameMatches(File file) {
            try {
                String fileName = _config._isCaseSensitiveQuery ? file.getName() : file.getName().toLowerCase();
                boolean isMatch = _config._isRegexQuery ? _regex.matcher(fileName).matches() : fileName.contains(_config._query);

                if (isMatch) {
                    String path = file.getCanonicalPath().replace(_config._rootSearchDir.getCanonicalPath() + "/", "");
                    _result.add(new FitFile(path, file.isDirectory()));
                }
            } catch (Exception ignored) {
            }
        }


        private Integer getDirectoryDepth(File parentDir, File childDir) {
            try {
                String parentPath = parentDir.getCanonicalPath();
                String childPath = childDir.getCanonicalPath();
                if (!childPath.startsWith(parentPath)) {
                    return -1;
                }

                String res = childPath.replace(parentPath, "");
                return res.split("/").length;
            } catch (Exception ignored) {
            }

            return -1;
        }


        private void preCancel() {
            if (_config._isShowResultOnCancel) {
                _isCanceled = true;
                return;
            }

            cancel(true);
        }


        private List<FitFile.ContentMatchUnit> getContentMatches(final File file, final boolean isFirstMatchOnly) {
            List<FitFile.ContentMatchUnit> ret = new ArrayList<>();

            if (!file.canRead() || file.isDirectory()) {
                return ret;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                int lineNumber = 0;
                for (String line; (line = br.readLine()) != null; ) {
                    if (isCancelled() || _isCanceled) {
                        break;
                    }

                    String preparedLine = _config._isCaseSensitiveQuery ? line : line.toLowerCase();
                    boolean isMatch = _config._isRegexQuery ? _regex.matcher(preparedLine).matches() : preparedLine.contains(_config._query);

                    if (isMatch) {
                        String matchPreview = _config._isShowMatchPreview ? line : "";
                        ret.add(new FitFile.ContentMatchUnit(lineNumber, matchPreview));

                        if (isFirstMatchOnly) {
                            break;
                        }
                    }
                    lineNumber++;
                }
            } catch (Exception ignored) {
            }

            return ret;
        }


        private boolean isFolderIgnored(File directory) {
            String dirName = directory.getName();

            for (Pattern pattern : SearchEngine.defaultIgnoredDirs) {
                if (pattern.matcher(dirName).matches()) {
                    return true;
                }
            }

            for (String pattern : _config._ignoredExactDirs) {
                if (dirName.equals(pattern)) {
                    return true;
                }
            }

            for (Pattern pattern : _config._ignoredRegexDirs) {
                if (pattern.matcher(dirName).matches()) {
                    return true;
                }
            }

            return false;
        }


        private boolean isFileIgnored(File file) {
            String fileName = file.getName();

            for (Pattern pattern : SearchEngine.defaultIgnoredFiles) {
                if (pattern.matcher(fileName).matches()) {
                    return true;
                }
            }

            for (String pattern : _config._ignoredExactFiles) {
                if (fileName.equals(pattern)) {
                    return true;
                }
            }

            for (Pattern pattern : _config._ignoredRegexFiles) {
                if (pattern.matcher(fileName).matches()) {
                    return true;
                }
            }

            return false;
        }


        private boolean isSearchByContentIgnoredFor(File file) {
            if (_config._searchByContentExtensions.isEmpty()) {
                return false;
            }

            String fileName = file.getName().toLowerCase();
            for (Pattern pattern : _config._searchByContentExtensions) {
                if (pattern.matcher(fileName).matches()) {
                    return false;
                }
            }

            return true;
        }

    }


}
