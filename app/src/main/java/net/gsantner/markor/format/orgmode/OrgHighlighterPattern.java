/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.orgmode;

import java.util.regex.Pattern;

import static net.gsantner.markor.format.orgmode.OrgReplacePatternGenerator.HEADING_CHAR_ESCAPED;

public enum OrgHighlighterPattern {
    BOLD(Pattern.compile("\\*(.*?)\\S\\*")),
    ITALICS(Pattern.compile("_(.*?)\\S_")),
    HEADING(Pattern.compile("(?m)((^" + HEADING_CHAR_ESCAPED + "{1,6}[^\\S\\n][^\\n]+)|((\\n|^)[^\\s]+.*?\\n(-{2,}|={2,})[^\\S\\n]*$))")),
    HEADING_SIMPLE(Pattern.compile("(?m)^(" + HEADING_CHAR_ESCAPED + "{1,6}\\s.*$)")),
    LINK(Pattern.compile("\\[([^\\[]+)\\]\\[([^\\]]+)\\]")),
    LIST_UNORDERED(Pattern.compile("(\\n|^)\\s{0,16}([*+-])( \\[[ xX]\\])?(?= )")),
    LIST_ORDERED(Pattern.compile("(?m)^\\s{0,16}(\\d+)(:?\\.|\\))\\s")),
    QUOTATION(Pattern.compile("(\\n|^)>")),
    STRIKETHROUGH(Pattern.compile("\\+(.*?)\\S\\+")),
    CODE(Pattern.compile("~(.*?)\\S~*")),
    DOUBLESPACE_LINE_ENDING(Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n")),
    ACTION_LINK_PATTERN(Pattern.compile("(?m)\\[(.*?)\\]\\((.*?)\\)")),
    ACTION_IMAGE_PATTERN(Pattern.compile("(?m)!\\[(.*?)\\]\\((.*?)\\)"));

    public final Pattern pattern;

    OrgHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
