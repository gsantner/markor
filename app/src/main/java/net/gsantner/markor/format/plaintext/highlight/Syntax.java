package net.gsantner.markor.format.plaintext.highlight;

import java.util.ArrayList;
import java.util.regex.Pattern;

// Load from JSON
public class Syntax {
    public String language;
    public ArrayList<Rule> rules;

    public static class Rule {
        public String type;
        public String regex;
        private Pattern m_pattern;

        public Pattern getPattern() {
            if (m_pattern == null) {
                m_pattern = Pattern.compile(regex);
            }
            return m_pattern;
        }
    }
}
