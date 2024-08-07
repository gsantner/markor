/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import static android.util.Patterns.WEB_URL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.TooltipCompat;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.frontend.AttachLinkOrFileDialog;
import net.gsantner.markor.frontend.DatetimeFormatDialog;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.GsSearchOrCustomTextDialog;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    private Activity _activity;
    private MarkorContextUtils _cu;
    private final int _buttonHorizontalMargin;
    private String _lastSnip;

    protected HighlightingEditor _hlEditor;
    protected WebView _webView;
    protected Document _document;
    protected AppSettings _appSettings;
    protected int _indent;

    private final GsSearchOrCustomTextDialog.DialogState _specialKeyDialogState = new GsSearchOrCustomTextDialog.DialogState();

    public static final String ACTION_ORDER_PREF_NAME = "action_order";
    private static final String ORDER_SUFFIX = "_order";
    private static final String DISABLED_SUFFIX = "_disabled";

    private static final Pattern UNTRIMMED_TEXT = Pattern.compile("(\\s*)(.*?)(\\s*)", Pattern.DOTALL);

    public ActionButtonBase(@NonNull final Context context, final Document document) {
        _document = document;
        _appSettings = ApplicationObject.settings();
        _buttonHorizontalMargin = GsContextUtils.instance.convertDpToPx(context, _appSettings.getEditorActionButtonItemPadding());
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
        MarkorDialogFactory.showSearchDialog(_activity, _hlEditor);
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
    protected abstract List<ActionItem> getFormatActionList();

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
        final List<ActionItem> actionList = getActionList();
        final List<String> keyList = getActiveActionKeys();

        final Map<String, ActionItem> map = new HashMap<>();
        for (int i = 0; i < actionList.size(); i++) {
            map.put(keyList.get(i), actionList.get(i));
        }
        return map;
    }

    /**
     * Get a combined action list - from derived format and the base actions
     */
    private List<ActionItem> getActionList() {
        final List<ActionItem> commonActions = Arrays.asList(
                new ActionItem(R.string.abid_common_delete_lines, R.drawable.ic_delete_black_24dp, R.string.delete_lines),
                new ActionItem(R.string.abid_common_duplicate_lines, R.drawable.ic_duplicate_lines_black_24dp, R.string.duplicate_lines),
                new ActionItem(R.string.abid_common_new_line_below, R.drawable.ic_baseline_keyboard_return_24, R.string.start_new_line_below),
                new ActionItem(R.string.abid_common_move_text_one_line_up, R.drawable.ic_baseline_arrow_upward_24, R.string.move_text_one_line_up).setRepeatable(true),
                new ActionItem(R.string.abid_common_move_text_one_line_down, R.drawable.ic_baseline_arrow_downward_24, R.string.move_text_one_line_down).setRepeatable(true),
                new ActionItem(R.string.abid_common_insert_snippet, R.drawable.ic_baseline_file_copy_24, R.string.insert_snippet),
                new ActionItem(R.string.abid_common_special_key, R.drawable.ic_keyboard_black_24dp, R.string.special_key),
                new ActionItem(R.string.abid_common_time, R.drawable.ic_access_time_black_24dp, R.string.date_and_time),
                new ActionItem(R.string.abid_common_open_link_browser, R.drawable.ic_open_in_browser_black_24dp, R.string.open_link),

                new ActionItem(R.string.abid_common_web_jump_to_very_top_or_bottom, R.drawable.ic_vertical_align_center_black_24dp, R.string.jump_to_bottom).setDisplayMode(ActionItem.DisplayMode.VIEW),
                new ActionItem(R.string.abid_common_view_file_in_other_app, R.drawable.ic_baseline_open_in_new_24, R.string.open_with).setDisplayMode(ActionItem.DisplayMode.VIEW),
                new ActionItem(R.string.abid_common_rotate_screen, R.drawable.ic_rotate_left_black_24dp, R.string.rotate).setDisplayMode(ActionItem.DisplayMode.ANY)
        );

        // Order is enforced separately
        final Map<Integer, ActionItem> unique = new HashMap<>();

        for (final ActionItem item : commonActions) {
            unique.put(item.keyId, item);
        }

        // Actions in the derived class override common actions if they share the same keyId
        for (final ActionItem item : getFormatActionList()) {
            unique.put(item.keyId, item);
        }

        return new ArrayList<>(unique.values());
    }

    /**
     * Get string for every ActionItem.keyId defined by getActiveActionList
     *
     * @return List or resource strings
     */
    public List<String> getActiveActionKeys() {
        return GsCollectionUtils.map(getActionList(), item -> rstr(item.keyId));
    }

    /**
     * Save an action order to preferences.
     * The Preference is derived from the key returned by getFormatActionsKey
     * <p>
     * Keys are joined into a comma separated list before saving.
     *
     * @param keys of keys (in order) to save
     */
    public void saveDisabledActions(final Collection<String> keys) {
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
    public void saveActionOrder(final Collection<String> keys) {
        saveActionPreference(ORDER_SUFFIX, keys);
    }

    private void saveActionPreference(final String suffix, final Collection<String> values) {
        final SharedPreferences settings = getContext().getSharedPreferences(ACTION_ORDER_PREF_NAME, Context.MODE_PRIVATE);
        final String formatKey = rstr(getFormatActionsKey()) + suffix;
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
        final Set<String> order = new LinkedHashSet<>(loadActionPreference(ORDER_SUFFIX));

        // Handle the case where order was stored without suffix. i.e. before this release.
        if (order.isEmpty()) {
            order.addAll(loadActionPreference(""));
        }

        final Set<String> defined = new LinkedHashSet<>(getActiveActionKeys());
        final Set<String> disabled = new LinkedHashSet<>(getDisabledActions());

        // Any definedKeys which are not in prefs or disabled keys are added to disabled
        final Set<String> existing = GsCollectionUtils.union(order, disabled);
        final Set<String> added = GsCollectionUtils.setDiff(defined, existing);
        final Set<String> removed = GsCollectionUtils.setDiff(existing, defined);

        // NOTE: suppressing this for increased discoverability
        // Disable any new actions unless none existing (i.e. first run)
        // if (!existing.isEmpty()) {
        //     disabled.addAll(added);
        // }

        // Add new ones to order
        order.addAll(added);

        // Removed removed from order and disabled
        disabled.removeAll(removed);
        order.removeAll(removed);

        if (!added.isEmpty() || !removed.isEmpty()) {
            saveActionOrder(order);
        }

        return new ArrayList<>(order);
    }

    @SuppressWarnings("ConstantConditions")
    public void recreateActionButtons(final ViewGroup barLayout, final ActionItem.DisplayMode displayMode) {
        barLayout.removeAllViews();
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

    @SuppressLint("ClickableViewAccessibility")
    private void setupRepeat(final View btn) {
        // Velocity and acceleration parameters
        final int INITIAL_DELAY = 400, DELTA_DELAY = 50, MIN_DELAY = 100;
        final Handler handler = new Handler();

        final Runnable repeater = new Runnable() {
            int delay = INITIAL_DELAY;

            @Override
            public void run() {
                btn.callOnClick();
                delay = Math.max(MIN_DELAY, delay - DELTA_DELAY);
                handler.postDelayed(this, delay);
            }
        };

        btn.setOnLongClickListener(v -> {
            btn.callOnClick(); // Trigger immediately
            handler.postDelayed(repeater, INITIAL_DELAY);
            return true;
        });

        btn.setOnTouchListener((view, event) -> {
            final int eac = event.getAction();
            if (eac == MotionEvent.ACTION_UP || eac == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacksAndMessages(null);
            }
            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void appendActionButtonToBar(final ViewGroup barLayout, final @NonNull ActionItem action) {
        final ImageView btn = (ImageView) _activity.getLayoutInflater().inflate(R.layout.quick_keyboard_button, null);
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

        if (action.isRepeatable) {
            setupRepeat(btn);
        } else {
            btn.setOnLongClickListener(v -> {
                try {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    return onActionLongClick(action.keyId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            });
        }
        final int sidePadding = _buttonHorizontalMargin + btn.getPaddingLeft(); // Left and right are symmetrical
        btn.setPadding(sidePadding, btn.getPaddingTop(), sidePadding, btn.getPaddingBottom());
        barLayout.addView(btn);
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
            ((HighlightingEditor) editor).withAutoFormatDisabled(() -> runRegexReplaceAction(editor.getText(), patterns));
        } else {
            runRegexReplaceAction(editor.getText(), patterns);
        }
    }

    public static void runRegexReplaceAction(final Editable editable, final ReplacePattern... patterns) {
        runRegexReplaceAction(editable, Arrays.asList(patterns));
    }

    private static void runRegexReplaceAction(final Editable editable, final List<ReplacePattern> patterns) {

        TextViewUtils.withKeepSelection(editable, (selStart, selEnd) -> {

            final TextViewUtils.ChunkedEditable text = TextViewUtils.ChunkedEditable.wrap(editable);
            // Start of line on which sel begins
            final int selStartStart = TextViewUtils.getLineStart(text, selStart);

            // Number of lines we will be modifying
            final int lineCount = GsTextUtils.countChars(text, selStart, selEnd, '\n')[0] + 1;
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
        });
    }

    protected void runSurroundAction(final String delim) {
        runSurroundAction(delim, delim, true);
    }

    /**
     * Surrounds the current selection with the given startDelimiter and end strings.
     * If the region is already surrounded by the given strings, they are removed instead.
     *
     * @param open  The string to insert at the start of the selection
     * @param close The string to insert at the end of the selection
     * @param trim  Whether to trim spaces from the start and end of the selection
     */
    protected void runSurroundAction(final String open, final String close, final boolean trim) {
        final Editable text = _hlEditor.getText();
        if (text == null) {
            return;
        }

        // Detect if delims within or around selection
        // If so, remove it
        // -------------------------------------------------------------------------
        final int[] sel = TextViewUtils.getSelection(_hlEditor);
        final int ss = sel[0], se = sel[1];
        final int ol = open.length(), cl = close.length(), sl = se - ss;
        // Left as a CharSequence to help maintain spans
        final CharSequence selection = text.subSequence(ss, se);

        // Case delims around selection
        if ((ss > ol) && ((se + cl) <= text.length())) {
            final String before = text.subSequence(ss - ol, ss).toString();
            final String after = text.subSequence(se, se + cl).toString();
            if (before.equals(open) && after.equals(close)) {
                text.replace(ss - ol, se + cl, selection);
                _hlEditor.setSelection(ss - ol, se - ol);
                return;
            }
        }

        // Case delims within selection
        if ((se - ss) >= (ol + cl)) {
            final String within = text.subSequence(ss, se).toString();
            if (within.startsWith(open) && within.endsWith(close)) {
                text.replace(ss, se, within.substring(ol, within.length() - cl));
                _hlEditor.setSelection(ss, se - ol - cl);
                return;
            }
        }

        final String replace;
        if (trim && selection.length() > 0) {
            final int f = TextViewUtils.getFirstNonWhitespace(selection);
            final int l = TextViewUtils.getLastNonWhitespace(selection) + 1;
            replace = selection.subSequence(0, f) + open +
                    selection.subSequence(f, l) + close +
                    selection.subSequence(l, sl);
        } else {
            replace = open + selection + close;
        }

        text.replace(ss, se, replace);
        _hlEditor.setSelection(ss + ol, se + ol);
    }

    public ActionButtonBase setUiReferences(@Nullable final Activity activity, @Nullable final HighlightingEditor hlEditor, @Nullable final WebView webview) {
        _activity = activity;
        _hlEditor = hlEditor;
        _webView = webview;
        _cu = new MarkorContextUtils(_activity);
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
        return _activity;
    }

    public Context getContext() {
        return _activity != null ? _activity : _appSettings.getContext();
    }

    public MarkorContextUtils getCu() {
        return _cu;
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
            ActionButtonBase.runRegexReplaceAction(_hlEditor, new ActionButtonBase.ReplacePattern(leadingIndentPattern, ""));
        } else {
            final String tabString = GsTextUtils.repeatChars(' ', _indent);
            ActionButtonBase.runRegexReplaceAction(_hlEditor, new ActionButtonBase.ReplacePattern("^", tabString));
        }
    }

    // Some actions common to multiple file types
    // Can be called _explicitly_ by a derived class
    protected final boolean runCommonAction(final @StringRes int action) {
        final Editable text = _hlEditor.getText();
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
                DatetimeFormatDialog.showDatetimeFormatDialog(_activity, _hlEditor);
                return true;
            }
            case R.string.abid_common_accordion: {
                _hlEditor.insertOrReplaceTextOnCursor("<details markdown='1'><summary>" + rstr(R.string.expand_collapse) + "</summary>\n" + HighlightingEditor.PLACE_CURSOR_HERE_TOKEN + "\n\n</details>");
                return true;
            }
            case R.string.abid_common_insert_audio: {
                AttachLinkOrFileDialog.showInsertImageOrLinkDialog(AttachLinkOrFileDialog.AUDIO_ACTION, _document.getFormat(), _activity, text, _document.getFile());
                return true;
            }
            case R.string.abid_common_insert_link: {
                AttachLinkOrFileDialog.showInsertImageOrLinkDialog(AttachLinkOrFileDialog.FILE_OR_LINK_ACTION, _document.getFormat(), _activity, text, _document.getFile());
                return true;
            }
            case R.string.abid_common_insert_image: {
                AttachLinkOrFileDialog.showInsertImageOrLinkDialog(AttachLinkOrFileDialog.IMAGE_ACTION, _document.getFormat(), _activity, text, _document.getFile());
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
                MarkorDialogFactory.showInsertSnippetDialog(_activity, (snip) -> {
                    _hlEditor.insertOrReplaceTextOnCursor(TextViewUtils.interpolateSnippet(snip, _document.getTitle(), TextViewUtils.getSelectedText(_hlEditor)));
                    _lastSnip = snip;
                });
                return true;
            }
            case R.string.abid_common_open_link_browser: {
                final int sel = TextViewUtils.getSelection(_hlEditor)[0];
                final String line = TextViewUtils.getSelectedLines(_hlEditor, sel);
                final int cursor = sel - TextViewUtils.getLineStart(_hlEditor.getText(), sel);

                // First try to pull a resource
                String url = null;
                final String resource = GsTextUtils.tryExtractResourceAroundPos(line, cursor);
                if (resource != null) {
                    if (WEB_URL.matcher(resource).matches()) {
                        url = resource;
                    } else {
                        final File f = GsFileUtils.makeAbsolute(resource, _document.getFile().getParentFile());
                        if (f.canRead()) {
                            DocumentActivity.launch(getActivity(), f, null, null);
                            return true;
                        }
                    }

                }

                // Then try to pull a tag
                url = url == null ? GsTextUtils.tryExtractUrlAroundPos(line, cursor) : url;
                if (url != null) {
                    if (url.endsWith(")")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    _cu.openWebpageInExternalBrowser(getContext(), url);
                }
                return true;
            }
            case R.string.abid_common_special_key: {
                runSpecialKeyAction();
                return true;
            }
            case R.string.abid_common_new_line_below: {
                // Go to end of line, works with wrapped lines too
                _hlEditor.setSelection(TextViewUtils.getLineEnd(text, TextViewUtils.getSelection(_hlEditor)[1]));
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_ENTER);
                return true;
            }
            case R.string.abid_common_delete_lines: {
                final int[] sel = TextViewUtils.getLineSelection(_hlEditor);
                final boolean lastLine = sel[1] == text.length();
                final boolean firstLine = sel[0] == 0;
                text.delete(sel[0] - (lastLine && !firstLine ? 1 : 0), sel[1] + (lastLine ? 0 : 1));
                return true;
            }
            case R.string.abid_common_duplicate_lines: {
                duplicateLineSelection(_hlEditor);
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.abid_common_web_jump_to_very_top_or_bottom: {
                runJumpBottomTopAction(ActionItem.DisplayMode.VIEW);
                return true;
            }
            case R.string.abid_common_web_jump_to_table_of_contents: {
                if (_appSettings.isMarkdownTableOfContentsEnabled()) {
                    _webView.loadUrl("javascript:document.getElementsByClassName('toc')[0].scrollIntoView();");
                } else {
                    runTitleClick();
                }
                return true;
            }
            case R.string.abid_common_view_file_in_other_app: {
                _cu.viewFileInOtherApp(getContext(), _document.getFile(), GsFileUtils.getMimeType(_document.getFile()));
                return true;
            }
            case R.string.abid_common_rotate_screen: {
                _cu.nextScreenRotationSetting(_activity);
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
                MarkorDialogFactory.showIndentSizeDialog(_activity, _indent, (size) -> {
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
                    _hlEditor.insertOrReplaceTextOnCursor(TextViewUtils.interpolateSnippet(_lastSnip, _document.getTitle(), TextViewUtils.getSelectedText(_hlEditor)));
                }
                return true;
            }
            case R.string.abid_common_insert_audio: {
                AttachLinkOrFileDialog.insertAudioRecording(_activity, _document.getFormat(), _hlEditor.getText(), _document.getFile());
                return true;
            }
            case R.string.abid_common_insert_link: {
                AttachLinkOrFileDialog.insertGalleryPhoto(_activity, _document.getFormat(), _hlEditor.getText(), _document.getFile());
                return true;
            }
            case R.string.abid_common_insert_image: {
                AttachLinkOrFileDialog.insertCameraPhoto(_activity, _document.getFormat(), _hlEditor.getText(), _document.getFile());
                return true;
            }
            case R.string.abid_common_new_line_below: {
                // Long press = line above
                final Editable text = _hlEditor.getText();
                if (text != null) {
                    final int sel = TextViewUtils.getSelection(text)[0];
                    final int lineStart = TextViewUtils.getLineStart(text, sel);
                    text.insert(lineStart, "\n");
                    _hlEditor.setSelection(lineStart);
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
        public DisplayMode displayMode = DisplayMode.EDIT;

        public boolean isRepeatable = false;

        public enum DisplayMode {EDIT, VIEW, ANY}

        public ActionItem(@StringRes int key, @DrawableRes int icon, @StringRes int string) {
            keyId = key;
            iconId = icon;
            stringId = string;
        }

        public ActionItem setDisplayMode(DisplayMode mode) {
            displayMode = mode;
            return this;
        }

        public ActionItem setRepeatable(boolean repeatable) {
            isRepeatable = repeatable;
            return this;
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

    public static void duplicateLineSelection(final HighlightingEditor hlEditor) {
        // Duplication is performed downwards, selection is moving alongside it and
        // cursor is preserved regarding column position (helpful for editing the
        // newly created line at the selected position right away).
        final Editable text = hlEditor.getText();

        final int[] sel = TextViewUtils.getSelection(hlEditor);
        final int linesStart = TextViewUtils.getLineStart(text, sel[0]);
        final int linesEnd = TextViewUtils.getLineEnd(text, sel[1]);

        final CharSequence lines = text.subSequence(linesStart, linesEnd);

        final int[] selStart = TextViewUtils.getLineOffsetFromIndex(text, sel[0]);
        final int[] selEnd = TextViewUtils.getLineOffsetFromIndex(text, sel[1]);

        hlEditor.withAutoFormatDisabled(() -> {
            // Prepending the newline instead of appending it is required for making
            // this logic work even if it's about the last line in the given file.
            final String lines_final = String.format("\n%s", lines);
            text.insert(linesEnd, lines_final);
        });

        final int sel_offset = selEnd[0] - selStart[0] + 1;
        selStart[0] += sel_offset;
        selEnd[0] += sel_offset;

        hlEditor.setSelection(
                TextViewUtils.getIndexFromLineOffset(text, selStart),
                TextViewUtils.getIndexFromLineOffset(text, selEnd));
    }

    public void withKeepSelection(final GsCallback.a2<Integer, Integer> action) {
        _hlEditor.withAutoFormatDisabled(() -> TextViewUtils.withKeepSelection(_hlEditor.getText(), action));
    }

    public void withKeepSelection(final GsCallback.a0 action) {
        withKeepSelection((start, end) -> action.callback());
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

        MarkorDialogFactory.showSpecialKeyDialog(getActivity(), _specialKeyDialogState, (callbackPayload) -> {
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

    public void runJumpBottomTopAction(ActionItem.DisplayMode displayMode) {
        if (displayMode == ActionItem.DisplayMode.EDIT) {
            int pos = _hlEditor.getSelectionStart();
            _hlEditor.setSelection(pos == 0 ? _hlEditor.getText().length() : 0);
        } else if (displayMode == ActionItem.DisplayMode.VIEW) {
            boolean top = _webView.getScrollY() > 100;
            _webView.scrollTo(0, top ? 0 : _webView.getContentHeight());
            if (!top) {
                _webView.scrollBy(0, 1000);
                _webView.scrollBy(0, 1000);
            }
        }
    }

}
