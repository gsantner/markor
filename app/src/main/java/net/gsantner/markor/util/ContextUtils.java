package net.gsantner.markor.util;

import android.content.Context;

import net.gsantner.markor.App;

public class ContextUtils extends net.gsantner.opoc.util.ContextUtils {
    public ContextUtils(Context context) {
        super(context);
    }

    public static ContextUtils get() {
        return new ContextUtils(App.get());
    }
}
