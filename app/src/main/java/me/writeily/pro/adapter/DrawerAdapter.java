package me.writeily.pro.adapter;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import me.writeily.pro.R;

/**
 * Created by jeff on 2014-04-11.
 */
public class DrawerAdapter extends BaseAdapter {

    private Context context;
    private String[] content;

    public DrawerAdapter(Context context, String[] content) {
        this.context = context;
        this.content = content;
    }

    @Override
    public int getCount() {
        return content.length;
    }

    @Override
    public String getItem(int i) {
        return content[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        String theme = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_theme_key), "");

        View drawerRow = inflater.inflate(R.layout.drawer_item, viewGroup, false);
        TextView drawerTitle = (TextView) drawerRow.findViewById(R.id.drawer_item_title);
        ImageView drawerIcon = (ImageView) drawerRow.findViewById(R.id.drawer_item_icon);

        if (theme.equals(context.getString(R.string.theme_dark))) {
            drawerTitle.setTextColor(context.getResources().getColor(android.R.color.white));

            if (getItem(i).equalsIgnoreCase(context.getString(R.string.notes))) {
                drawerIcon.setImageResource(R.drawable.ic_notes_light);
            } else if (getItem(i).equalsIgnoreCase(context.getString(R.string.import_from_device))) {
                drawerIcon.setImageResource(R.drawable.ic_folder_light);
            } else if (getItem(i).equalsIgnoreCase(context.getString(R.string.action_settings))) {
                drawerIcon.setImageResource(R.drawable.ic_settings_light);
            }

        } else {
            drawerTitle.setTextColor(context.getResources().getColor(R.color.dark_grey));

            if (getItem(i).equalsIgnoreCase(context.getString(R.string.notes))) {
                drawerIcon.setImageResource(R.drawable.ic_notes);
            } else if (getItem(i).equalsIgnoreCase(context.getString(R.string.import_from_device))) {
                drawerIcon.setImageResource(R.drawable.ic_folder);
            } else if (getItem(i).equalsIgnoreCase(context.getString(R.string.action_settings))) {
                drawerIcon.setImageResource(R.drawable.ic_settings);
            }
        }

        drawerTitle.setText(getItem(i));

        return drawerRow;
    }

}
