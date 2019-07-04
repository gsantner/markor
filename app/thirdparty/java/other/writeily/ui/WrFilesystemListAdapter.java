/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2019 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.ui;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.util.AppSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Note: Selection color is controller by state-list-drawable
public class WrFilesystemListAdapter extends ArrayAdapter<File> implements Filterable {

    public static final String EMPTY_STRING = "";
    private Context _context;
    private List<File> _data;
    private List<File> _filteredData;

    public WrFilesystemListAdapter(Context context, int resource, List<File> objects) {
        super(context, resource, objects);
        _context = context;
        _data = objects;
        _filteredData = _data;
    }

    @Override
    public int getCount() {
        return _filteredData.size();
    }

    @Override
    public File getItem(int i) {
        return i < _filteredData.size() ? _filteredData.get(i) : null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @NonNull
    @Override
    public View getView(int i, View view, @NonNull ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View row = inflater.inflate(R.layout.opoc_file__item, viewGroup, false);
        TextView noteTitle = row.findViewById(R.id.note_title);
        TextView noteExtra = row.findViewById(R.id.note_extra);
        ImageView fileIdentifierImageView = row.findViewById(R.id.file_identifier_icon);

        File item = getItem(i);
        if (item != null) {
            noteTitle.setText(MarkdownTextConverter.MD_EXTENSION_PATTERN.matcher(item.getName()).replaceAll(EMPTY_STRING));

            if (item.isDirectory()) {
                noteExtra.setText(generateExtraForFile(i));
            } else {
                noteExtra.setText(generateExtraForDirectory(i));
            }

            // Theme Adjustments
            if (AppSettings.get().isDarkThemeEnabled()) {
                noteTitle.setTextColor(_context.getResources().getColor(android.R.color.white));
                row.setBackgroundResource(R.drawable.ui__file__item__selection_background_dark);
                if (item.isDirectory()) {
                    fileIdentifierImageView.setImageResource(getIdentifierDrawable(true));
                } else {
                    fileIdentifierImageView.setImageResource(getIdentifierDrawable(false));
                }
            } else {
                noteTitle.setTextColor(_context.getResources().getColor(R.color.dark_grey));
                row.setBackgroundResource(R.drawable.ui__file__item__selection_background_light);
                if (item.isDirectory()) {
                    fileIdentifierImageView.setImageResource(getIdentifierDrawable(true));
                } else {
                    fileIdentifierImageView.setImageResource(getIdentifierDrawable(false));
                }
            }
        }
        return row;
    }

    @DrawableRes
    public int getIdentifierDrawable(boolean isFolder) {
        boolean isDark = AppSettings.get().isDarkThemeEnabled();
        if (isFolder) {
            return isDark ? R.drawable.ic_folder_white_24dp : R.drawable.ic_folder_gray_24dp;
        } else {
            return isDark ? R.drawable.ic_file_white_24dp : R.drawable.ic_file_gray_24dp;
        }
    }

    private String generateExtraForFile(int i) {
        try {
            File item = getItem(i);
            int documentAmount = (item == null ? 0
                    : item.listFiles(file -> TextFormat.isTextFile(file)).length);
            int filesAmount = (item == null ? 0 : item.listFiles().length);
            StringBuilder sb = new StringBuilder();
            sb.append(_context.getResources().getQuantityString(R.plurals.documents, documentAmount));
            sb.append(": ");
            sb.append(Integer.toString(documentAmount));
            if (filesAmount != documentAmount) {
                sb.append(String.format(Locale.ROOT, " / %s: %d", _context.getString(R.string.files), filesAmount));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String generateExtraForDirectory(int i) {
        File item = getItem(i);
        if (item == null) {
            return "";
        }
        String formattedDate = DateUtils.formatDateTime(_context, item.lastModified(),
                (DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE));
        return String.format(_context.getString(R.string.last_modified_witharg), formattedDate);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults searchResults = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    searchResults.values = _data;
                    searchResults.count = _data.size();
                } else {
                    ArrayList<File> searchResultsData = new ArrayList<>();

                    for (File item : _data) {
                        if (item.getName().toLowerCase(Locale.getDefault()).contains(constraint.toString().toLowerCase(Locale.getDefault()))) {
                            searchResultsData.add(item);
                        }
                    }

                    searchResults.values = searchResultsData;
                    searchResults.count = searchResultsData.size();
                }
                return searchResults;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                _filteredData = (ArrayList<File>) results.values;
                notifyDataSetChanged();
            }
        };
    }

}
