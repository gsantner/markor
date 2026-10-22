package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TodoTxtFilterTests {

    @Test
    public void dueCategoryCountsExcludeCompletedTasks() {
        final List<TodoTxtTask> tasks = new ArrayList<>();
        for (final String suffix : dueSuffixes()) {
            tasks.add(new TodoTxtTask("Pending task" + suffix));
            tasks.add(new TodoTxtTask("x Completed task" + suffix));
        }

        assertDueCountsMatchQueries(tasks, 1);
    }

    @Test
    public void dueCategoryCountsAreZeroForCompletedTasksOnly() {
        final List<TodoTxtTask> tasks = new ArrayList<>();
        for (final String suffix : dueSuffixes()) {
            tasks.add(new TodoTxtTask("x Completed task" + suffix));
        }

        assertDueCountsMatchQueries(tasks, 0);
    }

    @Test
    public void dueCategoryCountsAreZeroForEmptyTaskList() {
        assertDueCountsMatchQueries(Collections.emptyList(), 0);
    }

    private String[] dueSuffixes() {
        return new String[]{" due:9999-01-01", " due:" + TodoTxtTask.getToday(), " due:2000-01-01", ""};
    }

    private void assertDueCountsMatchQueries(final List<TodoTxtTask> tasks, final int expectedCount) {
        final List<TodoTxtFilter.SttFilterKey> keys = TodoTxtFilter.getDueKeys(tasks, "Future", "Today", "Overdue");
        assertThat(keys).extracting(key -> key.key).containsExactly("Future", "Today", "Overdue", TodoTxtFilter.STRING_NONE);
        assertThat(keys).extracting(key -> key.query).containsExactly(TodoTxtFilter.QUERY_DUE_FUTURE, TodoTxtFilter.QUERY_DUE_TODAY, TodoTxtFilter.QUERY_DUE_OVERDUE, null);

        for (final TodoTxtFilter.SttFilterKey key : keys) {
            final String query = TodoTxtFilter.makeQuery(Collections.singletonList(key.query), true, TodoTxtFilter.TYPE.DUE);
            int matchingCount = 0;
            for (final TodoTxtTask task : tasks) {
                if (TodoTxtFilter.isMatchQuery(task, query)) {
                    matchingCount++;
                }
            }
            assertThat(matchingCount).as("Tasks matching %s", query).isEqualTo(expectedCount);
            assertThat(key.count).as("Category count for %s", query).isEqualTo(matchingCount);
        }
    }
}
