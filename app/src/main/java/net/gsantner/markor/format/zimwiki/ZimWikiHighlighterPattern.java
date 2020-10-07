package net.gsantner.markor.format.zimwiki;

import net.gsantner.markor.format.markdown.MarkdownHighlighterPattern;

import java.util.regex.Pattern;

public enum ZimWikiHighlighterPattern {

    BOLD(Pattern.compile("(?<=(\\n|^|\\s))(\\*{2})(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    ITALICS(Pattern.compile("(?<=(\\n|^|\\s))(/{2})(?=((?!\\2)|\\2{2,}))(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    MARKED(Pattern.compile("(?<=(\\n|^|\\s))(_{2})(?=((?!\\2)|\\2{2,}))(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    STRIKETHROUGH(Pattern.compile("~{2}(.*?)\\S~{2}")),
    HEADING(Pattern.compile("(?<=(\\n|^|\\s))(==+)[ \\t]+\\S.*?[ \\t]=*(?=(\\n|$|\\s))")),
    PREFORMATTED_INLINE(Pattern.compile("''(?!')(.+?)''")),

    LIST_UNORDERED(Pattern.compile("(?<=((\n|^)\\s{0,16}))\\*(?= )")),
    LIST_ORDERED(Pattern.compile("(?<=((\n|^)(\\s{0,16})))(\\d+|[a-z])(\\.)(?= )"));

    public final Pattern pattern;

    ZimWikiHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
