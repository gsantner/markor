package net.gsantner.markor.format.plaintext.highlight;

import android.graphics.Color;

import java.util.ArrayList;

// Load from JSON
public class Style {
    private String name;
    private ArrayList<Define> defines;

    public String getName() {
        return name;
    }

    public ArrayList<Define> getDefines() {
        return defines;
    }

    public class Define {
        private String type;
        private String color;
        private Integer color_;
        // private boolean bold;

        public String getType() {
            return type;
        }

        public int getColor() {
            if (color_ == null) {
                color_ = Color.parseColor(color);
            }
            return color_;
        }
    }
}
