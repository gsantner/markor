/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.TooltipCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class SearchOrCustomTextDialog {

    public static class DialogOptions {
        public Callback.a1<String> callback = null;
        public Callback.a1<Integer> positionCallback = null;
        public Callback.a1<List<Integer>> multiSelectCallback = null;
        public List<? extends CharSequence> data = null;
        public List<? extends CharSequence> highlightData = null;
        public List<Integer> iconsForData;
        public String messageText = "";
        public String defaultText = "";
        public boolean isSearchEnabled = true;
        public boolean isDarkDialog = false;
        public int dialogWidthDp = WindowManager.LayoutParams.MATCH_PARENT;
        public int dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
        public int gravity = Gravity.NO_GRAVITY;
        public int searchInputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        public boolean searchIsRegex = false;
        public Callback.a1<Spannable> highlighter = null;
        public String extraFilter = null;
        public List<Integer> preSelected = null;

        public Callback.a0 neutralButtonCallback = null;

        @ColorInt
        public int textColor = 0xFF000000;
        @ColorInt
        public int highlightColor = 0xFF00FF00;
        @StringRes
        public int cancelButtonText = android.R.string.cancel;
        @StringRes
        public int neutralButtonText = 0;
        @StringRes
        public int okButtonText = android.R.string.ok;
        @StringRes
        public int titleText = 0;
        @StringRes
        public int searchHintText = android.R.string.search_go;
    }

    private static class Adapter extends ArrayAdapter<Integer> {
        @LayoutRes
        final int _layout;
        final LayoutInflater _inflater;
        final DialogOptions _dopt;
        final List<Integer> _filteredItems;
        final Pattern _extraPattern;
        final Set<Integer> _selected;
        final int _dp4px;

        public static Adapter newInstance(Context context, DialogOptions dopt) {
            return new Adapter(context, android.R.layout.simple_list_item_activated_1, new ArrayList<>(), dopt);
        }

        private Adapter(Context context, @LayoutRes int layout, List<Integer> filteredItems, DialogOptions dopt) {
            super(context, layout, filteredItems);
            _inflater = LayoutInflater.from(context);
            _layout = layout;
            _dopt = dopt;
            _filteredItems = filteredItems;
            _extraPattern = (_dopt.extraFilter == null ? null : Pattern.compile(_dopt.extraFilter));
            _selected = new HashSet<>(_dopt.preSelected != null ? _dopt.preSelected : Collections.emptyList());
            final ContextUtils cu = new ContextUtils(context);
            _dp4px = (int) Math.ceil(cu.convertDpToPx(4));
            cu.freeContextRef();
        }

        @NonNull
        @Override
        public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
            final int index = getItem(pos);

            final TextView textView;
            if (convertView == null) {
                textView = (TextView) _inflater.inflate(_layout, parent, false);
                textView.setBackgroundResource(R.drawable.search_dialog_selection);
                textView.setPadding(textView.getPaddingLeft(), _dp4px, textView.getPaddingRight(), _dp4px);
            } else {
                textView = (TextView) convertView;
            }

            textView.setActivated(_selected.contains(index));

            if (index >= 0 && _dopt.iconsForData != null && index < _dopt.iconsForData.size() && _dopt.iconsForData.get(index) != 0) {
                textView.setCompoundDrawablesWithIntrinsicBounds(_dopt.iconsForData.get(index), 0, 0, 0);
                textView.setCompoundDrawablePadding(32);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    textView.setCompoundDrawableTintList(ColorStateList.valueOf(_dopt.isDarkDialog ? Color.WHITE : Color.BLACK));
                }
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

            final CharSequence text = _dopt.data.get(index).toString();
            if (_dopt.highlightData != null) {
                final boolean hl = _dopt.highlightData.contains(text);
                textView.setTextColor(hl ? _dopt.highlightColor : _dopt.textColor);
                textView.setTypeface(null, hl ? Typeface.BOLD : Typeface.NORMAL);
            }

            if (_dopt.highlighter != null) {
                Spannable s = new SpannableString(text);
                _dopt.highlighter.callback(s);
                textView.setText(s);
            } else {
                textView.setText(text);
            }

            return textView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(final CharSequence constraint, final FilterResults results) {
                    _filteredItems.clear();
                    _filteredItems.addAll((List<Integer>) results.values);
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    final List<Integer> resList = new ArrayList<>();

                    if (_dopt.data != null) {
                        final String fil = constraint.toString();
                        final boolean emptySearch = fil.isEmpty();
                        for (int i = 0; i < _dopt.data.size(); i++) {
                            final String str = _dopt.data.get(i).toString();
                            final boolean matchExtra = (_extraPattern == null) || _extraPattern.matcher(str).find();
                            final Locale locale = Locale.getDefault();
                            final boolean matchNormal = str.toLowerCase(locale).contains(fil.toLowerCase(locale));
                            final boolean matchRegex = _dopt.searchIsRegex && (str.matches(fil));
                            if (matchExtra && (matchNormal || matchRegex || emptySearch)) {
                                resList.add(i);
                            }
                        }
                    }

                    final FilterResults res = new FilterResults();
                    res.values = resList;
                    res.count = resList.size();
                    return res;
                }
            };
        }
    }

    /**
     * Set dialog state between multi-select and normal mode.
     * <p>
     * Multi-select (if available) is activated when one or more items are selected.
     * It is deactivated when no items are selected. Items can be selected by long
     * press (in all modes) and by short press when multi-select is active.
     * <p>
     * In multi-select more the 'neutral button' is changed to 'Unselect all'
     */
    private static void setDialogState(final AlertDialog dialog, final ListView listView, final Adapter adapter) {
        final DialogOptions dopt = adapter._dopt;
        final List<Integer> filteredItems = adapter._filteredItems;
        final Set<Integer> selected = adapter._selected;
        final Button neutralButton = dialog.getButton(Dialog.BUTTON_NEUTRAL);

        final Callback.a0 setNeutralButtonToClear = () -> {
            final String unsel = dialog.getContext().getString(R.string.clear);
            neutralButton.setText(String.format("%s (%d)", unsel, selected.size()));
        };

        final Callback.a2<Integer, View> toggleSelection = (position, view) -> {
            final boolean startEmpty = selected.isEmpty();
            final int index = filteredItems.get(position);
            if (selected.contains(index)) {
                selected.remove(index);
                ((TextView) view).setActivated(false);
            } else {
                selected.add(index);
                ((TextView) view).setActivated(true);
            }
            setNeutralButtonToClear.callback();
            // Update the dialog state if selected transitions from empty <-> not empty
            if (startEmpty ^ selected.isEmpty()) {
                setDialogState(dialog, listView, adapter);
            }
        };

        // If in multi-select mode
        if (dopt.multiSelectCallback != null && !selected.isEmpty()) {

            // Set neutral button to clear
            neutralButton.setVisibility(Button.VISIBLE);
            setNeutralButtonToClear.callback();
            neutralButton.setOnClickListener((v) -> {
                selected.clear();
                adapter.notifyDataSetChanged();
                setDialogState(dialog, listView, adapter);
            });

            // Click listener set to select
            listView.setOnItemClickListener((parent, view, pos, id) -> toggleSelection.callback(pos, view));

        } else {

            // Specified neutral button action
            if (dopt.neutralButtonCallback != null && dopt.neutralButtonText != 0) {
                neutralButton.setVisibility(Button.VISIBLE);
                neutralButton.setText(adapter._dopt.neutralButtonText);
                neutralButton.setOnClickListener((v) -> {
                    dialog.dismiss();
                    adapter._dopt.neutralButtonCallback.callback();
                });
            } else {
                neutralButton.setVisibility(Button.INVISIBLE);
            }

            // Click listener set to activate
            listView.setOnItemClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                if (dopt.callback != null) {
                    dopt.callback.callback(dopt.data.get(filteredItems.get(position)).toString());
                }
                if (dopt.positionCallback != null) {
                    dopt.positionCallback.callback(filteredItems.get(position));
                }
            });
        }

        // Long click always selects, if multi select is possible
        if (dopt.multiSelectCallback != null) {
            listView.setOnItemLongClickListener((parent, view, pos, id) -> {
                toggleSelection.callback(pos, view);
                return true;
            });
        }
    }

    public static void showMultiChoiceDialogWithSearchFilterUI(final Activity activity, final DialogOptions dopt) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, dopt.isDarkDialog
                ? android.support.v7.appcompat.R.style.Theme_AppCompat_Dialog
                : android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog
        );
        final Adapter listAdapter = Adapter.newInstance(activity, dopt);

        final AppCompatEditText searchEditText = new AppCompatEditText(activity);
        searchEditText.setText(dopt.defaultText);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(dopt.textColor);
        searchEditText.setHintTextColor((dopt.textColor & 0x00FFFFFF) | 0x99000000);
        searchEditText.setHint(dopt.searchHintText);
        searchEditText.setInputType(dopt.searchInputType == 0 ? searchEditText.getInputType() : dopt.searchInputType);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable arg0) {
                listAdapter.getFilter().filter(searchEditText.getText());
            }

            @Override
            public void onTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
            }

            @Override
            public void beforeTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
            }
        });

        final int margin = 2 * listAdapter._dp4px;

        final LinearLayout searchLayout = new LinearLayout(activity);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams lp;
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.gravity = Gravity.START | Gravity.BOTTOM;
        searchLayout.addView(searchEditText, lp);

        // 'Button to clear the search box'
        final ImageView clearButton = new ImageView(activity);
        clearButton.setImageResource(R.drawable.ic_baseline_clear_24);
        TooltipCompat.setTooltipText(clearButton, activity.getString(R.string.clear));
        clearButton.setColorFilter(ContextCompat.getColor(activity, dopt.isDarkDialog ? android.R.color.white : R.color.grey));
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 0);
        lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        lp.setMargins(margin, 0, margin, 0);
        searchLayout.addView(clearButton, lp);
        clearButton.setOnClickListener((v) -> searchEditText.setText(""));

        final ListView listView = new ListView(activity);
        final LinearLayout linearLayout = new LinearLayout(activity);
        listView.setAdapter(listAdapter);
        listView.setVisibility(dopt.data != null && !dopt.data.isEmpty() ? View.VISIBLE : View.GONE);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        if (dopt.isSearchEnabled) {
            lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(margin, margin / 2, margin, margin / 2);
            linearLayout.addView(searchLayout, lp);
        }

        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        layoutParams.weight = 1;
        linearLayout.addView(listView, layoutParams);
        if (!TextUtils.isEmpty(dopt.messageText)) {
            dialogBuilder.setMessage(dopt.messageText);
        }

        dialogBuilder.setView(linearLayout)
                .setOnCancelListener(null)
                .setNegativeButton(dopt.cancelButtonText, (dialogInterface, i) -> dialogInterface.dismiss());

        if (dopt.titleText != 0) {
            dialogBuilder.setTitle(dopt.titleText);
        }

        if ((dopt.isSearchEnabled && dopt.callback != null) || (dopt.multiSelectCallback != null)) {
            dialogBuilder.setPositiveButton(dopt.okButtonText, (dialogInterface, i) -> {
                final String searchText = dopt.isSearchEnabled ? searchEditText.getText().toString() : null;
                // Prefer multiSelectCallback if present and one or more items are selected
                if (dopt.multiSelectCallback != null && !listAdapter._selected.isEmpty()) {
                    final List<Integer> sel = new ArrayList<>(listAdapter._selected);
                    Collections.sort(sel);
                    dopt.multiSelectCallback.callback(sel);
                } else if (dopt.callback != null && !TextUtils.isEmpty(searchText)) {
                    dopt.callback.callback(searchText);
                }
            });
        }

        final AlertDialog dialog = dialogBuilder.create();

        searchEditText.setOnKeyListener((keyView, keyCode, keyEvent) -> {
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                dialog.dismiss();
                if (dopt.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                    dopt.callback.callback(searchEditText.getText().toString());
                }
                return true;
            }
            return false;
        });

        Window w;
        if ((w = dialog.getWindow()) != null && dopt.isSearchEnabled) {
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        dialog.show();

        if ((w = dialog.getWindow()) != null) {
            int ds_w = dopt.dialogWidthDp < 100 ? dopt.dialogWidthDp : ((int) (dopt.dialogWidthDp * activity.getResources().getDisplayMetrics().density));
            int ds_h = dopt.dialogHeightDp < 100 ? dopt.dialogHeightDp : ((int) (dopt.dialogHeightDp * activity.getResources().getDisplayMetrics().density));
            w.setLayout(ds_w, ds_h);
        }

        if ((w = dialog.getWindow()) != null && dopt.gravity != Gravity.NO_GRAVITY) {
            WindowManager.LayoutParams wlp = w.getAttributes();
            wlp.gravity = dopt.gravity;
            w.setAttributes(wlp);
        }

        if (dopt.isSearchEnabled) {
            searchEditText.requestFocus();
        }

        if (dopt.defaultText != null) {
            listAdapter.getFilter().filter(searchEditText.getText());
        }

        setDialogState(dialog, listView, listAdapter);
    }
}
