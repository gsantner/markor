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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.TooltipCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.format.general.DatetimeFormatDialog;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
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
    private int _textActionSidePadding;
    protected int _indent;

    public static final String ACTION_ORDER_PREF_NAME = "action_order";
    private static final String ORDER_SUFFIX = "_order";
    private static final String DISABLED_SUFFIX = "_disabled";

    public TextActions(Activity activity, Document document) {
        _document = document;
        _activity = activity;
        _au = new ActivityUtils(activity);
        _context = activity != null ? activity : _hlEditor.getContext();
        _appSettings = new AppSettings(_context);
        _textActionSidePadding = (int) (_appSettings.getEditorTextActionItemPadding() * _context.getResources().getDisplayMetrics().density);
        _indent = _appSettings.getDocumentIndentSize(_document.getPath());
    }

    /**
     * Derived classes must implement a callback which inherits from ActionCallback
     */
    protected abstract static class ActionCallback implements View.OnLongClickListener, View.OnClickListener {
    }

    /**
     * Factory to generate ActionCallback for given keyId
     *
     * @param keyId Callback must handle keyId
     * @return Child class of ActionCallback
     */
    protected abstract ActionCallback getActionCallback(@StringRes int keyId);

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
                    final ActionCallback actionCallback = getActionCallback(action.keyId);
                    appendTextActionToBar(barLayout, action.iconId, action.stringId, actionCallback, actionCallback);
                }
            }
        }
    }

    protected void appendTextActionToBar(ViewGroup barLayout, @DrawableRes int iconRes, @StringRes int descRes, final View.OnClickListener listener, final View.OnLongClickListener longClickListener) {
        ImageView btn = (ImageView) _activity.getLayoutInflater().inflate(R.layout.quick_keyboard_button, null);
        btn.setImageResource(iconRes);
        btn.setContentDescription(_activity.getString(descRes));
        TooltipCompat.setTooltipText(btn, _activity.getString(descRes));
        btn.setOnClickListener(v -> {
            try {
                listener.onClick(v);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        if (longClickListener != null) {
            btn.setOnLongClickListener(v -> {
                try {
                    return longClickListener.onLongClick(v);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            });
        }
        btn.setPadding(_textActionSidePadding, btn.getPaddingTop(), _textActionSidePadding, btn.getPaddingBottom());

        boolean isDarkTheme = AppSettings.get().isDarkThemeEnabled();
        btn.setColorFilter(ContextCompat.getColor(_context, isDarkTheme ? android.R.color.white : R.color.grey));
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
        try {
            if (editor instanceof HighlightingEditor) {
                ((HighlightingEditor) editor).setAccessibilityEnabled(false);
            }
            _runRegexReplaceAction(editor, patterns, matchAll);
        } finally {
            if (editor instanceof HighlightingEditor) {
                ((HighlightingEditor) editor).setAccessibilityEnabled(true);
            }
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

    protected boolean runCommonTextAction(String action) {
        switch (action) {
            case "tmaid_common_unordered_list_char": {
                runRegularPrefixAction(_appSettings.getUnorderedListCharacter() + " ", true);
                return true;
            }
            case "tmaid_common_checkbox_list": {
                runRegularPrefixAction("- [ ] ", "- [x] ", true);
                return true;
            }
            case "tmaid_common_ordered_list_number": {
                runRegularPrefixAction("1. ", true);
                return true;
            }
            case "tmaid_common_time": {
                DatetimeFormatDialog.showDatetimeFormatDialog(getActivity(), _hlEditor);
                return true;
            }

            case "tmaid_common_time_insert_timestamp": {
                try {
                    _hlEditor.insertOrReplaceTextOnCursor(DatetimeFormatDialog.getMostRecentDate(_activity));
                } catch (Exception ignored) {
                }
                return true;
            }
            case "tmaid_common_accordion": {
                _hlEditor.insertOrReplaceTextOnCursor("<details markdown='1'><summary>" + _context.getString(R.string.expand_collapse) + "</summary>\n" + HighlightingEditor.PLACE_CURSOR_HERE_TOKEN + "\n\n</details>");
                return true;
            }
            case "tmaid_common_attach_something": {
                SearchOrCustomTextDialogCreator.showAttachSomethingDialog(_activity, itemId -> {
                    switch (itemId) {
                        case R.id.action_attach_color: {
                            new CommonTextActions(getActivity(), _hlEditor).runAction(CommonTextActions.ACTION_COLOR_PICKER);
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
            default: {
                return new CommonTextActions(_activity, _hlEditor).runAction(action);
            }
        }
    }

    public boolean runAction(final String action) {
        return runAction(action, false, null);
    }

    public abstract boolean runAction(final String action, boolean modLongClick, String anotherArg);

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

        public ActionItem(int[] data) {
            keyId = data[0];
            iconId = data[1];
            stringId = data[2];
        }
    }
}
