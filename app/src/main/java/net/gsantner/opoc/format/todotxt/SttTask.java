/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.format.todotxt;

import android.widget.TextView;

import net.gsantner.opoc.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SttTask {

    //
    // Static memebers
    //

    public static final Pattern TODOTXT_FILE_PATTERN = Pattern.compile("(?i)(^todo[-.]?.*)|(.*[-.]todo\\.((txt)|(text))$)");
    public static final SimpleDateFormat DATEF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    public static final int DATEF_YYYY_MM_DD_LEN = "yyyy-MM-dd".length();
    public static final String PT_DATE = "\\d{4}-\\d{2}-\\d{2}";
    public static final Pattern PATTERN_PROJECTS = Pattern.compile("\\B(?:\\++)(\\S+)");
    public static final Pattern PATTERN_CONTEXTS = Pattern.compile("\\B(?:\\@+)(\\S+)");
    public static final Pattern PATTERN_DONE = Pattern.compile("(?m)(^[Xx]) (.*)$");
    public static final Pattern PATTERN_DATE = Pattern.compile("(?:^|\\s|:)(" + PT_DATE + ")(?:$|\\s)");
    public static final Pattern PATTERN_KEY_VALUE_PAIRS__TAG_ONLY = Pattern.compile("(?i)([a-z]+):([a-z0-9_-]+)");
    public static final Pattern PATTERN_KEY_VALUE_PAIRS = Pattern.compile("(?i)((?:[a-z]+):(?:[a-z0-9_-]+))");
    public static final Pattern PATTERN_DUE_DATE = Pattern.compile("(?:due:)(" + PT_DATE + ")");
    public static final Pattern PATTERN_PRIORITY_ANY = Pattern.compile("(?:^|\\n)\\(([A-Za-z])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_A = Pattern.compile("(?:^|\\n)\\(([Aa])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_B = Pattern.compile("(?:^|\\n)\\(([Bb])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_C = Pattern.compile("(?:^|\\n)\\(([Cc])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_D = Pattern.compile("(?:^|\\n)\\(([Dd])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_E = Pattern.compile("(?:^|\\n)\\(([Ee])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_F = Pattern.compile("(?:^|\\n)\\(([Ff])\\)\\s");
    public static final Pattern PATTERN_COMPLETION_DATE = Pattern.compile("(?:^|\\n)(?:[Xx] )(" + PT_DATE + ")");
    public static final Pattern PATTERN_CREATION_DATE = Pattern.compile("(?:^|\\n)(?:\\([A-Za-z]\\)\\s)?(?:[Xx] " + PT_DATE + " )?(" + PT_DATE + ")");

    public static String getToday() {
        return DATEF_YYYY_MM_DD.format(new Date());
    }

    public static boolean isTodoFile(String filepath) {
        return filepath != null && TODOTXT_FILE_PATTERN.matcher(filepath).matches() && (filepath.endsWith(".txt") || filepath.endsWith(".text"));
    }

    public static SttTask[] getTasks(final TextView view, final int selStart, final int selEnd) {
        final CharSequence text = view.getText();
        final String[] lines = text.subSequence(
                StringUtils.getLineStart(text, selStart),
                StringUtils.getLineEnd(text, selEnd)
        ).toString().split("\n");

        final SttTask[] tasks = new SttTask[lines.length];
        for (int i = 0; i < lines.length; i++) {
            tasks[i] = new SttTask(lines[i]);
        }
        return tasks;
    }

    public static SttTask[] getSelectedTasks(final TextView view) {
        final int[] sel = StringUtils.getSelection(view);
        return getTasks(view, sel[0], sel[1]);
    }

    public static SttTask[] getAllTasks(final TextView view) {
        final int[] sel = StringUtils.getSelection(view);
        return getTasks(view, 0, view.length());
    }

    public static String[] getProjects(final SttTask[] tasks) {
        final Set<String> set = new HashSet<>();
        for (final SttTask task : tasks) {
            final String[] projects = task.getProjects();
            Collections.addAll(set, projects);
        }
        return set.toArray(new String[0]);
    }

    public static String[] getContexts(final SttTask[] tasks) {
        final Set<String> set = new HashSet<>();
        for (final SttTask task : tasks) {
            final String[] projects = task.getContexts();
            Collections.addAll(set, projects);
        }
        return set.toArray(new String[0]);
    }

    public static String tasksToString(final SttTask[] tasks) {
        return tasksToString(Arrays.asList(tasks));
    }

    public static String tasksToString(final List<SttTask> tasks) {
        StringBuilder builder = new StringBuilder();
        for (SttTask task : tasks) {
            builder.append(task.getLine());
            builder.append('\n');
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    //
    // Members
    //

    private final String line;
    private String[] contexts = null;
    private String[] projects = null;
    private Character priority = null;
    private Boolean done = null;
    private String creationDate = null;
    private String completionDate = null;
    private String dueDate = null;
    private String description = null;

    public SttTask(final String line) {
        this.line = line;
    }

    public String getLine() {
        return line;
    }

    public boolean isDone() {
        if (done == null) {
            done = isPatternFindable(line, PATTERN_DONE);
        }
        return done;
    }

    public String getDescription() {
        if (description == null) {
            // The description is what is left when all structured parts of the task are removed
            description = getLine()
                    .replaceAll(PATTERN_COMPLETION_DATE.pattern(), "")
                    .replaceAll(PATTERN_PRIORITY_ANY.pattern(), "")
                    .replaceAll(PATTERN_CREATION_DATE.pattern(), "")
                    .replaceAll(PATTERN_CONTEXTS.pattern(), "")
                    .replaceAll(PATTERN_PROJECTS.pattern(), "")
                    .replaceAll(PATTERN_KEY_VALUE_PAIRS.pattern(), "");
        }
        return description;
    }

    public char getPriority() {
        if (priority == null) {
            String ret = parseOneValueOrDefault(line, PATTERN_PRIORITY_ANY, "");
            if (ret.length() == 1) {
                priority = ret.charAt(0);
            } else {
                priority = 0;
            }
        }
        return priority;
    }

    public String[] getContexts() {
        if (contexts == null) {
            contexts = parseAllMatches(line, PATTERN_CONTEXTS);
        }
        return contexts;
    }

    public String[] getProjects() {
        if (projects == null) {
            projects = parseAllMatches(line, PATTERN_PROJECTS);
        }
        return projects;
    }

    public String getCreationDate() {
        return getDueDate("");
    }

    public String getCreationaDate(final String defaultValue) {
        if (creationDate == null) {
            creationDate = parseOneValueOrDefault(line, PATTERN_COMPLETION_DATE, defaultValue);
        }
        return creationDate;
    }

    public String getDueDate() {
        return getDueDate("");
    }

    public String getDueDate(final String defaultValue) {
        if (dueDate == null) {
            dueDate = parseOneValueOrDefault(line, PATTERN_DUE_DATE, defaultValue);
        }
        return dueDate;
    }

    public String getCompletionDate() {
        return getCompletionDate("");
    }

    public String getCompletionDate(final String defaultValue) {
        if (completionDate == null) {
            completionDate = parseOneValueOrDefault(line, PATTERN_COMPLETION_DATE, defaultValue);
        }
        return completionDate;
    }

    // Only captures the first group of each match
    private static String[] parseAllMatches(final String text, final Pattern pattern) {
        List<String> ret = new ArrayList<>();
        for (Matcher m = pattern.matcher(text); m.find(); ) {
            if (m.groupCount() > 0) {
                ret.add(m.group(1));
            }
        }
        return ret.toArray(new String[0]);
    }

    private static String parseOneValueOrDefault(String text, Pattern pattern, String defaultValue) {
        for (Matcher m = pattern.matcher(text); m.find(); ) {
            // group / group(0) => everything, including non-capturing. group 1 = first capturing group
            if (m.groupCount() > 0) {
                return m.group(1);
            }
        }
        return defaultValue;
    }

    private static boolean isPatternFindable(String text, Pattern pattern) {
        return pattern.matcher(text).find();
    }


    // Sort tasks array and return it. Changes input array.
    public static List<SttTask> sortTasks(List<SttTask> tasks, final String orderBy, final boolean descending) {
        Collections.sort(tasks, new SttTaskSimpleComparator(orderBy, descending));
        return tasks;
    }

    public static class SttTaskSimpleComparator implements Comparator<SttTask> {
        private String _orderBy;
        private boolean _descending;

        public static final String BY_PRIORITY = "priority";
        public static final String BY_CONTEXT = "context";
        public static final String BY_PROJECT = "project";
        public static final String BY_CREATION_DATE = "creation_date";
        public static final String BY_DUE_DATE = "due_date";
        public static final String BY_DESCRIPTION = "description";
        public static final String BY_LINE = "line_natural";

        public SttTaskSimpleComparator(String orderBy, Boolean descending) {
            _orderBy = orderBy;
            _descending = descending;
        }

        @Override
        public int compare(SttTask x, SttTask y) {
            int difference;
            switch (_orderBy) {
                case BY_PRIORITY: {
                    difference = compare(x.getPriority(), y.getPriority());
                    break;
                }
                case BY_CONTEXT: {
                    difference = compare(x.getContexts(), y.getContexts());
                    break;
                }
                case BY_PROJECT: {
                    difference = compare(x.getProjects(), y.getProjects());
                    break;
                }
                case BY_CREATION_DATE: {
                    difference = compare(x.getCreationDate(), y.getCreationDate());
                    break;
                }
                case BY_DUE_DATE: {
                    difference = compare(x.getDueDate(), y.getDueDate());
                    break;
                }
                case BY_DESCRIPTION: {
                    difference = compare(x.getDescription(), y.getDescription());
                    break;
                }
                case BY_LINE: {
                    difference = compare(x.getLine(), y.getLine());
                    break;
                }
                default: {
                    return 0;
                }
            }
            if (_descending) {
                difference = -1 * difference;
            }
            return difference;
        }

        private int compareNull(Object o1, Object o2) {
            return ((o1 == null && o2 == null) || (o1 != null && o2 != null))
                    ? 0
                    : o1 == null ? -1 : 0;
        }

        private int compare(Character x, Character y) {
            return Character.compare(Character.toLowerCase(x), Character.toLowerCase(y));
        }

        private int compare(final String[] x, final String[] y) {
            return compare(Arrays.asList(x), Arrays.asList(y));
        }

        private int compare(List<String> x, List<String> y) {
            if (x.isEmpty() & y.isEmpty()) {
                return 0;
            }
            if (x.isEmpty()) {
                return 1;
            }
            if (y.isEmpty()) {
                return -1;
            }
            return x.get(0).compareTo(y.get(0));
        }

        private int compare(String x, String y) {
            int n = compareNull(x, y);
            if (n != 0) {
                return n;
            } else {
                return x.trim().toLowerCase().compareTo(y.trim().toLowerCase());
            }
        }
    }
}
