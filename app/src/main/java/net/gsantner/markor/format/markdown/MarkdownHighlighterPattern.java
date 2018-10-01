/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import java.util.regex.Pattern;

public enum MarkdownHighlighterPattern {
    BOLD(Pattern.compile("(?<=(\\n|^|\\s))(([*_]){2,3})(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    ITALICS(Pattern.compile("(?<=(\\n|^|\\s))([*_])(?=((?!\\2)|\\2{2,}))(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    HEADER(Pattern.compile("(?m)((^#{1,6}[^\\S\\n][^\\n]+)|((\\n|^)[^\\s]+.*?\\n(-{2,}|={2,})[^\\S\\n]*$))")),
    LINK(Pattern.compile("\\[([^\\[]+)\\]\\(([^\\)]+)\\)")),
    LIST_UNORDERED(Pattern.compile("(\\n|^)\\s{0,12}([*+-])( \\[[ xX]\\])?(?= )")),
    LIST_ORDERED(Pattern.compile("(?m)^([0-9]+)(\\.)")),
    QUOTATION(Pattern.compile("(\\n|^)>")),
    STRIKETHROUGH(Pattern.compile("~{2}(.*?)\\S~{2}")),
    CODE(Pattern.compile("(?m)(`(.*?)`)|(^[^\\S\\n]{4}.*$)")),
    DOUBLESPACE_LINE_ENDING(Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n"));


    public final Pattern pattern;

    MarkdownHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
