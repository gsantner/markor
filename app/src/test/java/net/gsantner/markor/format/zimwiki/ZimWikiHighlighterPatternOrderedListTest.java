/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import net.gsantner.markor.format.markdown.MarkdownHighlighterPattern;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiHighlighterPatternOrderedListTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = ZimWikiHighlighterPattern.LIST_ORDERED.pattern;
    }

    @Test
    public void numberItem() {
        Matcher m = pattern.matcher("1. Item");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("1. ");
    }

    @Test
    public void letterItem() {
        Matcher m = pattern.matcher("b. Item");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("b. ");
    }

    @Test
    public void capitalItem() {
        Matcher m = pattern.matcher("C. Item");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("C. ");
    }
}
