package me.writeily.adapter;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.writeily.R;
import me.writeily.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class NotesAdapter extends ArrayAdapter<File> implements Filterable {

    public static final String EMPTY_STRING = "";
    private Context context;
    private List<File> data;
    private List<File> filteredData;

    public NotesAdapter(Context context, int resource, List<File> objects) {
        super(context, resource, objects);
        this.context = context;
        this.data = objects;
        this.filteredData = data;
    }

    @Override
    public int getCount() {
        return filteredData.size();
    }

    @Override
    public File getItem(int i) {
        return filteredData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        String theme = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_theme_key), "");

        View row = inflater.inflate(R.layout.file_item, viewGroup, false);
        TextView noteTitle = (TextView) row.findViewById(R.id.note_title);
        TextView noteExtra = (TextView) row.findViewById(R.id.note_extra);
        ImageView fileIdentifierImageView = (ImageView) row.findViewById(R.id.file_identifier_icon);

        noteTitle.setText(Constants.MD_EXTENSION.matcher(getItem(i).getName()).replaceAll(EMPTY_STRING));

        if (getItem(i).isDirectory()) {
            noteExtra.setText(generateExtraForFile(i));
        } else {
            noteExtra.setText(generateExtraForDirectory(i));
        }

        // Theme Adjustments
        if (theme.equals(context.getString(R.string.theme_dark))) {
            noteTitle.setTextColor(context.getResources().getColor(android.R.color.white));

            if (getItem(i).isDirectory()) {
                fileIdentifierImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder_light));
            } else {
                fileIdentifierImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_notes_light));
            }
        } else {
            noteTitle.setTextColor(context.getResources().getColor(R.color.dark_grey));

            if (getItem(i).isDirectory()) {
                fileIdentifierImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder));
            } else {
                fileIdentifierImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_notes));
            }
        }

        return row;
    }

    private String generateExtraForFile(int i) {
        int fileAmount = ((getItem(i).listFiles() == null) ? 0 : getItem(i).listFiles().length);
        return String.format(context.getString(R.string.number_of_files), fileAmount);
    }

    private String generateExtraForDirectory(int i) {
        String formattedDate = DateUtils.formatDateTime(context, getItem(i).lastModified(),
                (DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE));
        return String.format(context.getString(R.string.last_modified), formattedDate);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults searchResults = new FilterResults();

                if (constraint == null || constraint.length() == 0) {
                    searchResults.values = data;
                    searchResults.count = data.size();
                } else {
                    ArrayList<File> searchResultsData = new ArrayList<File>();

                    for (File item : data) {
                        if (item.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                            searchResultsData.add(item);
                        }
                    }

                    searchResults.values = searchResultsData;
                    searchResults.count = searchResultsData.size();
                }
                return searchResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredData = (ArrayList<File>) results.values;
                notifyDataSetChanged();
            }
        };
    }

}