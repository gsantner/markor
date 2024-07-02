package net.gsantner.markor.format.todotxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import net.gsantner.markor.R;
import net.gsantner.markor.format.todotxt.TodoTxtTask.TodoDueState;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.wrapper.GsCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

public class TodoTxtFilter {

    // Special query keywords
    // ----------------------------------------------------------------------------------------
    public final static String QUERY_PRIORITY_ANY = "pri";
    public final static String QUERY_DUE_TODAY = "due=";
    public final static String QUERY_DUE_OVERDUE = "due<";
    public final static String QUERY_DUE_FUTURE = "due>";
    public final static String QUERY_DUE_ANY = "due";
    public final static String QUERY_DONE = "done";

    // ----------------------------------------------------------------------------------------
    public static final String SAVED_TODO_VIEWS = "todo_txt__saved_todo_views";
    public static final String STRING_NONE = "-";
    private static final String TITLE = "TITLE";
    private static final String QUERY = "QUERY";
    private static final String NULL_SENTINEL = "NULL SENTINEL";

    public static final int MAX_RECENT_VIEWS = 10;

    public enum TYPE {
        PROJECT, CONTEXT, PRIORITY, DUE
    }

    public static class SttFilterKey {
        public final String key;    // Name
        public final int count;     // How many exist
        public final String query;  // What to stick in the query

        private SttFilterKey(String k, int c, String q) {
            key = k;
            count = c;
            query = q;
        }
    }

    public static List<SttFilterKey> getKeys(final Context context, final List<TodoTxtTask> tasks, final TYPE type) {
        if (type == TYPE.PROJECT) {
            return getStringListKeys(tasks, TodoTxtTask::getProjects);
        } else if (type == TYPE.CONTEXT) {
            return getStringListKeys(tasks, TodoTxtTask::getContexts);
        } else if (type == TYPE.PRIORITY) {
            return getStringListKeys(tasks, t -> t.getPriority() == TodoTxtTask.PRIORITY_NONE ? null : Collections.singletonList(Character.toString(Character.toUpperCase(t.getPriority()))));
        } else if (type == TYPE.DUE) {
            return getDueKeys(context, tasks);
        } else {
            return Collections.emptyList();
        }
    }

    private static List<SttFilterKey> getStringListKeys(final List<TodoTxtTask> tasks, final GsCallback.r1<List<String>, TodoTxtTask> keyGetter) {
        final List<String> all = new ArrayList<>();
        for (final TodoTxtTask task : tasks) {
            final List<String> tKeys = keyGetter.callback(task);
            all.addAll(tKeys == null || tKeys.isEmpty() ? Collections.singletonList(NULL_SENTINEL) : tKeys);
        }

        final List<SttFilterKey> keys = new ArrayList<>();
        final Set<String> unique = new TreeSet<>(all);
        if (unique.remove(NULL_SENTINEL)) {
            keys.add(new SttFilterKey(STRING_NONE, Collections.frequency(all, NULL_SENTINEL), null));
        }
        for (final String key : unique) {
            keys.add(new SttFilterKey(key, Collections.frequency(all, key), key));
        }

        return keys;
    }

    public static List<SttFilterKey> getDueKeys(final Context context, final List<TodoTxtTask> tasks) {
        final List<TodoTxtTask.TodoDueState> all = new ArrayList<>();
        for (final TodoTxtTask task : tasks) {
            all.add(task.getDueStatus());
        }
        final List<SttFilterKey> keys = new ArrayList<>();
        keys.add(new SttFilterKey(context.getString(R.string.due_future), Collections.frequency(all, TodoDueState.FUTURE), QUERY_DUE_FUTURE));
        keys.add(new SttFilterKey(context.getString(R.string.due_today), Collections.frequency(all, TodoDueState.TODAY), QUERY_DUE_TODAY));
        keys.add(new SttFilterKey(context.getString(R.string.due_overdue), Collections.frequency(all, TodoDueState.OVERDUE), QUERY_DUE_OVERDUE));
        keys.add(new SttFilterKey(STRING_NONE, Collections.frequency(all, TodoDueState.NONE), null));

        return keys;
    }

    // Convert a set of querty keys into a formatted query
    public static String makeQuery(final Collection<String> keys, final boolean isAnd, final TodoTxtFilter.TYPE type) {
        final String prefix;
        final String nullKey;
        if (type == TYPE.CONTEXT) {
            nullKey = "!@";
            prefix = "@";
        } else if (type == TYPE.PROJECT) {
            nullKey = "!+";
            prefix = "+";
        } else if (type == TYPE.PRIORITY) {
            nullKey = "!" + QUERY_PRIORITY_ANY;
            prefix = "pri:";
        } else { // type due
            nullKey = "!" + QUERY_DUE_ANY;
            prefix = "";
        }

        final List<String> adjusted = new ArrayList<>();
        for (final String key : keys) {
            if (key != null) {
                adjusted.add(prefix + key);
            } else {
                adjusted.add(nullKey);
            }
        }

        // We don't include done tasks by default
        return String.join(isAnd ? " & " : " | ", adjusted) + " & !" + QUERY_DONE;
    }

    public static void saveFilter(final Context context, final String title, final String query) {
        try {
            // Create the view dict
            final JSONObject obj = new JSONObject();
            obj.put(TITLE, title);
            obj.put(QUERY, query);

            // This oldArray / newArray approach needed as array.remove is api 19+
            final JSONArray newArray = new JSONArray();
            newArray.put(obj);

            // Load the existing list of views and append the required number to the newArray
            final SharedPreferences pref = context.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
            final JSONArray oldArray = new JSONArray(pref.getString(SAVED_TODO_VIEWS, "[]"));
            final int addCount = Math.min(MAX_RECENT_VIEWS - 1, oldArray.length());
            for (int i = 0; i < addCount; i++) {
                newArray.put(oldArray.get(i));
            }

            // Save
            pref.edit().putString(SAVED_TODO_VIEWS, newArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "𐄂", Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(context, String.format("✔ %s️", title), Toast.LENGTH_SHORT).show();
    }

    public static boolean deleteFilterIndex(final Context context, int index) {
        try {
            final SharedPreferences pref = context.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
            // Load the existing list of views

            final JSONArray oldArray = new JSONArray(pref.getString(SAVED_TODO_VIEWS, "[]"));
            if (index < 0 || index >= oldArray.length()) {
                return false;
            }

            final JSONArray newArray = new JSONArray();
            for (int i = 0; i < oldArray.length(); i++) {
                if (i != index) {
                    newArray.put(oldArray.get(i));
                }
            }

            pref.edit().putString(SAVED_TODO_VIEWS, newArray.toString()).apply();

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Pair<String, String>> loadSavedFilters(final Context context) {
        final SharedPreferences pref = context.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
        try {
            final List<Pair<String, String>> loadedViews = new ArrayList<>();
            final String jsonString = pref.getString(SAVED_TODO_VIEWS, "[]");
            final JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject obj = array.getJSONObject(i);
                loadedViews.add(Pair.create(obj.getString(TITLE), obj.getString(QUERY)));
            }
            return loadedViews;
        } catch (JSONException e) {
            e.printStackTrace();
            pref.edit().remove(SAVED_TODO_VIEWS).apply();
        }
        return Collections.emptyList();
    }

    // Query matching
    // -------------------------------------------------------------------------------------------

    public static boolean isMatchQuery(final TodoTxtTask task, final CharSequence query) {
        try {
            final CharSequence expression = parseQuery(task, query);
            return evaluateExpression(expression);
        } catch (EmptyStackException | IllegalArgumentException e) {
            // TODO - display a useful message somehow
            return false;
        }
    }

    // Pre-process the query to simplify the syntax
    private static String preProcess(final CharSequence query) {
        return String.format(" %s ", query)                  // Leading and trailing spaces
                .replace(" !", " ! ")    // Add space after exclamation mark
                .replace(" (", " ( ")    // Add space before opening paren
                .replace(") ", " ) ");   // Add space after closing paren
    }

    private static boolean isSyntax(final char c) {
        return c == '!' || c == '|' || c == '&' || c == '(' || c == ')';
    }

    // Parse a query into an expression.
    // i.e. evaluate the elements in the query to true or false
    @VisibleForTesting
    public static String parseQuery(final TodoTxtTask task, final CharSequence query) {
        final StringBuilder expression = new StringBuilder();
        final String[] parts = preProcess(query).split(" ");
        for (final String part : parts) {
            if (part.length() == 1 && isSyntax(part.charAt(0))) {
                expression.append(part);
            } else if (!part.isEmpty()) {
                expression.append(evalElement(task, part));
            }
        }
        return expression.toString();
    }

    // Evaluate a word (element) for truthyness or falsiness
    // Step through all the possible conditions
    private static char evalElement(final TodoTxtTask task, final String element) {

        final boolean result;
        if (element.startsWith(QUERY_PRIORITY_ANY)) {
            if (QUERY_PRIORITY_ANY.equals(element)) {
                result = task.getPriority() != TodoTxtTask.PRIORITY_NONE;
            } else if (element.length() == 5 && element.charAt(3) == ':') {
                result = task.getPriority() == element.charAt(4);
            } else {
                result = false;
            }
        } else if (QUERY_DUE_TODAY.equals(element)) {
            result = task.getDueStatus() == TodoDueState.TODAY;
        } else if (QUERY_DUE_OVERDUE.equals(element)) {
            result = task.getDueStatus() == TodoDueState.OVERDUE;
        } else if (QUERY_DUE_FUTURE.equals(element)) {
            result = task.getDueStatus() == TodoDueState.FUTURE;
        } else if (QUERY_DUE_ANY.equals(element)) {
            result = task.getDueStatus() != TodoDueState.NONE;
        } else if (QUERY_DONE.equals(element)) {
            result = task.isDone();
        } else if (element.equals("@")) {
            result = !task.getContexts().isEmpty();
        } else if (element.equals("+")) {
            result = !task.getProjects().isEmpty();
        } else if (element.startsWith("@")) {
            result = task.getContexts().contains(element.substring(1));
        } else if (element.startsWith("+")) {
            result = task.getProjects().contains(element.substring(1));
        } else {
            // Default to string match
            result = task.getLine().toLowerCase().contains(element.toLowerCase());
        }

        return result ? 'T' : 'F';
    }

    // Expression evaluator
    // ---------------------------------------------------------------------------------------------

    private static boolean isStart(final Stack<Character> stack) {
        return stack.isEmpty() || stack.peek() == '(';
    }

    public static boolean isValue(final Stack<Character> stack) {
        if (!stack.isEmpty()) {
            final char top = stack.peek();
            return top == 'T' || top == 'F';
        }
        return false;
    }

    private static char toChar(final boolean v) {
        return v ? 'T' : 'F';
    }

    public static void evaluateOperations(final Stack<Character> stack) {
        while (!isStart(stack) && isValue(stack)) {
            final char rhs = stack.pop();
            if (isStart(stack)) {
                stack.push(rhs);
                return;
            }
            final char op = stack.pop();
            if (op == '|') {
                stack.push(toChar(stack.pop() == 'T' | rhs == 'T'));
            } else if (op == '&') {
                stack.push(toChar(stack.pop() == 'T' & rhs == 'T'));
            } else if (op == '!') {
                stack.push(toChar(rhs == 'F'));
            } else {
                throw new IllegalArgumentException("Unexpected character");
            }
        }
    }

    public static boolean evaluateExpression(final CharSequence expression) {
        final Stack<Character> stack = new Stack<>();
        for (int i = 0; i < expression.length(); i++) {
            final char c = expression.charAt(i);
            if (c == ')') {
                final char value = stack.pop();
                if (stack.pop() != '(') {
                    throw new IllegalArgumentException("Mismatched parenthesis");
                }
                stack.push(value);
            } else {
                stack.push(c);
            }
            evaluateOperations(stack);
        }
        if (stack.size() == 1 && isValue(stack)) {
            return stack.pop() == 'T';
        }
        throw new IllegalArgumentException("Malformed expression");
    }
}