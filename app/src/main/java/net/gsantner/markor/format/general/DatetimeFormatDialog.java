/*#######################################################
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.general;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.os.ConfigurationCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.opoc.util.Callback;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DatetimeFormatDialog {

    private static final String DATETIME_SETTINGS = "datetime_dialog_settings";
    private static final String RECENT_FORMATS_STRING = "recent_formats_string";
    private static final String RECENT_FORMATS_LENGTHS = "recent_formats_lengths";

    private static final int MAX_RECENT_FORMATS = 5;

    private final static String[] PREDEFINED_DATE_TIME_FORMATS = {
            "hh:mm",
            "yyyy-MM-dd",
            "dd.MM.yyyy",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "yyyy/MM/dd",
            "MMM yyyy",
            "hh:mm:ss",
            "HH:mm:ss",
            "dd hh:mm",
            "dd-MM-yyyy hh:mm",
            "dd-MM-yyyy hh:mm:ss.s",
            "dd-MM-yyyy HH:mm",
            "dd-MM-yyyy HH:mm:ss.s",
            "dd-MM-yy",
            "MM/dd/yy",
            "dd.MM.yy",
            "yy/MM/dd",
            "dd hh:mm:ss",
            "'[W'w']' EEEE, dd.MM.yyyy",
            "'\\n[W'w']' EEEE, dd.MM.yyyy'\\n‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾\\n'",
    };

    /**
     * @param activity {@link Activity} from which is {@link DatetimeFormatDialog} called
     * @param hlEditor {@link HighlightingEditor} which 'll add selected result to cursor position
     */
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n, InflateParams"})
    public static void showDatetimeFormatDialog(final Activity activity, final HighlightingEditor hlEditor) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View viewRoot = activity.getLayoutInflater().inflate(R.layout.time_format_dialog, null);

        final ContextUtils cu = new ContextUtils(viewRoot.getContext());
        final AppSettings as = new AppSettings(viewRoot.getContext());

        final Locale locale = ConfigurationCompat.getLocales(activity.getResources().getConfiguration()).get(0);

        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);

        final AtomicReference<Dialog> dialog = new AtomicReference<>();
        final AtomicReference<Callback.a1<String>> callbackInsertTextToEditor = new AtomicReference<>();
        final ListPopupWindow popupWindow = new ListPopupWindow(activity);
        final TextView dateHeadline = viewRoot.findViewById(R.id.date_headline);
        final EditText formatEditText = viewRoot.findViewById(R.id.datetime_format_input);
        final TextView previewTextView = viewRoot.findViewById(R.id.formatted_example);
        final Button datePickButton = viewRoot.findViewById(R.id.start_datepicker_button);
        final Button timePickButton = viewRoot.findViewById(R.id.start_timepicker_button);
        final CheckBox formatInsteadCheckbox = viewRoot.findViewById(R.id.get_format_instead_date_or_time_checkbox);
        final CheckBox alwaysNowCheckBox = viewRoot.findViewById(R.id.always_use_current_datetime_checkbox);

        final List<String> recentFormats = getRecentFormats(activity);
        final List<String> allFormats = getAllFormats(recentFormats);

        // Popup window for ComboBox
        popupWindow.setAdapter(new SimpleAdapter(activity, createAdapterData(locale, allFormats),
                android.R.layout.simple_expandable_list_item_2, new String[]{"format", "date"},
                new int[]{android.R.id.text1, android.R.id.text2}
        ));

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            formatEditText.setText(allFormats.get(position));
            popupWindow.dismiss();
            setToNow(cal, alwaysNowCheckBox.isChecked());
            previewTextView.setText(parseDatetimeFormatToString(locale, formatEditText.getText().toString(), cal.getTimeInMillis()));
        });

        popupWindow.setAnchorView(formatEditText);
        popupWindow.setModal(true);
        viewRoot.findViewById(R.id.datetime_format_input_show_spinner).setOnClickListener(v -> popupWindow.show());

        // monitor format input at combobox and update resulting value
        formatEditText.addTextChangedListener(new TextWatcher() {
            private final int DELAY = 100;
            private long editTime = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                editTime = System.currentTimeMillis();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editTime = System.currentTimeMillis();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (editTime + DELAY > System.currentTimeMillis()) {
                    setToNow(cal, alwaysNowCheckBox.isChecked());
                    previewTextView.setText(parseDatetimeFormatToString(locale, formatEditText.getText().toString(), cal.getTimeInMillis()));
                    final boolean error = previewTextView.getText().toString().isEmpty() && !formatEditText.getText().toString().isEmpty();
                    formatEditText.setError(error ? "^^^!!!  'normal text'" : null);
                    previewTextView.setVisibility(error ? View.GONE : View.VISIBLE);
                }
            }
        });

        // Get most recent, falling back to lastusedformat if necessary
        final String LAST_USED_PREF = DatetimeFormatDialog.class.getCanonicalName() + ".lastusedformat";
        final String lastUsed = (recentFormats.size() > 0) ? recentFormats.get(0) : as.getString(LAST_USED_PREF, "");

        formatEditText.setText(lastUsed);
        viewRoot.findViewById(R.id.time_format_last_used).setEnabled(recentFormats.size() > 0);
        viewRoot.findViewById(R.id.time_format_last_used).setOnClickListener(b -> callbackInsertTextToEditor.get().callback(lastUsed));
        viewRoot.findViewById(R.id.time_format_just_date).setOnClickListener(b -> callbackInsertTextToEditor.get().callback(cu.getLocalizedDateFormat()));
        viewRoot.findViewById(R.id.time_format_just_time).setOnClickListener(b -> callbackInsertTextToEditor.get().callback(cu.getLocalizedTimeFormat()));
        viewRoot.findViewById(R.id.time_format_yyyy_mm_dd).setOnClickListener(b -> callbackInsertTextToEditor.get().callback("yyyy-MM-dd"));

        // Pick Date Dialog
        datePickButton.setOnClickListener(button -> new DatePickerDialog(activity, (view, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    previewTextView.setText(parseDatetimeFormatToString(locale, formatEditText.getText().toString(), cal.getTimeInMillis()));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        );

        // Pick Time Dialog
        timePickButton.setOnClickListener(button -> new TimePickerDialog(activity, (timePicker, hour, min) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, min);
                    previewTextView.setText(parseDatetimeFormatToString(locale, formatEditText.getText().toString(), cal.getTimeInMillis()));
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        );

        // hide buttons when both check box are checked
        View.OnClickListener onOptionsChangedListener = v -> {
            boolean dateChangeable = !formatInsteadCheckbox.isChecked() && !alwaysNowCheckBox.isChecked();
            timePickButton.setEnabled(dateChangeable);
            datePickButton.setEnabled(dateChangeable);
            dateHeadline.setEnabled(!formatInsteadCheckbox.isChecked());
            alwaysNowCheckBox.setEnabled(!formatInsteadCheckbox.isChecked());
        };
        formatInsteadCheckbox.setOnClickListener(onOptionsChangedListener);
        alwaysNowCheckBox.setOnClickListener(onOptionsChangedListener);

        callbackInsertTextToEditor.set((selectedFormat) -> {
            setToNow(cal, alwaysNowCheckBox.isChecked());
            String text = parseDatetimeFormatToString(locale, selectedFormat, cal.getTimeInMillis());
            previewTextView.setText(text);
            hlEditor.insertOrReplaceTextOnCursor(getOutput(
                    formatInsteadCheckbox.isChecked(), text, formatEditText.getText().toString())
            );
            dialog.get().dismiss();
        });

        // set builder and implement buttons to discard and submit
        builder.setView(viewRoot)
                .setNeutralButton(R.string.help, (dlgI, which) -> cu.openWebpageInExternalBrowser("https://developer.android.com/reference/java/text/SimpleDateFormat#date-and-time-patterns"))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlgI, which) -> {
                    final String current = formatEditText.getText().toString();
                    saveRecentFormats(activity, recentFormats, current);
                    callbackInsertTextToEditor.get().callback(current);
                });

        dialog.set(builder.show());
    }

    /**
     * @param locale     {@link Locale} locale
     * @param timeFormat {@link String} text which 'll be used as format for {@link SimpleDateFormat}
     * @param datetime   {@link Long} selected _datetime in milisecond
     * @return formatted _datetime
     */
    private static String parseDatetimeFormatToString(final Locale locale, final String timeFormat, final Long datetime) {
        try {
            DateFormat formatter = new SimpleDateFormat(timeFormat.replace("\\n", "\n"), locale);
            return formatter.format(datetime);
        } catch (Exception e) {
            // ToDO: some exception handler about not acceptable format maybe??
            return "";
        }
    }

    /**
     * @param isUseFormatInstead {@link Boolean} information if we want _datetime or format
     * @param datetime           selected _datetime as {@link String} based on given format
     * @param format             {@link String} pattern used to convert _datetime into text output
     * @return @datetime or @format, based on @isUseFormatInstead
     */
    private static String getOutput(Boolean isUseFormatInstead, String datetime, String format) {
        return isUseFormatInstead != null && isUseFormatInstead ? format : datetime;
    }

    /**
     * Combine recent formats with the predefined formats.
     *
     * @param recent List of recent formats.
     * @return Combined, de-duplicated list.
     */
    private static List<String> getAllFormats(final List<String> recent) {
        final LinkedHashSet<String> formats = new LinkedHashSet<>(recent);
        formats.addAll(Arrays.asList(PREDEFINED_DATE_TIME_FORMATS));
        return new ArrayList<>(formats);
    }

    /**
     * Create data for adapter.
     *
     * @param locale  Locale for generating date-time strings
     * @param formats List of formats to generate format-time pair maps for
     * @return List of format-pair maps
     */
    private static List<Map<String, String>> createAdapterData(final Locale locale, final List<String> formats) {
        List<Map<String, String>> formatsAndParsed = new ArrayList<>();
        final long currentMillis = System.currentTimeMillis();

        // Add recent formats
        for (final String f : formats) {
            Map<String, String> pair = new HashMap<>(2);
            pair.put("format", f);
            pair.put("date", parseDatetimeFormatToString(locale, f, currentMillis));
            formatsAndParsed.add(pair);
        }

        return formatsAndParsed;
    }

    /**
     * set cal to current time if doIt is set
     */
    private static void setToNow(final Calendar cal, boolean doIt) {
        if (doIt) {
            cal.setTime(new Date());
        }
    }

    /**
     * Load recent formats from settings.
     * <p>
     * As formats can contain any character, it is not possible to store an array as as a
     * delimited string. Instead, formats are stored as 2 stings
     * a. A String of concatenated format strings
     * b. A String of comma separated lengths
     * <p>
     * The lengths string is used to split the concatenated string.
     *
     * @param activity Activity in order to get settings
     * @return List of Strings representing recently used formats
     */
    private static List<String> getRecentFormats(final Activity activity) {
        final SharedPreferences settings = activity.getSharedPreferences(DATETIME_SETTINGS, Context.MODE_PRIVATE);
        final String combined = settings.getString(RECENT_FORMATS_STRING, null);
        final String lengths = settings.getString(RECENT_FORMATS_LENGTHS, null);

        // Split the
        final List<String> formats = new ArrayList<>();
        try {
            if (combined != null && lengths != null) {
                int prev = 0;
                for (final String s : lengths.split(",")) {
                    int val = Integer.parseInt(s) + prev;
                    formats.add(combined.substring(prev, val));
                    prev = val;
                }
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return Collections.emptyList();
        }
        return formats;
    }


    /**
     * Save recently used formats, inserting new format if necessary
     *
     * @param activity  Activity in order to get settings
     * @param formats   List of recently used formats
     * @param newFormat New format, will be inserted at the head of formats if it is not null or empty
     */
    private static void saveRecentFormats(final Activity activity, final List<String> formats, final String newFormat) {

        List<String> tempFormats = new ArrayList<>(formats);
        if (newFormat != null && newFormat.trim().length() > 0) {
            tempFormats.add(0, newFormat);
        }

        tempFormats = tempFormats.subList(0, Math.min(tempFormats.size(), MAX_RECENT_FORMATS));

        final List<String> lengths = new ArrayList<>();
        for (final String s : tempFormats) {
            lengths.add(Integer.toString(s.length()));
        }

        final SharedPreferences.Editor edit = activity.getSharedPreferences(DATETIME_SETTINGS, Context.MODE_PRIVATE).edit();
        edit.putString(RECENT_FORMATS_STRING, TextUtils.join("", tempFormats)).apply();
        edit.putString(RECENT_FORMATS_LENGTHS, TextUtils.join(",", lengths)).apply();
    }

    /**
     * Get a date string for the most recently used format
     *
     * @param activity Activity in order to get settings
     * @return String    representing current date / time in last used format
     */
    public static String getMostRecentDate(final Activity activity) {
        final List<String> formats = getRecentFormats(activity);
        if (formats.size() > 0) {
            final Locale locale = ConfigurationCompat.getLocales(activity.getResources().getConfiguration()).get(0);
            return parseDatetimeFormatToString(locale, formats.get(0), System.currentTimeMillis());
        } else {
            return "";
        }
    }
}