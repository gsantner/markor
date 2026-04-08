package net.gsantner.markor.frontend;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.DialogPreference;

import net.gsantner.markor.R;
import net.gsantner.markor.frontend.textview.CodeMirrorEditor;
import net.gsantner.opoc.util.GsContextUtils;

public class AdvancedEditTextPreference extends DialogPreference {

    private String defaultValue;
    private CodeMirrorEditor editText;
    private boolean initialized;

    public AdvancedEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public AdvancedEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AdvancedEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AdvancedEditTextPreference(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        if (!initialized) {
            setDialogLayoutResource(R.layout.edit_text_preference);
            initialized = true;
        }
    }

    @Nullable
    @Override
    protected Object onGetDefaultValue(@NonNull TypedArray a, int index) {
        defaultValue = a.getString(index);
        if (defaultValue == null) {
            defaultValue = "";
        } else {
            // Prettify indentation with 2 spaces
            defaultValue = defaultValue.replaceAll("\n ", "\n  ");
        }
        return defaultValue;
    }

    public AlertDialog createDialog(FragmentActivity activity) {
        View view = activity.getLayoutInflater().inflate(R.layout.edit_text_preference, null);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Point size = GsContextUtils.calculateDialogSize(displayMetrics, 0.95f, 1040, 0.52f, 1900);
        view.setMinimumWidth(size.x);
        view.setMinimumHeight(size.y);

        editText = view.findViewById(R.id.editor);
        editText.setOnPreparedListener(() -> {
            editText.resetText(getPersistedString(defaultValue));
            editText.requestFocusFromTouch();
            editText.focus();
        });

        TextView textView = view.findViewById(R.id.title);
        textView.setText(getTitle());
        view.findViewById(R.id.undo).setOnClickListener(v -> editText.undo());
        view.findViewById(R.id.redo).setOnClickListener(v -> editText.redo());
        view.findViewById(R.id.reset).setOnClickListener(v -> editText.setText(defaultValue));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);
        builder.setPositiveButton(activity.getString(R.string.save),
                (dialog, which) -> editText.getText(this::persistString));
        builder.setNegativeButton(activity.getString(R.string.cancel), null);

        return builder.create();
    }
}
