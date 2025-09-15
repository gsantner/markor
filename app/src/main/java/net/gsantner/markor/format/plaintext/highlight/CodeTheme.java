package net.gsantner.markor.format.plaintext.highlight;

import android.graphics.Color;

import java.util.HashMap;

// Load from JSON
public class CodeTheme {
    public String name;
    public HashMap<String, ThemeValue> styles; // <type, style>

    public static class ThemeValue {
        private String color;
        private Integer m_colorInt;
        // private boolean bold;

        public int getColor() {
            if (m_colorInt == null) {
                m_colorInt = Color.parseColor(color);
            }
            return m_colorInt;
        }
    }
}
