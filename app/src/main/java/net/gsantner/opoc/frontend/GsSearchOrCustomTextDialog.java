/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.widget.TextViewCompat;

import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("SetTextI18n")
public class GsSearchOrCustomTextDialog {

    public static final int LIST_VIEW_ID = 99;

    public static class DialogOptions {

        // Callback for search text or text of single item
        @Nullable
        public GsCallback.a1<String> callback = null;

        // Callback for indices of selected items.
        // List will contain single item if isMultiSelectEnabled == false;
        @Nullable
        public GsCallback.a1<List<Integer>> positionCallback = null;

        public boolean isMultiSelectEnabled = false;
        public List<? extends CharSequence> data = null;
        public List<? extends CharSequence> highlightData = null;
        public List<Integer> iconsForData;
        public CharSequence messageText = "";
        public String defaultText = "";
        public boolean isDarkDialog = false;
        public boolean isSearchEnabled = true;
        public boolean isSoftInputVisible = true;
        public boolean isDismissOnItemSelected = true;
        public int listPosition = -1;
        public int dialogWidthDp = WindowManager.LayoutParams.MATCH_PARENT;
        public int dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
        public int searchInputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        public String extraFilter = null;
        public Collection<Integer> preSelected = null;
        public GsCallback.a1<Spannable> highlighter = null;
        public GsCallback.a1<AlertDialog> neutralButtonCallback = null;
        public GsCallback.a1<DialogInterface> dismissCallback = null;
        public GsCallback.b2<CharSequence, CharSequence> searchFunction = GsSearchOrCustomTextDialog::standardSearch;
        public @Nullable InputFilter searchInputFilter = null;

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
        public int clearInputIcon = android.R.drawable.ic_menu_close_clear_cancel;
        @StyleRes
        public int dialogStyle = 0;
    }

    public static class DialogState {
        public int listPosition = -1;
        public String defaultText = "";
        public Parcelable instanceState;
    }

    public static class Adapter extends BaseAdapter {
        @LayoutRes
        private final int _layout;
        private final int _layoutHeight;
        private final LayoutInflater _inflater;
        private final DialogOptions _dopt;
        private final Set<Integer> _selectedItems;
        private final Matcher _extraPattern;
        private final ArrayList<Integer> _filteredItems;
        private String _lastConstraint = "";


        @Override
        public int getCount() {
            return _filteredItems.size();
        }

        @Override
        public Object getItem(int position) {
            return _filteredItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private Adapter(final Context context, final DialogOptions dopt) {
            super();
            _filteredItems = new ArrayList<>();
            _layout = dopt.isMultiSelectEnabled ? android.R.layout.simple_list_item_multiple_choice : android.R.layout.simple_list_item_1;
            _inflater = LayoutInflater.from(context);
            _dopt = dopt;
            _extraPattern = (_dopt.extraFilter == null ? null : Pattern.compile(_dopt.extraFilter).matcher(""));
            _selectedItems = new HashSet<>(_dopt.preSelected != null ? _dopt.preSelected : Collections.emptyList());
            _layoutHeight = GsContextUtils.instance.convertDpToPx(context, 36);
        }

        @NonNull
        @Override
        public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
            final int index = _filteredItems.get(pos);

            final TextView textView;
            if (convertView == null) {
                textView = (TextView) _inflater.inflate(_layout, parent, false);
                textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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
                    TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(_dopt.isDarkDialog ? Color.WHITE : Color.BLACK));
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

        public void filter(final CharSequence constraint) {
            _lastConstraint = constraint.toString();
            _filteredItems.clear();

            if (_dopt.data != null) {
                final boolean emptySearch = constraint.length() == 0;
                for (int i = 0; i < _dopt.data.size(); i++) {
                    final String str = _dopt.data.get(i).toString();
                    final boolean matchExtra = (_extraPattern == null) || _extraPattern.reset(str).find();
                    if (matchExtra && (emptySearch || _dopt.searchFunction.callback(constraint, str))) {
                        _filteredItems.add(i);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public void update() {
            filter(_lastConstraint);
        }
    }

    public static Adapter getAdapter(final AlertDialog dialog) {
        final ListView list = dialog.findViewById(GsSearchOrCustomTextDialog.LIST_VIEW_ID);
        return list != null ? (Adapter) list.getAdapter() : null;
    }

    public static boolean standardSearch(final CharSequence constraint, final CharSequence text) {
        final Locale locale = Locale.getDefault();
        return text.toString().toLowerCase(locale).contains(constraint.toString().toLowerCase(locale));
    }

    public static void showMultiChoiceDialogWithSearchFilterUI(final Activity activity, final DialogOptions dopt, final DialogState state) {
        final int dialogStyle = dopt.dialogStyle != 0 ? dopt.dialogStyle : GsContextUtils.instance.getResId(activity,
                GsContextUtils.ResType.STYLE, dopt.isDarkDialog ? "Theme_AppCompat_Dialog" : "Theme_AppCompat_Light_Dialog");
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, dialogStyle);

        final Adapter listAdapter = new Adapter(activity, dopt);

        // Constructing the dialog
        // =========================================================================================
        final View selectAll;
        if (dopt.titleText != 0 || !TextUtils.isEmpty(dopt.messageText) || dopt.isMultiSelectEnabled) {
            // Using a custom view for title and message.
            // This is needed because:
            // 1. https://stackoverflow.com/questions/61339887/alertdialog-doesnt-fit-long-list-view-buttons-if-used-together
            // 2. In order to control spacing
            // And is much less hacky than the other approaches
            final View title = makeTitleView(activity, dopt);
            dialogBuilder.setCustomTitle(title);
            selectAll = title.findViewWithTag("SELECT_ALL");
        } else {
            selectAll = null;
        }

        final LinearLayout mainLayout = new LinearLayout(activity);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // SearchView is currently constructed even if it isn't needed
        final View searchView = makeSearchView(activity, dopt);
        final EditText searchEditText = searchView.findViewWithTag("EDIT");
        searchEditText.addTextChangedListener(GsTextWatcherAdapter.after(listAdapter::filter));

        if (dopt.isSearchEnabled) {
            mainLayout.addView(searchView);

            if (dopt.searchInputFilter != null) {
                searchEditText.setFilters(new InputFilter[]{dopt.searchInputFilter});
            }
        }

        final ListView listView = new ListView(activity);
        listView.setId(LIST_VIEW_ID);
        listView.setAdapter(listAdapter);

        if (state != null && state.instanceState != null) {
            listView.onRestoreInstanceState(state.instanceState);
        }

        if (dopt.listPosition >= 0) {
            listView.setSelection(dopt.listPosition);
        }

        listView.setVisibility(dopt.data != null && !dopt.data.isEmpty() ? View.VISIBLE : View.GONE);
        final LinearLayout.LayoutParams listLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        listLayout.weight = 1;
        mainLayout.addView(listView, listLayout);

        dialogBuilder.setOnDismissListener((dialogInterface) -> {
            if (state != null) {
                // Store/Update dialog state
                state.instanceState = listView.onSaveInstanceState();
                state.listPosition = listView.getFirstVisiblePosition();
                state.defaultText = searchEditText.getText().toString();
            }

            if (dopt.dismissCallback != null) {
                dopt.dismissCallback.callback(dialogInterface);
            }
        });

        dialogBuilder.setView(mainLayout)
                .setOnCancelListener(null)
                .setNegativeButton(dopt.cancelButtonText, (dialogInterface, i) -> dialogInterface.dismiss());

        // =========================================================================================

        // Ok button only present under these circumstances
        final boolean isSearchOk = dopt.callback != null && dopt.isSearchEnabled;
        final boolean isMultiSelOk = dopt.positionCallback != null && dopt.isMultiSelectEnabled;
        final boolean isPlainDialog = dopt.callback != null && (dopt.data == null || dopt.data.isEmpty());
        if (isSearchOk || isMultiSelOk || isPlainDialog) {
            dialogBuilder.setPositiveButton(dopt.okButtonText, (dialogInterface, i) -> {
                final String searchText = dopt.isSearchEnabled ? searchEditText.getText().toString() : null;
                if (dopt.positionCallback != null && !GsCollectionUtils.setEquals(dopt.preSelected, listAdapter._selectedItems)) {
                    // Position callback triggered when selection changed
                    dopt.positionCallback.callback(new ArrayList<>(listAdapter._selectedItems));
                } else if (dopt.callback != null && (!dopt.isSearchEnabled || !TextUtils.isEmpty(searchText))) {
                    dopt.callback.callback(searchText);
                }
            });
        }

        final AlertDialog dialog = dialogBuilder.create();

        searchEditText.setOnKeyListener((keyView, keyCode, keyEvent) -> {
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER) && dopt.okButtonText != 0) {
                dialog.dismiss();
                if (dopt.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                    dopt.callback.callback(searchEditText.getText().toString());
                }
                return true;
            }
            return false;
        });

        dialog.show();

        final Window win = dialog.getWindow();
        if (win != null) {
            if (dopt.isSearchEnabled) {
                if (dopt.isSoftInputVisible) {
                    win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                } else {
                    win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                }
            }

            win.setLayout(
                    dopt.dialogWidthDp < 0 ? dopt.dialogWidthDp : GsContextUtils.instance.convertDpToPx(activity, dopt.dialogWidthDp),
                    dopt.dialogHeightDp < 0 ? dopt.dialogHeightDp : GsContextUtils.instance.convertDpToPx(activity, dopt.dialogHeightDp)
            );
        }

        final Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutralButton != null && dopt.neutralButtonText != 0 && dopt.neutralButtonCallback != null) {
            neutralButton.setVisibility(Button.VISIBLE);
            neutralButton.setText(dopt.neutralButtonText);
            neutralButton.setOnClickListener((button) -> dopt.neutralButtonCallback.callback(dialog));
        }

        if (dopt.isSearchEnabled) {
            searchEditText.requestFocus();
        }

        if (dopt.defaultText != null) {
            listAdapter.filter(searchEditText.getText());
        }

        // Helper function to trigger callback with single item
        final GsCallback.b1<Integer> directActivate = (position) -> {
            final int index = listAdapter._filteredItems.get(position);
            if (dopt.isDismissOnItemSelected) {
                dialog.dismiss();
            }
            if (dopt.callback != null) {
                dopt.callback.callback(dopt.data.get(index).toString());
            } else if (dopt.positionCallback != null) {
                dopt.positionCallback.callback(Collections.singletonList(index));
            }
            return true;
        };

        // Helper function to append selection count to OK button
        final String okString = dopt.okButtonText != 0 ? activity.getString(dopt.okButtonText) : "";
        final Button okButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
        final GsCallback.a0 setOkButtonState = () -> {
            if (okButton != null) {
                if (dopt.isMultiSelectEnabled) {
                    okButton.setText(okString + String.format(" (%d)", listAdapter._selectedItems.size()));
                } else {
                    okButton.setText(okString);
                }
            }
        };

        // Set ok button text initially
        setOkButtonState.callback();
        final GsCallback.a0 setSelectAllButtonState = () -> {
            if (selectAll != null) {
                ((Checkable) selectAll).setChecked(listAdapter._selectedItems.size() >= dopt.data.size());
            }
        };

        // Set select all button state initially
        setSelectAllButtonState.callback();

        if (selectAll != null && dopt.isMultiSelectEnabled) {
            selectAll.setOnClickListener((v) -> {
                if (listAdapter._selectedItems.size() < dopt.data.size()) {
                    listAdapter._selectedItems.addAll(GsCollectionUtils.range(dopt.data.size()));
                } else {
                    listAdapter._selectedItems.clear();
                }
                listAdapter.notifyDataSetChanged();
                setOkButtonState.callback();
                setSelectAllButtonState.callback();
            });

            selectAll.setContentDescription(activity.getString(android.R.string.selectAll));
        }

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
                setSelectAllButtonState.callback();
            } else {
                directActivate.callback(pos);
            }
        });

        listView.setOnItemLongClickListener((parent, view, pos, id) -> directActivate.callback(pos));
    }

    public static void showMultiChoiceDialogWithSearchFilterUI(final Activity activity, final DialogOptions dopt) {
        showMultiChoiceDialogWithSearchFilterUI(activity, dopt, null);
    }

    private static View makeTitleView(final Context context, final DialogOptions dopt) {
        // We nest the title layout within a horizontal layout so we can add a select all button
        // We return the horizontal layout so that the select all button can be found by tag

        final int padding = GsContextUtils.instance.convertDpToPx(context, 4);
        final LinearLayout titleLayout = new LinearLayout(context);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setPadding(4 * padding, 2 * padding, 4 * padding, padding);
        titleLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        );
        titleLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        final LinearLayout titleTextLayout = new LinearLayout(context);
        titleTextLayout.setOrientation(LinearLayout.VERTICAL);
        titleTextLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 5)
        );
        titleLayout.addView(titleTextLayout);

        if (dopt.titleText != 0) {
            final TextView title = new TextView(context, null, android.R.attr.windowTitleStyle);
            title.setSingleLine();
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setText(dopt.titleText);
            title.setPadding(0, 0, 0, 0);
            titleTextLayout.addView(title, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        if (!TextUtils.isEmpty(dopt.messageText)) {
            final TextView subTitle = new TextView(context, null, android.R.attr.textAppearanceMedium);
            subTitle.setPadding(0, dopt.titleText == 0 ? 0 : padding, 0, 0);
            subTitle.setText(dopt.messageText);
            subTitle.setTextIsSelectable(true);
            subTitle.setMovementMethod(LinkMovementMethod.getInstance()); // Allow links to be shown and followed
            titleTextLayout.addView(subTitle, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        if (dopt.isMultiSelectEnabled) {
            // Using a multiple choice text view as a selectable checkbox button
            // Requires no styling to match the existing check boxes
            final LayoutInflater inflater = LayoutInflater.from(context);
            final TextView selectAll = (TextView) inflater.inflate(android.R.layout.simple_list_item_multiple_choice, titleLayout, false);
            selectAll.setTag("SELECT_ALL");
            selectAll.setText("");
            TooltipCompat.setTooltipText(selectAll, context.getString(android.R.string.selectAll));
            // Remove padding to right to help align it
            titleLayout.setPadding(titleLayout.getPaddingLeft(), titleLayout.getPaddingTop(), 0, titleLayout.getPaddingBottom());

            final LinearLayout.LayoutParams selLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 0);
            selLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;

            titleLayout.addView(selectAll, selLp);
        }

        return titleLayout;
    }

    private static View makeSearchView(final Context context, final DialogOptions dopt) {
        final int margin = GsContextUtils.instance.convertDpToPx(context, 8);

        // Main layout
        final LinearLayout searchLayout = new LinearLayout(context);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchLayout.setPadding(margin, 0, margin, margin / 2);
        searchLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Edit text
        final AppCompatEditText searchEditText = new AppCompatEditText(context);
        searchEditText.setText(dopt.defaultText);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(dopt.textColor);
        searchEditText.setHintTextColor((dopt.textColor & 0x00FFFFFF) | 0x99000000);
        searchEditText.setHint(dopt.searchHintText);
        searchEditText.setInputType(dopt.searchInputType == 0 ? searchEditText.getInputType() : dopt.searchInputType);
        searchEditText.setTag("EDIT"); // So we can easily find the search edit text

        final LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        editLp.gravity = Gravity.START | Gravity.BOTTOM;
        searchLayout.addView(searchEditText, editLp);

        // 'Button to clear the search box'
        final ImageView clearButton = new ImageView(context);
        clearButton.setImageResource(dopt.clearInputIcon);
        TooltipCompat.setTooltipText(clearButton, context.getString(android.R.string.cancel));
        clearButton.setColorFilter(dopt.isDarkDialog ? Color.WHITE : Color.parseColor("#ff505050"));
        clearButton.setOnClickListener((v) -> searchEditText.setText(""));
        clearButton.setPadding(margin, 0, margin, 0);

        final LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT, 0);
        clearLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        searchLayout.addView(clearButton, clearLp);

        return searchLayout;
    }
}
