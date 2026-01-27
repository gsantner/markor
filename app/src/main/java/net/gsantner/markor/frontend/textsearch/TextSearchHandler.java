package net.gsantner.markor.frontend.textsearch;

import android.text.Editable;
import android.widget.EditText;

import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSearchHandler {
    private final ArrayList<Match> matches = new ArrayList<>();
    private int currentIndex;

    private boolean matchCase = false;
    private boolean matchWholeWord = false;
    private boolean useRegex = false;
    private boolean findInSelection = false;
    private boolean preserveCase = false;

    private SearchResultChangedListener resultChangedListener;

    public interface SearchResultChangedListener {
        void onResultChanged(int current, int count);
    }

    public void find(HighlightingEditor editText, String target, int activeIndex) {
        matches.clear();
        if (target.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        Editable editable = editText.getText();
        if (editable == null) {
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

        if (isMatchCase()) {
            pattern = Pattern.compile(target);
        } else {
            pattern = Pattern.compile(target, Pattern.CASE_INSENSITIVE);
        }

        if (isFindInSelection()) {
            if (selection.isSelected()) {
                CharSequence subCharSequence = editable.subSequence(selection.getStartIndex(), selection.getEndIndex());
                Matcher matcher = pattern.matcher(subCharSequence);
                loadMatches(matcher, selection.getStartIndex());
            }
        } else {
            Matcher matcher = pattern.matcher(editable);
            loadMatches(matcher, 0);
        }
        // End searching

        calculateCurrentIndex(activeIndex);
        if (currentIndex >= 0) {
            matches.get(currentIndex).useActiveMatchColor();
            highlightMatches(editText);
            jump(editText, currentIndex);
        }
        resultChangedListener.onResultChanged(currentIndex + 1, matches.size());
    }

    private void loadMatches(Matcher matcher, int selectionStart) {
        while (matcher.find()) {
            Match match = new Match();
            match.setStart(matcher.start() + selectionStart);
            match.setEnd(matcher.end() + selectionStart);
            match.useMatchColor();
            matches.add(match);
        }
    }

    private void highlightMatches(HighlightingEditor editText) {
        if (editText == null) {
            return;
        }

        // Clear old match highlight
        editText.clearSearchMatches();

        if (!matches.isEmpty()) {
            // Set new match highlight
            for (Match match : matches) {
                editText.putSearchMatch(match.spanGroup);
            }
            editText.addSearchMatches();
        }

        // Apply
        // editText.applyDynamicHighlight();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    private void calculateCurrentIndex(int activeIndex) {
        if (!matches.isEmpty()) {
            if (activeIndex < 0) {
                currentIndex = getNearbyMatchIndex(selection, matches);
            } else if (activeIndex == 0) {
                currentIndex = 0;
            } else if (activeIndex < matches.size()) {
                currentIndex = activeIndex;
            } else {
                currentIndex = matches.size() - 1;
            }
        } else {
            currentIndex = -1;
        }
    }

    private int getNearbyMatchIndex(Selection selection, ArrayList<Match> matches) {
        final int size = matches.size();
        if (size == 0) {
            return -1;
        }

        int i = 0;
        for (; i < size; i++) {
            Match match = matches.get(i);
            if (match.getStart() > selection.getStartIndex()) {
                if (i > 0) {
                    Match lastMatch = matches.get(i - 1);
                    if (selection.getStartIndex() - lastMatch.getEnd() < match.getStart() - selection.getEndIndex()) {
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

        resultChangedListener.onResultChanged(currentIndex + 1, size);

        TextViewUtils.showSelection(editText, match.getStart());
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
            // Clear active highlight for current active match
            matches.get(currentIndex).useMatchColor();

            // Apply active highlight for previous match
            Match match = matches.get(--currentIndex);
            match.useActiveMatchColor();

            resultChangedListener.onResultChanged(currentIndex + 1, size);

            TextViewUtils.showSelection(editText, match.getStart());
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
            // Clear active highlight for current active match
            matches.get(currentIndex).useMatchColor();

            // Apply active highlight for next match
            Match match = matches.get(++currentIndex);
            match.useActiveMatchColor();

            resultChangedListener.onResultChanged(currentIndex + 1, size);

            TextViewUtils.showSelection(editText, match.getStart());
            editText.applyDynamicHighlight();
        } else {
            matches.get(size - 1).useMatchColor();
            jump(editText, 0);
        }
    }

    public int replace(HighlightingEditor editText, String replacement) {
        if (editText == null || matches.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
            return 0;
        }

        if (isPreserveCase() && !replacement.isEmpty()) {
            char c = replacement.charAt(0);
            if (Character.isLetter(c)) {
                replacement = Character.toLowerCase(c) + (replacement.substring(1));
            }
        }

        Match currentMatch = matches.get(currentIndex);

        // Replace
        Editable editable = editText.getText();
        if (editable != null) {
            editable.replace(currentMatch.getStart(), currentMatch.getEnd(), replacement);
        }

        // Shift matches after replacing
        matches.remove(currentMatch);
        editText.removeSearchMatch(currentMatch.spanGroup);
        final int size = matches.size();
        final int offset = replacement.length() - currentMatch.getLength();
        for (int i = currentIndex; i < size; i++) {
            Match o = matches.get(i);
            // Shift index by offset
            o.shiftStart(offset);
            o.shiftEnd(offset);
        }

        if (size > 0) {
            if (currentIndex == size) currentIndex--;
            matches.get(currentIndex).useActiveMatchColor();
            editText.applyDynamicHighlight();
        }

        editText.applyDynamicHighlight();

        resultChangedListener.onResultChanged(currentIndex + 1, matches.size());
        return matches.size();
    }

    public void replaceAll(HighlightingEditor editText, String replacement) {
        int result = replace(editText, replacement);
        while (result != 0) {
            result = replace(editText, replacement);
        }
    }

    private final Selection selection = new Selection();

    public void clearSelection(HighlightingEditor editText, boolean force) {
        if (force || selection.isSelected()) {
            editText.clearSearchSelection();
            selection.reset();
        }
    }

    public void handleSelection(HighlightingEditor editText, EditText searchEditText) {
        clearSelection(editText, false);

        selection.setStartIndex(editText.getSelectionStart());
        selection.setEndIndex(editText.getSelectionEnd());

        Editable editable = editText.getText();
        if (selection.isSelected() && editable != null) {
            if (isFindInSelection()) {
                editText.addSearchSelection(selection.getStartIndex(), selection.getEndIndex(), selection.color);
            } else if (searchEditText != null) {
                String target = editable.subSequence(selection.getStartIndex(), selection.getEndIndex()).toString();
                if (!target.isEmpty()) {
                    searchEditText.setText(target);
                }
            }
        }

        editText.applyDynamicHighlight();
    }

    public boolean isFindInSelection() {
        return findInSelection;
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

    public void setFindInSelection(boolean findInSelection) {
        this.findInSelection = findInSelection;
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
