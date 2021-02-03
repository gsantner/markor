/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui.hleditor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ReplacePatternGeneratorHelper {
    /**
     * Constructs a list of replacement patterns for strings which match the replaceable patterns so that:
     * 1. Strings which do not match the targeted pattern are replaced in such a way
     * that the targeted pattern is achieved.
     * 2. Strings which already match the targeted pattern are replaced in such a way that an
     * alternative pattern is achieved.
     */
    public static List<TextActions.ReplacePattern> replaceWithTargetPatternOrAlternative(
            final Pattern[] replaceablePatterns,
            final Pattern targetPattern, final String targetReplacement,
            final String alternativeReplacement
    ) {
        final List<TextActions.ReplacePattern> replacePatterns = new ArrayList<>();
        for (final Pattern replaceablePattern : replaceablePatterns) {
            final String replacement;
            if (!replaceablePattern.equals(targetPattern)) {
                replacement = targetReplacement;
            } else {
                replacement = alternativeReplacement;
            }
            replacePatterns.add(new TextActions.ReplacePattern(replaceablePattern, replacement));
        }
        return replacePatterns;
    }

    /**
     * Constructs a list of prefix replacement patterns for strings which match the replaceable patterns so that:
     * 1. Strings which contain prefixes which are different from the targeted one are replaced in such
     * a way that the targeted prefix is achieved.
     * 2. In Strings which already contain the targeted prefix, this prefix is removed.
     */
    public static List<TextActions.ReplacePattern> replaceWithTargetPrefixOrRemove(final Pattern[] replaceablePrefixPatterns, final Pattern targetPrefixPattern, final String targetPrefixReplacement) {
        String removePrefixReplacement = "$1";  // only keep whitespaces before prefix
        return replaceWithTargetPatternOrAlternative(replaceablePrefixPatterns,
                targetPrefixPattern, targetPrefixReplacement, removePrefixReplacement);
    }
}
