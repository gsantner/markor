/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiHighlighterTests {

    public static class HeadingTests {

        private Pattern pattern;

        @Before
        public void before() {
            pattern = ZimWikiHighlighter.Patterns.HEADING.pattern;
        }

        @Test
        public void biggestHeading() {
            String heading = "====== this is the biggest heading ======";
            findAndAssertEqualHeading(heading);
        }

        @Test
        public void smallestHeading() {
            String heading = "== this is the smallest heading ==";
            findAndAssertEqualHeading(heading);
        }

        @Test
        public void emptyHeading() {
            String heading = "===  ===";
            findAndAssertEqualHeading(heading);
        }

        @Test
        public void invalidHeadingTooFewEqualSigns() {
            String invalidHeading = "= this is not a valid heading =";
            Matcher matcher = pattern.matcher(invalidHeading);
            assertThat(matcher.find()).isFalse();
        }

        @Test
        public void invalidHeadingUnequalCountOfEqualSigns() {
            String invalidHeading = "=== three signs on the left but only two on the right ==";
            Matcher matcher = pattern.matcher(invalidHeading);
            assertThat(matcher.find()).isFalse();
        }

        private void findAndAssertEqualHeading(String expectedHeading) {
            Matcher matcher = pattern.matcher(expectedHeading);
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo(expectedHeading);
        }
    }

    public static class PatternBoldTests {
        private Pattern pattern;

        @Before
        public void before() {
            pattern = ZimWikiHighlighter.Patterns.BOLD.pattern;
        }

        @Test
        public void starBoldInSentence() {
            Matcher m = pattern.matcher("this **sentence** has bold");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("**sentence**");
        }

        @Test
        public void multipleStarBoldInSentence() {
            Matcher m = pattern.matcher("this **sentence** has **bold**");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("**sentence**");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("**bold**");
        }

        @Test
        public void boldWithSpace() {
            Matcher m = pattern.matcher("**bold words**");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("**bold words**");
        }

        @Test
        public void boldWithOnlyOneStarAtTheEndShouldNotMatch() {
            Matcher m = pattern.matcher("**not bold*");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void boldInAList() {
            Matcher m = pattern.matcher("* **bold** word");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("**bold**");
        }

        @Test
        public void boldInAWordShouldNotMatch() {
            Matcher m = pattern.matcher("2**5 + 4**3");
            assertThat(m.find()).isFalse();
        }
    }


    public static class PatternCheckListTests {

        private Pattern pattern;

        @Test
        public void uncheckedItem() {
            pattern = ZimWikiHighlighter.Patterns.CHECKLIST_UNCHECKED.pattern;
            Matcher m = pattern.matcher("[ ] Unchecked Item");
            assertCorrectCheckboxWithInnerSymbol(m, " ");
        }

        @Test
        public void checkedItem() {
            pattern = ZimWikiHighlighter.Patterns.CHECKLIST_CHECKED.pattern;
            Matcher m = pattern.matcher("[*] Checked Item");
            assertCorrectCheckboxWithInnerSymbol(m, "*");
        }

        @Test
        public void crossedItem() {
            pattern = ZimWikiHighlighter.Patterns.CHECKLIST_CROSSED.pattern;
            Matcher m = pattern.matcher("[x] Crossed Item");
            assertCorrectCheckboxWithInnerSymbol(m, "x");
        }

        @Test
        public void itemWithArrow() {
            pattern = ZimWikiHighlighter.Patterns.CHECKLIST_ARROW.pattern;
            Matcher m = pattern.matcher("[>] Marked Item with a yellow arrow");
            assertCorrectCheckboxWithInnerSymbol(m, ">");
        }

        @Test
        public void roundBracesDoNotMatch() {
            Pattern[] checklistPatterns = {ZimWikiHighlighter.Patterns.CHECKLIST.pattern,
                    ZimWikiHighlighter.Patterns.CHECKLIST_CHECKED.pattern,
                    ZimWikiHighlighter.Patterns.CHECKLIST_CROSSED.pattern,
                    ZimWikiHighlighter.Patterns.CHECKLIST_ARROW.pattern};
            for (Pattern checklistPattern : checklistPatterns) {
                Matcher m = checklistPattern.matcher("( ) invalid item");
                assertThat(m.find()).isFalse();
            }
        }

        private void assertCorrectCheckboxWithInnerSymbol(Matcher m, String symbol) {
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("[" + symbol + "]");
            assertThat(m.group(ZimWikiHighlighter.Patterns.CHECKBOX_LEFT_BRACKET_GROUP)).isEqualTo("[");
            assertThat(m.group(ZimWikiHighlighter.Patterns.CHECKBOX_SYMBOL_GROUP)).isEqualTo(symbol);
            assertThat(m.group(ZimWikiHighlighter.Patterns.CHECKBOX_RIGHT_BRACKET_GROUP)).isEqualTo("]");
        }
    }


    public static class PatternItalicTest {

        private Pattern pattern;

        @Before
        public void before() {
            pattern = ZimWikiHighlighter.Patterns.ITALICS.pattern;
        }

        @Test
        public void underlineItalic() {
            Matcher m = pattern.matcher("//italic//");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//italic//");
        }

        @Test
        public void oneLetterUnderlineItalic() {
            Matcher m = pattern.matcher("//i//");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//i//");
        }

        @Test
        public void underlineItalicInSentence() {
            Matcher m = pattern.matcher("this //sentence// has italic");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//sentence//");
        }

        @Test
        public void multipleUnderlineItalicInSentence() {
            Matcher m = pattern.matcher("this //sentence// has //italic//");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//sentence//");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//italic//");
        }

        @Test
        public void italicWithSpace() {
            Matcher m = pattern.matcher("//italic words//");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//italic words//");
        }

        @Test
        public void mixedUnderlineAndStarShouldNotMatch() {
            Matcher m = pattern.matcher("_italic*");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void mixedStarAndUnderlineShouldNotMatch2() {
            Matcher m = pattern.matcher("*italic_");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void boldUnderlineShouldNotMatch() {
            Matcher m = pattern.matcher("__bold__");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void boldStarShouldNotMatch() {
            Matcher m = pattern.matcher("**bold**");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void boldAndItalicStarShouldMatch() {
            Matcher m = pattern.matcher("//**bold & italic**//");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//**bold & italic**//");
        }

        @Test
        public void boldAndItalicSlashWithExtraPrecedingSlashShouldMatch() {
            Matcher m = pattern.matcher("///**bold & italic**//");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//**bold & italic**//");
        }

        @Test
        public void boldAndItalicSlashWithExtraTrailingSlashShouldMatch() {
            Matcher m = pattern.matcher("//**bold & italic**///");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//**bold & italic**//");
        }

    /*@Ignore("Ideally this would pass, but I don't think it's possible with regex. As it is, the regex is pretty complex.")
    @Test
    public void italicStarWithExtraPrecedingStarShouldMatch() {
        Matcher m = pattern.matcher("**italic*");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("**italic*");
    }*/

        @Test
        public void italicSlashWithExtraTrailingSlashShouldMatch() {
            Matcher m = pattern.matcher("//italic///");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//italic//");
        }


        @Test
        public void italicInAList() {
            Matcher m = pattern.matcher("* //italic// word");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("//italic//");
        }
    }


    public static class PatternListTest {

        private Pattern pattern;

        @Before
        public void before() {
            pattern = ZimWikiHighlighter.Patterns.LIST_UNORDERED.pattern;
        }

        @Test
        public void dashItemShouldNotMatch() {
            Matcher m = pattern.matcher("- Item");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void starItem() {
            Matcher m = pattern.matcher("* Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*");
        }

        @Test
        public void starItemMultipleSpacesShouldMatch() {
            Matcher m = pattern.matcher("*    Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*");
        }

        @Test
        public void starItemIndentedShouldMatch() {
            Matcher m = pattern.matcher("\t* Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("*");
        }

        @Test
        public void linePeriodItemShouldNotMatch() {
            Matcher m = pattern.matcher("-. Item");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void starPeriodItemShouldNotMatch() {
            Matcher m = pattern.matcher("*. Item");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void headerShouldNotMatch() {
            Matcher m = pattern.matcher("==");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void headerWithCharachersShouldNotMatch() {
            Matcher m = pattern.matcher("====== header ======");
            assertThat(m.find()).isFalse();
        }

        @Test
        public void boldShouldNotMatch() {
            Matcher m = pattern.matcher("**bold**");
            assertThat(m.find()).isFalse();
        }

    }


    public static class PatternOrderedListTest {

        private Pattern pattern;

        @Before
        public void before() {
            pattern = ZimWikiHighlighter.Patterns.LIST_ORDERED.pattern;
        }

        @Test
        public void numberItem() {
            Matcher m = pattern.matcher("1. Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("1.");
        }

        @Test
        public void letterItem() {
            Matcher m = pattern.matcher("b. Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("b.");
        }

        @Test
        public void capitalItem() {
            Matcher m = pattern.matcher("C. Item");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("C.");
        }
    }


    public static class PatternStrikethroughTest {

        private Pattern pattern;

        @Before
        public void before() {
            pattern = ZimWikiHighlighter.Patterns.STRIKETHROUGH.pattern;
        }

        @Test
        public void strikethrough() {
            Matcher m = pattern.matcher("~~strikethrough~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~strikethrough~~");
        }

        @Test
        public void strikethroughSingleCharacter() {
            Matcher m = pattern.matcher("~~s~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~s~~");
        }

        @Test
        public void strikethroughWithSpace() {
            Matcher m = pattern.matcher("~~strike through~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~strike through~~");
        }

        @Test
        public void multipleStrikethrough() {
            Matcher m = pattern.matcher("~~one~~ ~~two~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~one~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~two~~");
        }

        @Test
        public void strikethroughWithPrecedingTilde() {
            Matcher m = pattern.matcher("~~~one~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~one~~");
        }

        @Test
        public void strikethroughWithTrailingTildeSkipsExtraTildes() {
            Matcher m = pattern.matcher("~~one~~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~one~~");
        }

        @Test
        public void strikethroughWithSpaceAsLastCharacherShouldMatch() {
            Matcher m = pattern.matcher("~~one ~~");
            assertThat(m.find()).isTrue();
            assertThat(m.group()).isEqualTo("~~one ~~");
        }
    }

    public static class PatternOverallTests {
        private Pattern pattern;

        @Test
        public void boldWordInSentence() {
            pattern = ZimWikiHighlighter.Patterns.BOLD.pattern;
            Matcher matcher = pattern.matcher("The following **word** is bold.");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("**word**");
        }

        @Test
        public void italicsWordInSentence() {
            pattern = ZimWikiHighlighter.Patterns.ITALICS.pattern;
            Matcher matcher = pattern.matcher("The following //word// is in italics.");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("//word//");
        }

        @Test
        public void highlightedWordInSentence() {
            pattern = ZimWikiHighlighter.Patterns.HIGHLIGHTED.pattern;
            Matcher matcher = pattern.matcher("The following __word__ is marked (highlighted).");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("__word__");
        }

        @Test
        public void struckThroughWordInSentence() {
            pattern = ZimWikiHighlighter.Patterns.STRIKETHROUGH.pattern;
            Matcher matcher = pattern.matcher("The following ~~word~~ is struck through.");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("~~word~~");
        }

        @Test
        public void preformattedWordInSentence() {
            pattern = ZimWikiHighlighter.Patterns.PREFORMATTED_INLINE.pattern;
            Matcher matcher = pattern.matcher("The following ''word'' is struck through.");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("''word''");
        }

        @Test
        public void preformattedTextBlock() {
            pattern = ZimWikiHighlighter.Patterns.PREFORMATTED_MULTILINE.pattern;
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
            pattern = ZimWikiHighlighter.Patterns.LIST_UNORDERED.pattern;
            Matcher matcher = pattern.matcher("some text...\n* first item\n\t* item 11\n* item 2");
            for (int i = 0; i < 3; i++) {
                assertThat(matcher.find());
                assertThat(matcher.group()).isEqualTo("*");
            }
        }


        @Test
        public void orderedListHighlightingsNumbers() {
            pattern = ZimWikiHighlighter.Patterns.LIST_ORDERED.pattern;
            Matcher matcher = pattern.matcher("\n1. first item\n\t2. second item\n");
            String[] expectedMatches = {"1.", "2."};
            for (String expectedMatch : expectedMatches) {
                assertThat(matcher.find());
                assertThat(matcher.group()).isEqualTo(expectedMatch);
            }
        }

        @Test
        public void orderedListHighlightingsCharacters() {
            pattern = ZimWikiHighlighter.Patterns.LIST_ORDERED.pattern;
            Matcher matcher = pattern.matcher("\na. first item\nb. second item\n");
            String[] expectedMatches = {"a.", "b."};
            for (String expectedMatch : expectedMatches) {
                assertThat(matcher.find());
                assertThat(matcher.group()).isEqualTo(expectedMatch);
            }
        }

        @Test
        public void orderedListHighlightingsNumbersAndCharacters() {
            pattern = ZimWikiHighlighter.Patterns.LIST_ORDERED.pattern;
            Matcher matcher = pattern.matcher("\n1. first item\n2. second item\n\ta. item 2a\n\tb. item 2b\n");
            String[] expectedMatches = {"1.", "2.", "a.", "b."};
            for (String expectedMatch : expectedMatches) {
                assertThat(matcher.find());
                assertThat(matcher.group()).isEqualTo(expectedMatch);
            }
        }

        @Test
        public void webLinkInSentence() {
            pattern = ZimWikiHighlighter.Patterns.LINK.pattern;
            Matcher matcher = pattern.matcher("Visit [[https://github.com/gsantner/markor|Markor on Github]] now!");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("[[https://github.com/gsantner/markor|Markor on Github]]");
        }

        @Test
        public void crossWikiLink() {
            pattern = ZimWikiHighlighter.Patterns.LINK.pattern;
            Matcher matcher = pattern.matcher("Go to another page [[Page Name]] in the same notebook.");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("[[Page Name]]");
        }

        @Test
        public void linkToLocalImage() {
            pattern = ZimWikiHighlighter.Patterns.IMAGE.pattern;
            Matcher matcher = pattern.matcher("Some text.\nSome more text.\n{{.pasted_image.png}}\nMore text.");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("{{.pasted_image.png}}");
        }

        @Test
        public void superscriptTextInSentence() {
            pattern = ZimWikiHighlighter.Patterns.SUPERSCRIPT.pattern;
            Matcher matcher = pattern.matcher("We also have _{subscript} and ^{superscript}.");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("^{superscript}");
        }

        @Test
        public void subscriptTextInSentence() {
            pattern = ZimWikiHighlighter.Patterns.SUBSCRIPT.pattern;
            Matcher matcher = pattern.matcher("We also have _{subscript} and ^{superscript}.");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("_{subscript}");
        }


        @Test
        public void checkListOverMultipleLines() {
            pattern = ZimWikiHighlighter.Patterns.CHECKLIST.pattern;
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
}
