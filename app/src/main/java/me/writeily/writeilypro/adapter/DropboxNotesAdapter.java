package me.writeily.writeilypro.adapter;

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

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import me.writeily.writeilypro.R;

/**
 * Created by jeff on 2014-04-11.
 */
public class DropboxNotesAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private ArrayList<DbxFileInfo> data;
    private ArrayList<DbxFileInfo> filteredData;

    public DropboxNotesAdapter(Context context, ArrayList<DbxFileInfo> content) {
        this.context = context;
        this.data = content;
        this.filteredData = data;
    }

    @Override
    public int getCount() {
        return filteredData.size();
    }

    @Override
    public DbxFileInfo getItem(int i) {
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

        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm");

        noteTitle.setText(getItem(i).path.getName());

        if (!getItem(i).isFolder) {
            noteExtra.setText("Last modified: " + formatter.format(getItem(i).modifiedTime));
        } else {
//                noteExtra.setText("Number of files: " + ((getItem(i).listFiles() == null) ? 0 : getItem(i).listFiles().length));
        }

        if (!theme.equals("")) {
            if (theme.equals(context.getString(R.string.theme_dark))) {
                noteTitle.setTextColor(context.getResources().getColor(android.R.color.white));
                if (getItem(i).isFolder) {
                    fileIdentifierImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder_light));
                } else {
                    fileIdentifierImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_notes_light));
                }
            } else {
                noteTitle.setTextColor(context.getResources().getColor(R.color.dark_grey));

                if (getItem(i).isFolder) {
                    fileIdentifierImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder));
                } else {
                    fileIdentifierImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_notes));
                }
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
                    ArrayList<DbxFileInfo> searchResultsData = new ArrayList<DbxFileInfo>();

                    for (DbxFileInfo item : data) {
                        if (item.toString().toLowerCase().contains(constraint.toString().toLowerCase())) {
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
                filteredData = (ArrayList<DbxFileInfo>) results.values;
                notifyDataSetChanged();
            }
        };
    }

}
