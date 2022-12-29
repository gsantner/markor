/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.TooltipCompat;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.Utils;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.frontend.AttachLinkOrFileDialog;
import net.gsantner.markor.frontend.DatetimeFormatDialog;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

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
public abstract class ActionButtonBase {
    private Activity m_activity;
    private MarkorContextUtils m_cu;
    private final int _buttonHorizontalMargin;
    private String _lastSnip;

    protected HighlightingEditor _hlEditor;
    protected WebView m_webView;
    protected Document _document;
    protected AppSettings _appSettings;
    protected int _indent;

    public static final String ACTION_ORDER_PREF_NAME = "action_order";
    private static final String ORDER_SUFFIX = "_order";
    private static final String DISABLED_SUFFIX = "_disabled";

    public ActionButtonBase(@NonNull final Context context, final Document document) {
        _document = document;
        _appSettings = ApplicationObject.settings();
        _buttonHorizontalMargin = (int) (_appSettings.getEditorActionButtonItemPadding() * context.getResources().getDisplayMetrics().density);
        _indent = _appSettings.getDocumentIndentSize(_document != null ? _document.getPath() : null);
    }

    // Override to implement custom onClick
    public boolean onActionClick(final @StringRes int action) {
        return runCommonAction(action);
    }

    // Override to implement custom onLongClick
    public boolean onActionLongClick(final @StringRes int action) {
        return runCommonLongPressAction(action);
    }

    // Override to implement custom search action
    public boolean onSearch() {
        MarkorDialogFactory.showSearchDialog(getActivity(), _hlEditor);
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
        final List<ActionItem> actionList = getActiveActionList();
        final ArrayList<String> keys = new ArrayList<>();

        for (ActionItem item : actionList) {
            keys.add(rstr(item.keyId));
        }

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

        SharedPreferences settings = getContext().getSharedPreferences(ACTION_ORDER_PREF_NAME, Context.MODE_PRIVATE);
        String formatKey = rstr(getFormatActionsKey()) + suffix;
        settings.edit().putString(formatKey, TextUtils.join(",", values)).apply();
    }

    private List<String> loadActionPreference(final String suffix) {
        String formatKey = rstr(getFormatActionsKey()) + suffix;
        SharedPreferences settings = getContext().getSharedPreferences(ACTION_ORDER_PREF_NAME, Context.MODE_PRIVATE);
        String combinedKeys = settings.getString(formatKey, null);
        return combinedKeys != null ? Arrays.asList(combinedKeys.split(",")) : Collections.emptyList();
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

    @SuppressWarnings("ConstantConditions")
    public void recreateActionButtons(ViewGroup barLayout, ActionItem.DisplayMode displayMode) {
        barLayout.removeAllViews();
        setBarVisible(barLayout, true);

        final Map<String, ActionItem> map = getActiveActionMap();
        final List<String> orderedKeys = getActionOrder();
        final Set<String> disabledKeys = new HashSet<>(getDisabledActions());
        for (final String key : orderedKeys) {
            final ActionItem action = map.get(key);
            if (!disabledKeys.contains(key) && (action.displayMode == displayMode || action.displayMode == ActionItem.DisplayMode.ANY)) {
                appendActionButtonToBar(barLayout, action);
            }
        }
    }

    protected void appendActionButtonToBar(ViewGroup barLayout, @NonNull ActionItem action) {
        final ImageView btn = (ImageView) getActivity().getLayoutInflater().inflate(R.layout.quick_keyboard_button, null);
        btn.setImageResource(action.iconId);
        final String desc = rstr(action.stringId);
        btn.setContentDescription(desc);
        TooltipCompat.setTooltipText(btn, desc);

        btn.setOnClickListener(v -> {
            try {
                // run action
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                onActionClick(action.keyId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btn.setOnLongClickListener(v -> {
            try {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                return onActionLongClick(action.keyId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        });
        final int sidePadding = _buttonHorizontalMargin + btn.getPaddingLeft(); // Left and right are symmetrical
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
        public final Matcher matcher;
        public final String replacePattern;
        public final boolean replaceAll;

        public boolean isSameReplace() {
            return replacePattern.equals("$0");
        }

        /**
         * Construct a ReplacePattern
         *
         * @param searchPattern  regex search pattern
         * @param replacePattern replace string
         * @param replaceAll     whether to replace all or just the first
         */
        public ReplacePattern(Pattern searchPattern, String replacePattern, boolean replaceAll) {
            this.matcher = searchPattern.matcher("");
            this.replacePattern = replacePattern;
            this.replaceAll = replaceAll;
        }

        public CharSequence replace() {
            return replaceAll ? matcher.replaceAll(replacePattern) : matcher.replaceFirst(replacePattern);
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
        runRegexReplaceAction(Arrays.asList(patterns));
    }

    public void runRegexReplaceAction(final List<ReplacePattern> patterns) {
        runRegexReplaceAction(_hlEditor, patterns);
    }

    public void runRegexReplaceAction(final String pattern, final String replace) {
        runRegexReplaceAction(Collections.singletonList(new ReplacePattern(pattern, replace)));
    }

    public static void runRegexReplaceAction(final EditText editor, final ReplacePattern... patterns) {
        runRegexReplaceAction(editor, Arrays.asList(patterns));
    }

    /**
     * Runs through a sequence of regex-search-and-replace actions on each selected line.
     * This function wraps _runRegexReplaceAction with a call to disable text trackers
     *
     * @param patterns An array of ReplacePattern
     */
    public static void runRegexReplaceAction(final EditText editor, final List<ReplacePattern> patterns) {
        if (editor instanceof HighlightingEditor) {
            ((HighlightingEditor) editor).withAutoFormatDisabled(() -> _runRegexReplaceAction(editor, patterns));
        } else {
            _runRegexReplaceAction(editor, patterns);
        }
    }

    private static void _runRegexReplaceAction(final EditText editor, final List<ReplacePattern> patterns) {

        final int[] sel = TextViewUtils.getSelection(editor);
        final TextViewUtils.ChunkedEditable text = TextViewUtils.ChunkedEditable.wrap(editor.getText());

        // Offset of selection start from text end - used to restore selection
        final int selEndOffset = text.length() - sel[1];
        // Offset of selection start from line end - used to restore selection
        final int selStartOffset = sel[1] == sel[0] ? selEndOffset : TextViewUtils.getLineEnd(text, sel[0]) - sel[0];

        // Start of line on which sel begins
        final int selStartStart = TextViewUtils.getLineStart(text, sel[0]);
        // Number of lines we will be modifying
        final int lineCount = TextViewUtils.countChars(text, sel[0], sel[1], '\n')[0] + 1;

        int lineStart = selStartStart;

        for (int i = 0; i < lineCount; i++) {

            int lineEnd = TextViewUtils.getLineEnd(text, lineStart);
            final String line = TextViewUtils.toString(text, lineStart, lineEnd);

            for (final ReplacePattern pattern : patterns) {
                if (pattern.matcher.reset(line).find()) {
                    if (!pattern.isSameReplace()) {
                        text.replace(lineStart, lineEnd, pattern.replace());
                    }
                    break;
                }
            }

            lineStart = TextViewUtils.getLineEnd(text, lineStart) + 1;
        }

        text.applyChanges();

        final int newSelEnd = text.length() - selEndOffset;
        final int newSelStart = sel[0] == sel[1] ? newSelEnd : TextViewUtils.getLineEnd(text, selStartStart) - selStartOffset;
        editor.setSelection(newSelStart, newSelEnd);
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


    public ActionButtonBase setUiReferences(@Nullable final Activity activity, @Nullable final HighlightingEditor hlEditor, @Nullable final WebView webview) {
        m_activity = activity;
        _hlEditor = hlEditor;
        m_webView = webview;
        m_cu = new MarkorContextUtils(m_activity);
        return this;
    }

    public Document getDocument() {
        return _document;
    }

    public ActionButtonBase setDocument(Document document) {
        _document = document;
        return this;
    }

    public Activity getActivity() {
        return m_activity;
    }

    public Context getContext() {
        return m_activity != null ? m_activity : _appSettings.getContext();
    }

    public MarkorContextUtils getCu() {
        return m_cu;
    }

    /**
     * Callable from background thread!
     */
    public void setEditorTextAsync(final String text) {
        getActivity().runOnUiThread(() -> _hlEditor.setText(text));
    }

    protected void runIndentLines(final boolean deIndent) {
        if (deIndent) {
            final String leadingIndentPattern = String.format("^\\s{1,%d}", _indent);
            ActionButtonBase.runRegexReplaceAction(_hlEditor, new ActionButtonBase.ReplacePattern(leadingIndentPattern, ""));
        } else {
            final String tabString = TextViewUtils.repeatChars(' ', _indent);
            ActionButtonBase.runRegexReplaceAction(_hlEditor, new ActionButtonBase.ReplacePattern("^", tabString));
        }
    }

    // Some actions common to multiple file types
    // Can be called _explicitly_ by a derived class
    protected final boolean runCommonAction(final @StringRes int action) {
        switch (action) {
            case R.string.abid_common_unordered_list_char: {
                runRegularPrefixAction(_appSettings.getUnorderedListCharacter() + " ", true);
                return true;
            }
            case R.string.abid_common_checkbox_list: {
                runRegularPrefixAction("- [ ] ", "- [x] ", true);
                return true;
            }
            case R.string.abid_common_ordered_list_number: {
                runRegularPrefixAction("1. ", true);
                return true;
            }
            case R.string.abid_common_time: {
                DatetimeFormatDialog.showDatetimeFormatDialog(getActivity(), _hlEditor);
                return true;
            }
            case R.string.abid_common_time_insert_timestamp: {

            }
            case R.string.abid_common_accordion: {
                _hlEditor.insertOrReplaceTextOnCursor("<details markdown='1'><summary>" + rstr(R.string.expand_collapse) + "</summary>\n" + HighlightingEditor.PLACE_CURSOR_HERE_TOKEN + "\n\n</details>");
                return true;
            }
            case R.string.abid_common_attach_something: {
                MarkorDialogFactory.showAttachSomethingDialog(getActivity(), itemId -> {
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
                            AttachLinkOrFileDialog.showInsertImageOrLinkDialog(actionId, _document.getFormat(), getActivity(), _hlEditor, _document.getFile());
                            break;
                        }
                    }
                });
                return true;
            }
            case R.string.abid_common_ordered_list_renumber: {
                renumberOrderedList();
                return true;
            }
            case R.string.abid_common_move_text_one_line_up:
            case R.string.abid_common_move_text_one_line_down: {
                moveLineSelectionBy1(_hlEditor, action == R.string.abid_common_move_text_one_line_up);
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.abid_common_indent:
            case R.string.abid_common_deindent: {
                runIndentLines(action == R.string.abid_common_deindent);
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.abid_common_insert_snippet: {
                MarkorDialogFactory.showInsertSnippetDialog(getActivity(), _hlEditor, (snip) -> {
                    _hlEditor.insertOrReplaceTextOnCursor(TextViewUtils.interpolateEscapedDateTime(snip));
                    _lastSnip = snip;
                });
                return true;
            }
            case R.string.abid_common_open_link_browser: {
                String url;
                if ((url = GsTextUtils.tryExtractUrlAroundPos(_hlEditor.getText().toString(), _hlEditor.getSelectionStart())) != null) {
                    if (url.endsWith(")")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    getCu().openWebpageInExternalBrowser(getContext(), url);
                }
                return true;
            }
            case R.string.abid_common_special_key: {
                runSpecialKeyAction();
                return true;
            }
            case R.string.abid_common_new_line_below: {
                // Go to end of line, works with wrapped lines too
                _hlEditor.setSelection(TextViewUtils.getLineEnd(_hlEditor.getText(), TextViewUtils.getSelection(_hlEditor)[1]));
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_ENTER);
                return true;
            }
            case R.string.abid_common_delete_lines: {
                final int[] sel = TextViewUtils.getLineSelection(_hlEditor);
                final Editable text = _hlEditor.getText();
                final boolean lastLine = sel[1] == text.length();
                final boolean firstLine = sel[0] == 0;
                text.delete(sel[0] - (lastLine && !firstLine ? 1 : 0), sel[1] + (lastLine ? 0 : 1));
                return true;
            }
            case R.string.abid_common_web_jump_to_very_top_or_bottom: {
                runJumpBottomTopAction(ActionItem.DisplayMode.VIEW);
                return true;
            }
            case R.string.abid_common_web_jump_to_table_of_contents: {
                m_webView.loadUrl("javascript:document.getElementsByClassName('toc')[0].scrollIntoView();");
                return true;
            }
            case R.string.abid_common_view_file_in_other_app: {
                getCu().viewFileInOtherApp(getContext(), _document.getFile(), GsFileUtils.getMimeType(_document.getFile()));
                return true;
            }
            case R.string.abid_common_rotate_screen: {
                getCu().nextScreenRotationSetting(getActivity());
                return true;
            }
        }
        return false;
    }

    // Some long-press actions common to multiple file types
    // Can be called _explicitly_ by a derived class
    @SuppressLint("NonConstantResourceId")
    protected final boolean runCommonLongPressAction(@StringRes int action) {
        switch (action) {
            case R.string.abid_common_deindent:
            case R.string.abid_common_indent: {
                MarkorDialogFactory.showIndentSizeDialog(getActivity(), _indent, (size) -> {
                    _indent = Integer.parseInt(size);
                    _appSettings.setDocumentIndentSize(_document.getPath(), _indent);
                });
                return true;
            }
            case R.string.abid_common_open_link_browser: {
                return onSearch();
            }
            case R.string.abid_common_special_key: {
                runJumpBottomTopAction(ActionItem.DisplayMode.EDIT);
                return true;
            }
            case R.string.abid_common_time: {
                try {
                    _hlEditor.insertOrReplaceTextOnCursor(DatetimeFormatDialog.getMostRecentDate(getContext()));
                } catch (Exception ignored) {
                }
                return true;
            }
            case R.string.abid_common_ordered_list_number: {
                runRenumberOrderedListIfRequired(true);
                return true;
            }
            case R.string.abid_common_move_text_one_line_up:
            case R.string.abid_common_move_text_one_line_down: {
                TextViewUtils.showSelection(_hlEditor);
                return true;
            }
            case R.string.abid_common_insert_snippet: {
                if (!TextUtils.isEmpty(_lastSnip)) {
                    _hlEditor.insertOrReplaceTextOnCursor(TextViewUtils.interpolateEscapedDateTime(_lastSnip));
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
        public DisplayMode displayMode;

        public enum DisplayMode {EDIT, VIEW, ANY}

        public ActionItem(@StringRes int key, @DrawableRes int icon, @StringRes int string, final DisplayMode... a_displayMode) {
            keyId = key;
            iconId = icon;
            stringId = string;
            displayMode = a_displayMode != null && a_displayMode.length > 0 ? a_displayMode[0] : DisplayMode.EDIT;
        }
    }

    public static void moveLineSelectionBy1(final HighlightingEditor hlEditor, final boolean isUp) {

        final Editable text = hlEditor.getText();

        final int[] sel = TextViewUtils.getSelection(hlEditor);
        final int linesStart = TextViewUtils.getLineStart(text, sel[0]);
        final int linesEnd = TextViewUtils.getLineEnd(text, sel[1]);

        if ((isUp && linesStart > 0) || (!isUp && linesEnd < text.length())) {

            final CharSequence lines = text.subSequence(linesStart, linesEnd);

            final int altStart = isUp ? TextViewUtils.getLineStart(text, linesStart - 1) : linesEnd + 1;
            final int altEnd = TextViewUtils.getLineEnd(text, altStart);
            final CharSequence altLine = text.subSequence(altStart, altEnd);

            final int[] selStart = TextViewUtils.getLineOffsetFromIndex(text, sel[0]);
            final int[] selEnd = TextViewUtils.getLineOffsetFromIndex(text, sel[1]);

            hlEditor.withAutoFormatDisabled(() -> {
                final String newPair = String.format("%s\n%s", isUp ? lines : altLine, isUp ? altLine : lines);
                text.replace(Math.min(linesStart, altStart), Math.max(altEnd, linesEnd), newPair);
            });

            selStart[0] += isUp ? -1 : 1;
            selEnd[0] += isUp ? -1 : 1;

            hlEditor.setSelection(
                    TextViewUtils.getIndexFromLineOffset(text, selStart),
                    TextViewUtils.getIndexFromLineOffset(text, selEnd));
        }
    }

    // Derived classes should override this to implement format-specific renumber logic
    protected void renumberOrderedList() {
        // No-op in base class
    }

    public final void runRenumberOrderedListIfRequired() {
        runRenumberOrderedListIfRequired(false);
    }

    public final void runRenumberOrderedListIfRequired(final boolean force) {
        if (force || _hlEditor.getAutoFormatEnabled()) {
            _hlEditor.withAutoFormatDisabled(this::renumberOrderedList);
        }
    }

    private String rstr(@StringRes int resKey) {
        return getContext().getString(resKey);
    }

    public void runSpecialKeyAction() {

        // Needed to prevent selection from being overwritten on refocus
        final int[] sel = TextViewUtils.getSelection(_hlEditor);
        _hlEditor.clearFocus();
        _hlEditor.requestFocus();
        _hlEditor.setSelection(sel[0], sel[1]);

        MarkorDialogFactory.showSpecialKeyDialog(getActivity(), (callbackPayload) -> {
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
                ActionButtonBase.moveLineSelectionBy1(_hlEditor, true);
            } else if (callbackPayload.equals(rstr(R.string.move_text_one_line_down))) {
                ActionButtonBase.moveLineSelectionBy1(_hlEditor, false);
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
        MarkorDialogFactory.showColorSelectionModeDialog(getActivity(), new GsCallback.a1<Integer>() {
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

    public void runJumpBottomTopAction(ActionItem.DisplayMode displayMode) {
        if (displayMode == ActionItem.DisplayMode.EDIT) {
            int pos = _hlEditor.getSelectionStart();
            _hlEditor.setSelection(pos == 0 ? _hlEditor.getText().length() : 0);
        } else if (displayMode == ActionItem.DisplayMode.VIEW) {
            boolean top = m_webView.getScrollY() > 100;
            m_webView.scrollTo(0, top ? 0 : m_webView.getContentHeight());
            if (!top) {
                m_webView.scrollBy(0, 1000);
                m_webView.scrollBy(0, 1000);
            }
        }
    }

}
