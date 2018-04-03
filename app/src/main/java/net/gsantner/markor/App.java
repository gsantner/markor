/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor;

import android.app.Application;

public class App extends Application {
    // Make resources not marked as unused
    @SuppressWarnings("unused")
    private static final Object[] unused_ignore = new Object[]
            {R.color.colorPrimary, R.color.icons, R.color.divider, R.plurals.item_selected, R.string.project_page, R.style.AppTheme, R.raw.readme};

    private volatile static App _app;

    public static App get() {
        return _app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _app = this;
    }
}
