/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.todotxt;


import android.util.Patterns;

import java.util.regex.Pattern;

// See for format description: https://github.com/todotxt/todo.txt/blob/master/README.md
public enum TodoTxtHighlighterPattern {

    LINK(Patterns.WEB_URL),
    NEWLINE_CHARACTER(Pattern.compile("(\\n|^)")),
    CONTEXT(Pattern.compile("(\\B\\+\\w+)")),
    CATEGORY(Pattern.compile("(\\B\\@\\w+)")), // Category = Project
    DONE(Pattern.compile("(?m)^([x|X] ).*$")),
    PRIORITY_A(reuse.prio(1)),
    PRIORITY_B(reuse.prio(2)),
    PRIORITY_C(reuse.prio(3)),
    PRIORITY_D(reuse.prio(4)),
    PRIORITY_E(reuse.prio(5)),
    PRIORITY_F(reuse.prio(6)),
    PRIORITY_ANY(Pattern.compile("(?i)(\\n|^)(?:x )?([(][ABCDEF][)] )")),
    DATE(Pattern.compile("( ?\\d{4}-\\d{2}-\\d{2} )")),
    LINESTART(Pattern.compile("(?m)^.")),
    KEYVALUE(Pattern.compile("(\\w+:)\\w+")),
    LINE_OF_TEXT(Pattern.compile("(?m)(.*)?")),


    // TODO:
    LIST(Pattern.compile("(\\n|^)\\s{0,3}(\\*|\\+|-)( \\[[ |x|X]\\])?(?= )")),
    ORDEREDLIST(Pattern.compile("(?m)^([0-9]+)(\\.)")),
    QUOTATION(Pattern.compile("(\\n|^)>")),
    STRIKETHROUGH(Pattern.compile("~{2}(.*?)\\S~{2}")),
    MONOSPACED(Pattern.compile("(?m)(`(.*?)`)|(^[^\\S\\n]{4}.*$)")),
    BOLD(Pattern.compile("(?<=(\\n|^|\\s))((\\*|_){2,3})(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    ITALICS(Pattern.compile("(?<=(\\n|^|\\s))(\\*|_)(?=((?!\\2)|\\2{2,}))(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s))")),
    DOUBLESPACE(Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n"));

    private Pattern pattern;

    TodoTxtHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }
}

class reuse {
    // 1=a, 6=f
    static Pattern prio(int prio) {
        String priority = Character.toString((char) (((int) 'A') - 1 + prio));
        return Pattern.compile("(?mi)^(?:x )?([(]" + priority + "[)] )");
    }
}
