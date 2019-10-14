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

import net.gsantner.opoc.format.todotxt.extension.SttTaskParserInfoExtension;
import net.gsantner.opoc.format.todotxt.extension.SttTaskWithParserInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Text = the whole document ;; line = one task line, \n separated
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "SameParameterValue"})
public class SttCommander {
    //
    // Statics
    //
    public static final Pattern TODOTXT_FILE_PATTERN = Pattern.compile("(?i)(^todo[-.]?.*)|(.*[-.]todo\\.((txt)|(text))$)");
    public static final SimpleDateFormat DATEF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private static final String PT_DATE = "\\d{4}-\\d{2}-\\d{2}";

    public static final Pattern PATTERN_DESCRIPTION = Pattern.compile("(?:^|\\n)" +
            "(?:" +
            "(?:[Xx](?: " + PT_DATE + " " + PT_DATE + ")?)" + // Done in front
            "|(?:\\([A-Za-z]\\)(?: " + PT_DATE + ")?)" + // Priority in front
            "|(?:" + PT_DATE + ")" + // Creation date in front
            " )?" + // End of prefix
            "((a|.)*)" // Take whats left
    );
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

    // Tasks from inside full text
    public static class SttTasksInTextRange {
        public final List<SttTaskWithParserInfo> tasks = new ArrayList<>();
        public int startIndex = -1;
        public int endIndex = -1;
    }

    //
    // Singleton
    //
    private static SttCommander __instance;

    public static SttCommander get() {
        if (__instance == null) {
            __instance = new SttCommander();
        }
        return __instance;
    }

    //
    // Members, Constructors
    //
    public SttCommander() {

    }

    //
    // Parsing Methods
    //

    public SttTaskWithParserInfo parseTask(String text, int cursorPosInText) {
        int iOffsetInLine = 0;
        int iLineStart = 0;
        String line = "";
        if (text != null && cursorPosInText <= text.length()) {
            iLineStart = text.lastIndexOf('\n', cursorPosInText - 1);
            iLineStart = iLineStart == -1 ? 0 : iLineStart + 1;
            if (iLineStart < text.length()) {
                int lineEnd = text.indexOf('\n', iLineStart);
                lineEnd = lineEnd == -1 ? text.length() : (lineEnd);
                line = text.substring(iLineStart, lineEnd);
            }
            iOffsetInLine = cursorPosInText - iLineStart;
        }

        SttTaskWithParserInfo ret = parseTask(line);
        ret.setLineOffsetInText(iLineStart);
        ret.setCursorOffsetInLine(iOffsetInLine);
        return ret;
    }

    public SttTaskWithParserInfo parseTask(final String line) {
        SttTaskWithParserInfo task = new SttTaskWithParserInfo();
        task.setTaskLine(line);
        task.setDescription(parseDescription(line));
        task.setProjects(parseProjects(line));
        task.setContexts(parseContexts(line));
        task.setDone(parseDone(line));
        task.setCompletionDate(parseCompletionDate(line));
        task.setCreationDate(parseCreationDate(line));
        task.setPriority(parsePriority(line));
        task.setKeyValuePairs(parseKeyValuePairs(line));
        task.setDueDate(parseDueDate(line));

        System.out.print("");
        return task;
    }

    public List<String> parseContexts(String text) {
        return parseAllUniqueMatchesWithOneValue(text, PATTERN_CONTEXTS);
    }

    public List<String> parseProjects(String text) {
        return parseAllUniqueMatchesWithOneValue(text, PATTERN_PROJECTS);
    }


    private boolean parseDone(String line) {
        return isPatternFindable(line, PATTERN_DONE);
    }

    private String parseCompletionDate(String line) {
        return parseOneValueOrDefault(line, PATTERN_COMPLETION_DATE, "");
    }

    private String parseCreationDate(String line) {
        return parseOneValueOrDefault(line, PATTERN_CREATION_DATE, "");
    }

    private String parseDueDate(String line) {
        return parseOneValueOrDefault(line, PATTERN_DUE_DATE, "");
    }

    private char parsePriority(String line) {
        String ret = parseOneValueOrDefault(line, PATTERN_PRIORITY_ANY, "");
        if (ret.length() == 1) {
            return ret.charAt(0);
        } else {
            return SttTask.PRIORITY_NONE;
        }
    }

    private String parseDescription(String line) {
        return parseOneValueOrDefault(line, PATTERN_DESCRIPTION, "");
    }

    private Map<String, String> parseKeyValuePairs(String line) {
        Map<String, String> values = new HashMap<>();
        for (String kvp : parseAllUniqueMatchesWithOneValue(line, PATTERN_KEY_VALUE_PAIRS)) {
            int s = kvp.indexOf(':');
            values.put(kvp.substring(0, s), kvp.substring(s + 1));
        }
        return values;
    }


    //
    // Applying methods
    //
    public void insertProject(SttTaskWithParserInfo task, String project, int atIndex) {
        String text = task.getTaskLine();
        project = project.startsWith("+") ? project.substring(1) : project;
        String[] split = splitAtIndexFailsafe(text, atIndex);

        String left = split[0];
        String right = split[1];
        left = (!left.endsWith(" ") && !left.isEmpty()) ? (left + " ") : left;
        right = (!right.startsWith(" ") && !right.isEmpty()) ? (" " + right) : right;
        task.setTaskLine(left + "+" + project + right);
        List<String> projects = task.getProjects();
        if (!projects.contains(project)) {
            projects.add(project);
        }
        task.setDescription(parseDescription(task.getTaskLine()));
    }

    public void insertContext(SttTaskWithParserInfo task, String context, int atIndex) {
        String text = task.getTaskLine();
        context = context.startsWith("@") ? context.substring(1) : context;
        String[] split = splitAtIndexFailsafe(text, atIndex);

        String left = split[0];
        String right = split[1];
        left = (!left.endsWith(" ") && !left.isEmpty()) ? (left + " ") : left;
        right = (!right.startsWith(" ") && !right.isEmpty()) ? (" " + right) : right;
        task.setTaskLine(left + "@" + context + right);
        List<String> contexts = task.getContexts();
        if (!contexts.contains(context)) {
            contexts.add(context);
        }
        task.setDescription(parseDescription(task.getTaskLine()));
    }

    //
    // More Stt Methods
    //
    public SttCommander regenerateTaskLine(SttTaskWithParserInfo task) {
        StringBuilder sb = new StringBuilder(task.getTaskLine().length() + 5);
        String tmp;
        if (task.isDone()) {
            sb.append("x ");
            if (nz(tmp = task.getCreationDate())) {
                if (!nz(task.getCompletionDate())) {
                    task.setCompletionDate(getToday());
                }
                sb.append(task.getCompletionDate());
                sb.append(" ");
                sb.append(tmp);
                sb.append(" ");
            }
        } else if (task.getPriority() != SttTask.PRIORITY_NONE) {
            sb.append("(");
            sb.append(task.getPriority());
            sb.append(") ");
        }
        if (!task.isDone() && nz(tmp = task.getCreationDate())) {
            sb.append(tmp);
            sb.append(" ");
        }

        // Make sure there is no more than one trailing space
        tmp = sb.toString().trim();
        tmp += tmp.isEmpty() ? "" : " ";
        sb.setLength(0);
        sb.append(tmp);
        sb.append(task.getDescription().trim());


        task.setTaskLine(sb.toString());
        return this;
    }

    public String regenerateText(String text, SttTaskWithParserInfo task) {
        regenerateTaskLine(task);
        return replaceTillEndOfLineFromIndex(task.getLineOffsetInText(), text, task.getTaskLine());
    }

    //
    // General methods
    //
    @SuppressWarnings("StatementWithEmptyBody")
    private static String[] splitAtIndexFailsafe(String text, int atIndex) {
        String left = "";
        String right = "";

        if (text == null || text.isEmpty()) {

        } else if (atIndex >= text.length()) {
            left = text;
        } else if (atIndex < 0) {
            right = text;
        } else {
            left = text.substring(0, atIndex);
            right = text.substring(atIndex);
        }


        return new String[]{left, right};
    }

    // Find all lines that are between first and second index param
    // These can be anywhere in a line and will expand to line start and ending
    public SttTasksInTextRange findTasksBetweenIndex(String text, int indexSomewhereInLineStart, int indexSomewhereInLineEnd) {
        final SttTasksInTextRange found = new SttTasksInTextRange();
        final SttCommander sttcmd = SttCommander.get();
        int i = indexSomewhereInLineStart;

        // Special case: Cursor position on file ending -> go back by one char
        if (i == text.length()) {
            i--;
        }

        while (i >= 0 && i <= indexSomewhereInLineEnd && i < text.length()) {
            final SttTaskWithParserInfo task = sttcmd.parseTask(text, i);
            found.tasks.add(task);
            if (found.startIndex == -1) {
                found.startIndex = task.getLineOffsetInText();
                i = found.startIndex;
            }
            i += task.getTaskLine().length() + 1; // +1 for linefeed
            found.endIndex = i;
        }

        // Delete till the end of file if we are over the end
        if (!text.isEmpty() && found.endIndex > text.length()) {
            found.endIndex = text.length();
        }

        // Finally delete
        if (found.startIndex >= 0 && found.startIndex < text.length() && found.endIndex >= 0 && found.endIndex <= text.length()) {
            return found;
        } else {
            return new SttTasksInTextRange();
        }
    }


    // Replace till the end of the line, starting from index
    public static String replaceTillEndOfLineFromIndex(int index, String text, String replacementLine) {
        String[] split = splitAtIndexFailsafe(text, index);
        split[1] = split[1].contains("\n") ? split[1].replaceFirst(".*(\\n)", replacementLine + "\n") : replacementLine;
        return split[0] + split[1];
    }

    // not empty
    public static boolean nz(String str) {
        return str != null && !str.isEmpty();
    }

    public static String getToday() {
        return DATEF_YYYY_MM_DD.format(new Date());
    }

    public static String getDaysFromToday(int days) {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, days);
        return DATEF_YYYY_MM_DD.format(cal.getTime());
    }

    public static boolean isTodoFile(String filepath) {
        return filepath != null && SttCommander.TODOTXT_FILE_PATTERN.matcher(filepath).matches() && (filepath.endsWith(".txt") || filepath.endsWith(".text"));
    }

    // Only captures the first group of each match
    private static List<String> parseAllUniqueMatchesWithOneValue(String text, Pattern pattern) {
        List<String> ret = new ArrayList<>();
        for (Matcher m = pattern.matcher(text); m.find(); ) {
            if (m.groupCount() > 0) {
                String found = m.group(1);
                if (!ret.contains(found)) {
                    ret.add(found);
                }
            }
        }
        return ret;
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

    public static class SttTaskSimpleComparator implements Comparator<SttTask> {
        private String _orderBy;
        private boolean _descending;

        public SttTaskSimpleComparator(String orderBy, Boolean descending) {
            _orderBy = orderBy;
            _descending = descending;
        }

        @Override
        public int compare(SttTask x, SttTask y) {
            int difference;
            switch (_orderBy) {
                case "priority": {
                    difference = compare(x.getPriority(), y.getPriority());
                    break;
                }
                case "context": {
                    difference = compare(x.getContexts(), y.getContexts());
                    break;
                }
                case "project": {
                    difference = compare(x.getProjects(), y.getProjects());
                    break;
                }
                case "date": {
                    difference = compare(x.getCreationDate(), y.getCreationDate());
                    break;
                }
                case "duedate": {
                    difference = compare(x.getDueDate(), y.getDueDate());
                    break;
                }
                case "description": {
                    difference = compare(x.getDescription(), y.getDescription());
                    break;
                }
                case "line": {
                    if (x instanceof SttTaskParserInfoExtension && y instanceof SttTaskParserInfoExtension) {
                        difference = compare(((SttTaskParserInfoExtension) x).getTaskLine(), ((SttTaskParserInfoExtension) y).getTaskLine());
                    } else {
                        difference = compare(x.getDescription(), y.getDescription());
                    }
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
