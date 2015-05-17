package me.writeily.adapter;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import me.writeily.R;

/**
 * Created by jeff on 2014-04-11.
 */
public class FileAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private ArrayList<File> data;
    private ArrayList<File> filteredData;

    public FileAdapter(Context context, ArrayList<File> content) {
        this.context = context;
        this.data = content;
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

        noteTitle.setText(getItem(i).getName());
        noteExtra.setText(getItem(i).getAbsolutePath());

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
