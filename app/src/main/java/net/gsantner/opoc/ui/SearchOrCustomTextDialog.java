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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.TooltipCompat;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.gsantner.opoc.android.dummy.TextWatcherDummy;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@SuppressLint("SetTextI18n")
public class SearchOrCustomTextDialog {

    public static class DialogOptions {

        // Callback for search text or text of single item
        @Nullable
        public Callback.a1<String> callback = null;

        // Callback for indices of selected items.
        // List will contain single item if isMultiSelectEnabled == false;
        @Nullable
        public Callback.a1<List<Integer>> positionCallback = null;

        public boolean isMultiSelectEnabled = false;
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
        @DrawableRes
        public int clearInputIcon = android.R.drawable.ic_input_delete;
    }

    private static class Adapter extends ArrayAdapter<Integer> {
        @LayoutRes
        private final int _layout;
        private final int _layoutHeight;
        private final LayoutInflater _inflater;
        private final DialogOptions _dopt;
        private final List<Integer> _filteredItems;
        private final Set<Integer> _selectedItems;
        private final Pattern _extraPattern;

        public static Adapter create(final Context context, final DialogOptions dopt) {
            return new Adapter(context, dopt, dopt.isMultiSelectEnabled ? android.R.layout.simple_list_item_multiple_choice : android.R.layout.simple_list_item_1, new ArrayList<>());
        }

        private Adapter(final Context context, final DialogOptions dopt, final int layout, final List<Integer> filteredItems) {
            super(context, layout, filteredItems);
            _layout = layout;
            _filteredItems = filteredItems;
            _inflater = LayoutInflater.from(context);
            _dopt = dopt;
            _extraPattern = (_dopt.extraFilter == null ? null : Pattern.compile(_dopt.extraFilter));
            _selectedItems = new HashSet<>(_dopt.preSelected != null ? _dopt.preSelected : Collections.emptyList());
            ContextUtils cu = new ContextUtils(context);
            _layoutHeight = (int) cu.convertDpToPx(36);
            cu.freeContextRef();
        }

        @NonNull
        @Override
        public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
            final int index = getItem(pos);

            final TextView textView;
            if (convertView == null) {
                textView = (TextView) _inflater.inflate(_layout, parent, false);
                ViewGroup.LayoutParams params = textView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                textView.setMinHeight(_layoutHeight);
            } else {
                textView = (TextView) convertView;
            }

            if (textView instanceof Checkable) {
                ((Checkable) textView).setChecked(_selectedItems.contains(index));
            }

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

    public static void showMultiChoiceDialogWithSearchFilterUI(final Activity activity, final DialogOptions dopt) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, dopt.isDarkDialog
                ? android.support.v7.appcompat.R.style.Theme_AppCompat_Dialog
                : android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog
        );
        final Adapter listAdapter = Adapter.create(activity, dopt);

        final AppCompatEditText searchEditText = new AppCompatEditText(activity);
        searchEditText.setText(dopt.defaultText);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(dopt.textColor);
        searchEditText.setHintTextColor((dopt.textColor & 0x00FFFFFF) | 0x99000000);
        searchEditText.setHint(dopt.searchHintText);
        searchEditText.setInputType(dopt.searchInputType == 0 ? searchEditText.getInputType() : dopt.searchInputType);
        searchEditText.addTextChangedListener(TextWatcherDummy.after((cbEditable) -> listAdapter.getFilter().filter(cbEditable)));

        final ContextUtils cu = new ContextUtils(activity);
        final int margin = (int) cu.convertDpToPx(8);
        cu.freeContextRef();

        final LinearLayout searchLayout = new LinearLayout(activity);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams lp;
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.gravity = Gravity.START | Gravity.BOTTOM;
        searchLayout.addView(searchEditText, lp);

        // 'Button to clear the search box'
        final ImageView clearButton = new ImageView(activity);
        clearButton.setImageResource(dopt.clearInputIcon);
        TooltipCompat.setTooltipText(clearButton, activity.getString(android.R.string.cancel));
        clearButton.setColorFilter(dopt.isDarkDialog ? Color.WHITE : Color.parseColor("#ff505050"));
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 0);
        lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        lp.setMargins(margin, 0, (int) (margin * 1.5), 0);
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

        // Ok button action
        if ((dopt.isSearchEnabled && dopt.callback != null) || (dopt.isMultiSelectEnabled)) {
            dialogBuilder.setPositiveButton(dopt.okButtonText, (dialogInterface, i) -> {
                final String searchText = dopt.isSearchEnabled ? searchEditText.getText().toString() : null;
                if (dopt.positionCallback != null && !listAdapter._selectedItems.isEmpty()) {
                    final List<Integer> sel = new ArrayList<>(listAdapter._selectedItems);
                    Collections.sort(sel);
                    dopt.positionCallback.callback(sel);
                } else if (dopt.callback != null && !TextUtils.isEmpty(searchText)) {
                    dopt.callback.callback(searchText);
                }
            });
        }

        // Setup neutralbutton
        if (dopt.neutralButtonCallback != null && dopt.neutralButtonText != 0) {
            dialogBuilder.setNeutralButton(dopt.neutralButtonText, (dialogInterface, i) -> {
                dopt.neutralButtonCallback.callback();
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

        // Helper function to trigger callback with single item
        final Callback.b1<Integer> directActivate = (position) -> {
            final int index = listAdapter._filteredItems.get(position);
            dialog.dismiss();
            if (dopt.callback != null) {
                dopt.callback.callback(dopt.data.get(index).toString());
            }
            if (dopt.positionCallback != null) {
                dopt.positionCallback.callback(Collections.singletonList(index));
            }
            return true;
        };

        // Helper function to append selection count to OK button
        final Button okButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
        final String okText = activity.getString(dopt.okButtonText) + (dopt.isMultiSelectEnabled ? " (%d)" : "");
        final Callback.a0 setOkButtonState = () -> {
            okButton.setText(okText.replace("%d", Integer.toString(listAdapter._selectedItems.size())));
        };

        // Set ok button text initially
        setOkButtonState.callback();

        // Item click action
        listView.setOnItemClickListener((parent, textView, pos, id) -> {
            if (dopt.isMultiSelectEnabled) {
                final int index = listAdapter._filteredItems.get(pos);
                if (listAdapter._selectedItems.contains(index)) {
                    listAdapter._selectedItems.remove(index);
                } else {
                    listAdapter._selectedItems.add(index);
                }
                if (textView instanceof Checkable) {
                    ((Checkable) textView).setChecked(listAdapter._selectedItems.contains(index));
                }
                setOkButtonState.callback();
            } else {
                directActivate.callback(pos);
            }
        });

        // long click always activates
        listView.setOnItemLongClickListener((parent, view, pos, id) -> directActivate.callback(pos));
    }
}
