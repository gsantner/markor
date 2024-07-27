/*#######################################################
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.gsantner.markor.R;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;

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

public class SearchAndReplaceTextDialog {
    private static final String RECENT_SEARCH_REPLACE_STRING = "search_replace_dialog__recent_search_replace_history";
    private static final int MAX_RECENT_SEARCH_REPLACE = 10;

    private final EditText searchText;
    private final EditText replaceText;
    private final CheckBox regexCheckBox;
    private final CheckBox multilineCheckBox;
    private final TextView matchState;
    private final Button replaceFirst;
    private final Button replaceAll;

    private final Activity _activity;
    private final Editable _edit;

    private final int[] _sel;
    private final int cursorPosition;
    private final CharSequence region;

    private static final ReplaceGroup[] DEFAULT_GROUPS = {
            // Delete trailing spaces
            new ReplaceGroup("[^\\S\\n\\r]+$", "", true, true),
            // Delete empty lines
            new ReplaceGroup("\\n\\s*\\n", "\\n", true, false),
            // Uncheck all checkboxes (Markdown)
            new ReplaceGroup("^(\\s*[-\\*]) \\[[xX]\\]", "$1 [ ]", true, true),
            // Check all checkboxes (Markdown)
            new ReplaceGroup("^(\\s*[-\\*]) \\[\\s\\]", "$1 [x]", true, true),
    };

    private final List<ReplaceGroup> recentReplaces;

    public static void showSearchReplaceDialog(final Activity activity, final Editable edit, final int[] sel) {
        new SearchAndReplaceTextDialog(activity, edit, sel);
    }

    private SearchAndReplaceTextDialog(final Activity activity, final Editable edit, final int[] sel) {

        _activity = activity;
        _edit = edit;

        if (sel != null && sel.length == 2) {
            final int start = Math.min(Math.max(0, Math.min(sel[0], sel[1])), _edit.length());
            final int end = Math.min(Math.max(0, Math.max(sel[0], sel[1])), _edit.length());

            if (start == end) {
                _sel = new int[]{0, edit.length()};
                cursorPosition = start;
            } else {
                cursorPosition = -1;
                _sel = new int[]{start, end};
            }
        } else {
            _sel = new int[]{0, edit.length()};
            cursorPosition = -1;
        }

        if (_sel[0] != 0 || _sel[1] != _edit.length()) {
            region = _edit.subSequence(_sel[0], _sel[1]);
        } else {
            region = _edit;
        }

        final Resources res = activity.getResources();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_DayNight_Dialog_Rounded);
        final View viewRoot = activity.getLayoutInflater().inflate(R.layout.search_replace_dialog, null);
        final AtomicReference<Dialog> dialog = new AtomicReference<>();

        searchText = viewRoot.findViewById(R.id.search_input);
        replaceText = viewRoot.findViewById(R.id.replace_input);
        regexCheckBox = viewRoot.findViewById(R.id.enable_regex);
        multilineCheckBox = viewRoot.findViewById(R.id.multiline);
        matchState = viewRoot.findViewById(R.id.match_count_or_error);
        replaceFirst = viewRoot.findViewById(R.id.replace_once);
        replaceAll = viewRoot.findViewById(R.id.replace_all);

        recentReplaces = loadRecentReplaces();

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
                            res.getString(R.string.regex), rg._isRegex,
                            res.getString(R.string.multiline), rg._isMultiline);
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

        //noinspection DataFlowIssue
        dialog.get().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void performReplace(final boolean replaceAll) {
        try {
            final String replacement = getReplacement(replaceAll);
            _edit.replace(_sel[0], _sel[1], replacement);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // Do not perform replacement
        }
    }

    private String getReplacement(final boolean replaceAll) {
        final Pattern sp = makePattern();

        if (replaceAll) {
            return sp.matcher(region).replaceAll(getReplacePattern());
        } else {
            final Matcher match = sp.matcher(region);
            // Handle case
            if (cursorPosition > 0 && cursorPosition < region.length()) {
                if (match.find(cursorPosition)) {
                    final CharSequence before = region.subSequence(0, cursorPosition);
                    final CharSequence after = region.subSequence(cursorPosition, region.length());
                    return before + sp.matcher(after).replaceFirst(getReplacePattern());
                }
            }
            return match.replaceFirst(getReplacePattern());
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
        return GsTextUtils.unescapeString(replaceText.getText().toString());
    }

    private void updateUI() {
        boolean error = false;
        int count = 0;

        if (searchText.length() > 0) {
            try {

                final Pattern sp = makePattern();

                // Determine count
                final Matcher match = sp.matcher(region);
                while (match.find()) count++;

                // Run a replace to check if it works
                if (count > 0) {
                    getReplacement(false);
                }

            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
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
            matchState.setText(String.format(res.getConfiguration().locale, "%s: %d", res.getString(R.string.matches), count));
        }
    }

    private List<ReplaceGroup> loadRecentReplaces() {
        final List<ReplaceGroup> recents = new ArrayList<>();

        try {
            final SharedPreferences settings = _activity.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
            final String jsonString = settings.getString(RECENT_SEARCH_REPLACE_STRING, "");
            if (jsonString.length() > 0) {
                final JSONArray array = new JSONArray(jsonString);
                for (int i = 0; i < array.length(); i++) {
                    recents.add(ReplaceGroup.fromJson(array.getJSONObject(i)));
                }
            }
        } catch (JSONException e) {
            // Do nothing
        }

        // Append recents if not already in group.
        for (final ReplaceGroup rg : DEFAULT_GROUPS) {
            if (!recents.contains(rg)) {
                recents.add(rg);
            }
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
        for (final ReplaceGroup rg : newReplaces) {
            array.put(rg.toJson());
        }

        final SharedPreferences.Editor edit = _activity.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE).edit();
        edit.putString(RECENT_SEARCH_REPLACE_STRING, array.toString()).apply();
    }

    private static class ReplaceGroup {
        final public CharSequence _search;
        final public CharSequence _replace;
        final public boolean _isRegex;
        final public boolean _isMultiline;

        private final static String SEARCH_KEY = "search";
        private final static String REPLACE_KEY = "replace";
        private final static String REGEX_KEY = "regex";
        private final static String MULTILINE_KEY = "multiline";

        public String key() {
            return String.format("%s%s%b%b", _search, _replace, _isRegex, _isMultiline);
        }

        @Override
        public boolean equals(Object other) {
            return (other instanceof ReplaceGroup && this.key().equals(((ReplaceGroup) other).key()));
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
                        obj.getString(SEARCH_KEY),
                        obj.getString(REPLACE_KEY),
                        obj.getBoolean(REGEX_KEY),
                        obj.getBoolean(MULTILINE_KEY));
            } catch (JSONException e) {
                return new ReplaceGroup("", "", false, false);
            }
        }

        public JSONObject toJson() {
            final JSONObject obj = new JSONObject();
            try {
                obj.put(SEARCH_KEY, _search);
                obj.put(REPLACE_KEY, _replace);
                obj.put(REGEX_KEY, _isRegex);
                obj.put(MULTILINE_KEY, _isMultiline);
            } catch (JSONException e) {
                // Do nothing
            }
            return obj;
        }
    }
}