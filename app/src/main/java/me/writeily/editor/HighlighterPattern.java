package me.writeily.editor;

import java.util.regex.Pattern;

enum HighlighterPattern {
    LIST(Pattern.compile("(\\n|^)\\s*(\\*|\\d+\\.)")),
    QUOTATION(Pattern.compile("(\\n|^)>")),
    HEADER(Pattern.compile("(((\\n|^)#+.*?\\n)|((\\n|^).*?\\n(-|=)+))")),
    LINK(Pattern.compile("\\[([^\\[]+)\\]\\(([^\\)]+)\\)")),
    STRIKETHROUGH(Pattern.compile("\\~\\~(.*?)\\~\\~")),
    MONOSPACED(Pattern.compile("`(.*?)`")),
    BOLD(Pattern.compile("(\\*\\*|__)(.*?)\\1")),
    ITALICS(Pattern.compile("(\\*|_)(.*?)\\1"));

    private Pattern pattern;

    HighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
