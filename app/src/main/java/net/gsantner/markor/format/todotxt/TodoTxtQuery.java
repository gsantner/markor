package net.gsantner.markor.format.todotxt;

import android.telecom.Call;

import net.gsantner.opoc.util.Callback;

import org.json.JSONArray;
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

    public static Query parse(final String op, final Object obj) throws Exception {

        switch (op) {
            case OR:
            case AND:
                if (!(obj instanceof JSONObject)) return null;

                final Group group = new Group(op.equals(AND));
                for (Iterator<String> it = ((JSONObject) obj).keys(); it.hasNext(); ) {
                    final String k = it.next();
                    group.add(parse(k, ((JSONObject) obj).get(k)));
                }
                return group;

            case NOT:
                if (!(obj instanceof JSONObject)) return null;

                // Value _must_ be object
                final String key = ((JSONObject) obj).keys().next();
                final Query sub = parse(key, ((JSONObject) obj).get(key));
                if (sub == null) return null;
                else return t -> !sub.apply(t);

            case PROJECT:
                if (obj == null) return t -> t.getProjects().length == 0;
                else if (obj instanceof String)
                    return t -> Arrays.asList(t.getProjects()).contains((String) obj);
            case CONTEXT:
                if (obj == null) return t -> t.getContexts().length == 0;
                else if (obj instanceof String)
                    return t -> Arrays.asList(t.getContexts()).contains((String) obj);
            case PRIORITY:
                if (obj == null) return t -> t.getPriority() == TodoTxtTask.PRIORITY_NONE;
                else if (obj instanceof String && ((String) obj).length() == 1)
                    return t -> t.getPriority() == ((String) obj).charAt(0);
            case DUE:
                if (obj == null) return t -> t.getDueStatus() == TodoTxtTask.TodoDueState.NONE;
                else if (obj instanceof String) {
                    switch ((String) obj) {
                        case DUE_FUTURE:
                            return t -> t.getDueStatus() == TodoTxtTask.TodoDueState.FUTURE;
                        case DUE_TODAY:
                            return t -> t.getDueStatus() == TodoTxtTask.TodoDueState.TODAY;
                        case DUE_OVERDUE:
                            return t -> t.getDueStatus() == TodoTxtTask.TodoDueState.OVERDUE;
                    }
                }
        }
        return null;
    }
}
