/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.net> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.markor.format.todotxt;

import net.gsantner.markor.format.converter.TodoTxtTextConverter;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class TodoTxtFileRecognitionTests {

    @Test
    public void checkTodoTxtFileRecognition() {
        Pattern p = TodoTxtTextConverter.TODOTXT_FILE_PATTERN;
        assertThat(ispm(p, "todo.txt")).isEqualTo(true);
        assertThat(ispm(p, "ToDO.txt")).isEqualTo(true);
        assertThat(ispm(p, "todo.archive.txt")).isEqualTo(true);
        assertThat(ispm(p, "todo-archive.txt")).isEqualTo(true);
        assertThat(ispm(p, "todo-history.txt")).isEqualTo(true);
        assertThat(ispm(p, "2017-todo.txt")).isEqualTo(true);
        assertThat(ispm(p, "2015.todo.txt")).isEqualTo(true);
        assertThat(ispm(p, "todo.md")).isEqualTo(true);
        assertThat(ispm(p, "TODO.md")).isEqualTo(true);
        assertThat(ispm(p, "welltodo.txt")).isEqualTo(false);
        assertThat(ispm(p, "asdftodo.md")).isEqualTo(false);
    }

    // is patter matching
    private boolean ispm(Pattern pattern, String str) {
        return pattern.matcher(str).matches();
    }
}
