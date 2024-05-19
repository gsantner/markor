/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.ReplacePatternGeneratorHelper;
import net.gsantner.opoc.format.GsTextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class WikitextReplacePatternGenerator {
    public static final Pattern PREFIX_UNORDERED_LIST = Pattern.compile("^(\\s*)((\\*)\\s)");
    public static final Pattern PREFIX_ORDERED_LIST = Pattern.compile("^(\\s*)((\\d+|[a-zA-z])(\\.)(\\s+))");
    public static final Pattern PREFIX_UNCHECKED_LIST = Pattern.compile("^(\\s*)(\\[\\s]\\s)");
    public static final Pattern PREFIX_CHECKED_LIST = Pattern.compile("^(\\s*)(\\[(\\*)]\\s)");
    public static final Pattern PREFIX_CROSSED_LIST = Pattern.compile("^(\\s*)(\\[(x)]\\s)");
    public static final Pattern PREFIX_RIGHT_ARROW_LIST = Pattern.compile("^(\\s*)(\\[(>)]\\s)");
    public static final Pattern PREFIX_LEFT_ARROW_LIST = Pattern.compile("^(\\s*)(\\[(<)]\\s)");
    public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^(\\s*)");
    public static final Pattern PREFIX_CHECKBOX_LIST = Pattern.compile("^(\\s*)((\\[)[\\sx*>](]\\s))");

    public static AutoTextFormatter.FormatPatterns formatPatterns = new AutoTextFormatter.FormatPatterns(
            WikitextReplacePatternGenerator.PREFIX_UNORDERED_LIST,
            PREFIX_CHECKBOX_LIST,
            WikitextReplacePatternGenerator.PREFIX_ORDERED_LIST,
            0);

    public static final Pattern[] PREFIX_PATTERNS = {
            PREFIX_ORDERED_LIST,
            PREFIX_CHECKED_LIST,
            PREFIX_UNCHECKED_LIST,
            PREFIX_CROSSED_LIST,
            PREFIX_RIGHT_ARROW_LIST,
            PREFIX_LEFT_ARROW_LIST,
            PREFIX_UNORDERED_LIST,
            PREFIX_LEADING_SPACE,
    };

    private static final String uncheckedReplacement = "$1[ ] ";
    private static final String checkedReplacement = "$1[*] ";
    private static final String crossedReplacement = "$1[x] ";
    private static final String rightArrowReplacement = "$1[>] ";
    private static final String leftArrowReplacement = "$1[<] ";
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
    public static List<ActionButtonBase.ReplacePattern> setOrUnsetHeadingWithLevel(int level) {
        List<ActionButtonBase.ReplacePattern> patterns = new ArrayList<>();

        final int numberOfEqualSigns = 7 - level;

        boolean isValidWikitextHeading = numberOfEqualSigns >= 2 && numberOfEqualSigns <= 6;
        if (!isValidWikitextHeading) {
            return patterns;
        }

        String headingChars = GsTextUtils.repeatChars('=', numberOfEqualSigns);

        patterns.add(removeHeadingCharsForExactHeadingLevel(headingChars));
        patterns.add(replaceDifferentHeadingLevelWithThisLevel(headingChars));
        patterns.add(createHeadingIfNoneThere(headingChars));

        return patterns;
    }

    private static ActionButtonBase.ReplacePattern removeHeadingCharsForExactHeadingLevel(String headingChars) {
        return new ActionButtonBase.ReplacePattern(
                "^\\s{0,3}" + headingChars + "[ \\t](.*)[ \\t]" + headingChars + "\\w*",
                "$1");
    }

    private static ActionButtonBase.ReplacePattern replaceDifferentHeadingLevelWithThisLevel(String headingChars) {
        return new ActionButtonBase.ReplacePattern("^\\s{0,3}={2,6}([ \\t].*[ \\t])={2,6}",
                headingChars + "$1" + headingChars);
    }

    private static ActionButtonBase.ReplacePattern createHeadingIfNoneThere(String headingChars) {
        return new ActionButtonBase.ReplacePattern("^\\s*?(\\S?.*)\\s*",
                headingChars + " $1 " + headingChars);
    }

    public static List<ActionButtonBase.ReplacePattern> replaceWithNextStateCheckbox() {
        List<ActionButtonBase.ReplacePattern> replacePatterns = new ArrayList<>();

        // toggle order: no checkbox -> unchecked -> checked -> crossed -> arrow -> unchecked -> ...
        replacePatterns.addAll(toggleCheckboxToNextState());
        replacePatterns.addAll(replaceOtherPrefixesWithUncheckedBox());
        return replacePatterns;
    }

    public static List<ActionButtonBase.ReplacePattern> removeCheckbox() {
        Pattern anyCheckboxItem = Pattern.compile("^(\\s*)(\\[([ x*><])]\\s)");
        return Collections.singletonList(new ActionButtonBase.ReplacePattern(anyCheckboxItem, "$1"));
    }

    private static List<ActionButtonBase.ReplacePattern> toggleCheckboxToNextState() {
        return Arrays.asList(
                new ActionButtonBase.ReplacePattern(PREFIX_UNCHECKED_LIST, checkedReplacement),
                new ActionButtonBase.ReplacePattern(PREFIX_CHECKED_LIST, crossedReplacement),
                new ActionButtonBase.ReplacePattern(PREFIX_CROSSED_LIST, rightArrowReplacement),
                new ActionButtonBase.ReplacePattern(PREFIX_RIGHT_ARROW_LIST, leftArrowReplacement),
                new ActionButtonBase.ReplacePattern(PREFIX_LEFT_ARROW_LIST, uncheckedReplacement));
    }

    private static List<ActionButtonBase.ReplacePattern> replaceOtherPrefixesWithUncheckedBox() {
        List<ActionButtonBase.ReplacePattern> replacePatterns = new ArrayList<>();
        for (final Pattern otherPattern : PREFIX_PATTERNS) {
            replacePatterns.add(new ActionButtonBase.ReplacePattern(otherPattern, uncheckedReplacement));
        }
        return replacePatterns;
    }

    public static List<ActionButtonBase.ReplacePattern> replaceWithUnorderedListPrefixOrRemovePrefix() {
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_UNORDERED_LIST, unorderedListReplacement);
    }

    public static List<ActionButtonBase.ReplacePattern> replaceWithOrderedListPrefixOrRemovePrefix() {
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_ORDERED_LIST, orderedListReplacement);
    }

    public static List<ActionButtonBase.ReplacePattern> deindentOneTab() {
        return Collections.singletonList(new ActionButtonBase.ReplacePattern("^\t", ""));
    }

    public static List<ActionButtonBase.ReplacePattern> indentOneTab() {
        return Collections.singletonList(new ActionButtonBase.ReplacePattern("^", "\t"));
    }
}
