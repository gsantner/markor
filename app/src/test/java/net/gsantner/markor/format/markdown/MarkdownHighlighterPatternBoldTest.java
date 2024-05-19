/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownHighlighterPatternBoldTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = MarkdownSyntaxHighlighter.BOLD;
    }

    @Test
    public void underlineBold() {
        Matcher m = pattern.matcher("__bold__");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("__bold__");
    }

    @Test
    public void oneLetterUnderlineBold() {
        Matcher m = pattern.matcher("__b__");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("__b__");
    }

    @Test
    public void underlineBoldInSentence() {
        Matcher m = pattern.matcher("this __sentence__ has bold");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("__sentence__");
    }

    @Test
    public void multipleUnderlineBoldInSentence() {
        Matcher m = pattern.matcher("this __sentence__ has __bold__");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("__sentence__");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("__bold__");
    }

    @Test
    public void starBold() {
        Matcher m = pattern.matcher("**bold**");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("**bold**");
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
    public void mixedUnderlineAndStarShouldNotMatch() {
        Matcher m = pattern.matcher("__bold**");
        assertThat(m.find()).isFalse();
    }

    @Test
    public void mixedStarAndUnderlineShouldNotMatch2() {
        Matcher m = pattern.matcher("**bold__");
        assertThat(m.find()).isFalse();
    }

    @Test
    public void mixedStarAndUnderlineOnSameSideShouldNotMatch() {
        Matcher m = pattern.matcher("_*bold*_");
        assertThat(m.find()).isFalse();
    }

    @Test
    public void italicUnderlineShouldNotMatch() {
        Matcher m = pattern.matcher("_italic_");
        assertThat(m.find()).isFalse();
    }

    @Test
    public void italicStarShouldNotMatch() {
        Matcher m = pattern.matcher("*italic*");
        assertThat(m.find()).isFalse();
    }

    @Test
    public void italicsStarWithTwoStartCharactersShouldNotMatch() {
        Matcher m = pattern.matcher("**italic*");
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
