package me.writeily.pro.editor;

import java.util.regex.Pattern;

enum HighlighterPattern {
    LIST(Pattern.compile("\\n\\s+(\\*|\\d*\\.)\\s")),
    HEADER(Pattern.compile("(((\\n|^)#+.*?\\n)|((\\n|^).*?\\n(-|=)+))")),
    LINK(Pattern.compile("\\[[^\\]]*\\]\\([^\\)]*\\)")),
    STRIKETHROUGH(Pattern.compile("~~.+~~")),
    MONOSPACED(Pattern.compile("`.+`")),
    BOLD(Pattern.compile("\\*{2}.+?\\*{2}")),
    ITALICS(Pattern.compile("[^\\*]\\*[^\\*\\n]+\\*[^\\*]"));

    private Pattern pattern;

    HighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
