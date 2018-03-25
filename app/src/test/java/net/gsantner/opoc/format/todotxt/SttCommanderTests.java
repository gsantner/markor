/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.format.todotxt;

import net.gsantner.opoc.format.todotxt.extension.SttTaskWithParserInfo;

import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SttCommanderTests {
    private SttCommander sttcmd = SttCommander.get();

    private String TODAY = SttCommander.DATEF_YYYY_MM_DD.format(new Date());
    private String DEMO_LINE_1 = "2017-11-29 create some fancy unit tests @foss";
    private String DEMO_LINE_MULTIPLE = DEMO_LINE_1 + "\n" + DEMO_LINE_1 + "\n" + DEMO_LINE_1;

    private SttTaskWithParserInfo task(String taskLine) {
        return sttcmd.parseTask(taskLine);
    }

    @Test()
    public void checkAddProject() {
        SttTaskWithParserInfo task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", task.getTaskLine().length());
        assertThat(task.getTaskLine()).isEqualTo(DEMO_LINE_1 + " +app");

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", task.getTaskLine().length() + 100);
        assertThat(task.getTaskLine()).isEqualTo(DEMO_LINE_1 + " +app");

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", -1);
        assertThat(task.getTaskLine()).isEqualTo("+app " + DEMO_LINE_1);

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", 0);
        assertThat(task.getTaskLine()).isEqualTo("+app 2017" + DEMO_LINE_1.substring(4));

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", 0);
        assertThat(task.getTaskLine()).isEqualTo("+app 2017" + DEMO_LINE_1.substring(4));

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", 1);
        assertThat(task.getTaskLine()).isEqualTo("2 +app 017" + DEMO_LINE_1.substring(4));

        assertThat(task.getProjects().contains("app")).isEqualTo(true);
    }

    @Test()
    public void checkAddContext() {
        SttTaskWithParserInfo task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", task.getTaskLine().length());
        assertThat(task.getTaskLine()).isEqualTo(DEMO_LINE_1 + " @app");

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", task.getTaskLine().length() + 100);
        assertThat(task.getTaskLine()).isEqualTo(DEMO_LINE_1 + " @app");

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", -1);
        assertThat(task.getTaskLine()).isEqualTo("@app " + DEMO_LINE_1);

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", 0);
        assertThat(task.getTaskLine()).isEqualTo("@app 2017" + DEMO_LINE_1.substring(4));

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", 0);
        assertThat(task.getTaskLine()).isEqualTo("@app 2017" + DEMO_LINE_1.substring(4));

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", 1);
        assertThat(task.getTaskLine()).isEqualTo("2 @app 017" + DEMO_LINE_1.substring(4));

        assertThat(task.getContexts().contains("app")).isEqualTo(true);
    }

    @Test()
    public void checkDates() {
        SttTaskWithParserInfo task = task("(A) " + DEMO_LINE_1);
        task.setDone(true);
        sttcmd.regenerateTaskLine(task);
        assertThat(task.getTaskLine()).isEqualTo("x " + TODAY + " " + DEMO_LINE_1);
    }

    @Test
    public void taskLineRegenTest() {
        SttTaskWithParserInfo task = task(DEMO_LINE_1);
        String prev = task.getTaskLine();

        // Check consistency
        sttcmd.regenerateTaskLine(task);
        assertThat(task.getTaskLine()).isEqualTo(prev);

        // Check correct done - with existing creation time. using not done-applying method
        task = task(DEMO_LINE_1);
        task.setDone(true);
        sttcmd.regenerateTaskLine(task);
        assertThat(task.getTaskLine()).isEqualTo("x " + SttCommander.getToday() + " " + DEMO_LINE_1);
    }

    @Test
    public void replaceLineTest() {
        assertThat(SttCommander.replaceTillEndOfLineFromIndex(0, "hello world", "hello")).isEqualTo("hello");
        assertThat(SttCommander.replaceTillEndOfLineFromIndex(0, "aa\nbb", "cc")).isEqualTo("cc\nbb");
        assertThat(SttCommander.replaceTillEndOfLineFromIndex(3, "aa\nbb\ncc", "55")).isEqualTo("aa\n55\ncc");
        assertThat(SttCommander.replaceTillEndOfLineFromIndex("aa\nbb\ncc".lastIndexOf("\n") + 1, "aa\nbb\ncc", "55")).isEqualTo("aa\nbb\n55");
    }

    @Test
    public void regenerateTest() {
        String text = DEMO_LINE_MULTIPLE;
        SttTaskWithParserInfo task = sttcmd.parseTask(text, text.indexOf('\n') + 1);
        sttcmd.regenerateTaskLine(task);
        assertThat(sttcmd.regenerateText(text, task)).isEqualTo(DEMO_LINE_MULTIPLE);

        task = sttcmd.parseTask(text, text.indexOf('\n') + 1);
        sttcmd.insertContext(task, "hello", 99999);
        sttcmd.regenerateTaskLine(task);
        assertThat(sttcmd.regenerateText(text, task)).isEqualTo(DEMO_LINE_1 + "\n" + DEMO_LINE_1 + " @hello\n" + DEMO_LINE_1);
    }
}
