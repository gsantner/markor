/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.editor;

import java.util.regex.Pattern;

enum HighlighterPattern {
    LIST(Pattern.compile("(\\n|^)\\s*(\\*|\\d+\\.|\\+|-)[^\\S\\n]")),
    QUOTATION(Pattern.compile("(\\n|^)>")),
    HEADER(Pattern.compile("(?m)((^#{1,6}[^\\S\\n][^\\n]+)|((\\n|^)[^\\s]+.*?\\n(-+|=+)[^\\S\\n]*$))")),
    LINK(Pattern.compile("\\[([^\\[]+)\\]\\(([^\\)]+)\\)")),
    STRIKETHROUGH(Pattern.compile("\\~\\~(.*?)\\~\\~")),
    MONOSPACED(Pattern.compile("`(.*?)`")),
    BOLD(Pattern.compile("(\\*\\*|__)(.*?)\\1")),
    ITALICS(Pattern.compile("(\\*|_)(.*?)\\1"));

    private Pattern pattern;

    HighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
