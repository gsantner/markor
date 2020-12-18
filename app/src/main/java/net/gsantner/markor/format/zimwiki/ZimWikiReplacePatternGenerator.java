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

import net.gsantner.markor.ui.hleditor.ReplacePatternGeneratorHelper;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.opoc.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ZimWikiReplacePatternGenerator {
    public static final Pattern PREFIX_UNORDERED_LIST = Pattern.compile("^(\\s*)((\\*)\\s)");
    public static final Pattern PREFIX_ORDERED_LIST = Pattern.compile("^(\\s*)((\\d+|[a-zA-z])(\\.)(\\s+))");
    public static final Pattern PREFIX_UNCHECKED_LIST = Pattern.compile("^(\\s*)(\\[\\s]\\s)");
    public static final Pattern PREFIX_CHECKED_LIST = Pattern.compile("^(\\s*)(\\[(\\*)]\\s)");
    public static final Pattern PREFIX_CROSSED_LIST = Pattern.compile("^(\\s*)(\\[(x)]\\s)");
    public static final Pattern PREFIX_ARROW_LIST = Pattern.compile("^(\\s*)(\\[(>)]\\s)");
    public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^(\\s*)");

    public static final Pattern[] PREFIX_PATTERNS = {
            PREFIX_ORDERED_LIST,
            PREFIX_CHECKED_LIST,
            PREFIX_UNCHECKED_LIST,
            PREFIX_CROSSED_LIST,
            PREFIX_ARROW_LIST,
            PREFIX_UNORDERED_LIST,
            PREFIX_LEADING_SPACE,
    };

    private static final String uncheckedReplacement = "$1[ ] ";
    private static final String checkedReplacement = "$1[*] ";
    private static final String crossedReplacement = "$1[x] ";
    private static final String arrowReplacement = "$1[>] ";
    private static final String unorderedListReplacement = "$1* ";
    private static final String orderedListReplacement = "$11. ";

    /**
     * Set/unset heading level for the line (the lower the level, the higher the count of equal signs)
     * <p>
     * This routine will make the following conditional changes
     * <p>
     * Line is heading of same level as requested -> remove heading
     * Line is heading of different level that that requested -> replace with requested heading
     * Line is not heading -> add heading of specified level
     *
     * @param level heading level
     */
    public static List<TextActions.ReplacePattern> setOrUnsetHeadingWithLevel(int level) {
        List<TextActions.ReplacePattern> patterns = new ArrayList<>();

        final int numberOfEqualSigns = 7 - level;

        boolean isValidZimWikiHeading = numberOfEqualSigns >= 2 && numberOfEqualSigns <= 6;
        if (!isValidZimWikiHeading) {
            return patterns;
        }

        String headingChars = StringUtils.repeatChars('=', numberOfEqualSigns);

        patterns.add(removeHeadingCharsForExactHeadingLevel(headingChars));
        patterns.add(replaceDifferentHeadingLevelWithThisLevel(headingChars));
        patterns.add(createHeadingIfNoneThere(headingChars));

        return patterns;
    }

    private static TextActions.ReplacePattern removeHeadingCharsForExactHeadingLevel(String headingChars) {
        return new TextActions.ReplacePattern(
                "^\\s{0,3}" + headingChars + "[ \\t](.*)[ \\t]" + headingChars + "\\w*",
                "$1");
    }

    private static TextActions.ReplacePattern replaceDifferentHeadingLevelWithThisLevel(String headingChars) {
        return new TextActions.ReplacePattern("^\\s{0,3}={2,6}([ \\t].*[ \\t])={2,6}",
                headingChars + "$1" + headingChars);
    }

    private static TextActions.ReplacePattern createHeadingIfNoneThere(String headingChars) {
        return new TextActions.ReplacePattern("^\\s*?(\\S?.*)\\s*",
                headingChars + " $1 " + headingChars);
    }

    public static List<TextActions.ReplacePattern> replaceWithNextStateCheckbox() {
        List<TextActions.ReplacePattern> replacePatterns = new ArrayList<>();

        // toggle order: no checkbox -> unchecked -> checked -> crossed -> arrow -> unchecked -> ...
        replacePatterns.addAll(toggleCheckboxToNextState());
        replacePatterns.addAll(replaceOtherPrefixesWithUncheckedBox());
        return replacePatterns;
    }

    private static List<TextActions.ReplacePattern> toggleCheckboxToNextState() {
        return Arrays.asList(
                new TextActions.ReplacePattern(PREFIX_UNCHECKED_LIST, checkedReplacement),
                new TextActions.ReplacePattern(PREFIX_CHECKED_LIST, crossedReplacement),
                new TextActions.ReplacePattern(PREFIX_CROSSED_LIST, arrowReplacement),
                new TextActions.ReplacePattern(PREFIX_ARROW_LIST, uncheckedReplacement));
    }

    private static List<TextActions.ReplacePattern> replaceOtherPrefixesWithUncheckedBox() {
        List<TextActions.ReplacePattern> replacePatterns = new ArrayList<>();
        for (final Pattern otherPattern : PREFIX_PATTERNS) {
            replacePatterns.add(new TextActions.ReplacePattern(otherPattern, uncheckedReplacement));
        }
        return replacePatterns;
    }

    public static List<TextActions.ReplacePattern> replaceWithUnorderedListPrefixOrRemovePrefix() {
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_UNORDERED_LIST, unorderedListReplacement);
    }

    public static List<TextActions.ReplacePattern> replaceWithOrderedListPrefixOrRemovePrefix() {
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_ORDERED_LIST, orderedListReplacement);
    }

    public static List<TextActions.ReplacePattern> deindentOneTab() {
        return Collections.singletonList(new TextActions.ReplacePattern("^\t", ""));
    }

    public static List<TextActions.ReplacePattern> indentOneTab() {
        return Collections.singletonList(new TextActions.ReplacePattern("^", "\t"));
    }
}
