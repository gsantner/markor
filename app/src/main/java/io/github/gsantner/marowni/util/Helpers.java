package io.github.gsantner.marowni.util;

import android.content.Context;

import io.github.gsantner.marowni.App;

public class Helpers extends io.github.gsantner.opoc.util.Helpers {
    public Helpers(Context context) {
        super(context);
    }

    public static Helpers get() {
        return new Helpers(App.get());
    }
}
