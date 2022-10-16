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
import java.util.Arrays;
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

    public final static String[] QUERY_PRIORITY_NONE = {"nopriority", "nopri", Character.toString(TodoTxtTask.PRIORITY_NONE)};
    public final static String[] QUERY_DUE_TODAY = {"today", "due"};
    public final static String[] QUERY_DUE_OVERDUE = {"overdue", "past"};
    public final static String[] QUERY_DUE_FUTURE = {"future"};
    public final static String[] QUERY_DUE_NONE = {"nodue"};
    public final static String[] QUERY_DONE = {"done"};
    public final static String[] QUERY_CONTEXT_NONE = {"nocontext", "nocontexts"};
    public final static String[] QUERY_PROJECT_NONE = {"noproject", "noprojects"};

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
        keys.add(new SttFilterKey(context.getString(R.string.due_future), Collections.frequency(all, TodoDueState.FUTURE), TodoDueState.FUTURE.toString()));
        keys.add(new SttFilterKey(context.getString(R.string.due_today), Collections.frequency(all, TodoDueState.TODAY), TodoDueState.TODAY.toString()));
        keys.add(new SttFilterKey(context.getString(R.string.due_overdue), Collections.frequency(all, TodoDueState.OVERDUE), TodoDueState.OVERDUE.toString()));
        keys.add(new SttFilterKey(STRING_NONE, Collections.frequency(all, TodoDueState.NONE), TodoDueState.NONE.toString()));

        return keys;
    }

    // Convert a set of querty keys into a formatted query
    public static String makeQuery(final Collection<String> keys, final boolean isAnd, final TodoTxtFilter.TYPE type) {
        final String prefix;
        final String nullKey;
        if (type == TYPE.CONTEXT) {
            nullKey = QUERY_CONTEXT_NONE[0];
            prefix = "@";
        } else if (type == TYPE.PROJECT) {
            nullKey = QUERY_PROJECT_NONE[0];
            prefix = "+";
        } else if (type == TYPE.PRIORITY) {
            nullKey = QUERY_PRIORITY_NONE[0];
            prefix = "";
        } else { // type due
            nullKey = QUERY_DUE_NONE[0];
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
        return String.join(isAnd ? " and " : " or ", adjusted) + " and not done";
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
            final String processed = preProcess(query);
            final CharSequence expression = parseQuery(task, processed);
            final boolean accept = evaluateExpression(expression);
            return accept;
        } catch (EmptyStackException e) {
            return false;
        }
    }

    // Pre-process the query to simplify the syntax
    private static String preProcess(final CharSequence query) {
        return query.toString()
                .replace(" AND ", " & ")
                .replace(" and ", " & ")
                .replace("&&", "&")
                .replace(" OR ", " | ")
                .replace(" or ", " | ")
                .replace("||", "|")
                .replaceAll("(\\(|\\s)NOT ", "$1! ")
                .replaceAll("(\\(|\\s)not ", "$1! ")
                .replace(" NOT ", " ! ")
                .replace(" not ", " ! ")
                .replace(" ", "");
    }

    // Is this char an operator
    private static boolean isOperator(final char c) {
        return c == '&' || c == '|' || c == '!';
    }

    // Is this char an a syntax element
    private static boolean isSyntax(final char c) {
        return c == ' ' || c == '(' || c == ')' || isOperator(c);
    }

    // Parse a query into an expression.
    // i.e. evaluate the elements in the query to true or false
    @VisibleForTesting
    public static String parseQuery(final TodoTxtTask task, final CharSequence query) {
        final StringBuilder expression = new StringBuilder();
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < query.length(); i++) {
            final char c = query.charAt(i);
            if (isSyntax(c)) {
                if (buffer.length() > 0) {
                    expression.append(evalElement(task, buffer.toString()));
                    buffer.setLength(0);
                }
                expression.append(c);
            } else {
                buffer.append(c);
            }
        }

        // Add the last element
        if (buffer.length() > 0) {
            expression.append(evalElement(task, buffer.toString()));
        }

        return expression.toString();
    }

    // Evaluate a word (element) for truthyness or falsiness
    private static char evalElement(final TodoTxtTask task, final String element) {

        // Prioritiy
        final boolean result;
        if ((element.length() == 1) && element.matches("[A-Z]")) {
            result = Character.toLowerCase(task.getPriority()) == Character.toLowerCase(element.charAt(0));
        } else if (Arrays.asList(QUERY_PRIORITY_NONE).contains(element)) {
            result = task.getPriority() == TodoTxtTask.PRIORITY_NONE;
        } else if (Arrays.asList(QUERY_DUE_TODAY).contains(element)) {
            result = task.getDueStatus() == TodoDueState.TODAY;
        } else if (Arrays.asList(QUERY_DUE_OVERDUE).contains(element)) {
            result = task.getDueStatus() == TodoDueState.OVERDUE;
        } else if (Arrays.asList(QUERY_DUE_FUTURE).contains(element)) {
            result = task.getDueStatus() == TodoDueState.FUTURE;
        } else if (Arrays.asList(QUERY_DUE_NONE).contains(element)) {
            result = task.getDueStatus() == TodoDueState.NONE;
        } else if (Arrays.asList(QUERY_DONE).contains(element)) {
            result = task.isDone();
        } else if (element.startsWith("@")) {
            result = task.getContexts().contains(element.substring(1));
        } else if (element.startsWith("+")) {
            result = task.getProjects().contains(element.substring(1));
        } else if (Arrays.asList(QUERY_CONTEXT_NONE).contains(element)) {
            result = task.getContexts().isEmpty();
        } else if (Arrays.asList(QUERY_PROJECT_NONE).contains(element)) {
            result = task.getProjects().isEmpty();
        } else {
            result = false;
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
                return;
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
                    return false; // Bad expression
                }
                stack.push(value);
            } else {
                stack.push(c);
            }
            evaluateOperations(stack);
        }
        return stack.size() == 1 && stack.pop() == 'T';
    }
}