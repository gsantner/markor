package net.gsantner.markor.format.todotxt;

import android.text.TextUtils;

import net.gsantner.opoc.util.Callback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TodoTxtQuery {

    public static final String PROJECT = "project";
    public static final String CONTEXT = "context";
    public static final String PRIORITY = "priority";
    public static final String DUE = "due_date";

    private static final String IS_AND = "is_and";
    private static final String KEYS = "keys";
    private static final String TYPE = "type";
    private static final String DUE_TODAY = "today";
    private static final String DUE_OVERDUE = "overdue";
    private static final String DUE_FUTURE = "future";

    public static String buildJsonQuery(final String type, final List<Object> keys, boolean isAnd) {
        try {
            final JSONObject stage = new JSONObject();
            stage.put(TYPE, type);
            stage.put(IS_AND, isAnd);

            final JSONArray jsonKeys = new JSONArray();
            for (final Object key : keys) {
                jsonKeys.put(translate(type, key));
            }

            stage.put(KEYS, jsonKeys);

            final JSONArray wrapper = new JSONArray();
            wrapper.put(stage);

            return wrapper.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Parse query into string
    public static Callback.b1<TodoTxtTask> parseJsonQuery(final String query) {

        try {
            // Top level must be an array
            final JSONArray top = new JSONArray(query.trim());
            final List<Callback.b1<TodoTxtTask>> stages = new ArrayList<>();
            for (int i = 0; i < top.length(); i++) {
                stages.add(parseStage(top.getJSONObject(i)));
            }

            return t -> {
                if (t.isDone()) {
                    return false;
                }

                for (final Callback.b1<TodoTxtTask> stage : stages) {
                    if (!stage.callback(t)) {
                        return false;
                    }
                }
                return true;
            };
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String translate(final String type, final Object value) {
        if ((type.equals(PROJECT) || type.equals(CONTEXT)) && value instanceof String) {
            return TextUtils.isEmpty((String) value) ? null : (String) value;
        } else if (type.equals(DUE) && value instanceof TodoTxtTask.TodoDueState) {
            final TodoTxtTask.TodoDueState state = (TodoTxtTask.TodoDueState) value;
            switch (state) {
                case NONE:
                    return null;
                case TODAY:
                    return DUE_TODAY;
                case FUTURE:
                    return DUE_FUTURE;
                case OVERDUE:
                    return DUE_OVERDUE;
            }
        } else if (type instanceof)

        }
    }


    private static Callback.b1<TodoTxtTask> parseStage(final JSONObject stage) throws JSONException {
        final String type = stage.getString(TYPE);
        final boolean isAnd = stage.optBoolean(IS_AND, false);

        final JSONArray jsonKeys = stage.getJSONArray(KEYS);
        final List<String> keys = new ArrayList<>();
        for (int i = 0; i < jsonKeys.length(); i++) {
            keys.add(jsonKeys.getString(i));
        }

        switch (type) {
            case PROJECT:
                return parseListType(keys, isAnd, TodoTxtTask::getProjects);
            case CONTEXT:
                return parseListType(keys, isAnd, TodoTxtTask::getContexts);
            case PRIORITY:
                return t -> (t.getPriority() == TodoTxtTask.PRIORITY_NONE && keys.contains(null)) || keys.contains(Character.toString(t.getPriority()));
            case DUE:
                return parseDue(keys);
        }
        return null;
    }

    private static Callback.b1<TodoTxtTask> parseListType(final List<String> keys, final boolean isAnd, final Callback.r1<List<String>, TodoTxtTask> getter) {
        return task -> {
            final List<String> items = getter.callback(task);
            if (isAnd) {
                if (keys.size() == 1 && keys.contains(null)) {
                    return items.isEmpty();
                } else {
                    return false; // Cant contain none and other
                }
            } else {
                return !Collections.disjoint(items, keys) || items.isEmpty() && keys.contains(null);
            }
        };
    }

    private static Callback.b1<TodoTxtTask> parseDue(final List<String> keys) {
        final List<TodoTxtTask.TodoDueState> req = new ArrayList<>();
        for (final String k : keys) {
            if (k == null) {
                req.add(TodoTxtTask.TodoDueState.NONE);
            } else if (k.equals(DUE_TODAY)) {
                req.add(TodoTxtTask.TodoDueState.TODAY);
            } else if (k.equals(DUE_OVERDUE)) {
                req.add(TodoTxtTask.TodoDueState.OVERDUE);
            } else if (k.equals(DUE_FUTURE)) {
                req.add(TodoTxtTask.TodoDueState.FUTURE);
            }
        }
        return t -> req.contains(t.getDueStatus());
    }
}
