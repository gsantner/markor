package net.gsantner.opoc.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;

import net.gsantner.opoc.util.Callback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")

public class SearchEngine {
    public static boolean isSearchExecuting = false;
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

        public final boolean isRegexQuery;
        public final boolean isCaseSensitiveQuery;
        public final String query;
        public final File rootSearchDir;
        public final boolean isSearchInContent;
        public final boolean isShowResultOnCancel;
        public final Integer maxSearchDepth;
        public final List<String> ignoredDirectories;
        public final List<String> ignoredFiles;

        public Config(final File rootSearchDir, String query, final boolean isShowResultOnCancel,
                      final Integer maxSearchDepth, final List<String> ignoredDirectories, final List<String> ignoredFiles,
                      final boolean isRegexQuery, final boolean isCaseSensitiveQuery, final boolean isSearchInContent) {

            this.rootSearchDir = rootSearchDir;
            this.isSearchInContent = isSearchInContent;
            this.isShowResultOnCancel = isShowResultOnCancel;
            this.maxSearchDepth = maxSearchDepth;
            this.ignoredDirectories = ignoredDirectories;
            this.ignoredFiles = ignoredFiles;
            this.isRegexQuery = isRegexQuery;
            this.isCaseSensitiveQuery = isCaseSensitiveQuery;

            this._ignoredExactDirs = new ArrayList<String>();
            this._ignoredRegexDirs = new ArrayList<Pattern>();
            splitRegexExactFiles(this.ignoredDirectories, this._ignoredExactDirs, this._ignoredRegexDirs);

            this._ignoredExactFiles = new ArrayList<String>();
            this._ignoredRegexFiles = new ArrayList<Pattern>();
            splitRegexExactFiles(this.ignoredFiles, this._ignoredExactFiles, this._ignoredRegexFiles);


            query = isRegexQuery ? query.replaceAll("(?<![.])[*]", ".*") : query;
            this.query = this.isCaseSensitiveQuery ? query : query.toLowerCase();
        }

        public Config(final File rootSearchDir, final String query, final boolean isShowResultOnCancel, final Integer maxSearchDepth, final List<String> ignoredDirectories, final List<String> ignoredFiles, final FileSearchDialog.Options.SearchConfigOptions configOptions) {
            this(rootSearchDir, query, isShowResultOnCancel, maxSearchDepth, ignoredDirectories, ignoredFiles, configOptions.isRegexQuery, configOptions.isCaseSensitiveQuery, configOptions.isSearchInContent);
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
                    regexList.add(Pattern.compile(pattern));
                }
            }
        }
    }


    public static SearchEngine.QueueSearchFilesTask queueFileSearch(SearchEngine.Config config, Callback.a1<List<String>> callback) {

        SearchEngine.isSearchExecuting = true;
        SearchEngine.QueueSearchFilesTask task = new SearchEngine.QueueSearchFilesTask(config, callback);
        task.execute();

        return task;
    }


    public static class QueueSearchFilesTask extends AsyncTask<Void, Integer, List<String>> {
        private SearchEngine.Config _config;
        private final Callback.a1<List<String>> _callback;
        private final Pattern _regex;

        private Snackbar _snackBar;
        private Integer _countCheckedFiles;
        private Integer _currentQueueLength;
        private boolean _isCanceled;
        private Integer _currentSearchDepth;
        private List<String> _result;

        public QueueSearchFilesTask(final SearchEngine.Config config, final Callback.a1<List<String>> callback) {
            _config = config;
            _callback = callback;
            _regex = _config.isRegexQuery ? Pattern.compile(_config.query) : null;

            _countCheckedFiles = 0;
            _isCanceled = false;
            _currentSearchDepth = 0;
            _currentQueueLength = 1;

            _result = new ArrayList<>();
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bindSnackBar(_config.query);
        }


        public void bindSnackBar(String text) {
            if (!SearchEngine.isSearchExecuting) {
                return;
            }

            try {
                // getActivity() + onDismissed = fix snackBar disappearing on screen rotation
                View view = getActivity().findViewById(android.R.id.content);
                _snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                bindSnackBar(text);
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
        protected List<String> doInBackground(Void... voidp) {
            Queue<File> mainQueue = new LinkedList<File>();
            mainQueue.add(_config.rootSearchDir);

            while (!mainQueue.isEmpty() && !isCancelled() && !_isCanceled) {
                File currentDirectory = mainQueue.remove();

                if (!currentDirectory.canRead() || currentDirectory.isFile()) {
                    continue;
                }

                _currentSearchDepth = getDirectoryDepth(_config.rootSearchDir, currentDirectory);
                if (_currentSearchDepth > _config.maxSearchDepth) {
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
                        if (isFolderIgnored(subDirOrFile) || isFileContainSymbolicLinks(subDirOrFile, currentDir)) {
                            continue;
                        }

                        subQueue.add(subDirOrFile);
                    } else {
                        if (isFileIgnored(subDirOrFile)) {
                            continue;
                        }

                        if (_config.isSearchInContent && isFileContainSearchQuery(subDirOrFile)) {
                            String path = subDirOrFile.getCanonicalPath().replace(_config.rootSearchDir.getCanonicalPath() + "/", "");
                            _result.add(path);
                        }
                    }

                    getFileIfNameMatches(subDirOrFile);

                    publishProgress(_currentQueueLength + subQueue.size(), _currentSearchDepth, _result.size(), _countCheckedFiles);
                }
            } catch (Exception ex) {
                ;
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
            String snackBarText = "f:" + filesFound + " qu:" + queueLength + "|" + queueDepth + " c:" + countCheckedFiles + "\n" + _config.query;
            if (_snackBar != null) {
                _snackBar.setText(snackBarText);
            }
        }


        @Override
        protected void onPostExecute(List<String> ret) {
            super.onPostExecute(ret);
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
            } catch (Exception ex) {
                ;
            }

            return true;
        }


        private void getFileIfNameMatches(File file) {
            try {
                String fileName = _config.isCaseSensitiveQuery ? file.getName() : file.getName().toLowerCase();
                boolean isMatch = _config.isRegexQuery ? _regex.matcher(fileName).matches() : fileName.contains(_config.query);

                if (isMatch) {
                    String path = file.getCanonicalPath().replace(_config.rootSearchDir.getCanonicalPath() + "/", "");
                    _result.add(path);
                }
            } catch (Exception ex) {
                ;
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
            } catch (Exception ex) {
                ;
            }

            return -1;
        }


        private void preCancel() {
            if (_config.isShowResultOnCancel) {
                _isCanceled = true;
                return;
            }

            cancel(true);
        }


        private boolean isFileContainSearchQuery(File file) {
            boolean ret = false;

            if (!file.canRead() || file.isDirectory()) {
                return ret;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                for (String line; (line = br.readLine()) != null; ) {
                    if (isCancelled() || _isCanceled) {
                        break;
                    }

                    line = _config.isCaseSensitiveQuery ? line : line.toLowerCase();
                    boolean isMatch = _config.isRegexQuery ? _regex.matcher(line).matches() : line.contains(_config.query);
                    if (isMatch) {
                        ret = true;
                        break;
                    }
                }
            } catch (Exception ex) {
                ;
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


        private static Activity getActivity() throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;

            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    return activity;
                }
            }

            return null;
        }
    }


}
