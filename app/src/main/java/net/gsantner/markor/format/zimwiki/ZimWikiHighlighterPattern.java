/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import java.util.regex.Pattern;

public enum ZimWikiHighlighterPattern {
    ACTION_IMAGE_PATTERN(Pattern.compile("(?m)\\{\\{(.*?)\\}\\}")),
    ACTION_LINK_PATTERN(Pattern.compile("(?m)\\[\\[(.*?)|(.*?)\\]\\]")),
    BOLD(Pattern.compile("(?<=(\\n|^|\\s))(\\*\\*([^*]*?)\\*\\*)(?=(\\n|$|\\s))")),
    CODE(Pattern.compile("(?m)('(?!')(.*?)')|(^[^\\S\\n]{4}(?![0-9\\-*+]).*$)")),
    DOUBLESPACE_LINE_ENDING(Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n")),
    HEADING(Pattern.compile("(^(={2,6})\\s[^=]+\\s\\1$)")),
    ITALICS(Pattern.compile("(//[^/]*?//)")),
    LINK(Pattern.compile("\\[\\[([^\\]]+)\\]|([^\\)]+)\\]\\]")),
    LINKSUB(Pattern.compile("\\[\\[\\+([^\\]]+)\\]\\]")),
    LINKTOP(Pattern.compile("\\[\\[:([^\\]]+)\\]\\]")),
    LIST_CHECK(Pattern.compile("^\t*(\\[[ xX*>]?\\]|\\([ xX*>]?\\)) ")),
    LIST_ORDERED(Pattern.compile("^\t*([0-9a-zA-Z]+\\.) ")),
    LIST_UNORDERED(Pattern.compile("^\t*(\\*)(?= )")),
    QUOTATION(Pattern.compile("^>")),
    STRIKETHROUGH(Pattern.compile("~~([^~]+)~~"));


    public final Pattern pattern;

    ZimWikiHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
