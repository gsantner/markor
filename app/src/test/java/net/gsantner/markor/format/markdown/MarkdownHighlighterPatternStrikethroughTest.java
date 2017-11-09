/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.markdown;

import net.gsantner.markor.format.highlighter.markdown.MarkdownHighlighterPattern;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkdownHighlighterPatternStrikethroughTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = MarkdownHighlighterPattern.STRIKETHROUGH.getPattern();
    }

    @Test
    public void strikethrough() {
        Matcher m = pattern.matcher("~~strikethrough~~");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("~~strikethrough~~");
    }

    @Test
    public void strikethroughSingleCharacter() {
        Matcher m = pattern.matcher("~~s~~");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("~~s~~");
    }

    @Test
    public void strikethroughWithSpace() {
        Matcher m = pattern.matcher("~~strike through~~");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("~~strike through~~");
    }

    @Test
    public void multipleStrikethrough() {
        Matcher m = pattern.matcher("~~one~~ ~~two~~");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("~~one~~");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("~~two~~");
    }

    @Test
    public void strikethroughWithPrecedingTilde() {
        Matcher m = pattern.matcher("~~~one~~");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("~~~one~~");
    }

    @Test
    public void strikethroughWithTrailingTildeSkipsExtraTildes() {
        Matcher m = pattern.matcher("~~one~~~");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("~~one~~");
    }

    @Test
    public void strikethroughWithSpaceAsLastCharacherShouldNotMatch() {
        Matcher m = pattern.matcher("~~one ~~");
        assertThat(m.find()).isFalse();
    }

}
