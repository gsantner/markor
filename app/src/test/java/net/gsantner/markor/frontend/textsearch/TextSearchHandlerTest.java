package net.gsantner.markor.frontend.textsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSearchHandlerTest {
    @Test
    public void expandRegexReplacementUsesNumberedGroups() {
        final String text = "before first-last after";
        final Pattern pattern = Pattern.compile("([a-z]+)-([a-z]+)");
        final Matcher matcher = pattern.matcher(text);
        matcher.find();

        assertEquals("last, first", TextSearchHandler.expandRegexReplacement(
                pattern, text, matcher.start(), matcher.end(), "$2, $1"));
    }

    @Test
    public void expandRegexReplacementUsesNamedGroups() {
        final String text = "before key=value after";
        final Pattern pattern = Pattern.compile("(?<key>[a-z]+)=(?<value>[a-z]+)");
        final Matcher matcher = pattern.matcher(text);
        matcher.find();

        assertEquals("value: key", TextSearchHandler.expandRegexReplacement(
                pattern, text, matcher.start(), matcher.end(), "${value}: ${key}"));
    }

    @Test
    public void expandRegexReplacementRetainsLookaroundContext() {
        final String text = "prefix-value-suffix";
        final Pattern pattern = Pattern.compile("(?<=prefix-)([a-z]+)(?=-suffix)");
        final Matcher matcher = pattern.matcher(text);
        matcher.find();

        assertEquals("value-value", TextSearchHandler.expandRegexReplacement(
                pattern, text, matcher.start(), matcher.end(), "$1-$0"));
    }

    @Test
    public void expandRegexReplacementUsesSelectionAsInputDomain() {
        final String selectedText = "value";
        final Pattern pattern = Pattern.compile("^([a-z]+)$");

        assertEquals("value-value", TextSearchHandler.expandRegexReplacement(
                pattern, selectedText, 0, selectedText.length(), "$1-$0"));
    }

    @Test
    public void expandRegexReplacementSupportsEscapedDollar() {
        final String text = "value";
        final Pattern pattern = Pattern.compile("([a-z]+)");

        assertEquals("$1", TextSearchHandler.expandRegexReplacement(
                pattern, text, 0, text.length(), "\\$1"));
    }

    @Test
    public void expandRegexReplacementRejectsMissingGroup() {
        final String text = "value";
        final Pattern pattern = Pattern.compile("([a-z]+)");

        assertThrows(IndexOutOfBoundsException.class, () -> TextSearchHandler.expandRegexReplacement(
                pattern, text, 0, text.length(), "$2"));
    }

    @Test
    public void expandRegexReplacementRetainsPreviousMatchState() {
        final String text = "ba";
        final Pattern pattern = Pattern.compile("\\G(a)|(a)");
        final Matcher matcher = pattern.matcher(text);
        matcher.find();

        assertEquals("-a", TextSearchHandler.expandRegexReplacement(
                pattern, text, matcher.start(), matcher.end(), "$1-$2"));
    }
}
