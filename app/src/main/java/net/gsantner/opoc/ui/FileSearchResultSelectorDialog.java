package net.gsantner.opoc.ui;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.util.ArrayList;
import java.util.List;

public class FileSearchResultSelectorDialog {
    public static void showDialog(final Activity activity, final List<SearchEngine.FitFile> searchResults, final Callback.a2<String, Integer> dialogCallback) {
        final Dialog dialog = new Dialog(activity);
        dialog.showDialog(searchResults, dialogCallback);
    }


    private static class Dialog {
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

        // List filling
        ArrayList<GroupItemsInfo> groupItemsData = filter(searchResults, "");
        CustomExpandableListAdapter adapter = new CustomExpandableListAdapter(initializer._activity, groupItemsData);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable arg0) {
                String filterText = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
                ArrayList<GroupItemsInfo> filteredGroups = filter(searchResults, filterText);
                CustomExpandableListAdapter adapter = new CustomExpandableListAdapter(initializer._activity, filteredGroups);
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
            GroupItemsInfo groupItem = (GroupItemsInfo) parent.getExpandableListAdapter().getGroup(groupPosition);

            if (groupItem.getCountMatches() <= 0) {
                if (initializer._dialog != null) {
                    initializer._dialog.dismiss();
                }

                dialogCallback.callback(groupItem.path, -1);
            }

            return false;
        });

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            GroupItemsInfo groupItem = (GroupItemsInfo) parent.getExpandableListAdapter().getGroup(groupPosition);
            ChildItemsInfo childItem = (ChildItemsInfo) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
            if (childItem.lineNumber >= 0) {
                if (initializer._dialog != null) {
                    initializer._dialog.dismiss();
                }

                dialogCallback.callback(groupItem.path, childItem.lineNumber);
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

    private static ArrayList<GroupItemsInfo> filter(final List<SearchEngine.FitFile> searchResults, String query) {
        ArrayList<GroupItemsInfo> groupItemsData = new ArrayList<>();
        query = query.toLowerCase();

        for (int i = 0; i < searchResults.size(); i++) {
            SearchEngine.FitFile fitFile = searchResults.get(i);
            boolean isPathContainsQuery = query.isEmpty() || fitFile.getPath().toLowerCase().contains(query);
            ArrayList<ChildItemsInfo> groupChildItems = new ArrayList<>();

            for (SearchEngine.FitFile.ContentMatchUnit contentMatch : fitFile.getContentMatches()) {
                final String previewMatch = contentMatch.getPreviewMatch();

                if (isPathContainsQuery || previewMatch.toLowerCase().contains(query)) {
                    ChildItemsInfo childItem = new ChildItemsInfo();
                    int lineNumber = contentMatch.getLineNumber();
                    childItem.lineNumber = (contentMatch.getLineNumber());
                    childItem.displayedText = ("Line " + lineNumber + ": " + previewMatch);
                    groupChildItems.add(childItem);
                }
            }

            groupItemsData.add(new GroupItemsInfo(fitFile.getPath(), fitFile.isDirectory(), (isPathContainsQuery || groupChildItems.size() > 0) ? groupChildItems : null));
        }

        return groupItemsData;
    }

    private static class CustomExpandableListAdapter implements ExpandableListAdapter {
        private final Context _context;
        private final List<GroupItemsInfo> _groupItems;

        public CustomExpandableListAdapter(Context context, List<GroupItemsInfo> groupItems) {
            _context = context;
            _groupItems = groupItems;
        }

        @Override
        public int getGroupCount() {
            return _groupItems.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return _groupItems.get(groupPosition).children.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return _groupItems.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return _groupItems.get(groupPosition).children.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            GroupItemsInfo groupInfo = (GroupItemsInfo) getGroup(groupPosition);
            TextView textView = (TextView) convertView;
            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                textView = (TextView) mInflater.inflate(R.layout.expandable_list_group_item, null);
                textView.setText(groupInfo.toString());
                textView.setClickable(false);
            }

            int icon = groupInfo.isDirectory || groupInfo.getCountMatches() == 0 ? 0 : isExpanded
                    ? R.drawable.ic_baseline_keyboard_arrow_up_24
                    : R.drawable.ic_baseline_keyboard_arrow_down_24;
            textView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);

            return textView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ChildItemsInfo childInfo = (ChildItemsInfo) getChild(groupPosition, childPosition);
            TextView textView = (TextView) convertView;
            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                textView = (TextView) mInflater.inflate(android.R.layout.simple_list_item_1, null);
                textView.setClickable(false);
            }
            textView.setText(childInfo.displayedText);

            return textView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
        }

        @Override
        public long getCombinedChildId(long groupPosition, long childPosition) {
            return 0;
        }

        @Override
        public long getCombinedGroupId(long groupPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        }
    }

    public static class GroupItemsInfo {
        public final String path;
        public final boolean isDirectory;
        public final ArrayList<ChildItemsInfo> children;

        public GroupItemsInfo(String a_path, boolean a_isDirectory, ArrayList<ChildItemsInfo> a_children) {
            path = a_path;
            isDirectory = a_isDirectory;
            children = a_children != null ? a_children : new ArrayList<>();
        }

        public int getCountMatches() {
            return children.size();
        }

        @Override
        public String toString() {
            String contentCountText = children.size() > 0 ? String.format("(%s) ", children.size()) : "";
            return contentCountText + path;
        }
    }

    public static class ChildItemsInfo {
        public int lineNumber;
        public String displayedText;
    }
}
