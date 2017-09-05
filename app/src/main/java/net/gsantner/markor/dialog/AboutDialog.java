package net.gsantner.markor.dialog;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import net.gsantner.markor.R;

public class AboutDialog extends DialogPreference {

    public AboutDialog(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.text_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(null);
        setDialogIcon(null);
    }
}
