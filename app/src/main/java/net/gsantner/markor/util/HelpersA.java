package net.gsantner.markor.util;

import android.app.Activity;

public class HelpersA extends io.github.gsantner.opoc.util.HelpersA {
    public HelpersA(Activity activity) {
        super(activity);
    }

    public static HelpersA get(Activity activity) {
        return new HelpersA(activity);
    }
}
