package net.gsantner.markor.format.todotxt;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.Callback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TodoTxtView {

    public static final String SAVED_TODO_VIEWS = "todo_txt__saved_todo_views";
    public static final int MAX_RECENT_VIEWS = 5;

    public static final String PROJECT = "todo_txt__view_type_project";
    public static final String CONTEXT = "todo_txt__view_type_context";
    public static final String PRIORITY = "todo_txt__view_type_priority";
    public static final String DUE = "todo_txt__view_type_due-date";

    private static final String TITLE = "title";
    private static final String IS_AND = "is_and";
    private static final String KEYS = "keys";
    private static final String TYPE = "type";

    public static Callback.r1<List<String>, TodoTxtTask> keyGetter(final Context context, final String type) {
        switch (type) {
            case PROJECT:
                return TodoTxtTask::getProjects;
            case CONTEXT:
                return TodoTxtTask::getContexts;
            case PRIORITY:
                return task -> task.getPriority() == TodoTxtTask.PRIORITY_NONE ? Collections.emptyList() : Collections.singletonList(Character.toString(task.getPriority()));
            case DUE:
                final Map<TodoTxtTask.TodoDueState, String> statusMap = new HashMap<>();
                statusMap.put(TodoTxtTask.TodoDueState.TODAY, context.getString(R.string.due_today));
                statusMap.put(TodoTxtTask.TodoDueState.OVERDUE, context.getString(R.string.due_overdue));
                statusMap.put(TodoTxtTask.TodoDueState.FUTURE, context.getString(R.string.due_future));
                return task -> task.getDueStatus() == TodoTxtTask.TodoDueState.NONE ? Collections.emptyList() : Collections.singletonList(statusMap.get(task.getDueStatus()));
        }

        return null;
    }

    public static Callback.b1<TodoTxtTask> taskSelector(final Collection<String> keys, final Callback.r1<List<String>, TodoTxtTask> keyGetter, final boolean isAnd) {

        final boolean noneIncluded = keys.remove(null);
        final Set<String> searchSet = (keys instanceof HashSet) ? (HashSet) keys : new HashSet<>(keys);

        return (task) -> {
            final List<String> taskKeys = keyGetter.callback(task);
            if (task.isDone()) {
                return false;
            } else if (isAnd) {
                return (taskKeys.isEmpty() && noneIncluded) || taskKeys.containsAll(searchSet);
            } else {
                return (taskKeys.isEmpty() && noneIncluded) || (!Collections.disjoint(searchSet, taskKeys));
            }
        };
    }

    public static class Group {
        public String title;
        public String queryType;
        public List<String> keys;
        public boolean isAnd;
    }

    public static void saveView(final Context context, final String saveTitle, final String queryType, final Collection<String> selKeys, final boolean isAnd) {
        try {
            // Create the view dict
            final JSONObject obj = new JSONObject();
            obj.put(TITLE, saveTitle);
            obj.put(TYPE, queryType);
            obj.put(IS_AND, isAnd);
            final JSONArray keysArray = new JSONArray();
            for (final String key : selKeys) {
                keysArray.put(key);
            }
            obj.put(KEYS, keysArray);

            // Load the existing list of views
            final JSONArray newArray = new JSONArray();
            newArray.put(obj);

            // This oldArray / newArray approach needed as array.remove is api 19+
            final SharedPreferences pref = context.getSharedPreferences(SharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
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
        Toast.makeText(context, String.format("✔, %s️", saveTitle), Toast.LENGTH_SHORT).show();
    }

    public static List<Group> loadViews(final Context context) {
        final List<Group> loadedViews = new ArrayList<>();
        try {
            final SharedPreferences pref = context.getSharedPreferences(SharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
            final String jsonString = pref.getString(SAVED_TODO_VIEWS, "[]");
            final JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject obj = array.getJSONObject(i);
                final Group gp = new Group();
                gp.isAnd = obj.optBoolean(IS_AND, false);
                gp.title = obj.optString(TITLE, "UNTITLED");
                gp.queryType = obj.getString(TYPE);
                gp.keys = new ArrayList<>();
                final JSONArray keysArray = obj.getJSONArray(KEYS);
                for (int j = 0; i < keysArray.length(); i++) {
                    gp.keys.add(keysArray.getString(j));
                }
                loadedViews.add(gp);
            }
            return loadedViews;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return loadedViews;
    }
}
