/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.txt2tags;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class Txt2tagsFileTests {

    public static class GeneratorTests {
        @SuppressWarnings("SpellCheckingInspection")
        @Test
        public void createsCorrectContentsForNewTxt2tagsFiles() {
            Locale.setDefault(Locale.ENGLISH);
            Calendar calendar = Calendar.getInstance();
            calendar.set(2020, Calendar.DECEMBER, 24, 18, 0, 30);
            calendar.setTimeZone(TimeZone.getTimeZone("GMT+01:00"));
            Date date = calendar.getTime();
            String expected = " \n" +
                    " \n" +
                    " \n" +
                    "\n" +
                    "== My new wiki page ==\n" +
                    "\n";
            // TODO: replace logic should not be necessary - find out why time and time zone is not created correctly in the test
            String actual = Txt2tagsTextActions.createTxt2tagsHeaderAndTitleContents("My_new_wiki_page", date, "Created");
            String actualReplaced = actual.replaceAll("(?m)T1[78]:00:30.+$", "");
            String expectedReplaced = expected.replaceAll("(?m)T1[78]:00:30.+$", "");
            assertThat(actualReplaced).isEqualTo(expectedReplaced);
        }
    }

    public static class ZimFileHeaderTest {

        private Pattern pattern;

        @Test
        public void zimHeaderAtBeginningOfTheFileShouldMatch() {
            pattern = Txt2tagsHighlighter.Patterns.ZIMHEADER.pattern;
            Matcher matcher = pattern.matcher("Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00\nOther content...");
            assertThat(matcher.find()).isTrue();
            assertThat(matcher.group()).isEqualTo("Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00");
        }

        @Test
        public void zimHeaderNotAtBeginningOfTheFileShouldNotMatch() {
            pattern = Txt2tagsHighlighter.Patterns.ZIMHEADER.pattern;
            Matcher matcher = pattern.matcher("Blabla\nContent-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00");
            assertThat(matcher.find()).isFalse();
        }
    }

}
