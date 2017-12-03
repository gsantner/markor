/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.net> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.opoc.ui;

import android.app.Activity;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.gsantner.opoc.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("WeakerAccess")
public class SearchOrCustomTextDialog {

    public static class DialogOptions {
        public Callback.a1<String> callback;
        public List<String> data = new ArrayList<>();
        public List<String> highlightData = new ArrayList<>();
        public String messageText = "";
        public boolean isSearchEnabled = true;
        public boolean isDarkDialog = false;

        @ColorInt
        public int textColor = 0xFF000000;
        @ColorInt
        public int highlightColor = 0xFF00FF00;
        @StringRes
        public int cancelButtonText = android.R.string.cancel;
        @StringRes
        public int okButtonText = android.R.string.ok;
        @StringRes
        public int titleText = android.R.string.untitled;
        @StringRes
        public int searchHintText = android.R.string.search_go;
    }

    public static void showMultiChoiceDialogWithSearchFilterUI(final Activity activity, final DialogOptions dopt) {
        final List<String> allItems = new ArrayList<>(dopt.data);
        final List<String> filteredItems = new ArrayList<>(allItems);
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, dopt.isDarkDialog
                ? android.support.v7.appcompat.R.style.Theme_AppCompat_Dialog
                : android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog
        );

        final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, filteredItems) {
            @NonNull
            @Override
            public View getView(int pos, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(pos, convertView, parent);
                String text = textView.getText().toString();

                textView.setTextColor(dopt.textColor);
                if (dopt.highlightData.contains(text)) {
                    textView.setTextColor(dopt.highlightColor);
                }
                return textView;
            }

            @Override
            public Filter getFilter() {
                return new Filter() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void publishResults(final CharSequence constraint, final FilterResults results) {
                        filteredItems.clear();
                        filteredItems.addAll((List<String>) results.values);
                        notifyDataSetChanged();
                    }

                    @Override
                    protected FilterResults performFiltering(final CharSequence constraint) {
                        final FilterResults res = new FilterResults();
                        final ArrayList<String> resList = new ArrayList<>();
                        final String fil = constraint.toString();

                        for (final String str : allItems) {
                            if ("".equals(fil) || str.toLowerCase(Locale.getDefault()).contains(fil.toLowerCase(Locale.getDefault()))) {
                                resList.add(str);
                            }
                        }
                        res.values = resList;
                        res.count = resList.size();
                        return res;
                    }
                };
            }
        };

        final AppCompatEditText searchEditText = new AppCompatEditText(activity);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(dopt.textColor);
        searchEditText.setHintTextColor((dopt.textColor & 0x00FFFFFF) | 0x99000000);
        searchEditText.setHint(dopt.searchHintText);

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
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (dopt.isSearchEnabled) {
            linearLayout.addView(searchEditText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        layoutParams.weight = 1;
        linearLayout.addView(listView, layoutParams);
        if (!TextUtils.isEmpty(dopt.messageText)) {
            dialogBuilder.setMessage(dopt.messageText);
        }
        dialogBuilder.setView(linearLayout)
                .setTitle(dopt.titleText)
                .setOnCancelListener(null)
                .setNegativeButton(dopt.cancelButtonText, null)
                .setPositiveButton(dopt.okButtonText, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    if (dopt.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                        dopt.callback.callback(searchEditText.getText().toString());
                    }
                });

        final AlertDialog dialog = dialogBuilder.create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            if (dopt.callback != null) {
                dopt.callback.callback(filteredItems.get(position));
            }
        });

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

        dialog.show();
    }
}
