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
    HEADING(Pattern.compile("^(==+\\s+\\S.*?\\s*=*)$")),
    LINK(Pattern.compile("(\\[\\[(?!\\[)(.+?\\]*)\\]\\])")),
    IMAGE(Pattern.compile("(\\{\\{(?!\\{)(.*?)\\}\\})")),
    LIST_CHECK(Pattern.compile("^\t*(\\[[ xX*>]?\\]|\\([ xX*>]?\\)) ")),
    LIST_ORDERED(Pattern.compile("^\t*([\\d]+\\.|[a-zA-Z]+\\.) ")),
    LIST_UNORDERED(Pattern.compile("^\t*(\\*)(?= )")),
    EMPHASIS(Pattern.compile("(//(?!/)(.*?)(?<!:)//)")),
    STRONG(Pattern.compile("(\\*\\*(?!\\*)(.*?)\\*\\*)")),
    MARK(Pattern.compile("(__(?!_)(.*?)__)")),
    STRIKE(Pattern.compile("(~~(?!~)(.+?)~~)")),
    SUBSCRIPT(Pattern.compile("(_\\{(?!~)(.+?)\\})")),
    SUPERSCRIPT(Pattern.compile("(\\^\\{(?!~)(.+?)\\})")),
    VERBATIM(Pattern.compile("(''(?!').+?'')")),
    VERBATIM_BLOCK(Pattern.compile("(?m)('''(?!''')(.+?)''')")),
    // TODO Table
    ;


    public final Pattern pattern;

    ZimWikiHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
