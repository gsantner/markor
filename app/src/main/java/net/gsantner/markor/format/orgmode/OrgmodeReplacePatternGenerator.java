/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.orgmode;

import android.util.Log;

import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.ReplacePatternGeneratorHelper;
import net.gsantner.opoc.format.GsTextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OrgmodeReplacePatternGenerator {

    // TODO: write tests

    public static final Pattern PREFIX_ORDERED_LIST = Pattern.compile("^(\\s*)((\\d+)(\\.|\\))(\\s))");
    public static final Pattern PREFIX_ATX_HEADING = Pattern.compile("^(\\s{0,3})(\\*+\\s)");
    public static final Pattern PREFIX_CHECKED_LIST = Pattern.compile("^(\\s*)((-|\\+)\\s\\[(X)]\\s)");
    public static final Pattern PREFIX_CHECKBOX_LIST = Pattern.compile("^(\\s*)(([-+]\\s\\[)[\\sX](]\\s))");
    public static final Pattern PREFIX_UNCHECKED_LIST = Pattern.compile("^(\\s*)((-|\\+)\\s\\[\\s]\\s)");
    public static final Pattern PREFIX_UNORDERED_LIST = Pattern.compile("^(\\s*)((-|\\+)\\s)");
    public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^(\\s*)");

    public static final AutoTextFormatter.FormatPatterns formatPatterns = new AutoTextFormatter.FormatPatterns(
            OrgmodeReplacePatternGenerator.PREFIX_UNORDERED_LIST,
            OrgmodeReplacePatternGenerator.PREFIX_CHECKBOX_LIST,
            OrgmodeReplacePatternGenerator.PREFIX_ORDERED_LIST,
            2);

    public static final Pattern[] PREFIX_PATTERNS = {
            PREFIX_ORDERED_LIST,
            PREFIX_ATX_HEADING,
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
    public static List<ActionButtonBase.ReplacePattern> setOrUnsetHeadingWithLevel(int level) {

        List<ActionButtonBase.ReplacePattern> patterns = new ArrayList<>();

        String heading = "\\*".repeat(level);

        // Replace this exact heading level with nothing
        patterns.add(new ActionButtonBase.ReplacePattern("^(\\s{0,3})" + heading + " ", "$1"));

        // Replace other headings with commonmark-compatible leading space
        patterns.add(new ActionButtonBase.ReplacePattern(OrgmodeReplacePatternGenerator.PREFIX_ATX_HEADING, "$1" + heading + " "));

        // Replace all other prefixes with heading
        for (final Pattern pp : OrgmodeReplacePatternGenerator.PREFIX_PATTERNS) {
            patterns.add(new ActionButtonBase.ReplacePattern(pp, heading + "$1 "));
        }

        return patterns;
    }

    public static List<ActionButtonBase.ReplacePattern> replaceWithUnorderedListPrefixOrRemovePrefix(String listChar) {
        final String unorderedListReplacement = "$1" + listChar + " ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_UNORDERED_LIST, unorderedListReplacement);
    }

    public static List<ActionButtonBase.ReplacePattern> toggleToCheckedOrUncheckedListPrefix(String listChar) {
        final String unchecked = "$1" + listChar + " [ ] ";
        final String checked = "$1" + listChar + " [X] ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPatternOrAlternative(PREFIX_PATTERNS, PREFIX_UNCHECKED_LIST, unchecked, checked);
    }

    public static List<ActionButtonBase.ReplacePattern> replaceWithOrderedListPrefixOrRemovePrefix() {
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_ORDERED_LIST, ORDERED_LIST_REPLACEMENT);
    }
}
