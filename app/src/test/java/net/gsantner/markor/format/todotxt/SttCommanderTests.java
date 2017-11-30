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

import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.format.todotxt.extension.SttTaskWithParserInfo;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SttCommanderTests {
    private SttCommander sttcmd = SttCommander.get();

    private String DEMO_LINE_1 = "2017-11-29 create some fancy unit tests @foss";

    @Test()
    public void checkAddProject() {
        SttTaskWithParserInfo task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", task.getTaskLine().length());
        assertThat(task.getTaskLine()).isEqualTo(DEMO_LINE_1 + " @app");

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", task.getTaskLine().length() + 100);
        assertThat(task.getTaskLine()).isEqualTo(DEMO_LINE_1 + " @app");

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", -1);
        assertThat(task.getTaskLine()).isEqualTo("@app " + DEMO_LINE_1);

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", 0);
        assertThat(task.getTaskLine()).isEqualTo("@app 2017" + DEMO_LINE_1.substring(4));

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", 0);
        assertThat(task.getTaskLine()).isEqualTo("@app 2017" + DEMO_LINE_1.substring(4));

        task = task(DEMO_LINE_1);
        sttcmd.insertProject(task, "app", 1);
        assertThat(task.getTaskLine()).isEqualTo("2 @app 017" + DEMO_LINE_1.substring(4));

        assertThat(task.getProjects().contains("app")).isEqualTo(true);
    }

    @Test()
    public void checkAddContext() {
        SttTaskWithParserInfo task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", task.getTaskLine().length());
        assertThat(task.getTaskLine()).isEqualTo(DEMO_LINE_1 + " +app");

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", task.getTaskLine().length() + 100);
        assertThat(task.getTaskLine()).isEqualTo(DEMO_LINE_1 + " +app");

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", -1);
        assertThat(task.getTaskLine()).isEqualTo("+app " + DEMO_LINE_1);

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", 0);
        assertThat(task.getTaskLine()).isEqualTo("+app 2017" + DEMO_LINE_1.substring(4));

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", 0);
        assertThat(task.getTaskLine()).isEqualTo("+app 2017" + DEMO_LINE_1.substring(4));

        task = task(DEMO_LINE_1);
        sttcmd.insertContext(task, "app", 1);
        assertThat(task.getTaskLine()).isEqualTo("2 +app 017" + DEMO_LINE_1.substring(4));

        assertThat(task.getContexts().contains("app")).isEqualTo(true);
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

    private SttTaskWithParserInfo task(String taskLine) {
        return sttcmd.parseTask(taskLine);
    }
}
