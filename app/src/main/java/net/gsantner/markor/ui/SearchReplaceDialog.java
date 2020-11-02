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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchReplaceDialog {
    private static final String SEARCH_REPLACE_SETTINGS = "search_replace_dialog_settings";
    private static final String RECENT_SEARCH_REPLACE_STRING = "recent_search_replace";
    private static final int MAX_RECENT_SEARCH_REPLACE = 10;

    public static void showSearchReplaceDialog(final Activity activity, final TextView text) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View viewRoot = activity.getLayoutInflater().inflate(R.layout.search_replace_dialog, null);

        final AtomicReference<Dialog> dialog = new AtomicReference<>();

        final EditText searchText = viewRoot.findViewById(R.id.search_input);
        final EditText replaceText = viewRoot.findViewById(R.id.replace_input);
        final CheckBox regexCheckBox = viewRoot.findViewById(R.id.use_regex);
        final TextView matchState = viewRoot.findViewById(R.id.match_count_or_error);
        final Button replaceFirst = viewRoot.findViewById(R.id.replace_first);
        final Button replaceAll = viewRoot.findViewById(R.id.replace_all);

        final ListPopupWindow popupWindow = new ListPopupWindow(activity);

        final List<ReplaceGroup> replaceGroups = getRecentReplaces(activity);

        // Popup window for ComboBox
        popupWindow.setAdapter(new ArrayAdapter<ReplaceGroup>(activity, R.layout.search_replace_recent_layout, replaceGroups) {
            @NonNull
            @Override
            public View getView(int pos, @Nullable View view, @NonNull ViewGroup parent) {
                final View root = super.getView(pos, view, parent);

                final ReplaceGroup rg = getItem(pos);

                final TextView searchView = root.findViewById(R.id.search_replace_recent_search);
                final TextView replaceView = root.findViewById(R.id.search_replace_recent_replace);
                final TextView regexView = root.findViewById(R.id.search_replace_recent_regex);

                final Resources res = activity.getResources();

                searchView.setText(res.getString(R.string.recent_text_to_search_label, rg._search));
                replaceView.setText(res.getString(R.string.recent_text_to_replace_label, rg._replace));
                regexView.setText(res.getString(R.string.recent_is_regex_label, rg._isRegex));

                return root;
            }
        });

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            final ReplaceGroup r = replaceGroups.get(position);
            searchText.setText(r._search);
            replaceText.setText(r._replace);
            regexCheckBox.setChecked(r._isRegex);
            popupWindow.dismiss();
        });

        popupWindow.setAnchorView(replaceText);
        popupWindow.setModal(true);
        viewRoot.findViewById(R.id.recent_show_spinner).setOnClickListener(v -> popupWindow.show());

        final Resources res = activity.getResources();

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
                onChange(text, res, searchText, replaceText, regexCheckBox, matchState, replaceFirst, replaceAll);
            }
        };

        searchText.addTextChangedListener(textWatcher);
        replaceText.addTextChangedListener(textWatcher);

        regexCheckBox.setOnClickListener(
                v -> onChange(text, res, searchText, replaceText, regexCheckBox, matchState, replaceFirst, replaceAll)
        );

        replaceFirst.setOnClickListener(button -> {
            performReplace(text, searchText.getText(), replaceText.getText(), regexCheckBox.isChecked(), false);
            saveRecentReplace(activity, replaceGroups, new ReplaceGroup(searchText.getText(), replaceText.getText(), regexCheckBox.isChecked()));
            dialog.get().dismiss();
        });

        replaceAll.setOnClickListener(button -> {
            performReplace(text, searchText.getText(), replaceText.getText(), regexCheckBox.isChecked(), true);
            saveRecentReplace(activity, replaceGroups, new ReplaceGroup(searchText.getText(), replaceText.getText(), regexCheckBox.isChecked()));
            dialog.get().dismiss();
        });

        builder.setView(viewRoot).setNegativeButton(R.string.cancel, null);
        dialog.set(builder.show());
    }

    private static int[] getReplaceSel(final TextView text) {
        int[] sel = StringUtils.getSelection(text);

        // no selection, replace with all
        if (sel[0] == sel[1]) {
            sel[0] = 0;
            sel[1] = text.length();
        }

        return sel;
    }

    private static void performReplace(
            final TextView text,
            final CharSequence search,
            final CharSequence replace,
            final boolean regex,
            final boolean replaceAll)
    {
        final String replacement = replaceSel(text, search, replace, regex, replaceAll);
        final int[] sel = getReplaceSel(text);
        text.getEditableText().replace(sel[0], sel[1], replacement);
    }

    private static String replaceSel(
            final TextView text,
            final CharSequence search,
            final CharSequence replace,
            final boolean regex,
            final boolean replaceAll)
    {
        final Pattern sp = makePattern(search, regex);
        final int[] sel = getReplaceSel(text);
        final CharSequence section = text.getText().subSequence(sel[0], sel[1]);

        if (replaceAll) {
            return sp.matcher(section).replaceAll(replace.toString());
        } else {
            return sp.matcher(section).replaceFirst(replace.toString());
        }
    }


    private static Pattern makePattern(final CharSequence searchPattern, boolean useRegex) {
        if (useRegex) {
            return Pattern.compile(searchPattern.toString());
        } else {
            return Pattern.compile(searchPattern.toString(), Pattern.LITERAL);
        }
    }

    private static void onChange(
            final TextView text,
            final Resources res,
            final EditText search,
            final EditText replace,
            final CheckBox regex,
            final TextView state,
            final Button replaceFirst,
            final Button replaceAll)
    {
        boolean error = false;
        int count = 0;
        try {

            final Pattern sp = makePattern(search.getText().toString(), regex.isChecked());

            // Determine count
            final int[] sel = getReplaceSel(text);
            final CharSequence section = text.getText().subSequence(sel[0], sel[1]);
            final Matcher match = sp.matcher(section);
            while (match.find()) count++;

            // Run a replace to check if it works
            if (count > 0) {
                replaceSel(text, search.getText(), replace.getText(), regex.isChecked(), false);
            }

        } catch (PatternSyntaxException e) {
            error = true;
        }

        final boolean enabled = (count > 0) && !error;
        replaceFirst.setEnabled(enabled);
        replaceAll.setEnabled(enabled);

        if (error) {
            state.setText(res.getString(R.string.search_replace_pattern_error_message));
        } else {
            state.setText(String.format(res.getString(R.string.search_replace_count_message), count));
        }
    }

    private static List<ReplaceGroup> getRecentReplaces(final Activity activity) {
        final List<ReplaceGroup> recents = new ArrayList<>();
        try {
            final SharedPreferences settings = activity.getSharedPreferences(SEARCH_REPLACE_SETTINGS, Context.MODE_PRIVATE);
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

    private static void saveRecentReplace(final Activity activity, final List<ReplaceGroup> replaces, final ReplaceGroup newReplace) {
        List<ReplaceGroup> tempReplaces = new ArrayList<ReplaceGroup>(replaces);

        if (newReplace != null) {
            tempReplaces.add(0, newReplace);
        }

        tempReplaces = tempReplaces.subList(0, Math.min(tempReplaces.size(), MAX_RECENT_SEARCH_REPLACE));

        final JSONArray array = new JSONArray();
        for (final ReplaceGroup rg : tempReplaces) {
            array.put(rg.toJson());
        }

        final SharedPreferences.Editor edit = activity.getSharedPreferences(SEARCH_REPLACE_SETTINGS, Context.MODE_PRIVATE).edit();
        edit.putString(RECENT_SEARCH_REPLACE_STRING, array.toString()).apply();
    }

    private static class ReplaceGroup {
        public CharSequence _search = "";
        public CharSequence _replace = "";
        public boolean _isRegex = false;


        public ReplaceGroup(final CharSequence search, final CharSequence replace, final boolean isRegex) {
            _search = search;
            _replace = replace;
            _isRegex = isRegex;
        }

        public static ReplaceGroup fromJson(JSONObject obj) {
            try {
                return new ReplaceGroup(
                        obj.getString("search"),
                        obj.getString("replace"),
                        obj.getBoolean("isRegex"));
            } catch (JSONException e) {
                return null;
            }
        }

        public JSONObject toJson() {
            final JSONObject obj = new JSONObject();
            try {
                obj.put("search", _search);
                obj.put("replace", _replace);
                obj.put("isRegex", _isRegex);
            } catch (JSONException e) {
                // Do nothing
            }
            return obj;
        }
    }
}