package net.gsantner.markor.frontend.filesearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;
<<<<<<< Updated upstream
import net.gsantner.markor.frontend.filesearch.FileSearchEngine.FitFile;
=======
import net.gsantner.opoc.wrapper.GsHashMap;
>>>>>>> Stashed changes

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FileSearchResultSelectorDialog {
    public static void showDialog(final Activity activity, final List<FileSearchEngine.FitFile> searchResults, final GsCallback.a3<String, Integer, Boolean> dialogCallback) {
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

    private static AlertDialog.Builder buildDialog(
            final Activity activity,
            final AtomicReference<AlertDialog> dialog,
            final List<FileSearchEngine.FitFile> searchResults,
            final GsCallback.a3<String, Integer, Boolean> dialogCallback
    ) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_DayNight_Dialog);

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
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable arg0) {
                String filterText = searchEditText.getText() == null ? "" : searchEditText.getText().toString();
                List<FitFile> filteredGroups = filter(searchResults, filterText);
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

<<<<<<< Updated upstream
        expandableListView.setOnGroupClickListener((parent, view, groupPosition, id) -> {
            final FitFile groupItem = (FitFile) parent.getExpandableListAdapter().getGroup(groupPosition);
=======
        final GsCallback.a0 dismiss = () -> {
            if (dialog != null && dialog.get() != null) {
                dialog.get().dismiss();
            }
        };
>>>>>>> Stashed changes

        expandableListView.setOnGroupClickListener((parent, view, groupPosition, id) -> {
            final GroupItemsInfo groupItem = (GroupItemsInfo) parent.getExpandableListAdapter().getGroup(groupPosition);
            if (groupItem.children.isEmpty()) {
                dismiss.callback();
                dialogCallback.callback(groupItem.path, null, false);
            }
            return false;
        });

        // Long click on file name takes us to the file's location
        expandableListView.setOnItemLongClickListener((parent, view, position, id) -> {
            try {
                final long packed = expandableListView.getExpandableListPosition(position);
                if (ExpandableListView.getPackedPositionType(packed) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    final int group = ExpandableListView.getPackedPositionGroup(packed);
<<<<<<< Updated upstream
                    final String path = ((FitFile) expandableListView.getExpandableListAdapter().getGroup(group)).path;
                    ((MainActivity) activity).getNotebook().showPathRelative(path);
                    if (dialog != null && dialog.get() != null) {
                        dialog.get().dismiss();
                    }
=======
                    final String path = ((GroupItemsInfo) expandableListView.getExpandableListAdapter().getGroup(group)).path;
                    dismiss.callback();
                    dialogCallback.callback(path, null, true);
>>>>>>> Stashed changes
                }
            } catch (ClassCastException | NullPointerException ignored) {
            }
            return true;
        });

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            final FitFile groupItem = (FitFile) parent.getExpandableListAdapter().getGroup(groupPosition);
            Pair<String, Integer> childItem = (Pair<String, Integer>) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
            if (childItem != null && childItem.second != null && childItem.second >= 0) {
                dialogCallback.callback(groupItem.path, childItem.second, false);
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
