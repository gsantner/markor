/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
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

}
