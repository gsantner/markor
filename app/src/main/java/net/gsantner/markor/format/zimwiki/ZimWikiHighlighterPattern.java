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

    BOLD(Pattern.compile("(?<=(\\n|^|\\s|\\*))(\\*{2})[^*\\s](?=\\S)(.*?)[^*\\s]?\\2(?=(\\n|$|\\s|\\*))")),
    ITALICS(Pattern.compile("(?<=(\\n|^|\\s|/))(/{2})[^/\\s](.*?)[^/\\s]?\\2(?=(\\n|$|\\s|/))")),
    HIGHLIGHTED(Pattern.compile("(?<=(\\n|^|\\s|_))(_{2})[^_\\s](.*?)[^_\\s]?\\2(?=(\\n|$|\\s|_))")),
    STRIKETHROUGH(Pattern.compile("(?<=(\\n|^|\\s|~))(~{2})[^~\\s](.*?)[^~\\s]?\\2(?=(\\n|$|\\s|~))")),
    HEADING(Pattern.compile("(?<=(\\n|^|\\s))(==+)[ \\t]+(.*?)[ \\t]\\2(?=(\\n|$|\\s))")),
    PREFORMATTED_INLINE(Pattern.compile("''(?!')(.+?)''")),
    PREFORMATTED_MULTILINE(Pattern.compile("(?s)(?<=[\\n^])'''[\\n$](.*?)[\\n^]'''(?=[\\n$])")),
    LIST_UNORDERED(Pattern.compile("(?<=((\n|^)\\s{0,16}))\\*(?= )")),
    LIST_ORDERED(Pattern.compile("(?<=((\n|^)(\\s{0,16})))(\\d+|[a-zA-Z])(\\.)(?= )")),
    LINK(Pattern.compile("(\\[\\[(?!\\[)(.+?\\]*)]\\])")),
    IMAGE(Pattern.compile("(\\{\\{(?!\\{)(.*?)\\}\\})")),
    CHECKLIST(Pattern.compile("(?<=(\\n|^))\t*(\\[)([ x*>])(])(?= )")),
    CHECKLIST_UNCHECKED(Pattern.compile("(?<=(\\n|^))\t*(\\[)( )(])(?= )")),
    CHECKLIST_CHECKED(Pattern.compile("(?<=(\\n|^))\t*(\\[)(\\*)(])(?= )")),
    CHECKLIST_CROSSED(Pattern.compile("(?<=(\\n|^))\t*(\\[)(x)(])(?= )")),
    CHECKLIST_ARROW(Pattern.compile("(?<=(\\n|^))\t*(\\[)(>)(])(?= )")),
    SUBSCRIPT(Pattern.compile("(_\\{(?!~)(.+?)\\})")),
    SUPERSCRIPT(Pattern.compile("(\\^\\{(?!~)(.+?)\\})")),
    ZIMHEADER(Pattern.compile("^Content-Type: text/x-zim-wiki(\r\n|\r|\n)" +
            "Wiki-Format: zim \\d+\\.\\d+(\r\n|\r|\n)" +
            "Creation-Date: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.+:\\d]+"));

    // groups for matching individual parts of the checklist regex
    public static final int CHECKBOX_LEFT_BRACKET_GROUP = 2;
    public static final int CHECKBOX_SYMBOL_GROUP = 3;
    public static final int CHECKBOX_RIGHT_BRACKET_GROUP = 4;

    public final Pattern pattern;

    ZimWikiHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
