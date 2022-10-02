/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;
// TODO: currently this a copy from markdown. needs to be adapted
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.ReplacePatternGeneratorHelper;
import net.gsantner.markor.frontend.textview.TextViewUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AsciidocReplacePatternGenerator {

    // TODO: write tests
    //adapted for asciidoc

    //check on https://regex101.com/
    //you can copy and paste from here, \\ will be automatically transformed into \
    // standard asciidoc section
    // https://docs.asciidoctor.org/asciidoc/latest/sections/titles-and-levels/
    public static final Pattern PREFIX_ATX_HEADING = Pattern.compile("^(={1,6}\\s)");
    //not yet adapted for asciidoc
    public static final Pattern PREFIX_CHECKBOX_LIST = Pattern.compile("^((\\*{1,6})\\s\\[(\\s|\\*|x|X)]\\s)");
    public static final Pattern PREFIX_CHECKED_LIST = Pattern.compile("^((\\*{1,6})\\s\\[(\\*|x|X)]\\s)");
    public static final Pattern PREFIX_UNCHECKED_LIST = Pattern.compile("^((\\*{1,6})\\s\\[(\\s)]\\s)");
    public static final Pattern PREFIX_UNORDERED_LIST = Pattern.compile("^((\\*{1,6})\\s)");
    public static final Pattern PREFIX_ORDERED_LIST = Pattern.compile("^((\\.{1,6}))(\\s)");
//    TODO: to be removed
    // public static final Pattern PREFIX_QUOTE = Pattern.compile("^(>\\s)");
    // public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^(\\s*)");

//    public static final Pattern BLOCK_DELIMITER_COMMENT = Pattern.compile("^////");

    //    TODO: verstehen, wofür das gebraucht wird
    public static final AutoTextFormatter.FormatPatterns formatPatterns = new AutoTextFormatter.FormatPatterns(
            AsciidocReplacePatternGenerator.PREFIX_UNORDERED_LIST,
            AsciidocReplacePatternGenerator.PREFIX_CHECKBOX_LIST,
            AsciidocReplacePatternGenerator.PREFIX_ORDERED_LIST,
            2);

    public static final Pattern[] PREFIX_PATTERNS = {
            PREFIX_ORDERED_LIST,
            PREFIX_ATX_HEADING,
//            PREFIX_QUOTE,
            PREFIX_CHECKED_LIST,
            PREFIX_UNCHECKED_LIST,
            // Unordered has to be after checked list. Otherwise checklist will match as an unordered list.
            PREFIX_UNORDERED_LIST,
//            PREFIX_LEADING_SPACE,
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

        String heading = TextViewUtils.repeatChars('=', level);

        // Replace this exact heading level with nothing
        // TODO: RegExp.$1-$9 https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/n
        patterns.add(new ActionButtonBase.ReplacePattern("^" + heading + " ", "$1"));

        // Replace other headings with commonmark-compatible leading space
        // TODO: verstehe ich nicht, braucht man das im AsciiDoc?
        patterns.add(new ActionButtonBase.ReplacePattern(AsciidocReplacePatternGenerator.PREFIX_ATX_HEADING, "$1" + heading + " "));

        // Replace all other prefixes with heading
        // TODO: Verstehen
        // warum wird eine Liste geloopt?
        // reicht es nicht, einfach den Header zu setzen?
        // warum kein Leerzeichen zwischen heding und $1?
        // Warum stattdessen ein Leerzeichen nach $1?
        for (final Pattern pp : AsciidocReplacePatternGenerator.PREFIX_PATTERNS) {
            patterns.add(new ActionButtonBase.ReplacePattern(pp, heading + "$1 "));
        }

        return patterns;
    }
    public static List<ActionButtonBase.ReplacePattern> toggleToCheckedOrUncheckedListPrefix(String listChar) {
        final String unchecked = "$1" + listChar + " [ ] ";
        final String checked = "$1" + listChar + " [x] ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPatternOrAlternative(PREFIX_PATTERNS, PREFIX_UNCHECKED_LIST, unchecked, checked);
    }

    // TODO: adapt from markdown to asciidoc, markdown uses indent, asciidoc uses multiple characters
    public static List<ActionButtonBase.ReplacePattern> replaceWithUnorderedListPrefixOrRemovePrefix(String listChar) {
        final String unorderedListReplacement = "$1" + listChar + " ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_UNORDERED_LIST, unorderedListReplacement);
    }

    // TODO: adapt from markdown to asciidoc, markdown uses indent, asciidoc uses multiple characters
    public static List<ActionButtonBase.ReplacePattern> replaceWithOrderedListPrefixOrRemovePrefix(String listChar) {
        final String orderedListReplacement = "$1" + listChar + " ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS, PREFIX_ORDERED_LIST, orderedListReplacement);
    }

//    public static List<ActionButtonBase.ReplacePattern> toggleQuote() {
//        return ReplacePatternGeneratorHelper.replaceWithTargetPatternOrAlternative(PREFIX_PATTERNS, PREFIX_QUOTE, ">$1 ", "");
//    }
}
