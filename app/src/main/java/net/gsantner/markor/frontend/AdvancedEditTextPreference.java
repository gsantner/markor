package net.gsantner.markor.frontend;

import android.content.Context;
import android.content.res.TypedArray;
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

public class AdvancedEditTextPreference extends DialogPreference {

    private String defaultValue;
    private CodeMirrorEditor editText;

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
        setDialogLayoutResource(R.layout.edit_text_preference);
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
        view.setMinimumWidth((int) (displayMetrics.widthPixels * 0.94));
        view.setMinimumHeight((int) (displayMetrics.heightPixels * 0.52));

        editText = view.findViewById(R.id.editor);
        editText.setOnPreparedListener(() -> {
            editText.setText(getPersistedString(defaultValue));
            editText.requestFocusFromTouch();
        });

        TextView textView = view.findViewById(R.id.title);
        textView.setText(getTitle());
        view.findViewById(R.id.undo).setOnClickListener(v -> editText.undo());
        view.findViewById(R.id.redo).setOnClickListener(v -> editText.redo());
        view.findViewById(R.id.reset).setOnClickListener(v -> editText.setText(defaultValue));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);
        builder.setPositiveButton(activity.getString(R.string.ok),
                (dialog, which) -> editText.getText(this::persistString));
        builder.setNegativeButton(activity.getString(R.string.cancel), null);

        return builder.create();
    }
}
