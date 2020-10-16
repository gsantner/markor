package net.gsantner.markor.format.zimwiki;

import java.util.regex.Pattern;

public enum ZimWikiHighlighterPattern {

    BOLD(Pattern.compile("(?<=(\\n|^|\\s|\\*))(\\*{2})[^*\\s](?=\\S)(.*?)[^*\\s]?\\2(?=(\\n|$|\\s|\\*))")),
    ITALICS(Pattern.compile("(?<=(\\n|^|\\s|/))(/{2})[^/\\s](.*?)[^/\\s]?\\2(?=(\\n|$|\\s|/))")),
    MARKED(Pattern.compile("(?<=(\\n|^|\\s|_))(_{2})[^_\\s](.*?)[^_\\s]?\\2(?=(\\n|$|\\s|_))")),
    STRIKETHROUGH(Pattern.compile("(?<=(\\n|^|\\s|~))(~{2})[^~\\s](.*?)[^~\\s]?\\2(?=(\\n|$|\\s|~))")),
    HEADING(Pattern.compile("(?<=(\\n|^|\\s))(==+)[ \\t]+\\S.*?[ \\t]=*(?=(\\n|$|\\s))")),
    PREFORMATTED_INLINE(Pattern.compile("''(?!')(.+?)''")),
    PREFORMATTED_MULTILINE(Pattern.compile("(?s)(?<=[\\n^])'''[\\n$](.*?)[\\n^]'''(?=[\\n$])")),
    LIST_UNORDERED(Pattern.compile("(?<=((\n|^)\\s{0,16}))\\*(?= )")),
    LIST_ORDERED(Pattern.compile("(?<=((\n|^)(\\s{0,16})))(\\d+|[a-zA-Z])(\\.)(?= )")),
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
