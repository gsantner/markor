package net.gsantner.markor.util;

public class TextCasingUtils {
    public static String toggleCase(String text) { // Toggle all text to Uppercase or Lowercase
        if (text.equals(text.toUpperCase())) {
            return text.toLowerCase();
        } else {
            return text.toUpperCase();
        }
    }

    public static String switchCase(String text) { // Switch the text case of each character
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append(Character.toLowerCase(c));
            } else if (Character.isLowerCase(c)) {
                result.append(Character.toUpperCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String capitalizeWords(String text) { // Capitalize the first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    public static String capitalizeSentences(String text) { // Capitalize the first letter of each sentence
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : text.toCharArray()) {
            if (c == '.' || c == '!' || c == '?') {
                capitalizeNext = true;
                result.append(c);
            } else if (Character.isWhitespace(c)) {
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
