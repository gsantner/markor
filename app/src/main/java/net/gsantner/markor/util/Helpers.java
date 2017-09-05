package net.gsantner.markor.util;

import android.content.Context;

import net.gsantner.markor.App;

public class Helpers extends io.github.gsantner.opoc.util.Helpers {
    public Helpers(Context context) {
        super(context);
    }

    public static Helpers get() {
        return new Helpers(App.get());
    }
}
