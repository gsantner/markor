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
    public void preformattedTextBlock() {
        pattern = ZimWikiHighlighterPattern.PREFORMATTED_MULTILINE.pattern;
        Matcher matcher = pattern.matcher("Some text before\n" +
                "'''\n" +
                "some\n" +
                "text\n" +
                "block\n" +
                "'''\n" +
                "Text after the block.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("'''\nsome\ntext\nblock\n'''");
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

    @Test
    public void webLinkInSentence() {
        pattern = ZimWikiHighlighterPattern.LINK.pattern;
        Matcher matcher = pattern.matcher("Visit [[https://github.com/gsantner/markor|Markor on Github]] now!");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("[[https://github.com/gsantner/markor|Markor on Github]]");
    }

    @Test
    public void crossWikiLink() {
        pattern = ZimWikiHighlighterPattern.LINK.pattern;
        Matcher matcher = pattern.matcher("Go to another page [[Page Name]] in the same notebook.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("[[Page Name]]");
    }

    @Test
    public void linkToLocalImage() {
        pattern = ZimWikiHighlighterPattern.IMAGE.pattern;
        Matcher matcher = pattern.matcher("Some text.\nSome more text.\n{{.pasted_image.png}}\nMore text.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("{{.pasted_image.png}}");
    }

    @Test
    public void superscriptTextInSentence() {
        pattern = ZimWikiHighlighterPattern.SUPERSCRIPT.pattern;
        Matcher matcher = pattern.matcher("We also have _{subscript} and ^{superscript}.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("^{superscript}");
    }

    @Test
    public void subscriptTextInSentence() {
        pattern = ZimWikiHighlighterPattern.SUBSCRIPT.pattern;
        Matcher matcher = pattern.matcher("We also have _{subscript} and ^{superscript}.");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("_{subscript}");
    }


    @Test
    public void checkListOverMultipleLines() {
        pattern = ZimWikiHighlighterPattern.LIST_CHECK.pattern;
        Matcher matcher = pattern.matcher("Some text before...\n" +
                "[ ] unchecked item\n" +
                "[*] checked item\n" +
                "\t[x] crossed and indented item\n" +
                "[>] item marked with a yellow left-to-right-arrow\n" +
                "[ ] another unchecked item");
        String[] expectedMatches = {"[ ]", "[*]", "\t[x]", "[>]", "[ ]"};
        for (String expectedMatch : expectedMatches) {
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo(expectedMatch);
        }
    }
}