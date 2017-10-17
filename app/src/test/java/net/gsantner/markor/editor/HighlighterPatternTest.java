/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.editor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class HighlighterPatternTest {


    @Parameterized.Parameters(name = "{index}: {0} should find text {1} {2} times")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {HighlighterPattern.HEADER, " # Hi", 0},
                {HighlighterPattern.HEADER, "# Hi\n", 1},
                {HighlighterPattern.HEADER, "#Hi\n", 0},
                {HighlighterPattern.HEADER, "## Hi\n", 1},
                {HighlighterPattern.HEADER, "####### Hi\n", 0},
                {HighlighterPattern.HEADER, "# Hi\n#Hi again\n", 1},
                {HighlighterPattern.HEADER, "# Hi\nWhatever\n# Hi again\n", 2},
                {HighlighterPattern.HEADER, "# Hi\n======", 1},
                {HighlighterPattern.HEADER, "# Hi\n--------", 1},
                {HighlighterPattern.LINK, "[sometext](some://url.com)", 1},
                {HighlighterPattern.LINK, "[some text ]( some://url.com)", 1},
                {HighlighterPattern.LINK, "[sometext] some://url.com)", 0},
                {HighlighterPattern.LINK, "[sometext]( some://url.com )", 1},
                {HighlighterPattern.LINK, "[sometext] ( some://url.com )", 0},
                {HighlighterPattern.STRIKETHROUGH, "~~s~~", 1},
                {HighlighterPattern.STRIKETHROUGH, "~~ s 4 ~~df~~", 1},
                {HighlighterPattern.STRIKETHROUGH, "~~ s 4 ~~df ~~", 0},
                {HighlighterPattern.STRIKETHROUGH, "~~ s 4 ~~d\n\nf ~~", 0},
                {HighlighterPattern.STRIKETHROUGH, "~~ s 4 ~\n~d\n\nf ~~", 0},
                {HighlighterPattern.STRIKETHROUGH, "~ s 4 ~~df ~\n ff ~~", 0},
                {HighlighterPattern.STRIKETHROUGH, "~~s~", 0},
                {HighlighterPattern.STRIKETHROUGH, "~s~~", 0},
                {HighlighterPattern.MONOSPACED, "`s`", 1},
                {HighlighterPattern.MONOSPACED, "``s`", 1},
                {HighlighterPattern.MONOSPACED, "`s``", 1},
                {HighlighterPattern.MONOSPACED, "`s`", 1},
                {HighlighterPattern.MONOSPACED, "`s\n\n`", 0},
                {HighlighterPattern.QUOTATION, "> asdfasdfas\n> sadfasdfasdf", 2},
                {HighlighterPattern.QUOTATION, "> asdfa > sdfas\n", 1},
                {HighlighterPattern.QUOTATION, ">> sdfas", 1},
                {HighlighterPattern.QUOTATION, "\n> sdfas", 1},
                {HighlighterPattern.LIST, "* asdfasdfas\n* sadfasdfasdf", 2},
                {HighlighterPattern.LIST, "* asdfa > sdfas\n", 1},
                {HighlighterPattern.LIST, "\n* sdfas", 1},
                {HighlighterPattern.LIST, "1. asdfasdfas\n2. sadfasdfasdf", 2},
                {HighlighterPattern.LIST, "1. asdfa 2. sdfas\n", 1},
                {HighlighterPattern.LIST, "\n99. sdfas", 1},
                {HighlighterPattern.LIST, "- [ ] item 1", 1},
                {HighlighterPattern.LIST, "- [x] item 2", 1},
                {HighlighterPattern.BOLD, "**s**", 1},
                {HighlighterPattern.BOLD, "****s**", 1},
                {HighlighterPattern.BOLD, "**s****", 1},
                {HighlighterPattern.BOLD, "**s**", 1},
                {HighlighterPattern.BOLD, "**s\n\n**", 0},
                {HighlighterPattern.BOLD, "__s__", 1},
                {HighlighterPattern.BOLD, "____s__", 1},
                {HighlighterPattern.BOLD, "__s____", 1},
                {HighlighterPattern.BOLD, "__s\n\n__", 0},
                {HighlighterPattern.ITALICS, "*s*", 1},
                {HighlighterPattern.ITALICS, "*s**", 1},
                {HighlighterPattern.ITALICS, "*s*", 1},
                {HighlighterPattern.ITALICS, "*s\n\n*", 0},
                {HighlighterPattern.ITALICS, "_s_", 1},
                {HighlighterPattern.ITALICS, "_s__", 1},
                {HighlighterPattern.ITALICS, "_s_", 1},
                {HighlighterPattern.ITALICS, "_s\n\n_", 0},
        });
    }

    private final Pattern pattern;
    private final String string;
    private final int foundCount;

    public HighlighterPatternTest(HighlighterPattern pattern, String string, int foundCount) {
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