package other.com.flask.colorpicker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.Utils;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import net.gsantner.markor.R;

public class ColorPickerPreference extends Preference {

    protected boolean alphaSlider;
    protected boolean lightSlider;

    protected int selectedColor = 0;

    protected ColorPickerView.WHEEL_TYPE wheelType;
    protected int density;

    private String pickerTitle;
    private String pickerButtonCancel;
    private String pickerButtonOk;

    protected ImageView colorIndicator;


    public ColorPickerPreference(Context context) {
        super(context);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWith(context, attrs);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initWith(context, attrs);
    }


    private void initWith(Context context, AttributeSet attrs) {
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorPickerPreference);

        try {
            alphaSlider = typedArray.getBoolean(R.styleable.ColorPickerPreference_alphaSlider, true);
            lightSlider = typedArray.getBoolean(R.styleable.ColorPickerPreference_lightnessSlider, true);

            density = typedArray.getInt(R.styleable.ColorPickerPreference_density, 10);
            wheelType = ColorPickerView.WHEEL_TYPE.indexOf(typedArray.getInt(R.styleable.ColorPickerPreference_wheelType, 0));

            selectedColor = typedArray.getInt(R.styleable.ColorPickerPreference_initialColor, 0xffffffff);

            pickerTitle = typedArray.getString(R.styleable.ColorPickerPreference_pickerTitle);
            if (pickerTitle == null)
                pickerTitle = "Choose color";

            pickerButtonCancel = typedArray.getString(R.styleable.ColorPickerPreference_pickerButtonCancel);
            if (pickerButtonCancel == null)
                pickerButtonCancel = context.getString(R.string.cancel);

            pickerButtonOk = typedArray.getString(R.styleable.ColorPickerPreference_pickerButtonOk);
            if (pickerButtonOk == null)
                pickerButtonOk = context.getString(android.R.string.ok);

        } finally {
            typedArray.recycle();
        }

        setWidgetLayoutResource(R.layout.color_widget);
    }


    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        Resources res = getContext().getResources();
        GradientDrawable colorChoiceDrawable = null;

        colorIndicator = (ImageView) view.findViewById(R.id.color_indicator);

        Drawable currentDrawable = colorIndicator.getDrawable();
        if (currentDrawable != null && currentDrawable instanceof GradientDrawable)
            colorChoiceDrawable = (GradientDrawable) currentDrawable;

        if (colorChoiceDrawable == null) {
            colorChoiceDrawable = new GradientDrawable();
            colorChoiceDrawable.setShape(GradientDrawable.OVAL);
        }

        int tmpColor = isEnabled()
                ? selectedColor
                : darken(selectedColor, .5f);

        colorChoiceDrawable.setColor(tmpColor);
        colorChoiceDrawable.setStroke((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1,
                res.getDisplayMetrics()
        ), darken(tmpColor, .8f));

        colorIndicator.setImageDrawable(colorChoiceDrawable);
    }

    @Override
    public CharSequence getSummary() {
        CharSequence text = super.getSummary();
        if (TextUtils.isEmpty(text)) {
            text = Utils.getHexString(selectedColor, false).toLowerCase();
        }
        return text;
    }

    public void setValue(int value) {
        if (callChangeListener(value)) {
            selectedColor = value;
            persistInt(value);
            notifyChanged();
        }
    }


    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onClick() {
        ColorPickerDialogBuilder builder = ColorPickerDialogBuilder
                .with(getContext())
                .setTitle(pickerTitle)
                .initialColor(selectedColor)
                .wheelType(wheelType)
                .density(density)
                .setPositiveButton(pickerButtonOk, new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColorFromPicker, Integer[] allColors) {
                        setValue(selectedColorFromPicker);
                    }
                })
                .setNegativeButton(pickerButtonCancel, null);

        if (!alphaSlider && !lightSlider) builder.noSliders();
        else if (!alphaSlider) builder.lightnessSliderOnly();
        else if (!lightSlider) builder.alphaSliderOnly();


        builder
                .build()
                .show();
    }

    public static int darken(int color, float factor) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        return Color.argb(a,
                Math.max((int) (r * factor), 0),
                Math.max((int) (g * factor), 0),
                Math.max((int) (b * factor), 0));
    }
}