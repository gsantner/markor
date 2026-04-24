/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikitextFileTests {

    public static class GeneratorTests {
        @SuppressWarnings("SpellCheckingInspection")
        @Test
        public void createsCorrectContentsForNewWikitextFiles() {
            Locale.setDefault(Locale.ENGLISH);
            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, Calendar.DECEMBER, 24, 18, 0, 30);
            calendar.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));
            Date date = calendar.getTime();
            String expected = "Content-Type: text/x-zim-wiki\n" +
                    "Wiki-Format: zim 0.6\n" +
                    "Creation-Date: 2020-12-24T18:00:30+01:00\n" +
                    "\n" +
                    "====== My new wiki page ======\n" +
                    "Created Thursday 24 December 2020\n";
            // TODO: replace logic should not be necessary - find out why time and time zone is not created correctly in the test
            String actual = WikitextActionButtons.createWikitextHeaderAndTitleContents("My_new_wiki_page", date, "Created");
            String actualReplaced = actual.replaceAll("(?m)T1[78]:00:30.+$", "");
            String expectedReplaced = expected.replaceAll("(?m)T1[78]:00:30.+$", "");
            assertThat(actualReplaced).isEqualTo(expectedReplaced);
        }
    }

    public static class ZimFileHeaderTest {

        private Pattern pattern;

        @Test
        public void zimHeaderAtBeginningOfTheFileShouldMatch() {
            pattern = WikitextSyntaxHighlighter.ZIMHEADER;
            Matcher matcher = pattern.matcher("Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00\nOther content...");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00");
        }

        @Test
        public void zimHeaderNotAtBeginningOfTheFileShouldNotMatch() {
            pattern = WikitextSyntaxHighlighter.ZIMHEADER;
            Matcher matcher = pattern.matcher("Blabla\nContent-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00");
            assertThat(matcher.find()).isFalse();
        }
    }

    public static class CodeBlockPreprocessingTest {

        @Test
        public void sourceViewBlockIsReplacedByPlaceholderAndFencedWithLang() {
            String input = "before\n"
                    + "{{{code: id=\"Front Template\" lang=\"html\" linenumbers=\"False\"\n"
                    + "{{#Article}}{{Article}} {{/Article}}{{Word}}\n"
                    + "}}}\n"
                    + "after";
            List<String> saved = new ArrayList<>();
            String result = WikitextTextConverter.preprocessCodeBlocks(input, saved);

            assertThat(saved).hasSize(1);
            assertThat(result).contains(WikitextTextConverter.codeBlockPlaceholder(0));
            assertThat(result).doesNotContain("{{{code:");
            assertThat(result).doesNotContain("{{Word}}");
            assertThat(saved.get(0))
                    .startsWith("\n```html\n")
                    .contains("{{#Article}}{{Article}} {{/Article}}{{Word}}")
                    .endsWith("```\n");
        }

        @Test
        public void sourceViewBlockWithoutLangProducesPlainFence() {
            String input = "{{{code: id=\"X\"\ncontent\n}}}";
            List<String> saved = new ArrayList<>();
            WikitextTextConverter.preprocessCodeBlocks(input, saved);

            assertThat(saved).hasSize(1);
            assertThat(saved.get(0)).startsWith("\n```\n").contains("content").endsWith("```\n");
        }

        @Test
        public void tripleQuoteBlockIsReplacedByPlaceholderAndFenced() {
            String input = "before\n'''\nraw {{Word}} text\n'''\nafter";
            List<String> saved = new ArrayList<>();
            String result = WikitextTextConverter.preprocessCodeBlocks(input, saved);

            assertThat(saved).hasSize(1);
            assertThat(result).contains(WikitextTextConverter.codeBlockPlaceholder(0));
            assertThat(result).doesNotContain("{{Word}}");
            assertThat(saved.get(0)).startsWith("\n```\n").contains("raw {{Word}} text").endsWith("```\n");
        }

        @Test
        public void twoSourceViewBlocksProduceTwoPlaceholders() {
            String input = "{{{code: lang=\"html\"\nfirst\n}}}\n\n{{{code: lang=\"js\"\nsecond\n}}}";
            List<String> saved = new ArrayList<>();
            String result = WikitextTextConverter.preprocessCodeBlocks(input, saved);

            assertThat(saved).hasSize(2);
            assertThat(result).contains(WikitextTextConverter.codeBlockPlaceholder(0));
            assertThat(result).contains(WikitextTextConverter.codeBlockPlaceholder(1));
            assertThat(saved.get(0)).contains("```html").contains("first");
            assertThat(saved.get(1)).contains("```js").contains("second");
        }

        @Test
        public void inputWithoutCodeBlocksIsUnchanged() {
            String input = "just some {{Word}} text\nwith multiple lines";
            List<String> saved = new ArrayList<>();
            String result = WikitextTextConverter.preprocessCodeBlocks(input, saved);

            assertThat(saved).isEmpty();
            assertThat(result).isEqualTo(input);
        }

        @Test
        public void tripleQuoteInsideSourceViewBlockIsNotExtractedSeparately() {
            String input = "{{{code: lang=\"md\"\n'''\ninner\n'''\n}}}";
            List<String> saved = new ArrayList<>();
            WikitextTextConverter.preprocessCodeBlocks(input, saved);

            assertThat(saved).hasSize(1);
            assertThat(saved.get(0)).contains("'''\ninner\n'''");
        }
    }

}
