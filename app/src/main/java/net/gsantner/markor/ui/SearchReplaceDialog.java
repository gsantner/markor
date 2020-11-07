/*#######################################################
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchReplaceDialog {

    private static final String SEARCH_REPLACE_SETTINGS = "search_replace_dialog_settings";
    private static final String RECENT_SEARCH_REPLACE_STRING = "recent_search_replace";
    private static final int MAX_RECENT_SEARCH_REPLACE = 10;

    private final EditText searchText;
    private final EditText replaceText;
    private final CheckBox regexCheckBox;
    private final CheckBox multilineCheckBox;
    private final TextView matchState;
    private final Button replaceFirst;
    private final Button replaceAll;

    private final Activity _activity;
    private final TextView _text;

    private final int[] sel;
    private final CharSequence region;

    private final List<ReplaceGroup> recentReplaces;

    public static void showSearchReplaceDialog(final Activity activity, final TextView text) {
        new SearchReplaceDialog(activity, text);
    }

    private SearchReplaceDialog(final Activity activity, final TextView text) {

        _activity = activity;
        _text = text;

        final Resources res = activity.getResources();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View viewRoot = activity.getLayoutInflater().inflate(R.layout.search_replace_dialog, null);
        final AtomicReference<Dialog> dialog = new AtomicReference<>();

        searchText = viewRoot.findViewById(R.id.search_input);
        replaceText = viewRoot.findViewById(R.id.replace_input);
        regexCheckBox = viewRoot.findViewById(R.id.use_regex);
        multilineCheckBox = viewRoot.findViewById(R.id.multiline);
        matchState = viewRoot.findViewById(R.id.match_count_or_error);
        replaceFirst = viewRoot.findViewById(R.id.replace_first);
        replaceAll = viewRoot.findViewById(R.id.replace_all);

        recentReplaces = loadRecentReplaces();

        // Set region for replace
        sel = StringUtils.getSelection(_text);
        // no selection, replace with all
        if (sel[0] == sel[1]) {
            sel[0] = 0;
            sel[1] = text.length();
        }
        region = _text.getText().subSequence(sel[0], sel[1]);

        final ListPopupWindow popupWindow = new ListPopupWindow(activity);

        // Popup window for ComboBox
        popupWindow.setAdapter(new ArrayAdapter<ReplaceGroup>(activity, android.R.layout.simple_list_item_1, recentReplaces) {
            @NonNull
            @Override
            public View getView(int pos, @Nullable View view, @NonNull ViewGroup parent) {
                final TextView textView = (TextView) super.getView(pos, view, parent);

                if (pos >= 0 && pos < recentReplaces.size()) {
                    final ReplaceGroup rg = recentReplaces.get(pos);
                    final String desc = String.format("%s: %s\n%s: %s\n%s: %b, %s: %b\n",
                            res.getString(R.string.search_for), rg._search,
                            res.getString(R.string.replace_with), rg._replace,
                            res.getString(R.string.use_regex), rg._isRegex,
                            res.getString(R.string.multiline_regex), rg._isMultiline);
                    textView.setText(desc);
                }

                return textView;
            }
        });

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            final ReplaceGroup r = recentReplaces.get(position);
            searchText.setText(r._search);
            replaceText.setText(r._replace);
            regexCheckBox.setChecked(r._isRegex);
            multilineCheckBox.setChecked(r._isMultiline);
            updateUI();
            popupWindow.dismiss();
        });

        popupWindow.setAnchorView(viewRoot.findViewById(R.id.search_replace_text_group));
        popupWindow.setModal(true);
        viewRoot.findViewById(R.id.recent_show_spinner).setOnClickListener(v -> popupWindow.show());

        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateUI();
            }
        };

        searchText.addTextChangedListener(textWatcher);
        replaceText.addTextChangedListener(textWatcher);

        regexCheckBox.setOnClickListener(
                v -> updateUI()
        );

        multilineCheckBox.setOnClickListener(
                v -> updateUI()
        );

        replaceFirst.setOnClickListener(button -> {
            performReplace(false);
            saveRecentReplace();
            dialog.get().dismiss();
        });

        replaceAll.setOnClickListener(button -> {
            performReplace(true);
            saveRecentReplace();
            dialog.get().dismiss();
        });

        updateUI();

        builder.setView(viewRoot).setNegativeButton(R.string.cancel, null);
        dialog.set(builder.show());
    }

    private void performReplace(final boolean replaceAll) {
        try {
            final String replacement = getReplacement(replaceAll);
            _text.getEditableText().replace(sel[0], sel[1], replacement);
        } catch (IllegalArgumentException e) {
            // Do not perform replacement
        }
    }

    private String getReplacement(final boolean replaceAll) {
        final Pattern sp = makePattern();

        if (replaceAll) {
            return sp.matcher(region).replaceAll(getReplacePattern());
        } else {
            return sp.matcher(region).replaceFirst(getReplacePattern());
        }
    }

    private Pattern makePattern() {
        if (regexCheckBox.isChecked()) {
            if (multilineCheckBox.isChecked()) {
                return Pattern.compile(searchText.getText().toString(), Pattern.MULTILINE);
            } else {
                return Pattern.compile(searchText.getText().toString());
            }
        } else {
            return Pattern.compile(searchText.getText().toString(), Pattern.LITERAL);
        }
    }

    private String getReplacePattern() {
        if (regexCheckBox.isChecked()) {
            return StringUtils.unescapeString(replaceText.getText().toString());
        } else {
            return replaceText.getText().toString();
        }
    }

    private void updateUI() {
        boolean error = false;
        int count = 0;

        if (searchText.length() > 0) {
            try {

                final Pattern sp = makePattern();

                // Determine count
                final CharSequence section = _text.getText().subSequence(sel[0], sel[1]);
                final Matcher match = sp.matcher(section);
                while (match.find()) count++;

                // Run a replace to check if it works
                if (count > 0) {
                    getReplacement(false);
                }

            } catch (IllegalArgumentException e) {
                error = true;
            }
        }

        final boolean enabled = (count > 0) && !error;
        replaceFirst.setEnabled(enabled);
        replaceAll.setEnabled(enabled);

        multilineCheckBox.setEnabled(regexCheckBox.isChecked());

        final Resources res = _activity.getResources();

        if (error) {
            matchState.setText(res.getString(R.string.search_replace_pattern_error_message));
        } else {
            matchState.setText(String.format(res.getConfiguration().locale, "%s: %d", res.getString(R.string.found_matches), count));
        }
    }

    private List<ReplaceGroup> loadRecentReplaces() {
        final List<ReplaceGroup> recents = new ArrayList<>();
        try {
            final SharedPreferences settings = _activity.getSharedPreferences(SEARCH_REPLACE_SETTINGS, Context.MODE_PRIVATE);
            final String jsonString = settings.getString(RECENT_SEARCH_REPLACE_STRING, "");
            final JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                recents.add(ReplaceGroup.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // Do nothing
        }
        return recents;
    }

    private void saveRecentReplace() {
        final ArrayList<ReplaceGroup> newReplaces = new ArrayList<>();
        final Set<String> dedupSet = new HashSet<>();

        final ReplaceGroup newReplace = new ReplaceGroup(
                searchText.getText(), replaceText.getText(), regexCheckBox.isChecked(), multilineCheckBox.isChecked());

        newReplaces.add(newReplace);
        dedupSet.add(newReplace.key());

        // -1 as we have just added the newReplace
        final int addCount = Math.min(recentReplaces.size(), MAX_RECENT_SEARCH_REPLACE - 1);
        for (int i = 0; i < addCount; i++) {
            final ReplaceGroup rg = recentReplaces.get(i);
            if (!dedupSet.contains(rg.key())) {
                newReplaces.add(rg);
                dedupSet.add(rg.key());
            }
        }

        final JSONArray array = new JSONArray();
        for (final ReplaceGroup rg: newReplaces) {
            array.put(rg.toJson());
        }

        final SharedPreferences.Editor edit = _activity.getSharedPreferences(SEARCH_REPLACE_SETTINGS, Context.MODE_PRIVATE).edit();
        edit.putString(RECENT_SEARCH_REPLACE_STRING, array.toString()).apply();
    }

    private static class ReplaceGroup {
        final public CharSequence _search;
        final public CharSequence _replace;
        final public boolean _isRegex;
        final public boolean _isMultiline;

        public String key() {
            return String.format("%s%s%b%b", _search, _replace, _isRegex, _isMultiline);
        }

        public ReplaceGroup(
                final CharSequence search,
                final CharSequence replace,
                final boolean isRegex,
                final boolean isMultiline
        ) {
            _search = search;
            _replace = replace;
            _isRegex = isRegex;
            _isMultiline = isMultiline;
        }

        public static ReplaceGroup fromJson(JSONObject obj) {
            try {
                return new ReplaceGroup(
                        obj.getString("search"),
                        obj.getString("replace"),
                        obj.getBoolean("isRegex"),
                        obj.getBoolean("isMultiline"));
            } catch (JSONException e) {
                return new ReplaceGroup("", "", false, false);
            }
        }

        public JSONObject toJson() {
            final JSONObject obj = new JSONObject();
            try {
                obj.put("search", _search);
                obj.put("replace", _replace);
                obj.put("isRegex", _isRegex);
                obj.put("isMultiline", _isMultiline);
            } catch (JSONException e) {
                // Do nothing
            }
            return obj;
        }
    }
}