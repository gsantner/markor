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
    PREFORMATTED_MULTILINE(Pattern.compile("(?s)(?<=[\\n^])'''[\\n$](.*?)[\\n^]'''(?=[\\n$])")),
    LIST_UNORDERED(Pattern.compile("(?<=((\n|^)\\s{0,16}))\\*(?= )")),
    LIST_ORDERED(Pattern.compile("(?<=((\n|^)(\\s{0,16})))(\\d+|[a-z])(\\.)(?= )")),
    LINK(Pattern.compile("(\\[\\[(?!\\[)(.+?\\]*)]\\])")),
    IMAGE(Pattern.compile("(\\{\\{(?!\\{)(.*?)\\}\\})")),
    LIST_CHECK(Pattern.compile("(?<=(\\n|^))\t*(\\[[ xX*>]?]|\\([ xX*>]?\\))(?= )")),
    SUBSCRIPT(Pattern.compile("(_\\{(?!~)(.+?)\\})")),
    SUPERSCRIPT(Pattern.compile("(\\^\\{(?!~)(.+?)\\})"));

    public final Pattern pattern;

    ZimWikiHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
