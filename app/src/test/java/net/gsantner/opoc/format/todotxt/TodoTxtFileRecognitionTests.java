/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.format.todotxt;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class TodoTxtFileRecognitionTests {

    @Test
    public void checkTodoTxtFileRecognition() {
        Pattern p = SttCommander.TODOTXT_FILE_PATTERN;
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
