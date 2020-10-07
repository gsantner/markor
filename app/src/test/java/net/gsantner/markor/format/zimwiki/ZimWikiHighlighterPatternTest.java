package net.gsantner.markor.format.zimwiki;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiHighlighterPatternTest {
    private Pattern pattern;

    @Test
    public void boldWordInSentence() {
        pattern = ZimWikiHighlighterPattern.BOLD.pattern;
        Matcher matcher = pattern.matcher("The following **word** is bold.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("**word**");
    }

    @Test
    public void italicsWordInSentence() {
        pattern = ZimWikiHighlighterPattern.ITALICS.pattern;
        Matcher matcher = pattern.matcher("The following //word// is in italics.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("//word//");
    }

    @Test
    public void markedWordInSentence() {
        pattern = ZimWikiHighlighterPattern.MARKED.pattern;
        Matcher matcher = pattern.matcher("The following __word__ is marked (highlighted).");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("__word__");
    }

    @Test
    public void struckThroughWordInSentence() {
        pattern = ZimWikiHighlighterPattern.STRIKETHROUGH.pattern;
        Matcher matcher = pattern.matcher("The following ~~word~~ is struck through.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("~~word~~");
    }

    @Test
    public void preformattedWordInSentence() {
        pattern = ZimWikiHighlighterPattern.PREFORMATTED_INLINE.pattern;
        Matcher matcher = pattern.matcher("The following ''word'' is struck through.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("''word''");
    }

    @Test
    public void unorderedListHighlightings() {
        pattern = ZimWikiHighlighterPattern.LIST_UNORDERED.pattern;
        Matcher matcher = pattern.matcher("some text...\n* first item\n\t* item 11\n* item 2");
        for (int i=0; i<3; i++) {
            assertThat(matcher.find());
            assertThat(matcher.group()).isEqualTo("*");
        }
    }


    @Test
    public void orderedListHighlightingsNumbers() {
        pattern = ZimWikiHighlighterPattern.LIST_ORDERED.pattern;
        Matcher matcher = pattern.matcher("\n1. first item\n\t2. second item\n");
        String[] expectedMatches = {"1.", "2."};
        for (String expectedMatch : expectedMatches) {
            assertThat(matcher.find());
            assertThat(matcher.group()).isEqualTo(expectedMatch);
        }
    }

    @Test
    public void orderedListHighlightingsCharacters() {
        pattern = ZimWikiHighlighterPattern.LIST_ORDERED.pattern;
        Matcher matcher = pattern.matcher("\na. first item\nb. second item\n");
        String[] expectedMatches = {"a.", "b."};
        for (String expectedMatch : expectedMatches) {
            assertThat(matcher.find());
            assertThat(matcher.group()).isEqualTo(expectedMatch);
        }
    }

    @Test
    public void orderedListHighlightingsNumbersAndCharacters() {
        pattern = ZimWikiHighlighterPattern.LIST_ORDERED.pattern;
        Matcher matcher = pattern.matcher("\n1. first item\n2. second item\n\ta. item 2a\n\tb. item 2b\n");
        String[] expectedMatches = {"1.", "2.", "a.", "b."};
        for (String expectedMatch : expectedMatches) {
            assertThat(matcher.find());
            assertThat(matcher.group()).isEqualTo(expectedMatch);
        }
    }
}