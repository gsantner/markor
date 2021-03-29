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
        private final boolean _isRegexQuery;
        private final List<Pattern> _ignoredRegexDirs;
        private final List<String> _ignoredExactDirs;
        private final List<Pattern> _ignoredRegexFiles;
        private final List<String> _ignoredExactFiles;

        public final File RootSearchDir;
        public final String Query;
        public final boolean IsSearchInFiles;
        public final boolean IsShowResultOnCancel;
        public final Integer MaxSearchDepth;
        public final List<String> IgnoredDirectories;
        public final List<String> IgnoredFiles;

        public Config(File rootSearchDir, String query, boolean isSearchInFiles, boolean isShowResultOnCancel,
                      Integer maxSearchDepth, List<String> ignoredDirectories, List<String> ignoredFiles){
            RootSearchDir = rootSearchDir;
            IsSearchInFiles = isSearchInFiles;
            IsShowResultOnCancel = isShowResultOnCancel;
            MaxSearchDepth = maxSearchDepth;
            IgnoredDirectories = ignoredDirectories;
            IgnoredFiles = ignoredFiles;

            _ignoredExactDirs = new ArrayList<String>();
            _ignoredRegexDirs = new ArrayList<Pattern>();
            splitRegexExactFiles(IgnoredDirectories, _ignoredExactDirs, _ignoredRegexDirs);

            _ignoredExactFiles = new ArrayList<String>();
            _ignoredRegexFiles = new ArrayList<Pattern>();
            splitRegexExactFiles(IgnoredFiles, _ignoredExactFiles, _ignoredRegexFiles);


            query = query.replaceAll("(?<![.])[*]", ".*");
            _isRegexQuery = query.startsWith("^") || query.contains("*");
            Query = _isRegexQuery ? query : query.toLowerCase();
        }

        private void splitRegexExactFiles(List<String> list, List<String> exactList, List<Pattern> regexList){
            for (Integer i = 0; i < list.size(); i++) {
                String pattern = list.get(i);
                if(pattern.isEmpty()){
                    continue;
                }

                if(pattern.startsWith("\"")){
                    pattern = pattern.replace("\"", "");
                    if(pattern.isEmpty()){
                        continue;
                    }
                    exactList.add(pattern);
                }
                else{
                    pattern = pattern.replaceAll("(?<![.])[*]", ".*");
                    regexList.add(Pattern.compile(pattern));
                }
            }
        }
    }


    public static SearchEngine.QueueSearchFilesTask QueueFileSearch(SearchEngine.Config config, Callback.a1<List<String>> callback) {

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
        private Integer _queueLength;
        private boolean _isCanceled;
        private Integer _searchDepth;
        private List<String> _result;

        public QueueSearchFilesTask(SearchEngine.Config config, Callback.a1<List<String>> callback) {
            _config = config;
            _callback = callback;
            _regex = _config._isRegexQuery ? Pattern.compile(_config.Query) : null;

            _countCheckedFiles = 0;
            _isCanceled = false;
            _searchDepth = 0;
            _queueLength = 1;

            _result = new ArrayList<>();
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bindSnackBar(_config.Query);
        }


        public void bindSnackBar(String text) {
            if(!SearchEngine.isSearchExecuting){
                return;
            }

            try {
                // getActivity() + onDismissed = fix snackBar disappearing on screen rotation
                View view = getActivity().findViewById(android.R.id.content);
                _snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE)
                        .addCallback(new Snackbar.Callback() {
                            @Override public void onDismissed(Snackbar snackbar, int event) {
                                bindSnackBar(text);
                            }
                        });
                _snackBar.setAction(android.R.string.cancel, (v) -> {
                    _snackBar.dismiss();
                    preCancel();
                });
                _snackBar.show();
            }
            catch (Exception ex){
                ;
            }
        }

        @Override
        protected List<String> doInBackground(Void... voidp) {
            Queue<File> mainQueue = new LinkedList<File>();
            mainQueue.add(_config.RootSearchDir);

            while (!mainQueue.isEmpty() && !isCancelled() && !_isCanceled) {
                File currentDirectory = mainQueue.remove();

                if(!currentDirectory.canRead() || currentDirectory.isFile()){
                    continue;
                }

                _searchDepth = GetDirectoryDepth(_config.RootSearchDir, currentDirectory);
                if(_searchDepth > _config.MaxSearchDepth){
                    break;
                }
                _queueLength = mainQueue.size()+1;
                publishProgress(_queueLength, _searchDepth, _result.size(), _countCheckedFiles);

                mainQueue.addAll(currentDirectoryHandler(currentDirectory));
            }

            if(_isCanceled && _result.size() == 0){
                cancel(true);
            }

            return _result;
        }


        private  Queue<File> currentDirectoryHandler(File currentDir){
            Queue<File> subQueue = new LinkedList<File>();

            try{
                if(!currentDir.canRead() || currentDir.isFile()){
                    return subQueue;
                }

                File[] subDirsOrFiles = currentDir.listFiles();
                for(int i = 0; i < subDirsOrFiles.length && !isCancelled() && !_isCanceled; i++) {
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

                        if (_config.IsSearchInFiles && isFileContainSearchQuery(subDirOrFile)) {
                            String path = subDirOrFile.getCanonicalPath().replace(_config.RootSearchDir.getCanonicalPath() + "/", "");
                            _result.add(path);
                        }
                    }

                    getFileIfNameMatches(subDirOrFile);

                    publishProgress(_queueLength + subQueue.size(), _searchDepth, _result.size() , _countCheckedFiles);
                }
            } catch (Exception ex){
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
            String snackBarText = "f:" + filesFound + " qu:" + queueLength + "|" + queueDepth + " c:" + countCheckedFiles + "\n" + _config.Query;
            if(_snackBar != null) {
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


        private boolean isFileContainSymbolicLinks(File file, File expectedParentDir){
            try {
                File realParentDir = file.getCanonicalFile().getParentFile();
                if (realParentDir != null && expectedParentDir.getCanonicalPath().equals(realParentDir.getCanonicalPath())) {
                    return false;
                }
            }
            catch (Exception ex){
                ;
            }

            return true;
        }


        private void getFileIfNameMatches(File file){
            try {
                String fileName = file.getName();
                boolean isMatch = _config._isRegexQuery ? _regex.matcher(fileName).matches() : fileName.toLowerCase().contains(_config.Query);

                if (isMatch) {
                    String path = file.getCanonicalPath().replace(_config.RootSearchDir.getCanonicalPath() + "/", "");
                    _result.add(path);
                }
            }
            catch (Exception ex){
                ;
            }
        }


        private Integer GetDirectoryDepth(File parentDir, File childDir){
            try {
                String parentPath = parentDir.getCanonicalPath();
                String childPath = childDir.getCanonicalPath();
                if (!childPath.startsWith(parentPath)) {
                    return -1;
                }

                String res = childPath.replace(parentPath, "");
                return res.split("/").length;
            }
            catch (Exception ex){
                ;
            }

            return -1;
        }


        private void preCancel(){
            if(_config.IsShowResultOnCancel){
                _isCanceled = true;
                return;
            }

            cancel(true);
        }


        private boolean isFileContainSearchQuery(File file){
            boolean ret = false;

            if(!file.canRead() || file.isDirectory()){
                return ret;
            }

            try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                for(String line; (line = br.readLine()) != null; ) {
                    if(isCancelled() || _isCanceled){
                        break;
                    }
                    boolean isMatch = _config._isRegexQuery ? _regex.matcher(line).matches() : line.toLowerCase().contains(_config.Query);
                    if(isMatch){
                        ret = true;
                        break;
                    }
                }
            }
            catch (Exception ex){
                ;
            }

            return ret;
        }


        private boolean isFolderIgnored(File directory){
            String dirName = directory.getName();

            for(Pattern pattern : SearchEngine.defaultIgnoredDirs){
                if(pattern.matcher(dirName).matches()){
                    return true;
                }
            }

            for(String pattern : _config._ignoredExactDirs){
                if(dirName.equals(pattern)){
                    return true;
                }
            }

            for(Pattern pattern : _config._ignoredRegexDirs){
                if(pattern.matcher(dirName).matches()){
                    return true;
                }
            }

            return false;
        }


        private boolean isFileIgnored(File file){
            String fileName = file.getName();

            for(Pattern pattern : SearchEngine.defaultIgnoredFiles){
                if(pattern.matcher(fileName).matches()){
                    return true;
                }
            }

            for(String pattern : _config._ignoredExactFiles){
                if(fileName.equals(pattern)){
                    return true;
                }
            }

            for(Pattern pattern : _config._ignoredRegexFiles){
                if(pattern.matcher(fileName).matches()){
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
