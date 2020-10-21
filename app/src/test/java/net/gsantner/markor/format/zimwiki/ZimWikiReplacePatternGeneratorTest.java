package net.gsantner.markor.format.zimwiki;

import net.gsantner.markor.ui.hleditor.TextActions;

import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiReplacePatternGeneratorTest {
    private TextActions.ReplacePattern replacePattern;
    private String result;
    private final ZimWikiReplacePatternGenerator replacePatternGenerator = new ZimWikiReplacePatternGenerator();

    @Test
    public void removeHeadingCharsForExactHeadingLevel() {
        String headingChars = "===";
        replacePattern = replacePatternGenerator.removeHeadingCharsForExactHeadingLevel(headingChars);
        assertCorrectReplacement("=== Heading ===","Heading");
    }

    @Test
    public void replaceDifferentLevelHeadings() {
        String newHeadingChars = "===";
        replacePattern = replacePatternGenerator.replaceDifferentHeadingLevelWithThisLevel(newHeadingChars);
        assertCorrectReplacement("==== Heading ====", "=== Heading ===");
    }

    @Test
    public void createEmptyHeading() {
        String newHeadingChars = "===";
        replacePattern = replacePatternGenerator.createHeadingIfNoneThere(newHeadingChars);
        assertCorrectReplacement("", "===  ===");
    }

    @Test
    public void addHeadingCharactersToText() {
        String newHeadingChars = "===";
        replacePattern = replacePatternGenerator.createHeadingIfNoneThere(newHeadingChars);
        assertCorrectReplacement("Heading", "=== Heading ===");
    }

    @Test
    public void toggleFromUncheckedToCheckedBox() {
        String uncheckedItem = "[ ] some item";
        String result = replaceWithFirstMatchingPattern(replacePatternGenerator.replaceWithNextStateCheckbox(), uncheckedItem);
        assertThat(result).isEqualTo("[*] some item");
    }

    @Test
    public void toggleCheckBoxInCorrectOrder() {
        String[] orderedCheckboxStates = {" ", "*", "x", ">"};
        // create checkbox
        String currentLine = "some item";
        currentLine = replaceWithFirstMatchingPattern(replacePatternGenerator.replaceWithNextStateCheckbox(), currentLine);
        for (int i = 0; i<orderedCheckboxStates.length+1; i++) {
            assertThat(currentLine).isEqualTo("["+orderedCheckboxStates[i%orderedCheckboxStates.length]+"] some item");
            currentLine = replaceWithFirstMatchingPattern(replacePatternGenerator.replaceWithNextStateCheckbox(), currentLine);
        }
    }

    @Test
    public void replaceNonChecklistPrefixesWithUncheckedBox() {
        String[] otherPrefixes = {"1.", "a.", "*"};
        for (String otherPrefix : otherPrefixes) {
            String itemWithOtherPrefix = otherPrefix + " some item";
            String result = replaceWithFirstMatchingPattern(replacePatternGenerator.replaceWithNextStateCheckbox(), itemWithOtherPrefix);
            assertThat(result).isEqualTo("[ ] some item");
        }
    }

    @Test
    public void keepsWhitespaceWhenAddingCheckbox() {
        String original = " some item";
        String result = replaceWithFirstMatchingPattern(replacePatternGenerator.replaceWithNextStateCheckbox(), original);
        assertThat(result).isEqualTo(" [ ] some item");
    }


    private void assertCorrectReplacement(String original, String expectedReplacement) {
        Matcher matcher = replacePattern.searchPattern.matcher(original);
        assertThat(matcher.find()).isTrue();
        System.out.println("Matched: "+matcher.group());
        result = matcher.replaceFirst(replacePattern.replacePattern);
        assertThat(result).isEqualTo(expectedReplacement);
    }

    private String replaceWithFirstMatchingPattern(List<TextActions.ReplacePattern> replacePatterns, String input) {
        for(TextActions.ReplacePattern replacePattern : replacePatterns) {
            Matcher matcher = replacePattern.searchPattern.matcher(input);
            if (matcher.find()) {
                return matcher.replaceFirst(replacePattern.replacePattern);
            }
        }
        return null;
    }
}