package net.gsantner.markor.frontend.textsearch;

import android.text.Editable;

import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.frontend.textview.TextViewUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextSearchHandler {
    private final ArrayList<Match> matches = new ArrayList<>();
    private int currentIndex;

    private boolean matchCase = false;
    private boolean matchWholeWord = false;
    private boolean useRegex = false;
    private boolean preserveCase = false;

    private SearchResultChangedListener resultChangedListener;

    public static final int ACTIVE_INDEX_KEEP = -1;
    public static final int ACTIVE_INDEX_NEARBY = -2;

    public static final int RESULT_BAD_PATTERN = -1;

    private Matcher matcher;

    public interface SearchResultChangedListener {
        default void onResultChanged(int active, int count) {
            onResultChanged(active, count, null);
        }

        void onResultChanged(int active, int count, String msg);
    }

    public void find(HighlightingEditor editText, String target, int activeIndex) {
        matches.clear();
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        editText.clearSearchMatches();
        if (target.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        Editable editable = editText.getText();
        if (editable == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        } else if (editable.length() == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        // Start searching
        Pattern pattern;
        if (!isUseRegex()) {
            target = Pattern.quote(target);
        }

        if (isMatchWholeWord()) {
            target = "\\b" + target + "\\b";
        }

        try {
            if (isMatchCase()) {
                pattern = Pattern.compile(target, Pattern.MULTILINE);
            } else {
                pattern = Pattern.compile(target, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            }
        } catch (PatternSyntaxException e) {
            resultChangedListener.onResultChanged(0, RESULT_BAD_PATTERN, e.getMessage());
            return;
        }

        matcher = pattern.matcher(editable);
        loadMatches(matcher);
        // End searching

        calculateCurrentIndex(activeIndex, editText.getSelectionStart());
        if (!matches.isEmpty()) {
            matches.get(currentIndex).useActiveMatchColor();
            highlightMatches(editText);
            jump(editText, currentIndex);
        }
        resultChangedListener.onResultChanged(currentIndex, matches.size());
    }

    private void loadMatches(Matcher matcher) {
        while (matcher.find()) {
            Match match = new Match();
            match.setStart(matcher.start());
            match.setEnd(matcher.end());
            match.useMatchColor();
            matches.add(match);
        }
    }

    private void highlightMatches(HighlightingEditor editText) {
        if (editText == null) {
            return;
        }

        if (matches.isEmpty()) {
            editText.clearSearchMatches();
        } else {
            List<SyntaxHighlighterBase.SpanGroup> spanGroups = new ArrayList<>();
            for (Match match : matches) {
                spanGroups.add(match.spanGroup);
            }
            editText.setSearchMatches(spanGroups);
        }
    }

    private void calculateCurrentIndex(int activeIndex, int selectionStart) {
        if (matches.isEmpty()) {
            currentIndex = 0;
            return;
        }

        if (activeIndex < 0) {
            if (activeIndex == ACTIVE_INDEX_NEARBY) {
                currentIndex = findNearbyIndex(selectionStart, matches);
            } else if (activeIndex == ACTIVE_INDEX_KEEP) {
                currentIndex = currentIndex < matches.size() ? currentIndex : findNearbyIndex(selectionStart, matches);
            } else {
                currentIndex = 0;
            }
        } else if (activeIndex < matches.size()) {
            currentIndex = activeIndex;
        } else {
            currentIndex = matches.size() - 1;
        }
    }

    private int findNearbyIndex(int selectionStart, ArrayList<Match> matches) {
        final int size = matches.size();
        if (size == 0) return -1;

        int i = 0;
        for (; i < size; i++) {
            Match match = matches.get(i);
            if (match.getStart() > selectionStart) {
                if (i > 0) {
                    Match lastMatch = matches.get(i - 1);
                    if (selectionStart - lastMatch.getEnd() < match.getStart() - selectionStart) {
                        return i - 1;
                    } else {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        }

        return Math.max(i - 1, 0);
    }

    public void jump(HighlightingEditor editText, int index) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        int size = matches.size();
        if (size == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        if (index < 0 || index > size - 1) {
            return;
        }

        // Clear active highlight for last active match
        matches.get(index).useMatchColor();
        // Apply active highlight for current active match
        currentIndex = index;
        Match match = matches.get(currentIndex);
        match.useActiveMatchColor();

        resultChangedListener.onResultChanged(currentIndex, size);

        int start = match.getStart();
        TextViewUtils.showSelection(editText, start);
        editText.applyDynamicHighlight();
    }

    public void previous(HighlightingEditor editText) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        int size = matches.size();
        if (size == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        if (currentIndex > 0 && currentIndex < size) {
            Match currentMatch = matches.get(currentIndex);

            // Clear active highlight for current active match
            currentMatch.useMatchColor();

            // Apply active highlight for previous match
            Match match = matches.get(--currentIndex);
            match.useActiveMatchColor();

            resultChangedListener.onResultChanged(currentIndex, size);

            int start = match.getStart();
            TextViewUtils.showSelection(editText, start);
            editText.applyDynamicHighlight();
        } else {
            matches.get(0).useMatchColor();
            jump(editText, size - 1);
        }
    }

    public void next(HighlightingEditor editText) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        int size = matches.size();
        if (size == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        if (currentIndex >= 0 && currentIndex <= size - 2) {
            Match currentMatch = matches.get(currentIndex);

            // Clear active highlight for current active match
            currentMatch.useMatchColor();

            // Apply active highlight for next match
            Match match = matches.get(++currentIndex);
            match.useActiveMatchColor();

            resultChangedListener.onResultChanged(currentIndex, size);

            int start = match.getStart();
            TextViewUtils.showSelection(editText, start);
            editText.applyDynamicHighlight();
        } else {
            matches.get(size - 1).useMatchColor();
            jump(editText, 0);
        }
    }

    private String applyPreserveCase(String originalText, String replacement) {
        if (replacement.isEmpty() || originalText.isEmpty()) {
            return replacement;
        }

        boolean isAllUpperCase = true;
        boolean hasLetters = false;
        for (char c : originalText.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetters = true;
                if (Character.isLowerCase(c)) {
                    isAllUpperCase = false;
                    break;
                }
            }
        }

        if (!hasLetters) {
            isAllUpperCase = false;
        }

        if (isAllUpperCase) {
            return replacement.toUpperCase();
        }

        if (Character.isUpperCase(originalText.charAt(0))) {
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }

        return replacement;
    }

    public int replace(HighlightingEditor editText, String replacement) {
        if (editText == null || matches.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
            return 0;
        }

        Editable editable = editText.getText();
        if (editable == null) {
            resultChangedListener.onResultChanged(0, 0);
            return 0;
        }

        Match currentMatch = matches.get(currentIndex);

        if (isPreserveCase()) {
            String originalText = editable.subSequence(currentMatch.getStart(), currentMatch.getEnd()).toString();
            replacement = applyPreserveCase(originalText, replacement);
        }

        // Replace
        editable.replace(currentMatch.getStart(), currentMatch.getEnd(), replacement);

        // Shift matches after replacing
        matches.remove(currentMatch);
        editText.removeSearchMatch(currentMatch.spanGroup);
        final int size = matches.size();
        final int offset = replacement.length() - currentMatch.getLength();
        for (int i = currentIndex; i < size; i++) {
            Match match = matches.get(i);
            // Shift index by offset
            match.shiftStart(offset);
            match.shiftEnd(offset);
        }

        if (size > 0) {
            if (currentIndex == size) {
                currentIndex--;
            }
            matches.get(currentIndex).useActiveMatchColor();
        }

        editText.applyDynamicHighlight();

        resultChangedListener.onResultChanged(currentIndex, matches.size());
        return matches.size();
    }

    public void replaceAll(HighlightingEditor editText, String replacement) {
        if (editText == null || matches.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        final Editable editable = editText.getText();
        if (editable == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        // Replace all
        if (isPreserveCase()) {
            final StringBuilder result = new StringBuilder(editable);
            final int tail = matches.size() - 1;
            int i = tail;
            for (; i >= 0; i--) {
                Match match = matches.get(i);
                final int start = match.getStart();
                final int end = match.getEnd();
                String originalText = editable.subSequence(start, end).toString();
                result.replace(start, end, applyPreserveCase(originalText, replacement));
            }

            if (i < tail) {
                editable.replace(0, editable.length(), result);
            }
        } else {
            if (matcher != null) {
                editable.replace(0, editable.length(), matcher.replaceAll(replacement));
            }
        }

        // Clear
        matches.clear();
        editText.clearSearchMatches();
        editText.applyDynamicHighlight();
        resultChangedListener.onResultChanged(0, 0);
        currentIndex = 0;
    }

    public boolean isMatchCase() {
        return matchCase;
    }

    public boolean isMatchWholeWord() {
        return matchWholeWord;
    }

    public boolean isUseRegex() {
        return useRegex;
    }

    public boolean isPreserveCase() {
        return preserveCase;
    }

    public void setMatchCase(boolean matchCase) {
        this.matchCase = matchCase;
    }

    public void setMatchWholeWord(boolean matchWholeWord) {
        this.matchWholeWord = matchWholeWord;
    }

    public void setUseRegex(boolean useRegex) {
        this.useRegex = useRegex;
    }

    public void setPreserveCase(boolean preserveCase) {
        this.preserveCase = preserveCase;
    }

    public void setResultChangedListener(SearchResultChangedListener resultChangedListener) {
        this.resultChangedListener = resultChangedListener;
    }
}
