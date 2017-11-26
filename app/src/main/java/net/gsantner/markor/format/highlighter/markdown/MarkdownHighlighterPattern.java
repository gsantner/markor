/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.markdown;

import java.util.regex.Pattern;

public enum MarkdownHighlighterPattern {
    LIST(Pattern.compile("(\\n|^)\\s{0,3}(\\*|\\+|-)( \\[[ |x|X]\\])?(?= )")),
    ORDEREDLIST(Pattern.compile("(?m)^([0-9]+)(\\.)")),
    QUOTATION(Pattern.compile("(\\n|^)>")),
    HEADER(Pattern.compile("(?m)((^#{1,6}[^\\S\\n][^\\n]+)|((\\n|^)[^\\s]+.*?\\n(-{2,}|={2,})[^\\S\\n]*$))")),
    LINK(Pattern.compile("\\[([^\\[]+)\\]\\(([^\\)]+)\\)")),
    STRIKETHROUGH(Pattern.compile("~{2}(.*?)\\S~{2}")),
    MONOSPACED(Pattern.compile("(?m)(`(.*?)`)|(^[^\\S\\n]{4}.*$)")),
    BOLD(Pattern.compile("(?<=(\\n|^|\\s))((\\*|_){2,3})(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    ITALICS(Pattern.compile("(?<=(\\n|^|\\s))(\\*|_)(?=((?!\\2)|\\2{2,}))(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    DOUBLESPACE_ENDING(Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n"));

    private Pattern _pattern;

    MarkdownHighlighterPattern(Pattern pattern) {
        _pattern = pattern;
    }

    public Pattern getPattern() {
        return _pattern;
    }
}
