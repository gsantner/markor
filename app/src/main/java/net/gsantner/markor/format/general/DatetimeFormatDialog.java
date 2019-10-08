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
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.support.v4.os.ConfigurationCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatetimeFormatDialog {

    private static Locale CLOCALE;
    private static final String LAST_USED_PREF = DatetimeFormatDialog.class.getCanonicalName() + ".lastusedformat";

    /**
     * @param activity {@link Activity} from which is {@link DatetimeFormatDialog} called
     * @param hlEditor {@link HighlightingEditor} which 'll add selected result to cursor position
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void showDatetimeFormatDialog(final Activity activity, final HighlightingEditor hlEditor) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View viewRoot = activity.getLayoutInflater().inflate(R.layout.time_format_dialog, (ViewGroup) null);

        ContextUtils cu = new ContextUtils(viewRoot.getContext());
        AppSettings as = new AppSettings(viewRoot.getContext());

        CLOCALE = ConfigurationCompat.getLocales(activity.getResources().getConfiguration()).get(0);

        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);

        String[] defaultDatetimeFormats = activity.getBaseContext().getResources().getStringArray(
                R.array.time_date_formats_array);

        final ListPopupWindow popupWindow = new ListPopupWindow(activity);
        final EditText formatEdit = viewRoot.findViewById(R.id.datetime_format_input);
        final TextView previewView = viewRoot.findViewById(R.id.formatted_example);
        final Button datePickButton = viewRoot.findViewById(R.id.start_datepicker_button);
        final Button timePickButton = viewRoot.findViewById(R.id.start_timepicker_button);
        final CheckBox formatInsteadCheckbox = viewRoot.findViewById(R.id.get_format_instead_date_or_time_checkbox);
        formatInsteadCheckbox.setChecked(false);
        final CheckBox alwaysNowCheckBox = viewRoot.findViewById(R.id.always_use_current_datetime_checkbox);

        viewRoot.findViewById(R.id.datetime_format_input_show_spinner).setOnClickListener(v -> popupWindow.show());
        popupWindow.setAdapter(new SimpleAdapter(activity, createAdapterData(defaultDatetimeFormats),
                android.R.layout.simple_expandable_list_item_2, new String[]{"format", "date"},
                new int[]{android.R.id.text1, android.R.id.text2}
        ));
        popupWindow.setAnchorView(formatEdit);
        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            formatEdit.setText(defaultDatetimeFormats[position]);
            popupWindow.dismiss();
            setToNow(cal, alwaysNowCheckBox.isChecked());
            previewView.setText(parseDatetimeFormatToString(formatEdit.getText().toString(), cal.getTimeInMillis()));
        });

        // check for changes in combo box every 2 sec(delay)
        formatEdit.addTextChangedListener(new TextWatcher() {
            boolean isTyping = false;
            private final int DELAY = 100;
            private long editTime = System.currentTimeMillis();

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
                    isTyping = false;
                    setToNow(cal, alwaysNowCheckBox.isChecked());
                    previewView.setText(parseDatetimeFormatToString(formatEdit.getText().toString(), cal.getTimeInMillis()));
                }
            }
        });

        formatEdit.setText(as.getString(LAST_USED_PREF, ""));
        viewRoot.findViewById(R.id.time_format_last_used).setEnabled(!as.getString(LAST_USED_PREF, "").isEmpty());
        viewRoot.findViewById(R.id.time_format_last_used).setOnClickListener(b -> formatEdit.setText(as.getString(LAST_USED_PREF, "")));
        viewRoot.findViewById(R.id.time_format_just_date).setOnClickListener(b -> formatEdit.setText(cu.getLocalizedDateFormat()));
        viewRoot.findViewById(R.id.time_format_just_time).setOnClickListener(b -> formatEdit.setText(cu.getLocalizedTimeFormat()));

        // DatePicker Dialog
        datePickButton.setOnClickListener(button -> new DatePickerDialog(activity, (view, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    previewView.setText(parseDatetimeFormatToString(formatEdit.getText().toString(), cal.getTimeInMillis()));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        );

        // TimePicker Dialog
        timePickButton.setOnClickListener(button -> new TimePickerDialog(activity, (timePicker, hour, min) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, min);
                    previewView.setText(parseDatetimeFormatToString(formatEdit.getText().toString(), cal.getTimeInMillis()));
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        );

        // hide buttons when both check box are checked
        View.OnClickListener onOptionsChangedListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean dateChangeable = !formatInsteadCheckbox.isChecked() && !alwaysNowCheckBox.isChecked();
                timePickButton.setEnabled(dateChangeable);
                datePickButton.setEnabled(dateChangeable);
                alwaysNowCheckBox.setEnabled(!formatInsteadCheckbox.isChecked());
            }
        };

        formatInsteadCheckbox.setOnClickListener(onOptionsChangedListener);
        alwaysNowCheckBox.setOnClickListener(onOptionsChangedListener);

        // set builder and implement buttons to discard and submit
        builder.setView(viewRoot)
                .setNeutralButton(R.string.help, (dialog, which) -> cu.openWebpageInExternalBrowser("https://developer.android.com/reference/java/text/SimpleDateFormat#date-and-time-patterns"))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        as.setString(LAST_USED_PREF, formatEdit.getText().toString());
                        setToNow(cal, alwaysNowCheckBox.isChecked());
                        String text = parseDatetimeFormatToString(formatEdit.getText().toString(), cal.getTimeInMillis());
                        previewView.setText(text);
                        hlEditor.insertOrReplaceTextOnCursor(getOutput(
                                formatInsteadCheckbox.isChecked(), text, formatEdit.getText().toString())
                        );
                    }
                });

        builder.show();
    }

    /**
     * @param timeFormat {@link String} text which 'll be used as format for {@link SimpleDateFormat}
     * @param datetime   {@link Long} selected _datetime in milisecond
     * @return formatted _datetime
     */
    private static String parseDatetimeFormatToString(String timeFormat, Long datetime) {
        try {
            DateFormat formatter = new SimpleDateFormat(timeFormat.replace("\\n", "\n"), CLOCALE);
            return formatter.format(datetime);
        } catch (Exception e) {
            // ToDO: some exception handler about not acceptable format maybe??
            return null;
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
     * @param defaultDatetimeFormats {@link String...} contains all default _datetime formats
     * @return extends {@link String...} with preview of given formats
     */
    private static String[] expandFormatsWithValues(String[] defaultDatetimeFormats) {
        String[] result = new String[defaultDatetimeFormats.length * 2];
        for (int i = 0; i < defaultDatetimeFormats.length; i++) {
            result[i * 2] = defaultDatetimeFormats[i];
            result[(i * 2) + 1] = parseDatetimeFormatToString(defaultDatetimeFormats[i], System.currentTimeMillis());
        }
        return result;
    }

    /**
     * @param formats {@link String...} contains all default _datetime formats with preview
     * @return {@link List} of mapped pairs ->> format + preview
     */
    private static List<Map<String, String>> createAdapterData(String[] formats) {
        List<Map<String, String>> pairs = new ArrayList<>();
        String[] formatAndParsed = expandFormatsWithValues(formats);

        for (int i = 0; i < formatAndParsed.length; i++) {
            Map<String, String> pair = new HashMap<>(2);
            pair.put("format", formatAndParsed[i++]);
            pair.put("date", formatAndParsed[i]);
            pairs.add(pair);
        }
        return pairs;
    }

    /**
     * set cal to current time if doIt is set
     */
    private static void setToNow(final Calendar cal, boolean doIt) {
        if (doIt) {
            cal.setTime(new Date());
        }
    }
}