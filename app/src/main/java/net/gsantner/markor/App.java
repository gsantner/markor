/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor;

import android.app.Application;

public class App extends Application {
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
