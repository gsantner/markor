package net.gsantner.markor.frontend.filesearch;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

@SuppressWarnings("WeakerAccess")

public class FileSearchEngine {
    public static final AtomicBoolean isSearchExecuting = new AtomicBoolean(false);
    public static final AtomicReference<WeakReference<Activity>> activity = new AtomicReference<>();

    private static final List<String> defaultIgnoredDirs = Arrays.asList("^\\.git$", "^\\.tmp$", ".*[Tt]humb.*");
    private static final int maxPreviewLength = 100;
    public static final int maxQueryHistoryCount = 20;
    public static final LinkedList<String> queryHistory = new LinkedList<>();

    public static void addToHistory(String query) {
        queryHistory.remove(query);

        if (queryHistory.size() == maxQueryHistoryCount) {
            queryHistory.removeLast();
        }
        queryHistory.addFirst(query);
    }

    public static class SearchOptions {
        public File rootSearchDir;
        public String query;

        public boolean isRegexQuery;
        public boolean isCaseSensitiveQuery;
        public boolean isSearchInContent;
        public boolean isOnlyFirstContentMatch;

        public int maxSearchDepth;
        public List<String> ignoredDirectories;
        public boolean isShowMatchPreview = true;
        public boolean isShowResultOnCancel = true;
        public char[] password = new char[0];
        public int message = 0;
    }

    public static class FitFile {
        public final String path;
        public final boolean isDirectory;
        public final List<Pair<String, Integer>> children;

        public FitFile(final String path, final boolean isDirectory, final List<Pair<String, Integer>> lineNumbers) {
            // Directories have a trailing slash
            this.path = path + (isDirectory && !path.endsWith("/") ? "/" : "");
            this.isDirectory = isDirectory;
            this.children = Collections.unmodifiableList(lineNumbers != null ? lineNumbers : Collections.emptyList());
        }

        @NonNull
        @Override
        public String toString() {
            return (children.size() > 0 ? String.format("(%s) ", children.size()) : "") + path;
        }
    }

    public static FileSearchEngine.QueueSearchFilesTask queueFileSearch(
            @NonNull final Activity activity,
            final SearchOptions config,
            final GsCallback.a1<List<FitFile>> callback
    ) {
        FileSearchEngine.activity.set(new WeakReference<>(activity));
        FileSearchEngine.isSearchExecuting.set(true);
        FileSearchEngine.addToHistory(config.query);
        FileSearchEngine.QueueSearchFilesTask task = new FileSearchEngine.QueueSearchFilesTask(config, callback);
        task.execute();

        return task;
    }

    public static class QueueSearchFilesTask extends AsyncTask<Void, Integer, List<FitFile>> {
        private final SearchOptions _config;
        private final GsCallback.a1<List<FitFile>> _callback;

        // _matcher.reset() is _not_ thread safe. Will need alternate approach when we make search parallel
        private final Matcher _matcher;

        private Snackbar _snackBar;
        private Integer _countCheckedFiles = 0;
        private Integer _currentQueueLength = 1;
        private boolean _isCanceled = false;
        private Integer _currentSearchDepth = 0;
        private final List<FitFile> _result = new ArrayList<>();
        private final Set<Matcher> _ignoredRegexDirs = new HashSet<>();
        private final Set<String> _ignoredExactDirs = new HashSet<>();

        public QueueSearchFilesTask(final SearchOptions config, final GsCallback.a1<List<FitFile>> callback) {
            _config = config;
            _callback = callback;

            _config.query = _config.isCaseSensitiveQuery ? _config.query : _config.query.toLowerCase();
            splitRegexExactFiles(config.ignoredDirectories, _ignoredExactDirs, _ignoredRegexDirs);
            splitRegexExactFiles(FileSearchEngine.defaultIgnoredDirs, _ignoredExactDirs, _ignoredRegexDirs);

            Pattern pattern = null;
            if (_config.isRegexQuery) {
                try {
                    _config.query = _config.query.replaceAll("(?<![.])[*]", ".*");
                    pattern = Pattern.compile(_config.query);
                } catch (Exception ex) {
                    final Activity a = activity.get().get();
                    if (a != null) {
                        final String errorMessage = a.getString(R.string.regex_can_not_be_compiled) + ": " + _config.query;
                        Toast.makeText(a, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
            _matcher = pattern != null ? pattern.matcher("") : null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (_config.isRegexQuery && _matcher == null) {
                cancel(true);
                return;
            }
            bindSnackBar(_config.query);
        }

        public void bindSnackBar(String text) {
            if (!FileSearchEngine.isSearchExecuting.get()) {
                return;
            }

            try {
                final View view = activity.get().get().findViewById(android.R.id.content);
                _snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);
                _snackBar.addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (FileSearchEngine.isSearchExecuting.get()) {
                                    bindSnackBar(text);
                                }
                            }
                        })
                        .setAction(android.R.string.cancel, (v) -> {
                            _snackBar.dismiss();
                            preCancel();
                        })
                        .show();
            } catch (Exception ignored) {
                cancel(true);
            }
        }

        @Override
        protected List<FitFile> doInBackground(Void... voidp) {
            Queue<File> mainQueue = new LinkedList<>();
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
            Collections.sort(_result, (o1, o2) -> o1.path.compareToIgnoreCase(o2.path));
            return _result;
        }

        private Queue<File> currentDirectoryHandler(final File currentDir) {
            final Queue<File> subQueue = new LinkedList<>();

            try {
                if (!currentDir.canRead() || currentDir.isFile()) {
                    return subQueue;
                }

                final File[] subDirsOrFiles = currentDir.listFiles();
                final int trimSize = _config.rootSearchDir.getCanonicalPath().length() + 1;

                for (final File f : (subDirsOrFiles != null ? subDirsOrFiles : new File[0])) {

                    if (isCancelled() || _isCanceled) {
                        break;
                    }
                    _countCheckedFiles++;

                    if (!isIgnored(f.getName())) {

                        final boolean isDir = f.isDirectory();

                        final int beforeContentCount = _result.size();
                        if (_config.isSearchInContent && !isDir && f.canRead() && GsFileUtils.isTextFile(f)) {
                            getContentMatches(f, _config.isOnlyFirstContentMatch, trimSize);
                        }

                        // Search name if director or not already included due to content
                        if (isDir || _result.size() == beforeContentCount) {
                            getFileIfNameMatches(f, trimSize);
                        }

                        if (isDir && !isFileContainSymbolicLinks(f, currentDir)) {
                            subQueue.add(f);
                        }
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
            if (_snackBar != null) {
                // _currentQueueLength, _currentSearchDepth, _result.size(), _countCheckedFiles
                _snackBar.setText("⭕" + values[2] + " || \uD83D\uDD0D" + values[0] + " || ⬇️ " + values[1] + " || \uD83D\uDC41️" + values[3] + "\n" + _config.query);
            }
        }

        @Override
        protected void onPostExecute(List<FitFile> ret) {
            super.onPostExecute(ret);
            FileSearchEngine.isSearchExecuting.set(false);
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
            FileSearchEngine.isSearchExecuting.set(false);
        }

        private void splitRegexExactFiles(final List<String> list, final Set<String> exactList, final Set<Matcher> regexList) {
            for (String pattern : (list != null ? list : new ArrayList<String>())) {
                if (pattern.isEmpty()) {
                    continue;
                }
                if (!_config.isCaseSensitiveQuery) {
                    pattern = pattern.toLowerCase();
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
                        regexList.add(Pattern.compile(pattern).matcher(""));
                    } catch (Exception ex) {
                        final Activity a = activity.get().get();
                        if (a != null) {
                            final String errorMessage = a.getString(R.string.regex_can_not_be_compiled) + ": " + pattern;
                            Toast.makeText(a, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
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

        private void getFileIfNameMatches(final File file, final int baseLength) {
            try {
                final String fileName = _config.isCaseSensitiveQuery ? file.getName() : file.getName().toLowerCase();
                if (_config.isRegexQuery ? _matcher.reset(fileName).matches() : fileName.contains(_config.query)) {
                    _result.add(new FitFile(file.getCanonicalPath().substring(baseLength), file.isDirectory(), null));
                }
            } catch (Exception ignored) {
            }
        }

        private int getDirectoryDepth(File parentDir, File childDir) {
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
            if (_config.isShowResultOnCancel) {
                _isCanceled = true;
                return;
            }

            cancel(true);
        }

        // Match line and return preview string. Preview will be null if no match found
        private String matchLine(final String line) {
            final String preparedLine = _config.isCaseSensitiveQuery ? line : line.toLowerCase();

            int start = -1, end = -1;
            if (_config.isRegexQuery) {
                if (_matcher.reset(preparedLine).find()) {
                    start = _matcher.start();
                    end = _matcher.end();
                }
            } else {
                start = preparedLine.indexOf(_config.query);
                if (start >= 0) {
                    end = start + _config.query.length();
                }
            }

            // Preview is based on original line
            if (start >= 0 && end <= line.length()) {
                if (!_config.isShowMatchPreview) {
                    return "";
                }
                if (line.length() < maxPreviewLength) {
                    return line;
                } else {
                    int offset = (maxPreviewLength - (end - start)) / 2;
                    int subStart = Math.max(start - offset, 0);
                    int subEnd = Math.min(end + offset, line.length());
                    return String.format("… %s …", line.substring(subStart, subEnd));
                }
            }
            return null;
        }

        private void getContentMatches(final File file, final boolean isFirstMatchOnly, final int trim) {
            List<Pair<String, Integer>> contentMatches = null;

            try (final BufferedReader br = new BufferedReader(new InputStreamReader(getInputStream(file)))) {
                int lineNumber = 0;
                for (String line; (line = br.readLine()) != null; ) {
                    if (isCancelled() || _isCanceled) {
                        break;
                    }
                    line = matchLine(line);
                    if (line != null) {

                        // We lazily create the match list
                        // And therefore avoid creating it for _every_ file
                        if (contentMatches == null) {
                            contentMatches = new ArrayList<>();

                            final String path = file.getCanonicalPath().substring(trim);
                            _result.add(new FitFile(path, false, contentMatches));
                        }

                        // Note that content matches is only created on the first find
                        contentMatches.add(new Pair<>(line, lineNumber));

                        if (isFirstMatchOnly) {
                            break;
                        }
                    }
                    lineNumber++;
                }
            } catch (Exception ignored) {
            }
        }

        private boolean isIgnored(String dirName) {
            dirName = _config.isCaseSensitiveQuery ? dirName : dirName.toLowerCase();
            for (final String pattern : _ignoredExactDirs) {
                if (dirName.equals(pattern)) {
                    return true;
                }
            }

            for (final Matcher matcher : _ignoredRegexDirs) {
                if (matcher.reset(dirName).matches()) {
                    return true;
                }
            }
            return false;
        }

        private InputStream getInputStream(File file) throws FileNotFoundException {
            if (isEncryptedFile(file)) {
                final byte[] encryptedContext = GsFileUtils.readCloseStreamWithSize(new FileInputStream(file), (int) file.length());
                return new ByteArrayInputStream(JavaPasswordbasedCryption.getDecryptedText(encryptedContext, _config.password.clone()).getBytes(StandardCharsets.UTF_8));
            } else {
                return new FileInputStream(file);
            }
        }
    }


    private static boolean isEncryptedFile(File file) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && file.getName().endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
    }
}
