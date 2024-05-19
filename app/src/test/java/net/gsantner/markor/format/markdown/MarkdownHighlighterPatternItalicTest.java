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

public class MarkdownHighlighterPatternItalicTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = MarkdownSyntaxHighlighter.ITALICS;
    }

    @Test
    public void underlineItalic() {
        Matcher m = pattern.matcher("_italic_");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("_italic_");
    }

    @Test
    public void oneLetterUnderlineItalic() {
        Matcher m = pattern.matcher("_i_");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("_i_");
    }

    @Test
    public void underlineItalicInSentence() {
        Matcher m = pattern.matcher("this _sentence_ has italic");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("_sentence_");
    }

    @Test
    public void multipleUnderlineItalicInSentence() {
        Matcher m = pattern.matcher("this _sentence_ has _italic_");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("_sentence_");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("_italic_");
    }

    @Test
    public void starItalic() {
        Matcher m = pattern.matcher("*italic*");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("*italic*");
    }

    @Test
    public void starItalicInSentence() {
        Matcher m = pattern.matcher("this *sentence* has italic");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("*sentence*");
    }

    @Test
    public void multipleStarItalicInSentence() {
        Matcher m = pattern.matcher("this *sentence* has *italic*");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("*sentence*");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("*italic*");
    }

    @Test
    public void italicWithSpace() {
        Matcher m = pattern.matcher("*italic words*");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("*italic words*");
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
        Matcher m = pattern.matcher("***bold & italic***");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("***bold & italic***");
    }

    @Test
    public void boldAndItalicStarWithExtraPrecedingStarShouldMatch() {
        Matcher m = pattern.matcher("****bold & italic***");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("****bold & italic***");
    }

    @Test
    public void boldAndItalicStarWithExtraTrailingStarShouldMatch() {
        Matcher m = pattern.matcher("***bold & italic****");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("***bold & italic****");
    }

    /*@Ignore("Ideally this would pass, but I don't think it's possible with regex. As it is, the regex is pretty complex.")
    @Test
    public void italicStarWithExtraPrecedingStarShouldMatch() {
        Matcher m = pattern.matcher("**italic*");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("**italic*");
    }*/

    @Test
    public void italicStarWithExtraTrailingStarShouldMatch() {
        Matcher m = pattern.matcher("*italic**");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("*italic**");
    }


    @Test
    public void italicInAList() {
        Matcher m = pattern.matcher("* *italic* word");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("*italic*");
    }

    @Test
    public void boldInAWordShouldNotMatch() {
        Matcher m = pattern.matcher("foo_bar_baz");
        assertThat(m.find()).isFalse();
    }

}
