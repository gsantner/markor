/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.markdown;

import net.gsantner.markor.format.highlighter.markdown.MarkdownHighlighterPattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class MarkdownHighlighterPatternTest {


    @Parameterized.Parameters(name = "{index}: {0} should find text {1} {2} times")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MarkdownHighlighterPattern.HEADER, " # Hi", 0},
                {MarkdownHighlighterPattern.HEADER, "# Hi\n", 1},
                {MarkdownHighlighterPattern.HEADER, "#Hi\n", 0},
                {MarkdownHighlighterPattern.HEADER, "## Hi\n", 1},
                {MarkdownHighlighterPattern.HEADER, "####### Hi\n", 0},
                {MarkdownHighlighterPattern.HEADER, "# Hi\n#Hi again\n", 1},
                {MarkdownHighlighterPattern.HEADER, "# Hi\nWhatever\n# Hi again\n", 2},
                {MarkdownHighlighterPattern.HEADER, "# Hi\n======", 1},
                {MarkdownHighlighterPattern.HEADER, "# Hi\n--------", 1},
                {MarkdownHighlighterPattern.LINK, "[sometext](some://url.com)", 1},
                {MarkdownHighlighterPattern.LINK, "[some text ]( some://url.com)", 1},
                {MarkdownHighlighterPattern.LINK, "[sometext] some://url.com)", 0},
                {MarkdownHighlighterPattern.LINK, "[sometext]( some://url.com )", 1},
                {MarkdownHighlighterPattern.LINK, "[sometext] ( some://url.com )", 0},
                {MarkdownHighlighterPattern.STRIKETHROUGH, "~~s~~", 1},
                {MarkdownHighlighterPattern.STRIKETHROUGH, "~~ s 4 ~~df~~", 1},
                {MarkdownHighlighterPattern.STRIKETHROUGH, "~~ s 4 ~~df ~~", 0},
                {MarkdownHighlighterPattern.STRIKETHROUGH, "~~ s 4 ~~d\n\nf ~~", 0},
                {MarkdownHighlighterPattern.STRIKETHROUGH, "~~ s 4 ~\n~d\n\nf ~~", 0},
                {MarkdownHighlighterPattern.STRIKETHROUGH, "~ s 4 ~~df ~\n ff ~~", 0},
                {MarkdownHighlighterPattern.STRIKETHROUGH, "~~s~", 0},
                {MarkdownHighlighterPattern.STRIKETHROUGH, "~s~~", 0},
                {MarkdownHighlighterPattern.MONOSPACED, "`s`", 1},
                {MarkdownHighlighterPattern.MONOSPACED, "``s`", 1},
                {MarkdownHighlighterPattern.MONOSPACED, "`s``", 1},
                {MarkdownHighlighterPattern.MONOSPACED, "`s`", 1},
                {MarkdownHighlighterPattern.MONOSPACED, "`s\n\n`", 0},
                {MarkdownHighlighterPattern.QUOTATION, "> asdfasdfas\n> sadfasdfasdf", 2},
                {MarkdownHighlighterPattern.QUOTATION, "> asdfa > sdfas\n", 1},
                {MarkdownHighlighterPattern.QUOTATION, ">> sdfas", 1},
                {MarkdownHighlighterPattern.QUOTATION, "\n> sdfas", 1},
                {MarkdownHighlighterPattern.LIST, "* asdfasdfas\n* sadfasdfasdf", 2},
                {MarkdownHighlighterPattern.LIST, "* asdfa > sdfas\n", 1},
                {MarkdownHighlighterPattern.LIST, "\n* sdfas", 1},
                {MarkdownHighlighterPattern.ORDEREDLIST, "1. asdfasdfas\n2. sadfasdfasdf", 2},
                {MarkdownHighlighterPattern.ORDEREDLIST, "1. asdfa 2. sdfas\n", 1},
                {MarkdownHighlighterPattern.ORDEREDLIST, "\n99. sdfas", 1},
                {MarkdownHighlighterPattern.LIST, "- [ ] item 1", 1},
                {MarkdownHighlighterPattern.LIST, "- [x] item 2", 1},
                {MarkdownHighlighterPattern.BOLD, "**s**", 1},
                {MarkdownHighlighterPattern.BOLD, "****s**", 1},
                {MarkdownHighlighterPattern.BOLD, "**s****", 1},
                {MarkdownHighlighterPattern.BOLD, "**s**", 1},
                {MarkdownHighlighterPattern.BOLD, "**s\n\n**", 0},
                {MarkdownHighlighterPattern.BOLD, "__s__", 1},
                {MarkdownHighlighterPattern.BOLD, "____s__", 1},
                {MarkdownHighlighterPattern.BOLD, "__s____", 1},
                {MarkdownHighlighterPattern.BOLD, "__s\n\n__", 0},
                {MarkdownHighlighterPattern.ITALICS, "*s*", 1},
                {MarkdownHighlighterPattern.ITALICS, "*s**", 1},
                {MarkdownHighlighterPattern.ITALICS, "*s*", 1},
                {MarkdownHighlighterPattern.ITALICS, "*s\n\n*", 0},
                {MarkdownHighlighterPattern.ITALICS, "_s_", 1},
                {MarkdownHighlighterPattern.ITALICS, "_s__", 1},
                {MarkdownHighlighterPattern.ITALICS, "_s_", 1},
                {MarkdownHighlighterPattern.ITALICS, "_s\n\n_", 0},
        });
    }

    private final Pattern pattern;
    private final String string;
    private final int foundCount;

    public MarkdownHighlighterPatternTest(MarkdownHighlighterPattern pattern, String string, int foundCount) {
        this.string = string;
        this.foundCount = foundCount;
        this.pattern = pattern.getPattern();
    }

    @Test
    public void testHighlightPattern() {
        int count = 0;

        for (Matcher m = pattern.matcher(string); m.find(); ) {
            count++;
        }

        assertThat(count).isEqualTo(foundCount);
    }
}