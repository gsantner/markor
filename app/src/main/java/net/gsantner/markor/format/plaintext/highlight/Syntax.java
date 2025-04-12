package net.gsantner.markor.format.plaintext.highlight;

import java.util.ArrayList;

public class Syntax {
    private String language;
    private ArrayList<Rule> rules;

    public String getLanguage() {
        return language;
    }

    public ArrayList<Rule> getRules() {
        return rules;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setRules(ArrayList<Rule> rules) {
        this.rules = rules;
    }
}
