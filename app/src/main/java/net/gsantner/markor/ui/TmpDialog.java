package net.gsantner.markor.ui;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// TODO: temporary solution for dialog - maybe go for a new one based on FileSystemDialog
// https://stackoverflow.com/questions/15868940/android-multichoice-item-dialog-with-search-functionality
public class TmpDialog {
    public static void showTodoTxtContextDialog(Activity activity, List<String> availableData, List<String> selectedData, Callback<String> callback) {
        TmpDialog.Options opt = new TmpDialog.Options();
        opt.callback = callback;
        opt.data = availableData;
        opt.alreadyContained = selectedData;
        opt.titleText = R.string.category;
        opt.searchHint = R.string.search_hint;

        TmpDialog.showMultiChoiceDialogWithSearchFilterUI(activity, opt);
    }

    public static class Options {
        public Callback<String> callback;
        public List<String> data = new ArrayList<>();
        public List<String> alreadyContained = new ArrayList<>();
        @StringRes
        public int cancelButtonText = android.R.string.cancel;
        @StringRes
        public int okButtonText = android.R.string.ok;
        @StringRes
        public int titleText = android.R.string.untitled;
        @StringRes
        public int searchHint = android.R.string.search_go;
        @ColorRes
        public int preSelectedColor = android.R.color.holo_green_dark;
    }

    public interface Callback<T> {
        void onDataSelected(T data);
    }

    private static final class ListItemWithIndex {
        public final int index;
        public final String value;

        public ListItemWithIndex(final int index, final String value) {
            super();
            this.index = index;
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static void showMultiChoiceDialogWithSearchFilterUI(final Activity activity, final Options dopt) {
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);

        final List<ListItemWithIndex> allItems = new ArrayList<ListItemWithIndex>();
        final List<ListItemWithIndex> filteredItems = new ArrayList<ListItemWithIndex>();

        for (int i = 0; i < dopt.data.size(); i++) {
            final Object obj = dopt.data.get(i);
            final ListItemWithIndex listItemWithIndex = new ListItemWithIndex(i, obj.toString());
            allItems.add(listItemWithIndex);
            filteredItems.add(listItemWithIndex);
        }

        dialogBuilder.setTitle(dopt.titleText);
        final ArrayAdapter<ListItemWithIndex> objectsAdapter = new ArrayAdapter<ListItemWithIndex>(activity,
                android.R.layout.simple_list_item_1, filteredItems) {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void publishResults(final CharSequence constraint, final FilterResults results) {
                        filteredItems.clear();
                        filteredItems.addAll((List<ListItemWithIndex>) results.values);
                        notifyDataSetChanged();
                    }

                    @Override
                    protected FilterResults performFiltering(final CharSequence constraint) {
                        final FilterResults results = new FilterResults();

                        final String filterString = constraint.toString();
                        final ArrayList<ListItemWithIndex> list = new ArrayList<>();
                        for (final ListItemWithIndex obj : allItems) {
                            final String objStr = obj.toString();
                            if ("".equals(filterString)
                                    || objStr.toLowerCase(Locale.getDefault()).contains(
                                    filterString.toLowerCase(Locale.getDefault()))) {
                                list.add(obj);
                            }
                        }

                        results.values = list;
                        results.count = list.size();
                        return results;
                    }
                };
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                Context c = textView.getContext();
                AppSettings as = new AppSettings(c);
                textView.setTextColor(ContextCompat.getColor(c, as.isDarkThemeEnabled() ? R.color.dark__primary_text : R.color.light__background));

                String text = textView.getText().toString();
                for (String str : dopt.alreadyContained) {
                    if (str.equalsIgnoreCase(text)) {
                        textView.setTextColor((ContextCompat.getColor(c, dopt.preSelectedColor)));
                    }
                }

                return textView;
            }
        };

        final EditText searchEditText = new EditText(activity);
        AppSettings as = new AppSettings(activity);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(ContextCompat.getColor(activity, as.isDarkThemeEnabled() ? R.color.dark__primary_text : R.color.light__background));
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
            }

            @Override
            public void beforeTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
            }

            @Override
            public void afterTextChanged(final Editable arg0) {
                objectsAdapter.getFilter().filter(searchEditText.getText());
            }
        });

        final ListView listView = new ListView(activity);
        listView.setAdapter(objectsAdapter);
        final LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(searchEditText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0);
        layoutParams.weight = 1;
        linearLayout.addView(listView, layoutParams);
        dialogBuilder.setView(linearLayout);
        dialogBuilder.setOnCancelListener(null);
        dialogBuilder.setNegativeButton(dopt.cancelButtonText, null);
        dialogBuilder.setPositiveButton(dopt.okButtonText, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            if (dopt.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                dopt.callback.onDataSelected(searchEditText.getText().toString());
            }
        });

        final AlertDialog dialog = dialogBuilder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                dialog.dismiss();
                if (dopt.callback != null) {
                    dopt.callback.onDataSelected(filteredItems.get(position).value);
                }
            }
        });

        searchEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    dialog.dismiss();
                    if (dopt.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                        dopt.callback.onDataSelected(searchEditText.getText().toString());
                    }
                    return true;
                }
                return false;
            }
        });

        dialog.show();
    }
}
