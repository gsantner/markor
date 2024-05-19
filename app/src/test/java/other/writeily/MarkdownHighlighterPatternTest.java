/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2024 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownHighlighterPatternTest {

    // {index}: {0} should find text {1} {2} times
    public static List<Object[]> tests = Arrays.asList(new Object[][]{
            {MarkdownSyntaxHighlighter.HEADING, " # Hi", 0},
            {MarkdownSyntaxHighlighter.HEADING, "# Hi\n", 1},
            {MarkdownSyntaxHighlighter.HEADING, "#Hi\n", 0},
            {MarkdownSyntaxHighlighter.HEADING, "## Hi\n", 1},
            {MarkdownSyntaxHighlighter.HEADING, "####### Hi\n", 0},
            {MarkdownSyntaxHighlighter.HEADING, "# Hi\n#Hi again\n", 1},
            {MarkdownSyntaxHighlighter.HEADING, "# Hi\nWhatever\n# Hi again\n", 2},
            {MarkdownSyntaxHighlighter.HEADING, "# Hi\n======", 1},
            {MarkdownSyntaxHighlighter.HEADING, "# Hi\n--------", 1},
            {MarkdownSyntaxHighlighter.LINK, "[sometext](some://url.com)", 1},
            {MarkdownSyntaxHighlighter.LINK, "[some text ]( some://url.com)", 1},
            {MarkdownSyntaxHighlighter.LINK, "[sometext] some://url.com)", 0},
            {MarkdownSyntaxHighlighter.LINK, "[sometext]( some://url.com )", 1},
            {MarkdownSyntaxHighlighter.LINK, "[sometext] ( some://url.com )", 0},
            {MarkdownSyntaxHighlighter.STRIKETHROUGH, "~~s~~", 1},
            {MarkdownSyntaxHighlighter.STRIKETHROUGH, "~~ s 4 ~~df~~", 1},
            {MarkdownSyntaxHighlighter.STRIKETHROUGH, "~~ s 4 ~~df ~~", 0},
            {MarkdownSyntaxHighlighter.STRIKETHROUGH, "~~ s 4 ~~d\n\nf ~~", 0},
            {MarkdownSyntaxHighlighter.STRIKETHROUGH, "~~ s 4 ~\n~d\n\nf ~~", 0},
            {MarkdownSyntaxHighlighter.STRIKETHROUGH, "~ s 4 ~~df ~\n ff ~~", 0},
            {MarkdownSyntaxHighlighter.STRIKETHROUGH, "~~s~", 0},
            {MarkdownSyntaxHighlighter.STRIKETHROUGH, "~s~~", 0},
            {MarkdownSyntaxHighlighter.CODE, "`s`", 1},
            {MarkdownSyntaxHighlighter.CODE, "``s`", 1},
            {MarkdownSyntaxHighlighter.CODE, "`s``", 1},
            {MarkdownSyntaxHighlighter.CODE, "`s`", 1},
            {MarkdownSyntaxHighlighter.CODE, "`s\n\n`", 0},
            {MarkdownSyntaxHighlighter.QUOTATION, "> asdfasdfas\n> sadfasdfasdf", 2},
            {MarkdownSyntaxHighlighter.QUOTATION, "> asdfa > sdfas\n", 1},
            {MarkdownSyntaxHighlighter.QUOTATION, ">> sdfas", 1},
            {MarkdownSyntaxHighlighter.QUOTATION, "\n> sdfas", 1},
            {MarkdownSyntaxHighlighter.LIST_UNORDERED, "* asdfasdfas\n* sadfasdfasdf", 2},
            {MarkdownSyntaxHighlighter.LIST_UNORDERED, "* asdfa > sdfas\n", 1},
            {MarkdownSyntaxHighlighter.LIST_UNORDERED, "\n* sdfas", 1},
            {MarkdownSyntaxHighlighter.LIST_ORDERED, "1. asdfasdfas\n2. sadfasdfasdf", 2},
            {MarkdownSyntaxHighlighter.LIST_ORDERED, "1. asdfa 2. sdfas\n", 1},
            {MarkdownSyntaxHighlighter.LIST_ORDERED, "\n99. sdfas", 1},
            {MarkdownSyntaxHighlighter.LIST_UNORDERED, "- [ ] item 1", 1},
            {MarkdownSyntaxHighlighter.LIST_UNORDERED, "- [x] item 2", 1},
            {MarkdownSyntaxHighlighter.BOLD, "**s**", 1},
            {MarkdownSyntaxHighlighter.BOLD, "****s**", 1},
            {MarkdownSyntaxHighlighter.BOLD, "**s****", 1},
            {MarkdownSyntaxHighlighter.BOLD, "**s**", 1},
            {MarkdownSyntaxHighlighter.BOLD, "**s\n\n**", 0},
            {MarkdownSyntaxHighlighter.BOLD, "__s__", 1},
            {MarkdownSyntaxHighlighter.BOLD, "____s__", 1},
            {MarkdownSyntaxHighlighter.BOLD, "__s____", 1},
            {MarkdownSyntaxHighlighter.BOLD, "__s\n\n__", 0},
            {MarkdownSyntaxHighlighter.ITALICS, "*s*", 1},
            {MarkdownSyntaxHighlighter.ITALICS, "*s**", 1},
            {MarkdownSyntaxHighlighter.ITALICS, "*s*", 1},
            {MarkdownSyntaxHighlighter.ITALICS, "*s\n\n*", 0},
            {MarkdownSyntaxHighlighter.ITALICS, "_s_", 1},
            {MarkdownSyntaxHighlighter.ITALICS, "_s__", 1},
            {MarkdownSyntaxHighlighter.ITALICS, "_s_", 1},
            {MarkdownSyntaxHighlighter.ITALICS, "_s\n\n_", 0},
    });

    public static int countMatcher(final Matcher m) {
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    @Test
    public void testHighlightPattern() {
        for (final Object[] test : tests) {
            assertThat(countMatcher(((Pattern) test[0]).matcher((String) test[1]))).isEqualTo((Integer) test[2]);
        }
    }
}