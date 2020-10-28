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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiHighlighterZimHeaderTest {

    private Pattern pattern;

    @Test
    public void zimHeaderAtBeginningOfTheFileShouldMatch() {
        pattern = ZimWikiHighlighterPattern.ZIMHEADER.pattern;
        Matcher matcher = pattern.matcher("Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00\nOther content...");
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo("Content-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00");
    }

    @Test
    public void zimHeaderNotAtBeginningOfTheFileShouldNotMatch() {
        pattern = ZimWikiHighlighterPattern.ZIMHEADER.pattern;
        Matcher matcher = pattern.matcher("Blabla\nContent-Type: text/x-zim-wiki\nWiki-Format: zim 0.4\nCreation-Date: 2019-03-31T14:48:06+02:00");
        assertThat(matcher.find()).isFalse();
    }
}
