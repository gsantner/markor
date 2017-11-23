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

public class MarkdownHighlighterPatternListTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = MarkdownHighlighterPattern.LIST.getPattern();
    }

    @Test
    public void dashItem() {
        Matcher m = pattern.matcher("- Item");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("-");
    }

    @Test
    public void starItem() {
        Matcher m = pattern.matcher("* Item");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("*");
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
    public void uncheckedTaskItem() {
        Matcher m = pattern.matcher("- [ ] Unchecked");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("- [ ]");
    }

    @Test
    public void lowercaseCheckedTaskItem() {
        Matcher m = pattern.matcher("- [x] Checked");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("- [x]");
    }

    @Test
    public void uppercaseCheckedTaskItem() {
        Matcher m = pattern.matcher("- [X] Checked");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("- [X]");
    }

    @Test
    public void unknownCheckedTaskItem() {
        Matcher m = pattern.matcher("- [p] Checked");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("-");
    }

    @Test
    public void headerShouldNotMatch() {
        Matcher m = pattern.matcher("--");
        assertThat(m.find()).isFalse();
    }

    @Test
    public void headerWithCharachersShouldNotMatch() {
        Matcher m = pattern.matcher("-------- header ---------");
        assertThat(m.find()).isFalse();
    }

    @Test
    public void boldShouldNotMatch() {
        Matcher m = pattern.matcher("**bold**");
        assertThat(m.find()).isFalse();
    }

}
