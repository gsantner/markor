package net.gsantner.markor.format.zimwiki;

import net.gsantner.markor.ui.hleditor.TextActions;

import java.util.regex.Pattern;

public class ZimWikiReplacePatternGenerator {

    public TextActions.ReplacePattern removeHeadingCharsForExactHeadingLevel(String headingChars) {
        return new TextActions.ReplacePattern(
                "^\\s{0,3}" + headingChars + "[ \\t](.*?)[ \\t]" + headingChars + "\\w*",
                "$1");
    }

    public TextActions.ReplacePattern replaceDifferentHeadingLevelWithThisLevel(String headingChars) {
        return new TextActions.ReplacePattern("^\\s{0,3}={0,6}([ \\t].*?[ \\t])={0,6}",
                headingChars+"$1"+headingChars);
    }

    public TextActions.ReplacePattern createHeadingIfNoneThere(String headingChars) {
        return new TextActions.ReplacePattern("^\\s*?(\\S?.*)\\s*",
                headingChars+" $1 "+headingChars);
    }

    public TextActions.ReplacePattern replacePrefixWithHeading(Pattern prefixPattern, String headingChars) {
        return new TextActions.ReplacePattern(prefixPattern,
                headingChars+"  "+headingChars);
    }

}
