/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownHighlighterPatternOrderedListTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = MarkdownSyntaxHighlighter.LIST_ORDERED;
    }

    @Test
    public void numberItem() {
        Matcher m = pattern.matcher("1. Item");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("1. ");
    }


}
