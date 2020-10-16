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

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiHighlighterPatternItalicTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = ZimWikiHighlighterPattern.EMPHASIS.pattern;
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
    public void italicStarWithExtraTrailingSlashShouldMatch() {
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
