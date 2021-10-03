package net.gsantner.markor.format.todotxt;

import android.util.Pair;

import net.gsantner.opoc.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TodoTxtQuery {
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
            _ops.addAll(ops);
            return this;
        }

        public Group add(final Query op) {
            return add(Collections.singletonList(op));
        }

        public Group add(final Group other) {
            return add(other._ops);
        }
    }

    private static class Not implements Query
    {
        public final Query _q;
        Not(Query q) {
            _q = q;
        }

        boolean apply(TodoTxtTask task) {
            return !_q.apply(task);
        }

    }

    private

    private static TodoTxtQueryTest parse(String query) {
        // Step 1 - normalize
        query = query.trim();
        query = query.replaceAll("\\s+", " "); // Replace space groups with with space
        query = query.replaceAll("!\\s*", "!"); // No space after not
        query = query.replaceAll("\"\"", ""); // Empty strings
        query = query.replaceAll("!\\s", ""); // Empty not statements

        final Group group = new Group();

        // Step 2 - Do parenthesis
        final List<Pair<Integer, Integer>> parens = topParens(query);

        // Whole thing is one list
        if (parens.size() == 0) {
        }

        return group;
    }

    private static List<Group.Op> parseAtoms(final String query) {
        final String[] parts = query.split(" ");

        Character join = null;
        String atom = null;
        for (final String part : parts) {
            if (part.equals("|") || part.equals("&"))  {
                join = part.charAt(0);
                atom = "";
            } else if {


            }
        }
    }

    private static TodoTxtQueryTest parseAtom(String atom) {

    }

    // Get pairs of top level parens
    private static List<Pair<Integer, Integer>> topParens(final String query) {
        final List<Pair<Integer, Integer>> pairs = new ArrayList<>();

        int start = -1, depth = 0;
        for (int i = 0; i < query.length(); i++) {
            final char c = query.charAt(i);
            if (c == '(') {
                if (start < 0) {
                    start = i;
                    depth = 0;
                } else {
                    depth++;
                }
            } else if (c == ')') {
                if (start < 0) return null;
                if (depth == 0) {
                    pairs.add(Pair.create(start, i));
                    start = -1;
                } else {
                    depth--;
                }
            }
        }
        if (depth != 0) return null;
        return pairs;
    }

}
