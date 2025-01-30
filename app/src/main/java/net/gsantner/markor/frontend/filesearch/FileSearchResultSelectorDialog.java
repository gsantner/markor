package net.gsantner.markor.frontend.filesearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.text.Editable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import net.gsantner.markor.R;
import net.gsantner.markor.frontend.filesearch.FileSearchEngine.FitFile;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSearchResultSelectorDialog {
    /**
     * Show a file system selector dialog
     *
     * @param activity      Activity to use
     * @param searchResults Search results to filter
     * @param callback      Callback to call when a item is selected
     *                      callback.first:  Path to file (relative)
     *                      callback.second: Line number (null if not applicable)
     *                      callback.third:  True if the dialog was dismissed by long clicking on a file
     */
    public static void showDialog(
            final Activity activity,
            final List<FileSearchEngine.FitFile> searchResults,
            final GsCallback.a3<String, Integer, Boolean> callback
    ) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_DayNight_Dialog_Rounded);

        final LinearLayout dialogLayout = new LinearLayout(activity);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);

        final ExpandableListView expandableListView = new ExpandableListView(activity);
        final AppCompatEditText searchEditText = new AppCompatEditText(activity);

        final int dp4px = GsContextUtils.instance.convertDpToPx(activity, 4);
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
        final List<FitFile> groupItemsData = filter(searchResults, "");
        final ExpandableSearchResultsListAdapter adapter = new ExpandableSearchResultsListAdapter(activity, groupItemsData);
        searchEditText.addTextChangedListener(new GsTextWatcherAdapter() {
            @Override
            public void afterTextChanged(final Editable arg0) {
                String filterText = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
                List<FitFile> filteredGroups = filter(searchResults, filterText);
                ExpandableSearchResultsListAdapter adapter = new ExpandableSearchResultsListAdapter(activity, filteredGroups);
                expandableListView.setAdapter(adapter);
            }
        });

        expandableListView.setGroupIndicator(null);
        expandableListView.setAdapter(adapter);
        dialogLayout.addView(expandableListView);

        // Configure dialog
        final AlertDialog dialog = dialogBuilder
                .setView(dialogLayout)
                .setTitle(R.string.select)
                .setOnCancelListener(null)
                .setMessage(searchResults.isEmpty() ? "     ¯\\_(ツ)_/¯     " : null)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        expandableListView.setOnGroupClickListener((parent, view, groupPosition, id) -> {
            final FitFile groupItem = (FitFile) parent.getExpandableListAdapter().getGroup(groupPosition);
            if (groupItem.children.isEmpty()) {
                dialog.dismiss();
                callback.callback(groupItem.path, null, false);
            }
            return false;
        });

        final GsCallback.b5<ExpandableListView, View, Integer, Integer, Long> onChildClick = (parent, view, groupPos, childPos, id) -> {
            final ExpandableListAdapter _adapter = parent.getExpandableListAdapter();
            final FitFile groupItem = (FitFile) _adapter.getGroup(groupPos);
            final Pair<String, Integer> childItem = (Pair<String, Integer>) _adapter.getChild(groupPos, childPos);
            if (childItem != null && childItem.second != null && childItem.second >= 0) {
                dialog.dismiss();
                callback.callback(groupItem.path, childItem.second, false);
            }
            return false;
        };

        expandableListView.setOnChildClickListener(onChildClick::callback);

        // Long click on file name takes us to the file's location
        expandableListView.setOnItemLongClickListener((parent, view, position, id) -> {
            try {
                final long packed = expandableListView.getExpandableListPosition(position);
                if (ExpandableListView.getPackedPositionType(packed) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    final int group = ExpandableListView.getPackedPositionGroup(packed);
                    final String path = ((FitFile) expandableListView.getExpandableListAdapter().getGroup(group)).path;
                    dialog.dismiss();
                    callback.callback(path, null, true);
                } else {
                    final int groupPos = ExpandableListView.getPackedPositionGroup(packed);
                    final int childPos = ExpandableListView.getPackedPositionChild(packed);
                    onChildClick.callback(expandableListView, view, groupPos, childPos, id);
                }
            } catch (ClassCastException | NullPointerException ignored) {
            }
            return true;
        });

        dialog.show();
        final Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static List<FitFile> filter(final List<FitFile> searchResults, String query) {
        if (query.trim().isEmpty()) {
            return searchResults;
        }

        final List<FitFile> groupItemsData = new ArrayList<>();
        query = query.toLowerCase();

        for (final FileSearchEngine.FitFile fitFile : searchResults) {
            final boolean isPathContainsQuery = query.isEmpty() || fitFile.path.toLowerCase().contains(query);
            final ArrayList<Pair<String, Integer>> groupChildItems = new ArrayList<>();

            for (final Pair<String, Integer> contentMatch : fitFile.children) {
                if (isPathContainsQuery || contentMatch.first.toLowerCase().contains(query)) {
                    groupChildItems.add(contentMatch);
                }
            }
            if (isPathContainsQuery || !groupChildItems.isEmpty()) {
                groupItemsData.add(new FitFile(fitFile.path, fitFile.isDirectory, groupChildItems));
            }
        }
        return groupItemsData;
    }

    private static class ExpandableSearchResultsListAdapter implements ExpandableListAdapter {
        public final List<FitFile> data;
        private final Context _context;

        public ExpandableSearchResultsListAdapter(Context context, List<FitFile> groupItems) {
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
            final FitFile groupInfo = (FitFile) getGroup(groupPosition);
            TextView textView = (TextView) convertView;
            if (convertView == null) {
                final LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                textView = (TextView) inflater.inflate(R.layout.expandable_list_group_item, null);
                textView.setClickable(false);
            }
            textView.setText(groupInfo.toString());

            final int iconResId;
            if (groupInfo.isDirectory || groupInfo.children.isEmpty()) {
                iconResId = 0;
            } else if (isExpanded) {
                iconResId = R.drawable.ic_baseline_keyboard_arrow_up_24;
            } else {
                iconResId = R.drawable.ic_baseline_keyboard_arrow_down_24;
            }
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
}
