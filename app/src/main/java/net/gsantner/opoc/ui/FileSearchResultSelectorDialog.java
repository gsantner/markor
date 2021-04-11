package net.gsantner.opoc.ui;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;

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
        private static String DISPLAYED_GROUP_FIELD_NAME = "groupName";
        private static String PATH_FIELD_NAME = "path";
        private static String COUNT_FIELD_NAME = "count";
        private static String DISPLAYED_CHILD_FIELD_NAME = "childName";
        private static String LINE_NUMBER_FIELD_NAME = "lineNumber";

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


        // Configure ExpandableListView
        final ExpandableListView expandableListView = new ExpandableListView(initializer._activity);
        ArrayList<Map<String, Object>> groupDataList = new ArrayList<>();
        ArrayList<ArrayList<Map<String, Object>>> сhildDataList = new ArrayList<>();
        String[] groupFrom = new String[]{Dialog.DISPLAYED_GROUP_FIELD_NAME};
        String[] childFrom = new String[]{Dialog.DISPLAYED_CHILD_FIELD_NAME};
        int[] groupTo = new int[]{android.R.id.text1};
        int[] childTo = new int[]{android.R.id.text1};

        // List filling
        for (int i = 0; i < searchResults.size(); i++) {
            final SearchEngine.FitFile fitFile = searchResults.get(i);
            final List<SearchEngine.FitFile.ContentMatchUnit> contentMatches = fitFile.getContentMatches();

            Map<String, Object> groupMap = new HashMap<>();
            String contentCountText = contentMatches.size() > 0 ? String.format("(%s) ", contentMatches.size()) : "";
            String groupName = contentCountText + fitFile.getPath();
            groupMap.put(Dialog.DISPLAYED_GROUP_FIELD_NAME, groupName);
            groupMap.put(Dialog.PATH_FIELD_NAME, fitFile.getPath());
            groupMap.put(Dialog.COUNT_FIELD_NAME, contentMatches.size());
            groupDataList.add(groupMap);

            ArrayList<Map<String, Object>> сhildDataItemList = new ArrayList<>();
            for (SearchEngine.FitFile.ContentMatchUnit contentMatch : contentMatches) {
                Map<String, Object> map = new HashMap<>();
                int lineNumber = contentMatch.getLineNumber();
                String displayedText = "Line " + lineNumber + ": " + contentMatch.getPreviewMatch();
                map.put(Dialog.DISPLAYED_CHILD_FIELD_NAME, displayedText);
                map.put(Dialog.LINE_NUMBER_FIELD_NAME, lineNumber);
                сhildDataItemList.add(map);
            }
            сhildDataList.add(сhildDataItemList);
        }

        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(initializer._activity, groupDataList, R.layout.expandable_list_group_item, groupFrom, groupTo, сhildDataList, android.R.layout.simple_list_item_1, childFrom, childTo);


        expandableListView.setAdapter(adapter);

        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {

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
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
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
            }
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
}
