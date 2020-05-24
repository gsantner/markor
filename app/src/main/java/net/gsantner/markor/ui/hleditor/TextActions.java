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
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.TooltipCompat;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
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
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.gsantner.opoc.format.todotxt.SttCommander.DATEF_YYYY_MM_DD;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public abstract class TextActions {
    protected HighlightingEditor _hlEditor;
    protected Document _document;
    protected Activity _activity;
    protected Context _context;
    protected AppSettings _appSettings;
    protected ActivityUtils _au;
    private int _textActionSidePadding;

    public TextActions(Activity activity, Document document) {
        _document = document;
        _activity = activity;
        _au = new ActivityUtils(activity);
        _context = activity != null ? activity : _hlEditor.getContext();
        _appSettings = new AppSettings(_context);
        _textActionSidePadding = (int) (_appSettings.getEditorTextActionItemPadding() * _context.getResources().getDisplayMetrics().density);
    }

    /**
     * Derived classes must implement a callback which inherits from ActionCallback
     */
    protected abstract static class ActionCallback implements View.OnLongClickListener, View.OnClickListener {
    }

    ;

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
    public void saveActionOrder(List<String> keys) {
        StringBuilder builder = new StringBuilder();
        for (String key : keys) builder.append(key).append(',');
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }
        String combinedKeys = builder.toString();

        // Store the keys
        SharedPreferences settings = _activity.getSharedPreferences("action_order", Context.MODE_PRIVATE);
        String formatKey = _activity.getResources().getString(getFormatActionsKey());
        settings.edit().putString(formatKey, combinedKeys).apply();
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
        ArrayList<String> prefKeys = definedKeys;

        String formatKey = _activity.getResources().getString(getFormatActionsKey());
        SharedPreferences settings = _activity.getSharedPreferences("action_order", Context.MODE_PRIVATE);
        String combinedKeys = settings.getString(formatKey, null);

        boolean changed = false;
        if (combinedKeys != null) {
            prefKeys = new ArrayList<String>(Arrays.asList(combinedKeys.split(",")));

            Set<String> prefSet = new HashSet<>(prefKeys);
            Set<String> defSet = new HashSet<>(definedKeys);

            // Add any defined keys which are not in prefs
            defSet.removeAll(prefSet);
            prefKeys.addAll(defSet);

            // Removed any pref keys which are not defined
            prefSet.removeAll(definedKeys);
            prefKeys.removeAll(prefSet);

            changed = defSet.size() > 0 || prefSet.size() > 0;

        }

        if (changed) saveActionOrder(prefKeys);

        return prefKeys;
    }

    public void appendTextActionsToBar(ViewGroup barLayout) {
        if (barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            Map<String, ActionItem> map = getActiveActionMap();
            List<String> orderedKeys = getActionOrder();
            for (String key : orderedKeys) {
                ActionItem action = map.get(key);
                ActionCallback actionCallback = getActionCallback(action.keyId);
                appendTextActionToBar(barLayout, action.iconId, action.stringId, actionCallback, actionCallback);
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

        String replacement = (replaceString == null)? "" : replaceString;

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

    protected class ReplacePattern {
        public Pattern searchPattern;
        public String replacePattern;
        public boolean replaceAll;

        /**
         * Construct a ReplacePattern
         * @param searchPattern regex search pattern
         * @param replacePattern replace string
         * @param replaceAll whether to replace all or just the first
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

    protected void runRegexReplaceAction(List<ReplacePattern> patterns) {
        runRegexReplaceAction(patterns, false);
    }

    /**
     * Runs through a sequence of regex-search-and-replace actions on each selected line.
     *
     * @param patterns An array of ReplacePattern
     * @param matchAll Whether to stop matching subsequent ReplacePatterns after first match+replace
     */
    protected void runRegexReplaceAction(final List<ReplacePattern> patterns, final boolean matchAll) {

        Editable text = _hlEditor.getText();
        int[] selection = StringUtils.getSelection(_hlEditor);

        int lineStart = StringUtils.getLineStart(text, selection[0]);
        int selEnd = StringUtils.getLineEnd(text, selection[1]);

        while (lineStart <= selEnd && lineStart <= text.length()) {

            int lineEnd = StringUtils.getLineEnd(text, lineStart, selEnd);
            CharSequence line = text.subSequence(lineStart, lineEnd);

            for (ReplacePattern pattern : patterns) {
                Matcher matcher = pattern.searchPattern.matcher(line);
                if (matcher.find()) {

                    String newLine;
                    if (pattern.replaceAll) newLine = matcher.replaceAll(pattern.replacePattern);
                    else newLine = matcher.replaceFirst(pattern.replacePattern);

                    text.replace(lineStart, lineEnd, newLine);
                    selEnd += newLine.length() - line.length();

                    if (!matchAll) break; // Exit after first match
                }
            }

            lineStart = StringUtils.getLineEnd(text, lineStart, selEnd) + 1;
        }
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
                    _hlEditor.insertOrReplaceTextOnCursor(new SimpleDateFormat(_appSettings.getString(DatetimeFormatDialog.class.getCanonicalName() + ".lastusedformat", ""), Locale.getDefault()).format(new Date()).replace("\\n", "\n"));
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
                            getAndInsertDate();
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

    protected void getAndInsertDate() { getAndInsertDate("",0); }

    protected void getAndInsertDate(String prefix) { getAndInsertDate(prefix, 0); }

    protected void getAndInsertDate(int deltaDays) { getAndInsertDate("", deltaDays); }

    protected void getAndInsertDate(String prefix, int deltaDays) {

        final int[] selection = StringUtils.getSelection(_hlEditor);
        Editable text = _hlEditor.getText();

        DatePickerDialog.OnDateSetListener listener = (view, year, month, day) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);
            String date = prefix + DATEF_YYYY_MM_DD.format(calendar.getTime());
            text.replace(selection[0], selection[1], date);
        };

        Calendar calendar = Calendar.getInstance();

        // Parse selection for date use if found
        try {
            CharSequence selText = text.subSequence(selection[0], selection[1]);
            Matcher match = SttCommander.PATTERN_IS_DATE.matcher(selText);
            if (match.find()) calendar.setTime(DATEF_YYYY_MM_DD.parse(selText.toString()));
        } catch (ParseException e) {
            // Regex failed?
            e.printStackTrace();
        }

        // Add requested offset
        calendar.add(Calendar.DATE, deltaDays);

        DateTimeFragment dateFragment = new DateTimeFragment()
                .setActivity(_activity)
                .setDateListener(listener)
                .setCalendar(calendar)
                .setExtra("Advanced", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatetimeFormatDialog.showDatetimeFormatDialog(_activity, _hlEditor);
                    }
                });

        dateFragment.show(((FragmentActivity) _activity).getSupportFragmentManager(), "dateFragment");
    }

    public static class DateTimeFragment extends DialogFragment {

        private DatePickerDialog.OnDateSetListener dateSetListener;
        private TimePickerDialog.OnTimeSetListener timeSetListener;
        private Activity activity;
        private String extraText;
        private DialogInterface.OnClickListener extraCallback;
        private int year;
        private int month;
        private int day;
        private int hour;
        private int minute;

        public DateTimeFragment() {
            super();
            setCalendar(Calendar.getInstance());
        }

        public DateTimeFragment setDateListener(DatePickerDialog.OnDateSetListener listener) {
            this.dateSetListener = listener;
            return this;
        }

        public DateTimeFragment setTimeListener(TimePickerDialog.OnTimeSetListener listener) {
            this.timeSetListener = listener;
            return this;
        }

        public DateTimeFragment setActivity(Activity activity) {
            this.activity = activity;
            return this;
        }

        private DateTimeFragment setYear(int year) {
            this.year = year;
            return this;
        }

        private DateTimeFragment setMonth(int month) {
            this.month = month;
            return this;
        }

        private DateTimeFragment setDay(int day) {
            this.day = day;
            return this;
        }

        private DateTimeFragment setHour(int hour) {
            this.hour = hour;
            return this;
        }

        private DateTimeFragment setMinute(int minute) {
            this.minute = minute;
            return this;
        }

        private DateTimeFragment setCalendar(Calendar calendar) {
            setYear(calendar.get(Calendar.YEAR));
            setMonth(calendar.get(Calendar.MONTH));
            setDay(calendar.get(Calendar.DAY_OF_MONTH));
            setHour(calendar.get(Calendar.HOUR_OF_DAY));
            setMinute(calendar.get(Calendar.MINUTE));
            return this;
        }

        private DateTimeFragment setExtra(String text, DialogInterface.OnClickListener listener) {
            this.extraText = text;
            this.extraCallback = listener;
            return this;
        }

        @Override
        public Dialog onCreateDialog (Bundle savedInstanceState){

            AlertDialog dialog = null;
            if (dateSetListener != null) dialog = new DatePickerDialog(activity, dateSetListener, year, month, day);
            else if (timeSetListener != null) dialog = new TimePickerDialog(activity, timeSetListener, hour, minute, true);

            if (dialog != null && extraText != null && extraCallback != null) {
                dialog.setButton(DialogInterface.BUTTON_NEUTRAL, extraText, extraCallback);
            }

            return dialog;
        }
    }
}
