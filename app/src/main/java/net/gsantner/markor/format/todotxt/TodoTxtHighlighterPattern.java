/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;


import android.util.Patterns;

import net.gsantner.opoc.format.todotxt.SttTask;

import java.util.regex.Pattern;

// See for format description: https://github.com/todotxt/todo.txt/blob/master/README.md
public enum TodoTxtHighlighterPattern {

    CONTEXT(SttTask.PATTERN_CONTEXTS),
    PROJECT(SttTask.PATTERN_PROJECTS), // Project = Category
    DONE(SttTask.PATTERN_DONE),
    DATE(SttTask.PATTERN_DATE),
    COMPLETION_DATE(SttTask.PATTERN_COMPLETION_DATE),
    CREATION_DATE(SttTask.PATTERN_CREATION_DATE),
    PATTERN_KEY_VALUE(SttTask.PATTERN_KEY_VALUE_PAIRS__TAG_ONLY),
    PRIORITY_ANY(SttTask.PATTERN_PRIORITY_ANY),
    PRIORITY_A(SttTask.PATTERN_PRIORITY_A),
    PRIORITY_B(SttTask.PATTERN_PRIORITY_B),
    PRIORITY_C(SttTask.PATTERN_PRIORITY_C),
    PRIORITY_D(SttTask.PATTERN_PRIORITY_D),
    PRIORITY_E(SttTask.PATTERN_PRIORITY_E),
    PRIORITY_F(SttTask.PATTERN_PRIORITY_F),
    DUE_DATE(SttTask.PATTERN_DUE_DATE),

    LINK(Patterns.WEB_URL),
    NEWLINE_CHARACTER(Pattern.compile("(\\n|^)")),
    LINESTART(Pattern.compile("(?m)^.")),
    LINE_OF_TEXT(Pattern.compile("(?m)(.*)?"));

    private Pattern pattern;

    TodoTxtHighlighterPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }
}