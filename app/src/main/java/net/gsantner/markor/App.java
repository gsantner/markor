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
