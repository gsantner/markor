/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package other.writeily.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class WrFilesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return (new WrFilesWidgetFactory(getApplicationContext(), intent));
    }
}
