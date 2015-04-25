package me.writeily.pro.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by jeff on 15-04-21.
 */
public class FilesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent)
    {
        return (new FilesListWidgetProvider(getApplicationContext(), intent));
    }
}
