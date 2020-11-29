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

import net.gsantner.markor.ui.hleditor.ReplacePatternGeneratorHelper;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.opoc.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MarkdownReplacePatternGenerator {

    // TODO: write tests

    public static final Pattern PREFIX_ORDERED_LIST = Pattern.compile("^(\\s*)((\\d+)(\\.|\\))(\\s+))");
    public static final Pattern PREFIX_ATX_HEADING = Pattern.compile("^(\\s{0,3})(#{1,6}\\s)");
    public static final Pattern PREFIX_QUOTE = Pattern.compile("^(>\\s)");
    public static final Pattern PREFIX_CHECKED_LIST = Pattern.compile("^(\\s*)((-|\\*|\\+)\\s\\[(x|X)]\\s)");
    public static final Pattern PREFIX_UNCHECKED_LIST = Pattern.compile("^(\\s*)((-|\\*|\\+)\\s\\[\\s]\\s)");
    public static final Pattern PREFIX_UNORDERED_LIST = Pattern.compile("^(\\s*)((-|\\*|\\+)\\s)");
    public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^(\\s*)");

    static final Pattern[] PREFIX_PATTERNS = {
            PREFIX_ORDERED_LIST,
            PREFIX_ATX_HEADING,
            PREFIX_QUOTE,
            PREFIX_CHECKED_LIST,
            PREFIX_UNCHECKED_LIST,
            // Unordered has to be after checked list. Otherwise checklist will match as an unordered list.
            PREFIX_UNORDERED_LIST,
            PREFIX_LEADING_SPACE,
    };

    private final static String ORDERED_LIST_REPLACEMENT = "$11. ";

    /**
     * Set/unset ATX heading level on each selected line
     * <p>
     * This routine will make the following conditional changes
     * <p>
     * Line is heading of same level as requested -> remove heading
     * Line is heading of different level that that requested -> add heading of specified level
     * Line is not heading -> add heading of specified level
     *
     * @param level ATX heading level
     */
    public static List<TextActions.ReplacePattern> setOrUnsetHeadingWithLevel(int level) {

        List<TextActions.ReplacePattern> patterns = new ArrayList<>();

        String heading = StringUtils.repeatChars('#', level);

        // Replace this exact heading level with nothing
        patterns.add(new TextActions.ReplacePattern("^(\\s{0,3})" + heading + " ", "$1"));

        // Replace other headings with commonmark-compatible leading space
        patterns.add(new TextActions.ReplacePattern(MarkdownReplacePatternGenerator.PREFIX_ATX_HEADING, "$1" + heading + " "));

        // Replace all other prefixes with heading
        for (final Pattern pp : MarkdownReplacePatternGenerator.PREFIX_PATTERNS) {
            patterns.add(new TextActions.ReplacePattern(pp, heading + "$1 "));
        }

        return patterns;
    }

    public static List<TextActions.ReplacePattern> replaceWithUnorderedListPrefixOrRemovePrefix(String listChar) {
        final String unorderedListReplacement = "$1" + listChar + " ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_UNORDERED_LIST, unorderedListReplacement);
    }

    public static List<TextActions.ReplacePattern> toggleToCheckedOrUncheckedListPrefix(String listChar) {
        final String unchecked = "$1" + listChar + " [ ] ";
        final String checked = "$1" + listChar + " [x] ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPatternOrAlternative(PREFIX_PATTERNS, PREFIX_UNCHECKED_LIST, unchecked, checked);
    }

    public static List<TextActions.ReplacePattern> replaceWithOrderedListPrefixOrRemovePrefix() {
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_ORDERED_LIST, ORDERED_LIST_REPLACEMENT);
    }

    public static List<TextActions.ReplacePattern> toggleQuote() {
        return ReplacePatternGeneratorHelper.replaceWithTargetPatternOrAlternative(PREFIX_PATTERNS, PREFIX_QUOTE, ">$1 ", "");
    }
}
