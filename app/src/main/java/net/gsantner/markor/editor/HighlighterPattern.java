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
    LIST(Pattern.compile("(\\n|^)\\s*(\\*|\\d+\\.|\\+|-)( \\[[ xX]\\])?")),
    QUOTATION(Pattern.compile("(\\n|^)>")),
    HEADER(Pattern.compile("(?m)((^#{1,6}[^\\S\\n][^\\n]+)|((\\n|^)[^\\s]+.*?\\n(-{2,}|={2,})[^\\S\\n]*$))")),
    LINK(Pattern.compile("\\[([^\\[]+)\\]\\(([^\\)]+)\\)")),
    STRIKETHROUGH(Pattern.compile("~{2}(.*?)~{2}")),
    MONOSPACED(Pattern.compile("(?m)(`(.*?)`)|(^[^\\S\\n]{4}.*$)")),
    BOLD(Pattern.compile("(\\*\\*|__)[^\\s](.*?)\\1")),
    ITALICS(Pattern.compile("(\\*|_)[^\\s](.*?)\\1"));

    private Pattern pattern;

    HighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
