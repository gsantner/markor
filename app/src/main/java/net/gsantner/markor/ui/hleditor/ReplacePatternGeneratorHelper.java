package net.gsantner.markor.ui.hleditor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ReplacePatternGeneratorHelper {
    public static List<TextActions.ReplacePattern> replaceNonSelectedPatternsWithSelectedOrReplaceWithAlternative(
            final Pattern[] allSearchPatterns,
            final Pattern selectedSearchPattern, final String selectedReplacement,
            final String alternativeReplacement
    ) {
        List<TextActions.ReplacePattern> replacePatterns = new ArrayList<>();
        for (final Pattern searchPattern : allSearchPatterns) {
            String replacement;
            if (!searchPattern.equals(selectedSearchPattern)) {
                replacement = selectedReplacement;
            } else {
                replacement = alternativeReplacement;
            }
            replacePatterns.add(new TextActions.ReplacePattern(searchPattern, replacement));
        }
        return replacePatterns;
    }

    public static List<TextActions.ReplacePattern> replaceOtherPrefixWithSelectedOrRemovePrefix(final Pattern[] allPrefixPatterns, final Pattern selectedPrefixPattern, final String selectedPrefixReplacement) {
        String removePrefixReplacement = "$1";  // only keep whitespaces before prefix
        return replaceNonSelectedPatternsWithSelectedOrReplaceWithAlternative(allPrefixPatterns,
                selectedPrefixPattern, selectedPrefixReplacement, removePrefixReplacement);
    }
}
