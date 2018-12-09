package net.gsantner.opoc.format.todotxt.extension;


import net.gsantner.opoc.format.todotxt.SttTask;

import java.util.Comparator;
import java.util.List;

public class SttTaskComparator implements Comparator<SttTask> {
    protected String sortBy;
    protected boolean isAscending;

    public SttTaskComparator() {
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String value) {
        sortBy = value;
    }

    public boolean getAscending() {
        return isAscending;
    }

    public void setAscending(boolean value) {
        isAscending = value;
    }

    @Override
    public int compare(SttTask x, SttTask y) {
        int difference = 0;
        switch (sortBy) {
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
            default: {
                return 0;
            }
        }
        if (getAscending()) difference = -1 * difference;
        return difference;
    }

    private int compare(char x, char y) {
        if (Character.toLowerCase(y) < Character.toLowerCase(x))
            return 1;
        if (Character.toLowerCase(y) == Character.toLowerCase(x))
            return 0;
        return -1;
    }

    private int compare(List<String> x, List<String> y) {
        if (x.isEmpty() & y.isEmpty()) return 0;
        if (x.isEmpty()) return 1;
        if (y.isEmpty()) return -1;
        return x.get(0).compareTo(y.get(0));

    }
}
