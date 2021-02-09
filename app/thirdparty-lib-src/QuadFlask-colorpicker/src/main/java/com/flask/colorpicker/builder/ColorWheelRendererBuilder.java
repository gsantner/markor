package com.flask.colorpicker.builder;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.renderer.ColorWheelRenderer;
import com.flask.colorpicker.renderer.FlowerColorWheelRenderer;
import com.flask.colorpicker.renderer.SimpleColorWheelRenderer;

public class ColorWheelRendererBuilder {
	public static ColorWheelRenderer getRenderer(ColorPickerView.WHEEL_TYPE wheelType) {
		switch (wheelType) {
			case CIRCLE:
				return new SimpleColorWheelRenderer();
			case FLOWER:
				return new FlowerColorWheelRenderer();
		}
		throw new IllegalArgumentException("wrong WHEEL_TYPE");
	}
}