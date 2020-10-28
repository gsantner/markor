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

public class ZimWikiHighlighterHeadingTest {
    private Pattern pattern;

    @Before
    public void before() {
        pattern = ZimWikiHighlighterPattern.HEADING.pattern;
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
    public void invalidHeading() {
        String invalidHeading = "= this is not a valid heading =";
        Matcher matcher = pattern.matcher(invalidHeading);
        assertThat(matcher.find()).isFalse();
    }

    private void findAndAssertEqualHeading(String expectedHeading) {
        Matcher matcher = pattern.matcher(expectedHeading);
        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group()).isEqualTo(expectedHeading);
    }
}
