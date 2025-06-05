package net.gsantner.markor.format.plaintext.highlight;

import java.util.ArrayList;
import java.util.regex.Pattern;

// Load from JSON
public class Syntax {
    private String language;
    private ArrayList<Rule> rules;

    public String getLanguage() {
        return language;
    }

    public ArrayList<Rule> getRules() {
        return rules;
    }

    public class Rule {
        private String type;
        private String regex;
        private Pattern pattern;

        public String getType() {
            return type;
        }

        public String getRegex() {
            return regex;
        }

        public Pattern getPattern() {
            if (pattern == null) {
                pattern = Pattern.compile(regex);
            }
            return pattern;
        }
    }
}
