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
    public static final Pattern PREFIX_ATX_HEADING = Pattern.compile("^(={1,6} {1})");
    //not yet adapted for asciidoc
    public static final Pattern PREFIX_CHECKBOX_LIST = Pattern.compile(
            "^( *)((\\*{1,6}) \\[( |\\*|x|X)] {1,})");
    public static final Pattern PREFIX_CHECKED_LIST = Pattern.compile(
            "^( *)((\\*{1,6}) \\[(\\*|x|X)] {1,})");
    public static final Pattern PREFIX_UNCHECKED_LIST = Pattern.compile(
            "^( *)((\\*{1,6}) \\[( )] {1,})");
    public static final Pattern PREFIX_UNORDERED_LIST = Pattern.compile("^( *)((\\*{1,6}) {1,})");
    public static final Pattern PREFIX_ORDERED_LIST = Pattern.compile("^( *)((\\.{1,6}) {1,})");
    //required as replacablePattern \s - any whitespace character: [\r\n\t\f\v ]
    public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^(\\s*)");
    //    TODO: to be removed
    // public static final Pattern PREFIX_QUOTE = Pattern.compile("^(>\\s)");
    // public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^(\\s*)");

    //    public static final Pattern BLOCK_DELIMITER_COMMENT = Pattern.compile("^////");

    //    TODO: verstehen, wofür das gebraucht wird
    public static final AutoTextFormatter.FormatPatterns formatPatterns =
            new AutoTextFormatter.FormatPatterns(
                    AsciidocReplacePatternGenerator.PREFIX_UNORDERED_LIST,
                    AsciidocReplacePatternGenerator.PREFIX_CHECKBOX_LIST,
                    AsciidocReplacePatternGenerator.PREFIX_ORDERED_LIST, 2);
    // these patterns will be replaced, when we toggle Header, ordered or unordered list, checkbox
    public static final Pattern[] PREFIX_PATTERNS = {
            PREFIX_ORDERED_LIST,
            PREFIX_ATX_HEADING,
            PREFIX_CHECKED_LIST,
            PREFIX_UNCHECKED_LIST,
            // Unordered has to be after checked list. Otherwise checklist will match as an
            // unordered list.
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

        // Create a new list in which patterns are inserted, which are then processed in order -
        // Replacements are performed.
        List<ActionButtonBase.ReplacePattern> patterns = new ArrayList<>();

        // AsciiDoc uses '=' to mark sections (headers).
        String heading = TextViewUtils.repeatChars('=', level);

        // Then and only then, if the current line matches the level, the header should be removed
        // Replace this exact heading level with nothing
        // Hint: RegExp.$1-$9 https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/n
        patterns.add(new ActionButtonBase.ReplacePattern("^" + heading + " ", "$1"));

        // Replace other headings with commonmark-compatible leading space
        // Other headers are to be preserved, their state as "to be preserved" must be marked
        // somehow.
        // Now, where there are headers after the above Replacement (for matching level only),
        // they header prefix are appended as suffixes:
        // ' + heading + " "'
        // So this is only for marking, to be used again further below.
        patterns.add(new ActionButtonBase.ReplacePattern(
                AsciidocReplacePatternGenerator.PREFIX_ATX_HEADING, "$1" + heading + " "));

        // Replace all other prefixes with heading
        // TODO: Verstehen
        // There are now 2 entries that are processed one after the other
        /*
* `ActionButtonBase.ReplacePattern` +
Generates 1 pattern
* `ActionButtonBase.ReplacePattern` +
Generates 1 pattern
* `for (final Pattern pp : AsciidocReplacePatternGenerator.PREFIX_PATTERNS)`
creates 6 patterns, because there are 6 entries in PREFIX_PATTERNS

Now you just have to understand what is supposed to happen and why it doesn't happen the way it
* is supposed to happen

for level = 1

. existing entries with prefix "= " should remove this prefix
. all other header lines are replaced by "$1= ", so the later required Suffix "=" is appended as
* suffix
But when and how will the suffixes be removed from there later?
. everything that matches the following 6 patterns gets the prefix removed and the prefix for
* header 1 instead

But now when are the headers corrected that moved the header to the suffix in step 2?

After the pattern is created, it is passed to the function `runRegexReplaceAction`:
         */

        // this list PREFIX_PATTERNS contains everything which should now be replaced by heading
        // + "$1 ".
        // But why 'heading + "$1 "' and not 'heading + " $1"'?
        // And when is the back translation of the previously marked headers with header
        // information in the suffix?

        for (final Pattern pp : AsciidocReplacePatternGenerator.PREFIX_PATTERNS) {
            patterns.add(new ActionButtonBase.ReplacePattern(pp, heading + "$1 "));
        }

        return patterns;
    }

    // TODO: works correctly only for the first level, lower levels are removed but downgraded
    public static List<ActionButtonBase.ReplacePattern> toggleToCheckedOrUncheckedListPrefix(
            String listChar) {
        final String unchecked = "$1" + listChar + " [ ] ";
        final String checked = "$1" + listChar + " [x] ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPatternOrAlternative(PREFIX_PATTERNS,
                PREFIX_UNCHECKED_LIST, unchecked, checked);
    }

    // TODO: works correctly only for the first level, lower levels are removed but downgraded on
    //  new insert
    public static List<ActionButtonBase.ReplacePattern> replaceWithUnorderedListPrefixOrRemovePrefix(
            String listChar) {
        final String unorderedListReplacement = "$1" + listChar + " ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS,
                PREFIX_UNORDERED_LIST, unorderedListReplacement);
    }

    // TODO: works correctly only for the first level, lower levels are removed but downgraded on
    //  new insert
    public static List<ActionButtonBase.ReplacePattern> replaceWithOrderedListPrefixOrRemovePrefix(
            String listChar) {
        final String orderedListReplacement = "$1" + listChar + " ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS,
                PREFIX_ORDERED_LIST, orderedListReplacement);
    }

}
