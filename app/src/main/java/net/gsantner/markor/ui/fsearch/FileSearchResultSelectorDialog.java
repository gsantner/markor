package net.gsantner.markor.ui.fsearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FileSearchResultSelectorDialog {
    public static void showDialog(final Activity activity, final List<SearchEngine.FitFile> searchResults, final Callback.a2<String, Integer> dialogCallback) {
        final AtomicReference<AlertDialog> dialog = new AtomicReference<>();
        dialog.set(buildDialog(activity, dialog, searchResults, dialogCallback).create());
        if (dialog.get().getWindow() != null) {
            dialog.get().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.get().show();
        if (dialog.get().getWindow() != null) {
            dialog.get().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static AlertDialog.Builder buildDialog(final Activity activity, final AtomicReference<AlertDialog> dialog, final List<SearchEngine.FitFile> searchResults, final Callback.a2<String, Integer> dialogCallback) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_DayNight_Dialog);

        final LinearLayout dialogLayout = new LinearLayout(activity);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);

        final ExpandableListView expandableListView = new ExpandableListView(activity);
        final AppCompatEditText searchEditText = new AppCompatEditText(activity);

        final int dp4px = (int) (new ContextUtils(dialogLayout.getContext()).convertDpToPx(4));
        final int textColor = ContextCompat.getColor(activity, R.color.primary_text);
        final LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        margins.setMargins(dp4px * 5, dp4px, dp4px * 5, dp4px);

        // EdiText: Search query input
        searchEditText.setHint(R.string.search);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(textColor);
        searchEditText.setHintTextColor((textColor & 0x00FFFFFF) | 0x99000000);

        if (!searchResults.isEmpty()) {
            dialogLayout.addView(searchEditText, margins);
        }

        // List filling
        final ArrayList<GroupItemsInfo> groupItemsData = filter(searchResults, "");
        final ExpandableSearchResultsListAdapter adapter = new ExpandableSearchResultsListAdapter(activity, groupItemsData);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable arg0) {
                String filterText = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
                ArrayList<GroupItemsInfo> filteredGroups = filter(searchResults, filterText);
                ExpandableSearchResultsListAdapter adapter = new ExpandableSearchResultsListAdapter(activity, filteredGroups);
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

            if (groupItem.children.isEmpty()) {
                if (dialog != null && dialog.get() != null) {
                    dialog.get().dismiss();
                }
                dialogCallback.callback(groupItem.path, -1);
            }
            return false;
        });

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            GroupItemsInfo groupItem = (GroupItemsInfo) parent.getExpandableListAdapter().getGroup(groupPosition);
            Pair<String, Integer> childItem = (Pair<String, Integer>) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
            if (childItem.second >= 0) {
                if (dialog != null && dialog.get() != null) {
                    dialog.get().dismiss();
                }
                dialogCallback.callback(groupItem.path, childItem.second);
            }
            return false;
        });

        dialogLayout.addView(expandableListView);

        // Configure dialog
        dialogBuilder.setView(dialogLayout)
                .setTitle(R.string.select)
                .setOnCancelListener(null)
                .setMessage(searchResults.isEmpty() ? "     ¯\\_(ツ)_/¯     " : null)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());
        return dialogBuilder;
    }

    @SuppressWarnings("ConstantConditions")
    private static ArrayList<GroupItemsInfo> filter(final List<SearchEngine.FitFile> searchResults, String query) {
        final ArrayList<GroupItemsInfo> groupItemsData = new ArrayList<>();
        query = query.toLowerCase();

        for (final SearchEngine.FitFile fitFile : searchResults) {
            final boolean isPathContainsQuery = query.isEmpty() || fitFile.path.toLowerCase().contains(query);
            final ArrayList<Pair<String, Integer>> groupChildItems = new ArrayList<>();

            for (final Pair<String, Integer> contentMatch : fitFile.matchesWithLineNumberAndLineText) {
                if (isPathContainsQuery || contentMatch.first.toLowerCase().contains(query)) {
                    groupChildItems.add(contentMatch);
                }
            }
            if (isPathContainsQuery || !groupChildItems.isEmpty()) {
                groupItemsData.add(new GroupItemsInfo(fitFile.path, fitFile.isDirectory, groupChildItems));
            }
        }
        return groupItemsData;
    }

    private static class ExpandableSearchResultsListAdapter implements ExpandableListAdapter {
        public final List<GroupItemsInfo> data;
        private final Context _context;

        public ExpandableSearchResultsListAdapter(Context context, List<GroupItemsInfo> groupItems) {
            _context = context;
            data = Collections.unmodifiableList(groupItems);
        }

        @Override
        public int getGroupCount() {
            return data.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return data.get(groupPosition).children.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return data.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return data.get(groupPosition).children.get(childPosition);
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
        public View getGroupView(final int groupPosition, final boolean isExpanded, final View convertView, final ViewGroup parent) {
            GroupItemsInfo groupInfo = (GroupItemsInfo) getGroup(groupPosition);
            TextView textView = (TextView) convertView;
            if (convertView == null) {
                final LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                textView = (TextView) inflater.inflate(R.layout.expandable_list_group_item, null);
                textView.setClickable(false);
            }
            textView.setText(groupInfo.toString());

            final int iconResId = groupInfo.isDirectory || groupInfo.children.isEmpty() ? 0 : isExpanded
                    ? R.drawable.ic_baseline_keyboard_arrow_up_24
                    : R.drawable.ic_baseline_keyboard_arrow_down_24;
            textView.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);

            return textView;
        }

        @SuppressWarnings("unchecked")
        @SuppressLint("SetTextI18n")
        @Override
        public View getChildView(final int groupPosition, final int childPosition, final boolean isLastChild, final View convertView, final ViewGroup parent) {
            Pair<String, Integer> childInfo = (Pair<String, Integer>) getChild(groupPosition, childPosition);
            TextView textView = (TextView) convertView;
            if (convertView == null) {
                LayoutInflater mInflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                textView = (TextView) mInflater.inflate(android.R.layout.simple_list_item_1, null);
                textView.setClickable(false);
            }
            textView.setText("+" + childInfo.second + ": " + childInfo.first);

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
        public final ArrayList<Pair<String, Integer>> children;

        public GroupItemsInfo(String a_path, boolean a_isDirectory, ArrayList<Pair<String, Integer>> a_children) {
            path = a_path;
            isDirectory = a_isDirectory;
            children = a_children != null ? a_children : new ArrayList<>();
        }

        @Override
        public String toString() {
            return (children.size() > 0 ? String.format("(%s) ", children.size()) : "") + path;
        }
    }
}
