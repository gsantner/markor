/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;

import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.ReplacePatternGeneratorHelper;
import net.gsantner.opoc.format.GsTextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AsciidocReplacePatternGenerator {

    // TODO: write tests
    // How to automate test in Java developement?

    //check on https://regex101.com/
    // in Android Studio (not in VSC), you can copy and paste from here, \\ will be
    // automatically transformed into \
    // standard asciidoc section
    // https://docs.asciidoctor.org/asciidoc/latest/sections/titles-and-levels/
    public static final Pattern PREFIX_HEADING = Pattern.compile("^( {0})(=)(={0,5})( {1})");
    // Level greater than 1, minimum 2 ==
    // we use {1,6} instead of {1,5} to be able to deindent also this level
    public static final Pattern PREFIX_HEADING_GT1 = Pattern.compile(
            "^( {0})(=)(={1,6})( {1})");

    // simplified syntax for all lists: In fact, leading spaces are also possible
    // This can be changed with ^( *) or ^( {0,16}) or similar.
    // But then syntax highlighting would have to be adjusted as well
    // This would conflict with formatting of "code", using indent .
    // You would have to parse the previous line
    // And Syntax Highlight doesn't have to do everything, but should only support it.
    public static final Pattern PREFIX_UNORDERED_LIST = Pattern.compile(
            "^( {0})(\\*)(\\*{0,5})( {1,})");
    // Level greater than 1, minimum 2 **
    // we use {1,6} instead of {1,5} to be able to deindent also this level
    public static final Pattern PREFIX_UNORDERED_LIST_GT1 = Pattern.compile(
            "^( {0})(\\*)(\\*{1,6})( {1,})");
    public static final Pattern PREFIX_ORDERED_LIST = Pattern.compile(
            "^( {0})(\\.)(\\.{0,5})( {1,})");
    // Level greater than 1, minimum 2 ..
    // we use {1,6} instead of {1,5} to be able to deindent also this level
    public static final Pattern PREFIX_ORDERED_LIST_GT1 = Pattern.compile(
            "^( {0})(\\.)(\\.{1,6})( {1,})");

    public static final Pattern PREFIX_CHECKBOX_LIST = Pattern.compile(
            "^( {0})(\\*{1,6})( \\[( |\\*|x|X)] {1,})");
    public static final Pattern PREFIX_CHECKED_LIST = Pattern.compile(
            "^( {0})(\\*{1,6})( \\[(\\*|x|X)] {1,})");
    public static final Pattern PREFIX_UNCHECKED_LIST = Pattern.compile(
            "^( {0})(\\*{1,6})( \\[( )] {1,})");

    //required as replacablePattern \s - any whitespace character: [\r\n\t\f\v ]
    public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^( *)");

    public static final AutoTextFormatter.FormatPatterns formatPatterns =
            new AutoTextFormatter.FormatPatterns(
                    AsciidocReplacePatternGenerator.PREFIX_UNORDERED_LIST,
                    AsciidocReplacePatternGenerator.PREFIX_CHECKBOX_LIST,
                    AsciidocReplacePatternGenerator.PREFIX_ORDERED_LIST, 2);
    // these patterns will be replaced, when we toggle Header, ordered or unordered list, checkbox
    public static final Pattern[] PREFIX_PATTERNS = {
            PREFIX_ORDERED_LIST,
            PREFIX_HEADING,
            PREFIX_CHECKED_LIST,
            PREFIX_UNCHECKED_LIST,
            // Unordered has to be after checked list. Otherwise checklist will match as an
            // unordered list.
            PREFIX_UNORDERED_LIST,
            PREFIX_LEADING_SPACE,
    };

//    // these patterns could be used to identify lines for indent_level and deindent_level
//    public static final Pattern[] PREFIX_LEVEL_PATTERNS = {
//            PREFIX_ATX_HEADING,
//            PREFIX_ORDERED_LIST,
//            PREFIX_UNORDERED_LIST,
//    };
//    private final static String ORDERED_LIST_REPLACEMENT = "$11. ";

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
        String heading = GsTextUtils.repeatChars('=', level);

        // pattern no 1:
        // Then and only then, if the current line matches the level, the header should be removed
        // Replace this exact heading level with nothing
        // Hint: RegExp.$1-$9 https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/n
        //"$1" normaly is "", because it is the first group like "( {0})" or "( *)"
//        patterns.add(new ActionButtonBase.ReplacePattern("^" + heading + " ", "$1"));
        patterns.add(new ActionButtonBase.ReplacePattern("^" + heading + " ", ""));

        // pattern no 2:
        // Replace other headings
        // should be the same result as with replacePattern: heading + " "
//        patterns.add(new ActionButtonBase.ReplacePattern(
//                AsciidocReplacePatternGenerator.PREFIX_ATX_HEADING, "$1" + heading + " "));
        patterns.add(new ActionButtonBase.ReplacePattern(
                AsciidocReplacePatternGenerator.PREFIX_HEADING, heading + " "));

        // pattern no 3 to 8:
        // Replace all other prefixes with heading
        // this list PREFIX_PATTERNS contains everything which should now be replaced by heading
        // + "$1 ", which could be " ", "  ", "   " and so on, if the list character doesn't
        // start at first column
        // But why 'heading + "$1 "' and not 'heading + " $1"'?

        for (final Pattern pp : AsciidocReplacePatternGenerator.PREFIX_PATTERNS) {
//            patterns.add(new ActionButtonBase.ReplacePattern(pp, heading + "$1 "));
            patterns.add(new ActionButtonBase.ReplacePattern(pp, heading + " "));
        }

        /*
example: for level = 1

. existing entries with prefix "= " should remove this prefix: +
  "= aaa" => "aaa"
. all other header lines are replaced by "$1= ": +
  "=== aaa" => "= "
. all matchings from PREFIX_PATTERNS should be replaced

* After the patterns are created, they are passed to the function `runRegexReplaceAction`:
         */

        return patterns;
    }

    public static List<ActionButtonBase.ReplacePattern> indentLevel() {

        // Create a new list in which patterns are inserted, which are then processed in order -
        // Replacements are performed.
        List<ActionButtonBase.ReplacePattern> patterns = new ArrayList<>();

        // we could also use PREFIX_LEVEL_PATTERNS, instead of 3 direct statements

        // insert one (1) level: duplicate $2, which is "=" or "*" or "."
        patterns.add(new ActionButtonBase.ReplacePattern(PREFIX_HEADING, "$1$2$2$3$4"));
        patterns.add(new ActionButtonBase.ReplacePattern(PREFIX_ORDERED_LIST, "$1$2$2$3$4"));
        patterns.add(new ActionButtonBase.ReplacePattern(PREFIX_UNORDERED_LIST, "$1$2$2$3$4"));

        return patterns;
    }

    public static List<ActionButtonBase.ReplacePattern> deindentLevel() {

        // Create a new list in which patterns are inserted, which are then processed in order -
        // Replacements are performed.
        List<ActionButtonBase.ReplacePattern> patterns = new ArrayList<>();

        // we could also use PREFIX_LEVEL_PATTERNS, instead of 3 direct statements

        // remove one (1) level: remove $2, which is the first "=" or "*" or "."
        // $3 contains minimum one (1) "=" or "*" or "."
        patterns.add(new ActionButtonBase.ReplacePattern(PREFIX_HEADING_GT1, "$1$3$4"));
        patterns.add(new ActionButtonBase.ReplacePattern(PREFIX_ORDERED_LIST_GT1, "$1$3$4"));
        patterns.add(new ActionButtonBase.ReplacePattern(PREFIX_UNORDERED_LIST_GT1, "$1$3$4"));

        return patterns;
    }

    // TODO: works correctly only for the first level "* [ ]",
    //  other levels like "** [ ]" are upgraded to "* [x]"
    // Think about ways to keep the level
    // I tried, but it didn't work:
    // unchecked = "$1$2" + " [ ] ";
    // checked = "$1$2" + " [x] ";
    public static List<ActionButtonBase.ReplacePattern> toggleToCheckedOrUncheckedListPrefix(
            String listChar) {
        final String unchecked = "$1" + listChar + " [ ] ";
        final String checked = "$1" + listChar + " [x] ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPatternOrAlternative(PREFIX_PATTERNS,
                PREFIX_UNCHECKED_LIST, unchecked, checked);
    }

    // works fine for the first level "* ",
    // other levels like "** " are only removed
    // but they can't be restored, because how we should know the previous level?
    public static List<ActionButtonBase.ReplacePattern> replaceWithUnorderedListPrefixOrRemovePrefix(
            String listChar) {
        final String unorderedListReplacement = "$1" + listChar + " ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS,
                PREFIX_UNORDERED_LIST, unorderedListReplacement);
    }

    // works fine for the first level "* ",
    // other levels like "** " are only removed
    // but they can't be restored, because how we should know the previous level?
    public static List<ActionButtonBase.ReplacePattern> replaceWithOrderedListPrefixOrRemovePrefix(
            String listChar) {
        final String orderedListReplacement = "$1" + listChar + " ";
        return ReplacePatternGeneratorHelper.replaceWithTargetPrefixOrRemove(PREFIX_PATTERNS,
                PREFIX_ORDERED_LIST, orderedListReplacement);
    }

}
