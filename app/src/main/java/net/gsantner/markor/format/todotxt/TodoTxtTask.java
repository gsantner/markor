/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;

import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.opoc.format.GsTextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TodoTxtTask {

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
    public static final Pattern PATTERN_DUE_DATE = Pattern.compile("(^|\\s)(due:)(" + PT_DATE + ")(\\s|$)");
    public static final Pattern PATTERN_PRIORITY_ANY = Pattern.compile("(?:^|\\n)\\(([A-Za-z])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_A = Pattern.compile("(?:^|\\n)\\(([Aa])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_B = Pattern.compile("(?:^|\\n)\\(([Bb])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_C = Pattern.compile("(?:^|\\n)\\(([Cc])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_D = Pattern.compile("(?:^|\\n)\\(([Dd])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_E = Pattern.compile("(?:^|\\n)\\(([Ee])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_F = Pattern.compile("(?:^|\\n)\\(([Ff])\\)\\s");
    public static final Pattern PATTERN_PRIORITY_G_TO_Z = Pattern.compile("(?:^|\\n)\\(([g-zG-Z])\\)\\s");
    public static final Pattern PATTERN_COMPLETION_DATE = Pattern.compile("(?:^|\\n)(?:[Xx] )(" + PT_DATE + ")?");
    public static final Pattern PATTERN_CREATION_DATE = Pattern.compile("(?:^|\\n)(?:\\([A-Za-z]\\)\\s)?(?:[Xx] " + PT_DATE + " )?(" + PT_DATE + ")");

    public static final char PRIORITY_NONE = '~';

    public enum TodoDueState {
        NONE("nodue"),
        OVERDUE("overdue"),
        TODAY("today"),
        FUTURE("future")
        ;

        private final String value;
        TodoDueState(final String text) {
            value = text;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static String getToday() {
        return DATEF_YYYY_MM_DD.format(new Date());
    }

    public static boolean isTodoFile(String filepath) {
        return filepath != null && (filepath.endsWith("/todo.txt")
                || TODOTXT_FILE_PATTERN.matcher(filepath).matches() && (filepath.endsWith(".txt") || filepath.endsWith(".text")));
    }

    public static List<TodoTxtTask> getTasks(final CharSequence text, final int selStart, final int selEnd) {
        final String[] lines = text.subSequence(
                TextViewUtils.getLineStart(text, selStart),
                TextViewUtils.getLineEnd(text, selEnd)
        ).toString().split("\n");

        final List<TodoTxtTask> tasks = new ArrayList<>();
        for (final String line : lines) {
            tasks.add(new TodoTxtTask(line));
        }
        return tasks;
    }

    public static List<TodoTxtTask> getSelectedTasks(final TextView view) {
        final int[] sel = TextViewUtils.getSelection(view);
        return getTasks(view.getText(), sel[0], sel[1]);
    }

    public static List<TodoTxtTask> getAllTasks(final CharSequence text) {
        return getTasks(text, 0, text.length());
    }

    public static List<String> getProjects(final List<TodoTxtTask> tasks) {
        final TreeSet<String> set = new TreeSet<>();
        for (final TodoTxtTask task : tasks) {
            set.addAll(task.getProjects());
        }
        return new ArrayList<>(set);
    }

    public static List<String> getContexts(final List<TodoTxtTask> tasks) {
        final TreeSet<String> set = new TreeSet<>();
        for (final TodoTxtTask task : tasks) {
            set.addAll(task.getContexts());
        }
        return new ArrayList<>(set);
    }

    public static List<Character> getPriorities(final List<TodoTxtTask> tasks) {
        final TreeSet<Character> set = new TreeSet<>();
        for (final TodoTxtTask task : tasks) {
            set.add(task.getPriority());
        }
        return new ArrayList<>(set);
    }

    public static List<TodoDueState> getDueStates(final List<TodoTxtTask> tasks) {
        final TreeSet<TodoDueState> set = new TreeSet<>();
        for (final TodoTxtTask task : tasks) {
            set.add(task.getDueStatus());
        }
        return new ArrayList<>(set);
    }

    public static String tasksToString(final List<TodoTxtTask> tasks) {
        StringBuilder builder = new StringBuilder();
        for (TodoTxtTask task : tasks) {
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
    private List<String> contexts = null;
    private List<String> projects = null;
    private Character priority = null;
    private Boolean done = null;
    private String creationDate = null;
    private String completionDate = null;
    private String dueDate = null;
    private String description = null;
    private TodoDueState dueStatus = null;

    public TodoTxtTask(final CharSequence line) {
        this.line = line.toString();
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
            final String ret = parseOneValueOrDefault(line, PATTERN_PRIORITY_ANY, "");
            priority = ret.isEmpty() ? PRIORITY_NONE : Character.toUpperCase(ret.charAt(0));
        }
        return priority;
    }

    public List<String> getContexts() {
        if (contexts == null) {
            contexts = parseAllMatches(line, PATTERN_CONTEXTS);
        }
        return contexts;
    }

    public List<String> getProjects() {
        if (projects == null) {
            projects = parseAllMatches(line, PATTERN_PROJECTS);
        }
        return projects;
    }

    public String getCreationDate() {
        return getCreationaDate("");
    }

    public String getCreationaDate(final String defaultValue) {
        if (creationDate == null) {
            creationDate = parseOneValueOrDefault(line, PATTERN_CREATION_DATE, defaultValue);
        }
        return creationDate;
    }

    public String getDueDate() {
        return getDueDate("");
    }

    public String getDueDate(final String defaultValue) {
        if (dueDate == null) {
            dueDate = parseOneValueOrDefault(line, PATTERN_DUE_DATE, 3, defaultValue);
        }
        return dueDate;
    }

    public TodoDueState getDueStatus() {
        if (dueStatus == null) {
            final String date = getDueDate();
            if (GsTextUtils.isNullOrEmpty(date)) {
                dueStatus = TodoDueState.NONE;
            } else {
                final int comp = date.compareTo(getToday());
                dueStatus = (comp > 0) ? TodoDueState.FUTURE : (comp < 0) ? TodoDueState.OVERDUE : TodoDueState.TODAY;
            }
        }
        return dueStatus;
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
    private static List<String> parseAllMatches(final String text, final Pattern pattern) {
        List<String> ret = new ArrayList<>();
        for (Matcher m = pattern.matcher(text); m.find(); ) {
            if (m.groupCount() > 0) {
                ret.add(m.group(1));
            }
        }
        return ret;
    }

    private static String parseOneValueOrDefault(final String text, final Pattern pattern, final String defaultValue) {
        return parseOneValueOrDefault(text, pattern, 1, defaultValue);
    }

    private static String parseOneValueOrDefault(final String text, final Pattern pattern, final int group, final String defaultValue) {
        for (final Matcher m = pattern.matcher(text); m.find(); ) {
            if (m.groupCount() >= group) {  // Groups are 1-indexed
                return m.group(group);
            }
        }
        return defaultValue;
    }

    private static boolean isPatternFindable(final String text, final Pattern pattern) {
        return pattern.matcher(text).find();
    }

    // Sort tasks array and return it. Changes input array.
    public static List<TodoTxtTask> sortTasks(List<TodoTxtTask> tasks, final String orderBy, final boolean descending) {
        Collections.sort(tasks, new SttTaskSimpleComparator(orderBy, descending));
        return tasks;
    }

    public static class SttTaskSimpleComparator implements Comparator<TodoTxtTask> {
        private final String _orderBy;
        private final boolean _descending;

        public static final String BY_PRIORITY = "priority";
        public static final String BY_CONTEXT = "context";
        public static final String BY_PROJECT = "project";
        public static final String BY_CREATION_DATE = "creation_date";
        public static final String BY_DUE_DATE = "due_date";
        public static final String BY_DESCRIPTION = "description";
        public static final String BY_LINE = "line_natural";

        public SttTaskSimpleComparator(final String orderBy, final Boolean descending) {
            _orderBy = orderBy;
            _descending = descending;
        }

        @Override
        public int compare(final TodoTxtTask x, final TodoTxtTask y) {

            // Always push done tasks to the bottom. Note ascending is small -> big.
            final int doneCompare = Integer.compare(x.isDone() ? 1 : 0, y.isDone() ? 1 : 0);
            if (doneCompare != 0) return doneCompare;

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
                    difference = 0;
                }
            }

            // Always resolve sorts by due date and then priority
            if (difference == 0) {
                difference = compare(x.getDueDate(), y.getDueDate());
            }
            if (difference == 0) {
                difference = compare(x.getPriority(), y.getPriority());
            }

            if (_descending) {
                difference = -1 * difference;
            }
            return difference;
        }

        private static int compareNull(final String x, final String y) {
            final int xi = GsTextUtils.isNullOrEmpty(x) ? 1 : 0;
            final int yi = GsTextUtils.isNullOrEmpty(y) ? 1 : 0;
            return Integer.compare(xi, yi);
        }

        private static int compareDone(final TodoTxtTask a, TodoTxtTask b) {
            return Integer.compare(a.isDone() ? 1 : 0, b.isDone() ? 1 : 0);
        }

        private static int compare(final char x, final char y) {
            return compare(Character.toString(x), Character.toString(y));
        }

        private static int compare(final String[] x, final String[] y) {
            return compare(Arrays.asList(x), Arrays.asList(y));
        }

        private static int compare(final List<String> x, final List<String> y) {
            Collections.sort(x);
            Collections.sort(y);
            return compare(TextUtils.join("", x), TextUtils.join("", y));
        }

        private static int compare(final String x, final String y) {
            final int n = compareNull(x, y);
            if (n != 0) {
                return n;
            } else {
                return x.trim().toLowerCase().compareTo(y.trim().toLowerCase());
            }
        }

    }

    // Query matching
    // -------------------------------------------------------------------------------------------

    public boolean isMatchQuery(final CharSequence query) {
        try {
            final CharSequence expression = parseQuery(query);
            return shuntingYard(expression);
        } catch (EmptyStackException e) {
            return false;
        }
    }

    private static boolean isOperator(final char c) {
        return c == '&' || c == '|' || c == '!';
    }

    private static boolean isSyntax(final char c) {
        return c == ' ' || c == '(' || c == ')' || isOperator(c);
    }

    public static boolean stackIsHigher(final Stack<Character> ops, final char op) {
        if (ops.isEmpty()) {
            return false;
        }

        if (op == '!') {
            return false;
        } else if (op == '&') {
            return ops.peek() == '!';
        } else {
            return ops.peek() != '('; // Everything is higher than OR
        }
    }

    public static void evalTop(final Stack<Character> ops, final Stack<Boolean> values) {
        // Remove operands from stack
        final char op = ops.pop();
        if (op == '!') {
            values.push(!values.pop());
        } else if (op == '|') {
            values.push(values.pop() | values.pop());
        } else if (op == '&') {
            values.push(values.pop() & values.pop());
        }
    }

    /**
     * An implementation of the shunting yard expression evaluator for boolean expressions
     * i.e. evaluate expressions of the form `T | F & !(T | F)` etc etc
     *
     * @param expression String expression to evaluate
     * @return whether the expression evaluates to True or False (error = false)
     */
    @VisibleForTesting
    public static boolean shuntingYard(final CharSequence expression) {
        final Stack<Character> ops = new Stack<>();
        final Stack<Boolean> values = new Stack<>();
        for (int i = 0; i < expression.length(); i++) {
            final char symbol = expression.charAt(i);
            if (symbol == '(') {
                ops.push(symbol);
            } else if (isOperator(symbol)) {
                // Evaluate per precedence
                while (stackIsHigher(ops, symbol)) evalTop(ops, values);
                ops.push(symbol);
            } else if (symbol == 'T' || symbol == 'F') {
                values.push(symbol == 'T');
            } else if (symbol == ')') {
                while(!ops.isEmpty() && ops.peek() != '(') evalTop(ops, values);
                if (!ops.isEmpty() && ops.pop() != '(') {
                    return false; // Should be at end or paren
                }
            } else {
                return false; // We have an unexpected symbol
            }
        }
        while(!ops.isEmpty()) evalTop(ops, values);
        return (values.size() == 1) && values.pop();
    }

    @VisibleForTesting
    public String parseQuery(final CharSequence query) {
        final StringBuilder expression = new StringBuilder();
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < query.length(); i++) {
            final char c = query.charAt(i);
            if (isSyntax(c)) {
                if (buffer.length() > 0) {
                    expression.append(evalElement(buffer.toString()) ? 'T' : 'F');
                    buffer.setLength(0);
                }
                expression.append(c);
            } else {
                buffer.append(c);
            }
        }

        if (buffer.length() > 0) {
            expression.append(evalElement(buffer.toString()) ? 'T' : 'F');
        }

        return expression.toString();
    }

    @VisibleForTesting
    public boolean evalElement(final String element) {

        // Prioritiy
        if ((element.length() == 1) && element.matches("[A-Z]")) {
            return (getPriority() == element.charAt(0));
        } else if (element.equals("~") || element.equals("nopri") || element.equals("nopriority")) {
            return getPriority() == PRIORITY_NONE;
        } else if (element.equals("today") || element.equals("due")) {
            return getDueStatus() == TodoDueState.TODAY;
        } else if (element.equals("past") || element.equals("overdue")) {
            return getDueStatus() == TodoDueState.OVERDUE;
        } else if (element.equals("future")) {
            return getDueStatus() == TodoDueState.FUTURE;
        } else if (element.equals("nodue")) {
            return getDueStatus() == TodoDueState.NONE;
        } else if (element.equals("done")) {
            return isDone();
        } else if (element.startsWith("@")) {
            return getContexts().contains(element.substring(1));
        } else if (element.startsWith("+")) {
            return getProjects().contains(element.substring(1));
        } else if (element.equals("nocontext") || element.equals("nocontexts")) {
            return getContexts().isEmpty();
        } else if (element.equals("noproject") || element.equals("noprojects")) {
            return getProjects().isEmpty();
        }

        return false;
    }
}
