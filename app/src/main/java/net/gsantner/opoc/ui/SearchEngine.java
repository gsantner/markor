package net.gsantner.opoc.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;

import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.Callback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")


public class SearchEngine {
    public static class Config {
        public final Activity Activity;
        public final File SearchDir;
        public final String Query;
        public final boolean IsSearchInFiles;
        public final boolean IsShowResultOnCancel;
        public final Integer MaxSearchDepth;
        private final boolean _isRegex;

        public Config(Activity activity, File searchDir, String query, boolean isSearchInFiles, boolean isShowResultOnCancel, Integer maxSearchDepth){
            Activity = activity;
            SearchDir = searchDir;
            IsSearchInFiles = isSearchInFiles;
            IsShowResultOnCancel = isShowResultOnCancel;
            MaxSearchDepth = maxSearchDepth;

            query = query.replaceAll("(?<![.])[*]", ".*");
            _isRegex = query.startsWith("^") || query.contains("*");
            Query = _isRegex ? query : query.toLowerCase();
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
        private final WeakReference<Activity> _activityRef;
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
            _regex = _config._isRegex ? Pattern.compile(_config.Query) : null;
            _activityRef = new WeakReference<>(_config.Activity);

            _countCheckedFiles = 0;
            _isCanceled = false;
            _searchDepth = 0;

            _result = new ArrayList<>();
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (_activityRef.get() != null) {
                _snackBar = Snackbar.make(_activityRef.get().findViewById(android.R.id.content), _config.Query + "...", Snackbar.LENGTH_INDEFINITE);
                _snackBar.setAction(android.R.string.cancel, (v) -> {
                    _snackBar.dismiss();
                    preCancel();
                }).show();
            }
        }


        private void preCancel(){
            if(_config.IsShowResultOnCancel){
                _isCanceled = true;
                return;
            }

            cancel(true);
        }


        private void getFilesByEqualsFileNames(File currentDir){
            try {
                if(!currentDir.canRead() || currentDir.isFile()){
                    return;
                }

                File[] files = currentDir.listFiles();

                for (int i = 0; i < files.length; i++) {
                    if(isCancelled() || _isCanceled){
                        break;
                    }

                    _countCheckedFiles++;
                    File file = files[i];
                    String fileName = file.getName();
                    boolean isMatch = _config._isRegex ? _regex.matcher(fileName).matches() : fileName.toLowerCase().contains(_config.Query);

                    if(isMatch){
                        String path = file.getCanonicalPath().replace(_config.SearchDir.getCanonicalPath() + "/", "");
                        _result.add(path);
                    };
                }
            } catch (Exception ex){
                ;
            }
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
                    boolean isMatch = _config._isRegex ? _regex.matcher(line).matches() : line.toLowerCase().contains(_config.Query);
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


        private void getFilesByContext(File currentDir){
            try{
                if(!currentDir.canRead()){
                    return;
                }

                File[] files = currentDir.listFiles();

                for (int i = 0; i < files.length; i++) {
                    if(isCancelled() || _isCanceled){
                        break;
                    }

                    File file = files[i];
                    _countCheckedFiles++;
                    publishProgress(_queueLength, _searchDepth, _result.size(), _countCheckedFiles);
                    if(!file.canRead() || file.isDirectory()){
                        continue;
                    }

                    if(isFileContainSearchQuery(file)){
                        String path = file.getCanonicalPath().replace(_config.SearchDir.getCanonicalPath() + "/", "");
                        _result.add(path);
                    }
                }
            } catch (Exception ex){
                ;
            }
        }


        @Override
        protected List<String> doInBackground(Void... voidp) {
            Queue<File> queue = new LinkedList<File>();
            queue.add(_config.SearchDir);

            while (!queue.isEmpty() && !isCancelled() && !_isCanceled && _searchDepth <= _config.MaxSearchDepth) {
                _searchDepth++;
                _queueLength = queue.size();
                publishProgress(_queueLength, _searchDepth, _result.size(), _countCheckedFiles);
                File currentDirectory = queue.remove();


                if(!currentDirectory.canRead() || currentDirectory.isFile()){
                    continue;
                }

                getFilesByEqualsFileNames(currentDirectory);

                if(_config.IsSearchInFiles){
                    getFilesByContext(currentDirectory);
                }

                try{
                    File[] files = currentDirectory.listFiles();
                    for(int i = 0; i < files.length; i++){
                        File file = files[i];

                        if(!file.canRead() || file.isFile()){
                            continue;
                        }

                        // ignore symbolic links
                        File parentFile =  file.getCanonicalFile().getParentFile();
                        if(parentFile == null || !currentDirectory.getCanonicalPath().equals(parentFile.getCanonicalPath())){
                            continue;
                        }

                        queue.add(file);
                    }

                    _queueLength = queue.size();
                    publishProgress(_queueLength, _searchDepth, _result.size(), _countCheckedFiles);
                } catch (Exception ex){
                    ;
                }
            }

            if(_isCanceled && _result.size() == 0){
                cancel(true);
            }

            return _result;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            Integer queueLength = values[0];
            Integer queueDepth = values[1];
            Integer filesFound = values[2];
            Integer countCheckedFiles = values[3];
            String snackBarText = _config.Query + "...(" + filesFound + ") qu:" + queueLength + "|" + queueDepth + " c:" + countCheckedFiles;
            _snackBar.setText(snackBarText);
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
            new ActivityUtils(_activityRef.get()).hideSoftKeyboard().freeContextRef();
        }
    }
}
