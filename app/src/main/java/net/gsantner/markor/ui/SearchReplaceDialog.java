/*#######################################################
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchReplaceDialog {

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

        final List<RecentReplace> recentReplaces = getRecentReplaces(activity);

        // TODO: Popup window for ComboBox
        // popupWindow.setAdapter(new);

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            final RecentReplace r = recentReplaces.get(position);
            searchText.setText(r.search);
            replaceText.setText(r.replace);
            regexCheckBox.setChecked(r.isRegex);
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
            dialog.get().dismiss();
        });

        replaceAll.setOnClickListener(button -> {
            performReplace(text, searchText.getText(), replaceText.getText(), regexCheckBox.isChecked(), true);
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

    private static List<RecentReplace> getRecentReplaces(final Activity activity) {
        return new ArrayList<>();
    }

    private static void saveRecentReplace(final Activity activity, final List<RecentReplace> replaces, final RecentReplace newReplace) {
    }

    private static class RecentReplace {
        public String search;
        public String replace;
        public boolean isRegex;
    }
}