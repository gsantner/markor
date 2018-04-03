/*
 * Copyright (c) 2017-2018 Gregor Santner
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

public class MarkdownHighlighterPatternOrderedListTest {

    private Pattern pattern;

    @Before
    public void before() {
        pattern = MarkdownHighlighterPattern.LIST_ORDERED.pattern;
    }

    @Test
    public void numberItem() {
        Matcher m = pattern.matcher("1. Item");
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("1.");
    }


}
