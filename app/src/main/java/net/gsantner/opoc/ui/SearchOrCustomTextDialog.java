/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class SearchOrCustomTextDialog {

    public static class DialogOptions {
        public Callback.a1<String> callback;
        public Callback.a2<String, Integer> withPositionCallback;
        public List<? extends CharSequence> data;
        public List<? extends CharSequence> highlightData;
        public List<Integer> iconsForData;
        public String messageText = "";
        public String defaultText = "";
        public boolean isSearchEnabled = true;
        public boolean isDarkDialog = false;
        public int dialogWidthDp = WindowManager.LayoutParams.MATCH_PARENT;
        public int dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
        public int gravity = Gravity.NO_GRAVITY;
        public int searchInputType = 0;
        public boolean searchIsRegex = false;
        public Callback.a1<Spannable> highlighter;
        public String extraFilter = null;

        public Callback.a0 neutralButtonCallback = null;

        @ColorInt
        public int textColor = 0xFF000000;
        @ColorInt
        public int highlightColor = 0xFF00FF00;
        @StringRes
        public int cancelButtonText = android.R.string.cancel;
        @StringRes
        public int neutralButtonText = 0;
        @StringRes
        public int okButtonText = android.R.string.ok;
        @StringRes
        public int titleText = 0;
        @StringRes
        public int searchHintText = android.R.string.search_go;
    }

    private static class WithPositionAdapter extends ArrayAdapter<Pair<String, Integer>> {

        final LayoutInflater mInflater;
        final @LayoutRes
        int mLayout;
        final DialogOptions dopt;
        final List<Pair<String, Integer>> filteredItems;
        final Pattern extraPattern;

        WithPositionAdapter(Context context, @LayoutRes int layout, List<Pair<String, Integer>> filteredItems, DialogOptions dopt) {
            super(context, layout, filteredItems);
            mInflater = LayoutInflater.from(context);
            mLayout = layout;
            this.dopt = dopt;
            this.filteredItems = filteredItems;
            extraPattern = dopt.extraFilter == null ? null : Pattern.compile(dopt.extraFilter);
        }

        @NonNull
        @Override
        public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
            final Pair<String, Integer> item = getItem(pos);
            final String text = item.first;
            final int posInOriginalList = item.second;

            final TextView textView;
            if (convertView == null) {
                textView = (TextView) mInflater.inflate(mLayout, parent, false);
            } else {
                textView = (TextView) convertView;
            }

            if (posInOriginalList >= 0 && dopt.iconsForData != null && posInOriginalList < dopt.iconsForData.size() && dopt.iconsForData.get(posInOriginalList) != 0) {
                textView.setCompoundDrawablesWithIntrinsicBounds(dopt.iconsForData.get(posInOriginalList), 0, 0, 0);
                textView.setCompoundDrawablePadding(32);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    textView.setCompoundDrawableTintList(ColorStateList.valueOf(dopt.isDarkDialog ? Color.WHITE : Color.BLACK));
                }
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

            if (dopt.highlightData != null) {
                final boolean hl = dopt.highlightData.contains(text);
                textView.setTextColor(hl ? dopt.highlightColor : dopt.textColor);
                textView.setTypeface(null, hl ? Typeface.BOLD : Typeface.NORMAL);
            }

            if (dopt.highlighter != null) {
                Spannable s = new SpannableString(text);
                dopt.highlighter.callback(s);
                textView.setText(s);
            } else {
                textView.setText(text);
            }

            return textView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(final CharSequence constraint, final FilterResults results) {
                    filteredItems.clear();
                    filteredItems.addAll((List<Pair<String, Integer>>) results.values);
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    final ArrayList<Pair<CharSequence, Integer>> resList = new ArrayList<>();

                    if (dopt.data != null) {
                        final String fil = constraint.toString();
                        final boolean emptySearch = fil.isEmpty();
                        for (int i = 0; i < dopt.data.size(); i++) {
                            final CharSequence str = dopt.data.get(i);
                            final boolean matchExtra = (extraPattern == null) || extraPattern.matcher(str).find();
                            final boolean matchNormal = str.toString().toLowerCase(Locale.getDefault()).contains(fil.toLowerCase(Locale.getDefault()));
                            final boolean matchRegex = dopt.searchIsRegex && (str.toString().matches(fil));
                            if (matchExtra && (matchNormal || matchRegex || emptySearch)) {
                                resList.add(new Pair<>(str, i));
                            }
                        }
                    }

                    final FilterResults res = new FilterResults();
                    res.values = resList;
                    res.count = resList.size();
                    return res;
                }
            };
        }
    }

    public static void showMultiChoiceDialogWithSearchFilterUI(final Activity activity, final DialogOptions dopt) {
        final List<Pair<String, Integer>> filteredItems = new ArrayList<>();
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, dopt.isDarkDialog
                ? android.support.v7.appcompat.R.style.Theme_AppCompat_Dialog
                : android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog
        );
        final WithPositionAdapter listAdapter = new WithPositionAdapter(activity, android.R.layout.simple_list_item_1, filteredItems, dopt);

        final AppCompatEditText searchEditText = new AppCompatEditText(activity);
        searchEditText.setText(dopt.defaultText);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(dopt.textColor);
        searchEditText.setHintTextColor((dopt.textColor & 0x00FFFFFF) | 0x99000000);
        searchEditText.setHint(dopt.searchHintText);
        searchEditText.setInputType(dopt.searchInputType == 0 ? searchEditText.getInputType() : dopt.searchInputType);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable arg0) {
                listAdapter.getFilter().filter(searchEditText.getText());
            }

            @Override
            public void onTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
            }

            @Override
            public void beforeTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
            }
        });

        final ListView listView = new ListView(activity);
        final LinearLayout linearLayout = new LinearLayout(activity);
        listView.setAdapter(listAdapter);
        listView.setVisibility(dopt.data != null && !dopt.data.isEmpty() ? View.VISIBLE : View.GONE);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        if (dopt.isSearchEnabled) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int px = (int) (new ContextUtils(listView.getContext()).convertDpToPx(8));
            lp.setMargins(px, px / 2, px, px / 2);
            linearLayout.addView(searchEditText, lp);
        }

        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        layoutParams.weight = 1;
        linearLayout.addView(listView, layoutParams);
        if (!TextUtils.isEmpty(dopt.messageText)) {
            dialogBuilder.setMessage(dopt.messageText);
        }

        dialogBuilder.setView(linearLayout)
                .setOnCancelListener(null)
                .setNegativeButton(dopt.cancelButtonText, (dialogInterface, i) -> dialogInterface.dismiss());

        if (dopt.neutralButtonCallback != null && dopt.neutralButtonText != 0) {
            dialogBuilder.setNeutralButton(dopt.neutralButtonText, (dialogInterface, i) -> {
                dopt.neutralButtonCallback.callback();
            });
        }

        if (dopt.titleText != 0) {
            dialogBuilder.setTitle(dopt.titleText);
        }

        if (dopt.isSearchEnabled) {
            dialogBuilder.setPositiveButton(dopt.okButtonText, (dialogInterface, i) -> {
                dialogInterface.dismiss();
                if (dopt.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                    dopt.callback.callback(searchEditText.getText().toString());
                }
            });
        }

        final AlertDialog dialog = dialogBuilder.create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            if (dopt.callback != null) {
                dopt.callback.callback(filteredItems.get(position).first);
            }
            if (dopt.withPositionCallback != null) {
                final Pair<String, Integer> item = filteredItems.get(position);
                dopt.withPositionCallback.callback(item.first, item.second);
            }
        });

        searchEditText.setOnKeyListener((keyView, keyCode, keyEvent) -> {
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                dialog.dismiss();
                if (dopt.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                    dopt.callback.callback(searchEditText.getText().toString());
                }
                return true;
            }
            return false;
        });

        Window w;
        if ((w = dialog.getWindow()) != null && dopt.isSearchEnabled) {
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
        if ((w = dialog.getWindow()) != null) {
            int ds_w = dopt.dialogWidthDp < 100 ? dopt.dialogWidthDp : ((int) (dopt.dialogWidthDp * activity.getResources().getDisplayMetrics().density));
            int ds_h = dopt.dialogHeightDp < 100 ? dopt.dialogHeightDp : ((int) (dopt.dialogHeightDp * activity.getResources().getDisplayMetrics().density));
            w.setLayout(ds_w, ds_h);
        }

        if ((w = dialog.getWindow()) != null && dopt.gravity != Gravity.NO_GRAVITY) {
            WindowManager.LayoutParams wlp = w.getAttributes();
            wlp.gravity = dopt.gravity;
            w.setAttributes(wlp);
        }

        if (dopt.isSearchEnabled) {
            searchEditText.requestFocus();
        }
        if (dopt.defaultText != null) {
            listAdapter.getFilter().filter(searchEditText.getText());
        }
    }


    public static SearchFilesTask recursiveFileSearch(Activity activity, File searchDir, String query, Callback.a1<List<String>> callback) {
        query = query.replaceAll("(?<![.])[*]", ".*");
        SearchFilesTask task = new SearchFilesTask(activity, searchDir, query, callback, query.startsWith("^") || query.contains("*"));
        task.execute();
        return task;
    }

    public static class SearchFilesTask extends AsyncTask<Void, File, List<String>> implements IOFileFilter {
        private final Callback.a1<List<String>> _callback;
        private final File _searchDir;
        private final String _query;
        private final boolean _isRegex;
        private final WeakReference<Activity> _activityRef;

        private final Pattern _regex;
        private Snackbar _snackBar;

        public SearchFilesTask(Activity activity, File searchDir, String query, Callback.a1<List<String>> callback, boolean isRegex) {
            _searchDir = searchDir;
            _query = isRegex ? query : query.toLowerCase();
            _callback = callback;
            _isRegex = isRegex;
            _regex = isRegex ? Pattern.compile(_query) : null;
            _activityRef = new WeakReference<>(activity);
        }

        // Called for both, file and folder filter
        @Override
        public boolean accept(File file) {
            return isMatching(file, true);
        }

        // Not called
        @Override
        public boolean accept(File dir, String name) {
            return isMatching(new File(dir, name), true);
        }

        // In iterateFilesAndDirs, subdirs are only scanned when returning true on it
        // But those dirs will also occur in iterator
        // Hence call this aagain with alwaysMatchDir=false
        public boolean isMatching(File file, boolean alwaysMatchDir) {
            if (file.isDirectory()) {
                // Do never scan .git directories, lots of files, lots of time
                if (file.getName().equals(".git")) {
                    return false;
                }
                if (alwaysMatchDir) {
                    return true;
                }
            }
            String name = file.getName();
            file = file.getParentFile();
            return _isRegex ? _regex.matcher(name).matches() : name.toLowerCase().contains(_query);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (_activityRef.get() != null) {
                _snackBar = Snackbar.make(_activityRef.get().findViewById(android.R.id.content), _query + "...", Snackbar.LENGTH_INDEFINITE);
                _snackBar.setAction(android.R.string.cancel, (v) -> {
                    _snackBar.dismiss();
                    cancel(true);
                }).show();
            }
        }

        @Override
        protected List<String> doInBackground(Void... voidp) {
            List<String> ret = new ArrayList<>();

            boolean first = true;
            Iterator<File> iter = null;
            try {
                iter = FileUtils.iterateFilesAndDirs(_searchDir, this, this);
            } catch (Exception ex) {
                // Iterator may throw an error at creation
                return ret;
            }
            while (iter.hasNext() && !isCancelled()) {
                File f = iter.next();
                if (first) {
                    first = false;
                    if (f.equals(_searchDir)) {
                        continue;
                    }
                }
                if (f.isFile() || (f.isDirectory() && isMatching(f, false))) {
                    ret.add(f.getAbsolutePath().replace(_searchDir.getAbsolutePath() + "/", ""));
                }
            }
            return ret;
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
