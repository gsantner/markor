/*#######################################################
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.general;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class DatetimeFormatDialog {

    private static Calendar _datetime;

    /**
     *
     * @param activity
     *          {@link Activity} from which is {@link DatetimeFormatDialog} called
     * @param hlEditor
     *          {@link HighlightingEditor} which 'll add selected result to cursor position
     */
    public static void showDatetimeFormatDialog(final Activity activity,
                                                final HighlightingEditor hlEditor) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View viewRoot = activity.getLayoutInflater().inflate(R.layout.time_format_dialog, (ViewGroup) null);

        _datetime = Calendar.getInstance();
        _datetime.set(Calendar.MILLISECOND, 0);
        _datetime.set(Calendar.SECOND, 0);

        String[] defaultDatetimeFormats = activity.getBaseContext().getResources().getStringArray(
                R.array.time_date_formats_array);

        final ListPopupWindow popupWindow = new ListPopupWindow(activity);;
        final EditText timeFormatEditText = viewRoot.findViewById(R.id.time_date_format_input);
        final TextView datetimeTextView = viewRoot.findViewById(R.id.format_example);
        final Button datePickButton = viewRoot.findViewById(R.id.date_format_picker);
        final Button timePickButton = viewRoot.findViewById(R.id.time_format_picker);
        final CheckBox selectFormatCheckBox = viewRoot.findViewById(R.id.inser_format_check_box);
        selectFormatCheckBox.setChecked(false);
        final CheckBox useActualTimeCheckBox = viewRoot.findViewById(R.id.use_actual_time);
        useActualTimeCheckBox.setChecked(false);

        // combo box for format ->> we can write our own format or select one of default formats
        timeFormatEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT= 2;
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getX() >= (v.getWidth() - ((EditText) v)
                            .getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        popupWindow.show();
                        return true;
                    }
                }
                return false;
            }
        });

        popupWindow.setAdapter(new SimpleAdapter(activity, getFormatAndDatetimeExample(defaultDatetimeFormats),
                R.layout.extended_simple_list_item_2, new String[] {"format", "_datetime"},
                new int[] {android.R.id.text1, android.R.id.text2}));
        popupWindow.setAnchorView(timeFormatEditText);
        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                timeFormatEditText.setText(defaultDatetimeFormats[position]);
                popupWindow.dismiss();
                if(useActualTimeCheckBox.isChecked()) {
                    setDatetimeActualTime();
                }
                datetimeTextView.setText(parseDateTimeToCustomFromat(
                        timeFormatEditText.getText().toString(), _datetime.getTimeInMillis()));
            }
        });

        // check for changes in combo box every 2 sec(delay)
        timeFormatEditText.addTextChangedListener(new TextWatcher() {
            boolean isTyping = false;
            private final int DELAY = 2000;
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
                if(!isTyping) {
                    isTyping = true;
                } else {
                    if(editTime + DELAY > System.currentTimeMillis()) {
                        isTyping = false;
                        datetimeTextView.setText(parseDateTimeToCustomFromat(
                                timeFormatEditText.getText().toString(), _datetime.getTimeInMillis()));
                    }
                }
            }
        });

        // implement date picker
        datePickButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatePickerDialog(activity,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int day) {
                                _datetime.set(Calendar.YEAR, year);
                                _datetime.set(Calendar.MONTH, month);
                                _datetime.set(Calendar.DAY_OF_MONTH, day);
                                datetimeTextView.setText(parseDateTimeToCustomFromat(
                                        timeFormatEditText.getText().toString(), _datetime.getTimeInMillis()));
                            }
                        }, _datetime.get(Calendar.YEAR), _datetime.get(Calendar.MONTH),
                        _datetime.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // implement time picker
        timePickButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new TimePickerDialog(activity,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hour, int min) {
                                _datetime.set(Calendar.HOUR_OF_DAY, hour);
                                _datetime.set(Calendar.MINUTE, min);
                                datetimeTextView.setText(parseDateTimeToCustomFromat(
                                        timeFormatEditText.getText().toString(), _datetime.getTimeInMillis()));
                            }
                        }, _datetime.get(Calendar.HOUR_OF_DAY), _datetime.get(Calendar.MINUTE),
                        true).show();
            }
        });

        // hide buttons when both check box are checked
        selectFormatCheckBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(isButtonVisible(selectFormatCheckBox.isChecked(), useActualTimeCheckBox.isChecked())) {
                    timePickButton.setVisibility(View.GONE);
                    datePickButton.setVisibility(View.GONE);
                } else {
                    timePickButton.setVisibility(View.VISIBLE);
                    datePickButton.setVisibility(View.VISIBLE);
                }

            }
        });

        // hide buttons when both check box are checked
        useActualTimeCheckBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(isButtonVisible(selectFormatCheckBox.isChecked(), useActualTimeCheckBox.isChecked())) {
                    timePickButton.setVisibility(View.GONE);
                    datePickButton.setVisibility(View.GONE);
                } else {
                    timePickButton.setVisibility(View.VISIBLE);
                    datePickButton.setVisibility(View.VISIBLE);
                }

            }
        });

        // set builder and implement buttons to discard and submit
        builder.setView(viewRoot)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        hlEditor.insertOrReplaceTextOnCursor(getOutput(
                                selectFormatCheckBox.isChecked(),
                                datetimeTextView.getText().toString(),
                                timeFormatEditText.getText().toString()));
                    }
                });

        builder.show();
    }

    /**
     *
     * @param timeFormat
     *          {@link String} text which 'll be used as format for {@link SimpleDateFormat}
     * @param datetime
     *          {@link Long} selected _datetime in milisecond
     * @return
     *          formatted _datetime
     */
    private static String parseDateTimeToCustomFromat(String timeFormat, Long datetime) {
        try {
            DateFormat formatter = new SimpleDateFormat(timeFormat);
            return formatter.format(datetime);
        } catch (Exception e) {
            // ToDO: some exception handler about not acceptable format maybe??
            return null;
        }
    }

    /**
     *
     * @param isDatetimeSelected
     *          {@link Boolean} information if we want _datetime or format
     * @param datetime
     *          selected _datetime as {@link String} based on given format
     * @param format
     *          {@link String} pattern used to convert _datetime into text output
     * @return
     *          @datetime or @format, based on @isDatetimeSelected
     */
    private static String getOutput(Boolean isDatetimeSelected, String datetime, String format) {
        return isDatetimeSelected != null && isDatetimeSelected ? format : datetime;
    }

    /**
     *
     * @param defaultDatetimeFormats
     *          {@link String...} contains all default _datetime formats
     * @return
     *          extends {@link String...} with preview of given formats
     */
    private static String[] getFormatsAndDatetimeExamples(String[] defaultDatetimeFormats) {
        String[] result = new String[defaultDatetimeFormats.length * 2];
        for (int i = 0; i < defaultDatetimeFormats.length; i++) {
            result[i * 2] = defaultDatetimeFormats[i];
            result[(i * 2) + 1] = parseDateTimeToCustomFromat(
                    defaultDatetimeFormats[i], System.currentTimeMillis());
        }
        return  result;
    }

    /**
     *
     * @param defaultDatetimeFormatsWithExample
     *          {@link String...} contains all default _datetime formats with preview
     * @return
     *          {@link List} of mapped pairs ->> format + preview
     */
    private static List<Map<String, String>> getFormatAndDatetimeExample(String[] defaultDatetimeFormatsWithExample ) {
        List<Map<String, String>> formatAndDatetimeExample = new ArrayList<>();
        String[] defaultFormatsWithExample = getFormatsAndDatetimeExamples(defaultDatetimeFormatsWithExample);

        for (int i = 0; i < defaultFormatsWithExample.length; i++) {
            Map<String, String> pair = new HashMap<>(2);
            pair.put("format", defaultFormatsWithExample[i++]);
            pair.put("_datetime", defaultFormatsWithExample[i]);
            formatAndDatetimeExample.add(pair);
        }
        return formatAndDatetimeExample;
    }

    /**
     * set _datetime to current time
     */
    private static void setDatetimeActualTime() {
        _datetime = Calendar.getInstance();
    }

    /**
     *
     * @param insertFormat
     *      {@link Boolean} pass information if selectFormatCheckBox is checked
     * @param useActualTime
     *      {@link Boolean} pass information if useActualTimeCheckBox is checked
     * @return
     *      TRUE when both checkbox are checked otherwise return FALSE
     */
    private static boolean isButtonVisible(boolean insertFormat, boolean useActualTime) {
        return insertFormat && useActualTime;
    }

}