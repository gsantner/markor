package net.gsantner.markor.format.plaintext.highlight;

import android.graphics.Color;

import java.util.regex.Pattern;

public class Rule {
    private String type;
    private String regex;
    private Pattern pattern;
    private String color;
    private Integer parsedColor;
    // private boolean bold;

    public String getType() {
        return type;
    }

    public String getRegex() {
        return regex;
    }

    public String getColor() {
        return color;
    }

    public int getParsedColor() {
        if (parsedColor == null) {
            parsedColor = Color.parseColor(color);
        }
        return parsedColor;
    }

    public Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile(regex);
        }
        return pattern;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
