/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class FilesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new FilesWidgetFactory(getApplicationContext(), intent));
    }
}
