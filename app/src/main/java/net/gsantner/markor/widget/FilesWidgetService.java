package net.gsantner.markor.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class FilesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new FilesWidgetFactory(getApplicationContext(), intent));
    }
}
