package net.gsantner.markor.format.zimwiki;

import net.gsantner.markor.ui.hleditor.TextActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ZimWikiReplacePatternGenerator {
    private static final String uncheckedReplacement = "$1[ ] ";
    private static final String checkedReplacement = "$1[*] ";
    private static final String crossedReplacement = "$1[x] ";
    private static final String arrowReplacement = "$1[>] ";

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

    public List<TextActions.ReplacePattern> replaceWithNextStateCheckbox() {
        List<TextActions.ReplacePattern> replacePatterns = new ArrayList<>();

        // toggle order: no checkbox -> unchecked -> checked -> crossed -> arrow -> unchecked -> ...
        replacePatterns.addAll(toggleCheckboxToNextState());
        replacePatterns.addAll(replaceOtherPrefixesWithUncheckedBox());
        return replacePatterns;
    }

    private List<TextActions.ReplacePattern> toggleCheckboxToNextState() {
        return Arrays.asList(
                new TextActions.ReplacePattern(ZimWikiTextActions.PREFIX_UNCHECKED_LIST, checkedReplacement),
                new TextActions.ReplacePattern(ZimWikiTextActions.PREFIX_CHECKED_LIST, crossedReplacement),
                new TextActions.ReplacePattern(ZimWikiTextActions.PREFIX_CROSSED_LIST, arrowReplacement),
                new TextActions.ReplacePattern(ZimWikiTextActions.PREFIX_ARROW_LIST, uncheckedReplacement));
    }

    private List<TextActions.ReplacePattern> replaceOtherPrefixesWithUncheckedBox() {
        List<TextActions.ReplacePattern> replacePatterns = new ArrayList<>();
        for (Pattern otherPattern : ZimWikiTextActions.PREFIX_PATTERNS) {
            replacePatterns.add(new TextActions.ReplacePattern(otherPattern, uncheckedReplacement));
        }
        return replacePatterns;
    }
}
