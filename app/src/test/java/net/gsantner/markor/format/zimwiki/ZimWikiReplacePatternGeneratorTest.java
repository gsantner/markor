package net.gsantner.markor.format.zimwiki;

import net.gsantner.markor.ui.hleditor.TextActions;

import org.junit.Test;

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

    private void assertCorrectReplacement(String original, String expectedReplacement) {
        Matcher matcher = replacePattern.searchPattern.matcher(original);
        assertThat(matcher.find()).isTrue();
        System.out.println("Matched: "+matcher.group());
        result = matcher.replaceFirst(replacePattern.replacePattern);
        assertThat(result).isEqualTo(expectedReplacement);
    }
}