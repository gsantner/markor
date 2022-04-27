/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui.hleditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.TooltipCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.Utils;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.DatetimeFormatDialog;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.plaintext.PlainTextStuff;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;
import net.gsantner.opoc.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public abstract class TextActions {
    protected HighlightingEditor _hlEditor;
    protected Document _document;
    protected Activity _activity;
    protected Context _context;
    protected AppSettings _appSettings;
    protected ActivityUtils _au;
    private final int _textActionSidePadding;
    protected int _indent;
    private String _lastSnip;

    public static final String ACTION_ORDER_PREF_NAME = "action_order";
    private static final String ORDER_SUFFIX = "_order";
    private static final String DISABLED_SUFFIX = "_disabled";

    public TextActions(final Activity activity, final Document document) {
        _document = document;
        _activity = activity;
        _au = new ActivityUtils(activity);
        _context = activity != null ? activity : _hlEditor.getContext();
        _appSettings = new AppSettings(_context);
        _textActionSidePadding = (int) (_appSettings.getEditorTextActionItemPadding() * _context.getResources().getDisplayMetrics().density);
        _indent = _appSettings.getDocumentIndentSize(_document != null ? _document.getPath() : null);
    }

    // Override to implement custom onClick
    public boolean onActionClick(final @StringRes int action) {
        return runCommonTextAction(action);
    }

    // Override to implement custom onLongClick
    public boolean onActionLongClick(final @StringRes int action) {
        return runCommonLongPressTextActions(action);
    }

    // Override to implement custom search action
    public boolean onSearch() {
        SearchOrCustomTextDialogCreator.showSearchDialog(_activity, _hlEditor);
        return true;
    }

    // Override to implement custom title action
    public boolean runTitleClick() {
        return false;
    }

    /**
     * Derived classes must return a unique StringRes id.
     * This is used to extract the appropriate action order preference.
     *
     * @return StringRes preference key
     */
    @StringRes
    protected abstract int getFormatActionsKey();

    /**
     * Derived classes must return a List of ActionItem. One for each action they want to implement.
     *
     * @return List of ActionItems
     */
    protected abstract List<ActionItem> getActiveActionList();

    /**
     * These will not be added to the actions list.
     *
     * @return List of keyId strings.
     */
    public List<String> getDisabledActions() {
        return loadActionPreference(DISABLED_SUFFIX);
    }

    /**
     * Map every string Action identifier -> ActionItem
     *
     * @return Map of String key -> Action
     */
    public Map<String, ActionItem> getActiveActionMap() {
        List<ActionItem> actionList = getActiveActionList();
        List<String> keyList = getActiveActionKeys();

        Map<String, ActionItem> map = new HashMap<String, ActionItem>();

        for (int i = 0; i < actionList.size(); i++) {
            map.put(keyList.get(i), actionList.get(i));
        }
        return map;
    }

    /**
     * Get string for every ActionItem.keyId defined by getActiveActionList
     *
     * @return List or resource strings
     */
    public List<String> getActiveActionKeys() {
        List<ActionItem> actionList = getActiveActionList();
        ArrayList<String> keys = new ArrayList<String>();

        Resources res = _activity.getResources();
        for (ActionItem item : actionList) keys.add(res.getString(item.keyId));

        return keys;
    }

    /**
     * Save an action order to preferences.
     * The Preference is derived from the key returned by getFormatActionsKey
     * <p>
     * Keys are joined into a comma separated list before saving.
     *
     * @param keys of keys (in order) to save
     */
    public void saveDisabledActions(final List<String> keys) {
        saveActionPreference(DISABLED_SUFFIX, keys);
    }

    /**
     * Save an action order to preferences.
     * The Preference is derived from the key returned by getFormatActionsKey
     * <p>
     * Keys are joined into a comma separated list before saving.
     *
     * @param keys of keys (in order) to save
     */
    public void saveActionOrder(final List<String> keys) {
        saveActionPreference(ORDER_SUFFIX, keys);
    }

    private void saveActionPreference(final String suffix, List<String> values) {
        // Remove any values not in current actions
        values = new ArrayList<>(values);
        values.retainAll(getActiveActionKeys());

        SharedPreferences settings = _activity.getSharedPreferences(ACTION_ORDER_PREF_NAME, Context.MODE_PRIVATE);
        String formatKey = _activity.getResources().getString(getFormatActionsKey()) + suffix;
        settings.edit().putString(formatKey, TextUtils.join(",", values)).apply();
    }

    private List<String> loadActionPreference(final String suffix) {
        String formatKey = _activity.getResources().getString(getFormatActionsKey()) + suffix;
        SharedPreferences settings = _activity.getSharedPreferences(ACTION_ORDER_PREF_NAME, Context.MODE_PRIVATE);
        String combinedKeys = settings.getString(formatKey, null);
        List<String> values = Collections.emptyList();
        if (combinedKeys != null) {
            values = new ArrayList<String>(Arrays.asList(combinedKeys.split(",")));
        }
        return values;
    }

    /**
     * Get the ordered list of preference keys.
     * <p>
     * This routine does the following:
     * 1. Extract list of currently defined actions
     * 2. Extract saved action-order-list (Comma separated) from preferences
     * 3. Split action order list into list of action keys
     * 4. Remove action keys which are no longer present in currently defined actions from the preference list
     * 5. Add new actions which are not in the preference list to the preference list
     * 6. If changes were made (i.e. actions have been added or removed), re-save the preference list
     *
     * @return List of Action Item keys in order specified by preferences
     */
    public List<String> getActionOrder() {

        ArrayList<String> definedKeys = new ArrayList<>(getActiveActionKeys());
        List<String> prefKeys = new ArrayList<>(loadActionPreference(ORDER_SUFFIX));

        // Handle the case where order was stored without suffix. i.e. before this release.
        if (prefKeys.size() == 0) {
            prefKeys = new ArrayList<>(loadActionPreference(""));
        }

        Set<String> prefSet = new LinkedHashSet<>(prefKeys);
        Set<String> defSet = new LinkedHashSet<>(definedKeys);

        // Add any defined keys which are not in prefs
        defSet.removeAll(prefSet);
        prefKeys.addAll(defSet);

        // Remove any pref keys which are not defined
        prefSet.removeAll(definedKeys);
        prefKeys.removeAll(prefSet);

        if (defSet.size() > 0 || prefSet.size() > 0) {
            saveActionOrder(prefKeys);
        }

        return prefKeys;
    }

    public void appendTextActionsToBar(ViewGroup barLayout) {
        if (barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            final Map<String, ActionItem> map = getActiveActionMap();
            final List<String> orderedKeys = getActionOrder();
            final Set<String> disabledKeys = new HashSet<>(getDisabledActions());
            for (final String key : orderedKeys) {
                if (!disabledKeys.contains(key)) {
                    final ActionItem action = map.get(key);
                    appendTextActionToBar(barLayout, action.iconId, action.stringId, action.keyId);
                }
            }
        }
    }

    protected void appendTextActionToBar(ViewGroup barLayout, @DrawableRes int iconRes, @StringRes int descRes, @StringRes int actionKey) {
        final ImageView btn = (ImageView) _activity.getLayoutInflater().inflate(R.layout.quick_keyboard_button, null);
        btn.setImageResource(iconRes);
        btn.setContentDescription(_activity.getString(descRes));
        TooltipCompat.setTooltipText(btn, _activity.getString(descRes));
        btn.setOnClickListener(v -> {
            try {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                onActionClick(actionKey);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btn.setOnLongClickListener(v -> {
            try {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                return onActionLongClick(actionKey);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        });
        final int sidePadding = _textActionSidePadding + btn.getPaddingLeft(); // Left and right are symmetrical
        btn.setPadding(sidePadding, btn.getPaddingTop(), sidePadding, btn.getPaddingBottom());
        barLayout.addView(btn);
    }

    protected void setBarVisible(ViewGroup barLayout, boolean visible) {
        if (barLayout.getId() == R.id.document__fragment__edit__text_actions_bar && barLayout.getParent() instanceof HorizontalScrollView) {
            ((HorizontalScrollView) barLayout.getParent()).setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    protected void runRegularPrefixAction(String action) {
        runRegularPrefixAction(action, null, false);
    }

    protected void runRegularPrefixAction(String action, Boolean ignoreIndent) {
        runRegularPrefixAction(action, null, ignoreIndent);
    }

    protected void runRegularPrefixAction(String action, String replaceString) {
        runRegularPrefixAction(action, replaceString, false);
    }

    protected void runRegularPrefixAction(final String action, final String replaceString, final Boolean ignoreIndent) {

        String replacement = (replaceString == null) ? "" : replaceString;

        String patternIndent = ignoreIndent ? "(^\\s*)" : "(^)";
        String replaceIndent = "$1";

        String escapedAction = String.format("\\Q%s\\E", action);
        String escapedReplace = String.format("(\\Q%s\\E)?", replacement);

        ReplacePattern[] patterns = {
                // Replace action with replacement
                new ReplacePattern(patternIndent + escapedAction, replaceIndent + replacement),
                // Replace replacement or nothing with action
                new ReplacePattern(patternIndent + escapedReplace, replaceIndent + action),
        };

        runRegexReplaceAction(Arrays.asList(patterns));
    }

    public static class ReplacePattern {
        public final Pattern searchPattern;
        public final String replacePattern;
        public final boolean replaceAll;

        /**
         * Construct a ReplacePattern
         *
         * @param searchPattern  regex search pattern
         * @param replacePattern replace string
         * @param replaceAll     whether to replace all or just the first
         */
        public ReplacePattern(Pattern searchPattern, String replacePattern, boolean replaceAll) {
            this.searchPattern = searchPattern;
            this.replacePattern = replacePattern;
            this.replaceAll = replaceAll;
        }

        public ReplacePattern(String searchPattern, String replacePattern, boolean replaceAll) {
            this(Pattern.compile(searchPattern), replacePattern, replaceAll);
        }

        public ReplacePattern(Pattern searchPattern, String replacePattern) {
            this(searchPattern, replacePattern, false);
        }

        public ReplacePattern(String searchPattern, String replacePattern) {
            this(Pattern.compile(searchPattern), replacePattern, false);
        }
    }

    public void runRegexReplaceAction(final ReplacePattern... patterns) {
        runRegexReplaceAction(Arrays.asList(patterns), false);
    }

    public void runRegexReplaceAction(final List<ReplacePattern> patterns) {
        runRegexReplaceAction(patterns, false);
    }

    public void runRegexReplaceAction(final String pattern, final String replace) {
        runRegexReplaceAction(Arrays.asList(new ReplacePattern(pattern, replace)), false);
    }

    public void runRegexReplaceAction(final List<ReplacePattern> patterns, final boolean matchAll) {
        runRegexReplaceAction(_hlEditor, patterns, matchAll);
    }

    public static void runRegexReplaceAction(final EditText editor, final ReplacePattern... patterns) {
        runRegexReplaceAction(editor, Arrays.asList(patterns), false);
    }

    /**
     * Runs through a sequence of regex-search-and-replace actions on each selected line.
     * This function wraps _runRegexReplaceAction with a call to disable text trackers
     *
     * @param patterns An array of ReplacePattern
     * @param matchAll Whether to stop matching subsequent ReplacePatterns after first match+replace
     */
    public static void runRegexReplaceAction(final EditText editor, final List<ReplacePattern> patterns, final boolean matchAll) {
        if (editor instanceof HighlightingEditor) {
            ((HighlightingEditor) editor).withAutoFormatDisabled(() -> _runRegexReplaceAction(editor, patterns, matchAll));
        } else {
            _runRegexReplaceAction(editor, patterns, matchAll);
        }
    }

    private static void _runRegexReplaceAction(final EditText editor, final List<ReplacePattern> patterns, final boolean matchAll) {

        final Editable text = editor.getText();
        final int[] selection = StringUtils.getSelection(editor);
        final int[] lStart = StringUtils.getLineOffsetFromIndex(text, selection[0]);
        final int[] lEnd = StringUtils.getLineOffsetFromIndex(text, selection[1]);

        int lineStart = StringUtils.getLineStart(text, selection[0]);
        int selEnd = StringUtils.getLineEnd(text, selection[1]);

        while (lineStart <= selEnd && lineStart <= text.length()) {

            final int lineEnd = StringUtils.getLineEnd(text, lineStart, selEnd);
            final CharSequence line = text.subSequence(lineStart, lineEnd);

            for (final ReplacePattern pattern : patterns) {

                final Matcher searcher = pattern.searchPattern.matcher(line);

                // Find matched region
                int matchStart = line.length();
                int matchEnd = -1;
                while (searcher.find()) {
                    matchStart = Math.min(matchStart, searcher.start());
                    matchEnd = Math.max(matchEnd, searcher.end());

                    if (!pattern.replaceAll) break; // Limit region based on search type
                }

                if (matchEnd >= matchStart) { // Will be true iff at least one match has been found
                    if (!pattern.replacePattern.equals("$0")) {
                        final CharSequence oldRegion = line.subSequence(matchStart, matchEnd);
                        // Have to create a new matcher, unfortunately, as replace does not respect region
                        final Matcher replacer = pattern.searchPattern.matcher(oldRegion);
                        final String newRegion = pattern.replaceAll ? replacer.replaceAll(pattern.replacePattern) : replacer.replaceFirst(pattern.replacePattern);
                        text.replace(matchStart + lineStart, matchEnd + lineStart, newRegion);
                        // Change effective selection based on update
                        selEnd += newRegion.length() - oldRegion.length();
                    }

                    if (!matchAll) break; // Exit after first match
                }
            }

            lineStart = StringUtils.getLineEnd(text, lineStart, selEnd) + 1;
        }

        editor.setSelection(
                StringUtils.getIndexFromLineOffset(text, lStart),
                StringUtils.getIndexFromLineOffset(text, lEnd));
    }

    protected void runInlineAction(String _action) {
        if (_hlEditor.getText() == null) {
            return;
        }
        if (_hlEditor.hasSelection()) {
            String text = _hlEditor.getText().toString();
            int selectionStart = _hlEditor.getSelectionStart();
            int selectionEnd = _hlEditor.getSelectionEnd();

            //Check if Selection includes the shortcut characters
            if (selectionEnd < text.length() && selectionStart >= 0 && (text.substring(selectionStart, selectionEnd)
                    .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                text = text.substring(selectionStart + _action.length(),
                        selectionEnd - _action.length());
                _hlEditor.getText()
                        .replace(selectionStart, selectionEnd, text);

            }
            //Check if Selection is Preceded and succeeded by shortcut characters
            else if (((selectionEnd <= (_hlEditor.length() - _action.length())) &&
                    (selectionStart >= _action.length())) &&
                    (text.substring(selectionStart - _action.length(),
                            selectionEnd + _action.length())
                            .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                text = text.substring(selectionStart, selectionEnd);
                _hlEditor.getText()
                        .replace(selectionStart - _action.length(),
                                selectionEnd + _action.length(), text);

            }
            //Condition to insert shortcut preceding and succeeding the selection
            else {
                _hlEditor.getText().insert(selectionStart, _action);
                _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), _action);
            }
        } else {
            //Condition for Empty Selection
                /*if (false) {
                    // Condition for things that should only be placed at the start of the line even if no text is selected
                } else */
            if ("----\n".equals(_action)) {
                _hlEditor.getText().insert(_hlEditor.getSelectionStart(), _action);
            } else {
                // Condition for formatting which is inserted on either side of the cursor
                _hlEditor.getText().insert(_hlEditor.getSelectionStart(), _action)
                        .insert(_hlEditor.getSelectionEnd(), _action);
                _hlEditor.setSelection(_hlEditor.getSelectionStart() - _action.length());
            }
        }
    }

    //
    //
    //
    //
    public HighlightingEditor getHighlightingEditor() {
        return _hlEditor;
    }

    public TextActions setHighlightingEditor(HighlightingEditor hlEditor) {
        _hlEditor = hlEditor;
        return this;
    }

    public Document getDocument() {
        return _document;
    }

    public TextActions setDocument(Document document) {
        _document = document;
        return this;
    }

    public Activity getActivity() {
        return _activity;
    }

    public TextActions setActivity(Activity activity) {
        _activity = activity;
        return this;
    }

    public Context getContext() {
        return _context;
    }

    public TextActions setContext(Context context) {
        _context = context;
        return this;
    }

    /**
     * Callable from background thread!
     */
    public void setEditorTextAsync(final String text) {
        _activity.runOnUiThread(() -> _hlEditor.setText(text));
    }

    protected void runIndentLines(final boolean deIndent) {
        if (deIndent) {
            final String leadingIndentPattern = String.format("^\\s{1,%d}", _indent);
            TextActions.runRegexReplaceAction(_hlEditor, new TextActions.ReplacePattern(leadingIndentPattern, ""));
        } else {
            final String tabString = StringUtils.repeatChars(' ', _indent);
            TextActions.runRegexReplaceAction(_hlEditor, new TextActions.ReplacePattern("^", tabString));
        }
    }

    // Some actions common to multiple file types
    // Can be called _explicitly_ by a derived class
    protected final boolean runCommonTextAction(final @StringRes int action) {
        switch (action) {
            case R.string.tmaid_common_unordered_list_char: {
                runRegularPrefixAction(_appSettings.getUnorderedListCharacter() + " ", true);
                return true;
            }
            case R.string.tmaid_common_checkbox_list: {
                runRegularPrefixAction("- [ ] ", "- [x] ", true);
                return true;
            }
            case R.string.tmaid_common_ordered_list_number: {
                runRegularPrefixAction("1. ", true);
                return true;
            }
            case R.string.tmaid_common_time: {
                DatetimeFormatDialog.showDatetimeFormatDialog(getActivity(), _hlEditor);
                return true;
            }
            case R.string.tmaid_common_time_insert_timestamp: {

            }
            case R.string.tmaid_common_accordion: {
                _hlEditor.insertOrReplaceTextOnCursor("<details markdown='1'><summary>" + _context.getString(R.string.expand_collapse) + "</summary>\n" + HighlightingEditor.PLACE_CURSOR_HERE_TOKEN + "\n\n</details>");
                return true;
            }
            case R.string.tmaid_common_attach_something: {
                SearchOrCustomTextDialogCreator.showAttachSomethingDialog(_activity, itemId -> {
                    switch (itemId) {
                        case R.id.action_attach_color: {
                            showColorPickerDialog();
                            break;
                        }
                        case R.id.action_attach_date: {
                            DatetimeFormatDialog.showDatetimeFormatDialog(getActivity(), _hlEditor);
                            break;
                        }
                        case R.id.action_attach_audio:
                        case R.id.action_attach_file:
                        case R.id.action_attach_image:
                        case R.id.action_attach_link: {
                            int actionId = (itemId == R.id.action_attach_audio ? 4 : (itemId == R.id.action_attach_image ? 2 : 3));
                            AttachImageOrLinkDialog.showInsertImageOrLinkDialog(actionId, _document.getFormat(), getActivity(), _hlEditor, _document.getFile());
                            break;
                        }
                    }
                });
                return true;
            }
            case R.string.tmaid_common_ordered_list_renumber: {
                renumberOrderedList(StringUtils.getSelection(_hlEditor)[0]);
                return true;
            }
            case R.string.tmaid_common_move_text_one_line_up:
            case R.string.tmaid_common_move_text_one_line_down: {
                moveLineSelectionBy1(_hlEditor, action == R.string.tmaid_common_move_text_one_line_up);
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.tmaid_common_indent:
            case R.string.tmaid_common_deindent: {
                runIndentLines(action == R.string.tmaid_common_deindent);
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.tmaid_common_insert_snippet: {
                SearchOrCustomTextDialogCreator.showInsertSnippetDialog(_activity, (snip) -> {
                    _hlEditor.insertOrReplaceTextOnCursor(StringUtils.interpolateEscapedDateTime(snip));
                    _lastSnip = snip;
                });
                return true;
            }
            case R.string.tmaid_common_open_link_browser: {
                String url;
                if ((url = PlainTextStuff.tryExtractUrlAroundPos(_hlEditor.getText().toString(), _hlEditor.getSelectionStart())) != null) {
                    if (url.endsWith(")")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    new ContextUtils(_activity).openWebpageInExternalBrowser(url);
                }
                return true;
            }
            case R.string.tmaid_common_special_key: {
                runSpecialKeyAction();
                return true;
            }
            case R.string.tmaid_common_new_line_below: {
                // Go to end of line, works with wrapped lines too
                _hlEditor.setSelection(StringUtils.getLineEnd(_hlEditor.getText(), StringUtils.getSelection(_hlEditor)[1]));
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_ENTER);
                return true;
            }
            case R.string.tmaid_common_delete_lines: {
                final int[] sel = StringUtils.getLineSelection(_hlEditor);
                final Editable text = _hlEditor.getText();
                final boolean lastLine = sel[1] == text.length();
                final boolean firstLine = sel[0] == 0;
                text.delete(sel[0] - (lastLine && !firstLine ? 1 : 0), sel[1] + (lastLine ? 0 : 1));
                return true;
            }
        }
        return false;
    }

    // Some long-press actions common to multiple file types
    // Can be called _explicitly_ by a derived class
    @SuppressLint("NonConstantResourceId")
    protected final boolean runCommonLongPressTextActions(@StringRes int action) {
        switch (action) {
            case R.string.tmaid_common_deindent:
            case R.string.tmaid_common_indent: {
                SearchOrCustomTextDialogCreator.showIndentSizeDialog(_activity, _indent, (size) -> {
                    _indent = Integer.parseInt(size);
                    _appSettings.setDocumentIndentSize(_document.getPath(), _indent);
                });
                return true;
            }
            case R.string.tmaid_common_open_link_browser: {
                return onSearch();
            }
            case R.string.tmaid_common_special_key: {
                runJumpBottomTopAction();
                return true;
            }
            case R.string.tmaid_common_time: {
                try {
                    _hlEditor.insertOrReplaceTextOnCursor(DatetimeFormatDialog.getMostRecentDate(_activity));
                } catch (Exception ignored) {
                }
                return true;
            }
            case R.string.tmaid_common_ordered_list_number: {
                runRenumberOrderedListIfRequired(true);
                return true;
            }
            case R.string.tmaid_common_move_text_one_line_up:
            case R.string.tmaid_common_move_text_one_line_down: {
                StringUtils.showSelection(_hlEditor);
                return true;
            }
            case R.string.tmaid_common_insert_snippet: {
                if (!TextUtils.isEmpty(_lastSnip)) {
                    _hlEditor.insertOrReplaceTextOnCursor(StringUtils.interpolateEscapedDateTime(_lastSnip));
                }
                return true;
            }
        }
        return false;
    }

    public static class ActionItem {
        @StringRes
        public int keyId;
        @DrawableRes
        public int iconId;
        @StringRes
        public int stringId;

        public ActionItem(@StringRes int key, @DrawableRes int icon, @StringRes int string) {
            keyId = key;
            iconId = icon;
            stringId = string;
        }
    }

    public static void moveLineSelectionBy1(final HighlightingEditor hlEditor, final boolean isUp) {

        final Editable text = hlEditor.getText();

        final int[] sel = StringUtils.getSelection(hlEditor);
        final int linesStart = StringUtils.getLineStart(text, sel[0]);
        final int linesEnd = StringUtils.getLineEnd(text, sel[1]);

        if ((isUp && linesStart > 0) || (!isUp && linesEnd < text.length())) {

            final CharSequence lines = text.subSequence(linesStart, linesEnd);

            final int altStart = isUp ? StringUtils.getLineStart(text, linesStart - 1) : linesEnd + 1;
            final int altEnd = StringUtils.getLineEnd(text, altStart);
            final CharSequence altLine = text.subSequence(altStart, altEnd);

            final int[] selStart = StringUtils.getLineOffsetFromIndex(text, sel[0]);
            final int[] selEnd = StringUtils.getLineOffsetFromIndex(text, sel[1]);

            hlEditor.withAutoFormatDisabled(() -> {
                final String newPair = String.format("%s\n%s", isUp ? lines : altLine, isUp ? altLine : lines);
                text.replace(Math.min(linesStart, altStart), Math.max(altEnd, linesEnd), newPair);
            });

            selStart[0] += isUp ? -1 : 1;
            selEnd[0] += isUp ? -1 : 1;

            hlEditor.setSelection(
                    StringUtils.getIndexFromLineOffset(text, selStart),
                    StringUtils.getIndexFromLineOffset(text, selEnd));
        }
    }

    // Derived classes should override this to implement format-specific renumber logic
    protected void renumberOrderedList(final int cursorPosition) {
        // No-op in base class
    }

    public final void runRenumberOrderedListIfRequired() {
        runRenumberOrderedListIfRequired(false);
    }

    public final void runRenumberOrderedListIfRequired(final boolean force) {
        if (force || _hlEditor.getAutoFormatEnabled()) {
            _hlEditor.withAutoFormatDisabled(() -> renumberOrderedList(StringUtils.getSelection(_hlEditor)[0]));
        }
    }

    private String rstr(@StringRes int resKey) {
        return _activity.getString(resKey);
    }

    public void runSpecialKeyAction() {

        // Needed to prevent selection from being overwritten on refocus
        final int[] sel = StringUtils.getSelection(_hlEditor);
        _hlEditor.clearFocus();
        _hlEditor.requestFocus();
        _hlEditor.setSelection(sel[0], sel[1]);

        SearchOrCustomTextDialogCreator.showSpecialKeyDialog(_activity, (callbackPayload) -> {
            if (!_hlEditor.hasSelection() && _hlEditor.length() > 0) {
                _hlEditor.requestFocus();
            }
            if (callbackPayload.equals(rstr(R.string.key_page_down))) {
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_PAGE_DOWN);
            } else if (callbackPayload.equals(rstr(R.string.key_page_up))) {
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_PAGE_UP);
            } else if (callbackPayload.equals(rstr(R.string.key_pos_1))) {
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_MOVE_HOME);
            } else if (callbackPayload.equals(rstr(R.string.key_pos_end))) {
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_MOVE_END);
            } else if (callbackPayload.equals(rstr(R.string.key_pos_1_document))) {
                _hlEditor.setSelection(0);
            } else if (callbackPayload.equals(rstr(R.string.move_text_one_line_up))) {
                TextActions.moveLineSelectionBy1(_hlEditor, true);
            } else if (callbackPayload.equals(rstr(R.string.move_text_one_line_down))) {
                TextActions.moveLineSelectionBy1(_hlEditor, false);
            } else if (callbackPayload.equals(rstr(R.string.key_pos_end_document))) {
                _hlEditor.setSelection(_hlEditor.length());
            } else if (callbackPayload.equals(rstr(R.string.key_ctrl_a))) {
                _hlEditor.setSelection(0, _hlEditor.length());
            } else if (callbackPayload.equals(rstr(R.string.key_tab))) {
                _hlEditor.insertOrReplaceTextOnCursor("\u0009");
            } else if (callbackPayload.equals(rstr(R.string.zero_width_space))) {
                _hlEditor.insertOrReplaceTextOnCursor("\u200B");
            } else if (callbackPayload.equals(rstr(R.string.search))) {
                onSearch();
            } else if (callbackPayload.equals(rstr(R.string.break_page_pdf_print))) {
                _hlEditor.insertOrReplaceTextOnCursor("<div style='page-break-after:always;'></div>");
            } else if (callbackPayload.equals(rstr(R.string.ohm))) {
                _hlEditor.insertOrReplaceTextOnCursor("Ω");
            } else if (callbackPayload.equals(rstr(R.string.continued_overline))) {
                _hlEditor.insertOrReplaceTextOnCursor("‾‾‾‾‾");
            } else if (callbackPayload.equals(rstr(R.string.shrug))) {
                _hlEditor.insertOrReplaceTextOnCursor("¯\\_(ツ)_/¯");
            } else if (callbackPayload.equals(rstr(R.string.char_punctation_mark_arrows))) {
                _hlEditor.insertOrReplaceTextOnCursor("»«");
            } else if (callbackPayload.equals(rstr(R.string.select_current_line))) {
                _hlEditor.setSelectionExpandWholeLines();
            }
        });
    }

    public void showColorPickerDialog() {
        SearchOrCustomTextDialogCreator.showColorSelectionModeDialog(_activity, new Callback.a1<Integer>() {
            @Override
            public void callback(Integer colorInsertType) {
                ColorPickerDialogBuilder
                        .with(_hlEditor.getContext())
                        .setTitle(R.string.color)
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(12)
                        .setPositiveButton(android.R.string.ok, new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                String hex = Utils.getHexString(selectedColor, false).toLowerCase();
                                int pos = _hlEditor.getSelectionStart();
                                switch (colorInsertType) {
                                    case R.string.hexcode: {
                                        _hlEditor.getText().insert(pos, hex);
                                        break;
                                    }
                                    case R.string.foreground: {
                                        _hlEditor.getText().insert(pos, "<span style='color:" + hex + ";'></span>");
                                        _hlEditor.setSelection(_hlEditor.getSelectionStart() - 7);
                                        break;
                                    }
                                    case R.string.background: {
                                        _hlEditor.getText().insert(pos, "<span style='background-color:" + hex + ";'></span>");
                                        _hlEditor.setSelection(_hlEditor.getSelectionStart() - 7);
                                        break;
                                    }
                                }

                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .build()
                        .show();
            }
        });
    }

    public void runJumpBottomTopAction() {
        int pos = _hlEditor.getSelectionStart();
        _hlEditor.setSelection(pos == 0 ? _hlEditor.getText().length() : 0);
    }

}
