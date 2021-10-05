package net.gsantner.markor.format.todotxt;

import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TodoTxtQuery {

    private static final String AND = "and";
    private static final String OR = "or";
    private static final String NOT = "not";
    private static final String PROJECT = "project";
    private static final String CONTEXT = "context";
    private static final String PRIORITY = "pri";
    private static final String DUE = "due";
    private static final String DONE = "done";
    private static final String DUE_TODAY = "today";
    private static final String DUE_OVERDUE = "overdue";
    private static final String DUE_FUTURE = "future";

    private interface Query {
        boolean apply(final TodoTxtTask task);
    }

    private static class Group implements Query {

        final public boolean _isAnd;

        private final ArrayList<Query> _ops;

        Group(final boolean isAnd) {
            _isAnd = isAnd;
            _ops = new ArrayList<>();
        }

        public boolean apply(final TodoTxtTask task) {
            boolean result = true;
            for (final Query o : _ops) {
                final boolean opRes = o.apply(task);
                if (_isAnd) {
                    result &= opRes;
                } else {
                    result |= opRes;
                }
            }
            return result;
        }

        public Group add(final List<Query> ops) {
            for (Query op : ops) {
                if (op != null) {
                    _ops.add(op);
                }
            }
            return this;
        }

        public Group add(final Query op) {
            return add(Collections.singletonList(op));
        }

        public Group add(final Group other) {
            return add(other._ops);
        }
    }

    public static Query parseProject(final Object obj) {
        if (obj == null) {
            return t -> t.getProjects().length == 0;
        } else if (obj instanceof String) {
            return t -> Arrays.asList(t.getProjects()).contains(obj);
        } else {
            return null;
        }
    }

    public static Query parseContext(final Object obj) {
        if (obj == null) {
            return t -> t.getContexts().length == 0;
        } else if (obj instanceof String) {
            return t -> Arrays.asList(t.getContexts()).contains(obj);
        } else {
            return null;
        }
    }

    public static Query parsePriority(final Object obj) {
        if (obj == null) {
            return t -> t.getPriority() == TodoTxtTask.PRIORITY_NONE;
        } else if (obj instanceof String && ((String) obj).matches("^[A-Z]$")) {
            return t -> t.getPriority() == ((String) obj).charAt(0);
        } else {
            return null;
        }
    }

    public static Query parseDue(final Object obj) {
        if (obj == null) {
            return t -> t.getDueStatus() == TodoTxtTask.TodoDueState.NONE;
        } else if (obj instanceof String) {
            switch ((String) obj) {
                case DUE_FUTURE:
                    return t -> t.getDueStatus() == TodoTxtTask.TodoDueState.FUTURE;
                case DUE_TODAY:
                    return t -> t.getDueStatus() == TodoTxtTask.TodoDueState.TODAY;
                case DUE_OVERDUE:
                    return t -> t.getDueStatus() == TodoTxtTask.TodoDueState.OVERDUE;
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    public static Query parseDone(final Object obj) {
        if (obj instanceof Boolean) {
            return t -> t.isDone() == (Boolean) obj;
        } else {
            return null;
        }
    }

    private static Object getValue(final Object obj, final String key) {
        if (key != null && obj instanceof JSONObject) {
            try {
                return ((JSONObject) obj).get(key);
            } catch (JSONException e) {
                return null;
            }
        }
        return null;
    }

    private static Query parseOp(final String op, final Object obj){

        // checking things and calling appropriate code
        switch (op) {
            case OR:
            case AND:
                if (!(obj instanceof JSONObject)) return null;

                final Group group = new Group(op.equals(AND));
                for (Iterator<String> it = ((JSONObject) obj).keys(); it.hasNext(); ) {
                    final String k = it.next();
                    group.add(parseOp(k, getValue(obj, k)));
                }
                return group;

            case NOT:

                // Must me object with length == 1
                if (!(obj instanceof JSONObject) || !(((JSONObject) obj).length() == 1)) {
                    return null;
                }

                final String k = ((JSONObject) obj).keys().next();
                final Query sub = parseOp(k, getValue(obj, k));
                if (sub == null) {
                    return null;
                } else {
                    return t -> !sub.apply(t);
                }

            case PROJECT:
                return parseProject(obj);
            case CONTEXT:
                return parseContext(obj);
            case PRIORITY:
                return parsePriority(obj);
            case DUE:
                return parseDue(obj);
            case DONE:
                return parseDone(obj);
        }
        return null;
    }

    public static Query parse (final String string) {
        try {
            final JSONObject top = new JSONObject(string.trim());
            if (top.length() != 1) {
                return null;
            } else {

            }

            return parseOp()
        } catch (JSONException e) {
            return null;
        }

    }
}
