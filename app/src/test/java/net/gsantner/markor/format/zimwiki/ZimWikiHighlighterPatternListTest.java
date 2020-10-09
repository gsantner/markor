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

public class ZimWikiHighlighterPatternListTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = ZimWikiHighlighterPattern.LIST_UNORDERED.pattern;
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
        assertThat(m.group()).isEqualTo("\t*");
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
