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

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiFileContentsGeneratorTest {
    @Test
    public void createsCorrectContentsForNewZimWikiFiles() {
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
        String actual = ZimWikiFileContentsGenerator.createZimWikiHeaderAndTitleContents("My_new_wiki_page", date, "Created");
        assertThat(actual).isEqualTo(expected);
    }
}
