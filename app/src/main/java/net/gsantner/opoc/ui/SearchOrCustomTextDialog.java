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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class SearchOrCustomTextDialog {

    public static class DialogOptions {
        public Callback.a1<String> callback = null;
        public Callback.a2<String, Integer> withPositionCallback = null;
        public Callback.a1<List<Pair<String, Integer>>> multiSelectCallback = null;
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
        public List<Integer> preSelected = Collections.emptyList();

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

    private static class WithPositionAdapter extends ArrayAdapter<Pair<String, Integer>> {
        @LayoutRes
        final int _layout;
        final LayoutInflater _inflater;
        final DialogOptions _dopt;
        final List<Pair<String, Integer>> _filteredItems;
        final Pattern _extraPattern;
        final Set<Integer> _selected;

        WithPositionAdapter(Context c_context, @LayoutRes int c_layout, List<Pair<String, Integer>> c_filteredItems, DialogOptions c_dopt) {
            super(c_context, c_layout, c_filteredItems);
            _inflater = LayoutInflater.from(c_context);
            _layout = c_layout;
            _dopt = c_dopt;
            _filteredItems = c_filteredItems;
            _extraPattern = (c_dopt.extraFilter == null ? null : Pattern.compile(c_dopt.extraFilter));
            _selected = new TreeSet<>(c_dopt.preSelected != null ? c_dopt.preSelected : Collections.emptyList());
        }

        @NonNull
        @Override
        public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
            final Pair<String, Integer> item = getItem(pos);
            final String text = item.first;
            final int posInOriginalList = item.second;

            final TextView textView;
            if (convertView == null) {
                textView = (TextView) _inflater.inflate(_layout, parent, false);
            } else {
                textView = (TextView) convertView;
            }

            textView.setActivated(_selected.contains(posInOriginalList));

            if (posInOriginalList >= 0 && _dopt.iconsForData != null && posInOriginalList < _dopt.iconsForData.size() && _dopt.iconsForData.get(posInOriginalList) != 0) {
                textView.setCompoundDrawablesWithIntrinsicBounds(_dopt.iconsForData.get(posInOriginalList), 0, 0, 0);
                textView.setCompoundDrawablePadding(32);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    textView.setCompoundDrawableTintList(ColorStateList.valueOf(_dopt.isDarkDialog ? Color.WHITE : Color.BLACK));
                }
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

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

        @Override
        public Filter getFilter() {
            return new Filter() {
                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(final CharSequence constraint, final FilterResults results) {
                    _filteredItems.clear();
                    _filteredItems.addAll((List<Pair<String, Integer>>) results.values);
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    final ArrayList<Pair<CharSequence, Integer>> resList = new ArrayList<>();

                    if (_dopt.data != null) {
                        final String fil = constraint.toString();
                        final boolean emptySearch = fil.isEmpty();
                        for (int i = 0; i < _dopt.data.size(); i++) {
                            final CharSequence str = _dopt.data.get(i);
                            final boolean matchExtra = (_extraPattern == null) || _extraPattern.matcher(str).find();
                            final boolean matchNormal = str.toString().toLowerCase(Locale.getDefault()).contains(fil.toLowerCase(Locale.getDefault()));
                            final boolean matchRegex = _dopt.searchIsRegex && (str.toString().matches(fil));
                            if (matchExtra && (matchNormal || matchRegex || emptySearch)) {
                                resList.add(new Pair<>(str, i));
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

    private static <T> boolean toggleSet(Set<T> set, T item) {
        if (set.contains(item)) {
            set.remove(item);
        } else {
            set.add(item);
        }
        return set.contains(item);
    }

    /**
     * Acts as a state machine setting the dialog state beween multi-select and normal mode.
     *
     * Multi-select (if available) is activated when one or more items are selected.
     * It is deactivated when no items are selected. Items can be selected by long
     * press (in all modes) and by short press when multi-select is active.
     *
     * In multi-select more the 'neutral button' is changed to clear
     */
    private static void setDialogState(final AlertDialog dialog, final ListView listView, final WithPositionAdapter adapter) {
        final DialogOptions dopt = adapter._dopt;
        final List<Pair<String, Integer>> filteredItems = adapter._filteredItems;
        final Set<Integer> selected = adapter._selected;
        final Button neutralButton = dialog.getButton(Dialog.BUTTON_NEUTRAL);

        // If in multi-select mode
        if (dopt.multiSelectCallback != null && adapter._selected.size() > 0) {

            // Set neutral button to clear
            neutralButton.setVisibility(Button.VISIBLE);
            neutralButton.setText(R.string.clear);
            neutralButton.setOnClickListener((v) -> {
                adapter._selected.clear();
                adapter.notifyDataSetChanged();
                setDialogState(dialog, listView, adapter);
            });

            // Click listener set to select
            listView.setOnItemClickListener((parent, view, position, id) -> {
                ((TextView) view).setActivated(toggleSet(selected, filteredItems.get(position).second));
                setDialogState(dialog, listView, adapter);
            });

        } else {

            // Specified neutral button action
            if (adapter._dopt.neutralButtonCallback != null && adapter._dopt.neutralButtonText != 0) {
                neutralButton.setVisibility(Button.VISIBLE);
                neutralButton.setText(adapter._dopt.neutralButtonText);
                neutralButton.setOnClickListener((v) -> adapter._dopt.neutralButtonCallback.callback());
            } else {
                neutralButton.setVisibility(Button.INVISIBLE);
            }

            // Click listener set to activate
            listView.setOnItemClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                if (dopt.callback != null) {
                    dopt.callback.callback(filteredItems.get(position).first);
                }
                if (dopt.withPositionCallback != null) {
                    final Pair<String, Integer> item = filteredItems.get(position);
                    dopt.withPositionCallback.callback(item.first, item.second);
                }
            });
        }

        // Long click always selects
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            ((TextView) view).setActivated(toggleSet(selected, filteredItems.get(position).second));
            setDialogState(dialog, listView, adapter);
            return true;
        });
    }

    public static void showMultiChoiceDialogWithSearchFilterUI(final Activity activity, final DialogOptions dopt) {
        final List<Pair<String, Integer>> filteredItems = new ArrayList<>();
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, dopt.isDarkDialog
                ? android.support.v7.appcompat.R.style.Theme_AppCompat_Dialog
                : android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog
        );
        final WithPositionAdapter listAdapter = new WithPositionAdapter(activity, android.R.layout.simple_list_item_activated_1, filteredItems, dopt);

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

        final ListView listView = new ListView(activity);
        final LinearLayout linearLayout = new LinearLayout(activity);
        listView.setAdapter(listAdapter);
        listView.setVisibility(dopt.data != null && !dopt.data.isEmpty() ? View.VISIBLE : View.GONE);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        if (dopt.isSearchEnabled) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int px = (int) (new ContextUtils(listView.getContext()).convertDpToPx(8));
            lp.setMargins(px, px / 2, px, px / 2);
            linearLayout.addView(searchEditText, lp);
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

        if (dopt.isSearchEnabled || dopt.multiSelectCallback != null) {
            dialogBuilder.setPositiveButton(dopt.okButtonText, (dialogInterface, i) -> {
                final String searchText = dopt.isSearchEnabled ? searchEditText.getText().toString() : null;

                // Prefer multiSelectCallback if present and one or more items are selected
                if (dopt.multiSelectCallback != null && listAdapter._selected.size() > 0) {
                    List<Pair<String, Integer>> res = new ArrayList<>();
                    for (final int position : listAdapter._selected) {
                        res.add(new Pair<>(dopt.data.get(position).toString(), position));
                    }
                    dopt.multiSelectCallback.callback(res);
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
