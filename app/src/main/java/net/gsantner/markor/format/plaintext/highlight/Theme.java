package net.gsantner.markor.format.plaintext.highlight;

import android.graphics.Color;

import java.util.HashMap;

// Load from JSON
public class Theme {
    private String name;
    private HashMap<String, Style> styles; // <type, style>

    public String getName() {
        return name;
    }

    public HashMap<String, Style> getStyles() {
        return styles;
    }

    public class Style {
        private String color;
        private Integer color_;
        // private boolean bold;

        public int getColor() {
            if (color_ == null) {
                color_ = Color.parseColor(color);
            }
            return color_;
        }
    }
}
