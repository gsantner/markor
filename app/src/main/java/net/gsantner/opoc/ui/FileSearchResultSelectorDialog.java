package net.gsantner.opoc.ui;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileSearchResultSelectorDialog {
    public static void showDialog(final Activity activity, final List<SearchEngine.FitFile> searchResults, final Callback.a2<String, Integer> dialogCallback) {
        final Dialog dialog = new Dialog(activity);
        dialog.showDialog(searchResults, dialogCallback);
    }


    private static class Dialog {
        private static final String DISPLAYED_GROUP_FIELD_NAME = "groupName";
        private static final String PATH_FIELD_NAME = "path";
        private static final String COUNT_FIELD_NAME = "count";
        private static final String DISPLAYED_CHILD_FIELD_NAME = "childName";
        private static final String LINE_NUMBER_FIELD_NAME = "lineNumber";
        private static final String IS_DIRECTORY_FIELD_NAME = "isDirectory";

        private AlertDialog _dialog;
        private final Activity _activity;


        private Dialog(final Activity activity) {
            _activity = activity;
        }

        private void showDialog(final List<SearchEngine.FitFile> searchResults, final Callback.a2<String, Integer> dialogCallback) {
            AlertDialog.Builder dialogBuilder = buildDialog(this, searchResults, dialogCallback);
            _dialog = dialogBuilder.create();
            Window _window = _dialog.getWindow();
            if (_window != null) {
                _window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
            _dialog.show();
            if (_window != null) {
                _window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        }
    }


    private static AlertDialog.Builder buildDialog(final Dialog initializer, final List<SearchEngine.FitFile> searchResults, final Callback.a2<String, Integer> dialogCallback) {
        final AppSettings appSettings = new AppSettings(initializer._activity);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(initializer._activity, appSettings.isDarkThemeEnabled() ? R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);

        final LinearLayout dialogLayout = new LinearLayout(initializer._activity);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);

        final ExpandableListView expandableListView = new ExpandableListView(initializer._activity);
        final AppCompatEditText searchEditText = new AppCompatEditText(initializer._activity);

        final int dp4px = (int) (new ContextUtils(dialogLayout.getContext()).convertDpToPx(4));
        final int textColor = ContextCompat.getColor(initializer._activity, appSettings.isDarkThemeEnabled() ? R.color.dark__primary_text : R.color.light__primary_text);
        final LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        margins.setMargins(dp4px * 5, dp4px, dp4px * 5, dp4px);

        // EdiText: Search query input
        searchEditText.setHint(R.string.search);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(textColor);
        searchEditText.setHintTextColor((textColor & 0x00FFFFFF) | 0x99000000);
        searchEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        if (!searchResults.isEmpty()) {
            dialogLayout.addView(searchEditText, margins);
        }


        // Configure ExpandableListView
        String[] groupFrom = new String[]{Dialog.DISPLAYED_GROUP_FIELD_NAME};
        String[] childFrom = new String[]{Dialog.DISPLAYED_CHILD_FIELD_NAME};
        int[] groupTo = new int[]{android.R.id.text1};
        int[] childTo = new int[]{android.R.id.text1};

        // List filling
        Pair<ArrayList<Map<String, Object>>, ArrayList<ArrayList<Map<String, Object>>>> filteredGroups = filter(searchResults, "");

        CustomExpandableListAdapter adapter = new CustomExpandableListAdapter(initializer._activity, filteredGroups.first, R.layout.expandable_list_group_item, groupFrom, groupTo, filteredGroups.second, android.R.layout.simple_list_item_1, childFrom, childTo);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable arg0) {
                String filterText = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
                Pair<ArrayList<Map<String, Object>>, ArrayList<ArrayList<Map<String, Object>>>> filteredGroups = filter(searchResults, filterText);
                CustomExpandableListAdapter adapter = new CustomExpandableListAdapter(initializer._activity, filteredGroups.first, R.layout.expandable_list_group_item, groupFrom, groupTo, filteredGroups.second, android.R.layout.simple_list_item_1, childFrom, childTo);

                expandableListView.setAdapter(adapter);
            }

            @Override
            public void onTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
            }

            @Override
            public void beforeTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
            }
        });

        expandableListView.setGroupIndicator(null);
        expandableListView.setAdapter(adapter);

        expandableListView.setOnGroupClickListener((parent, view, groupPosition, id) -> {

            Map<String, Object> map = (Map<String, Object>) parent.getExpandableListAdapter().getGroup(groupPosition);
            String path = (String) map.get(Dialog.PATH_FIELD_NAME);
            int count = (int) map.get(Dialog.COUNT_FIELD_NAME);

            if (count <= 0) {
                if (initializer._dialog != null) {
                    initializer._dialog.dismiss();
                }

                dialogCallback.callback(path, -1);
            }

            return false;
        });

        expandableListView.setOnChildClickListener((parent, view, groupPosition, childPosition, id) -> {
            Map<String, Object> groupMap = (Map<String, Object>) parent.getExpandableListAdapter().getGroup(groupPosition);
            Map<String, Object> childMap = (Map<String, Object>) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
            String path = (String) groupMap.get(Dialog.PATH_FIELD_NAME);
            int lineNumber = (int) childMap.get(Dialog.LINE_NUMBER_FIELD_NAME);
            if (lineNumber >= 0) {
                if (initializer._dialog != null) {
                    initializer._dialog.dismiss();
                }

                dialogCallback.callback(path, lineNumber);
            }

            return false;
        });

        dialogLayout.addView(expandableListView);


        // Configure dialog
        dialogBuilder.setView(dialogLayout)
                .setTitle(R.string.select)
                .setOnCancelListener(null)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());

        if (searchResults.isEmpty()) {
            dialogBuilder.setMessage("     ¯\\_(ツ)_/¯     ");
        }

        return dialogBuilder;
    }


    private static Pair<ArrayList<Map<String, Object>>, ArrayList<ArrayList<Map<String, Object>>>> filter(final List<SearchEngine.FitFile> searchResults, String query) {
        ArrayList<Map<String, Object>> groupDataList = new ArrayList<>();
        ArrayList<ArrayList<Map<String, Object>>> childDataList = new ArrayList<>();

        query = query.toLowerCase();

        for (int i = 0; i < searchResults.size(); i++) {
            final SearchEngine.FitFile fitFile = searchResults.get(i);
            final List<SearchEngine.FitFile.ContentMatchUnit> contentMatches = fitFile.getContentMatches();


            Map<String, Object> groupMap = new HashMap<>();
            String contentCountText = contentMatches.size() > 0 ? String.format("(%s) ", contentMatches.size()) : "";
            final String path = fitFile.getPath();
            String groupName = contentCountText + path;
            groupMap.put(Dialog.DISPLAYED_GROUP_FIELD_NAME, groupName);
            groupMap.put(Dialog.PATH_FIELD_NAME, path);
            groupMap.put(Dialog.IS_DIRECTORY_FIELD_NAME, fitFile.isDirectory());
            groupMap.put(Dialog.COUNT_FIELD_NAME, contentMatches.size());

            boolean isPathContainsQuery = query.isEmpty() || path.toLowerCase().contains(query);

            ArrayList<Map<String, Object>> childDataItemList = new ArrayList<>();
            for (SearchEngine.FitFile.ContentMatchUnit contentMatch : contentMatches) {
                final String previewMatch = contentMatch.getPreviewMatch();

                if (isPathContainsQuery || previewMatch.toLowerCase().contains(query)) {
                    Map<String, Object> map = new HashMap<>();
                    int lineNumber = contentMatch.getLineNumber();
                    String displayedText = "Line " + lineNumber + ": " + previewMatch;
                    map.put(Dialog.DISPLAYED_CHILD_FIELD_NAME, displayedText);
                    map.put(Dialog.LINE_NUMBER_FIELD_NAME, lineNumber);

                    childDataItemList.add(map);
                }
            }

            if (isPathContainsQuery || childDataItemList.size() > 0) {
                groupDataList.add(groupMap);
                childDataList.add(childDataItemList);
            }
        }

        return new Pair<>(groupDataList, childDataList);
    }

    private static class CustomExpandableListAdapter extends SimpleExpandableListAdapter {

        public CustomExpandableListAdapter(Context context, List<? extends Map<String, ?>> groupData, int groupLayout, String[] groupFrom, int[] groupTo, List<? extends List<? extends Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo) {
            super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            Map<String, Object> groupMap = (Map<String, Object>) getGroup(groupPosition);
            boolean isDirectory = (boolean) groupMap.get(FileSearchResultSelectorDialog.Dialog.IS_DIRECTORY_FIELD_NAME);
            int countMatches = (int) groupMap.get(FileSearchResultSelectorDialog.Dialog.COUNT_FIELD_NAME);


            TextView view = (TextView) super.getGroupView(groupPosition, isExpanded, convertView, parent);
            int icon = isDirectory || countMatches == 0 ? 0 : isExpanded
                    ? R.drawable.ic_baseline_keyboard_arrow_up_24
                    : R.drawable.ic_baseline_keyboard_arrow_down_24;
            view.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);

            return view;
        }
    }
}
