package net.gsantner.markor.frontend.textsearch;

import android.text.Editable;
import android.widget.EditText;

import net.gsantner.markor.frontend.textsearch.Selection;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSearchHandler {
    private final ArrayList<Match> matches = new ArrayList<>();
    private int currentIndex;

    private boolean findInSelection = false;
    private boolean matchCase = false;
    private boolean matchWholeWord = false;
    private boolean useRegex = false;
    private boolean preserveCase = false;

    private SearchResultChangedListener resultChangedListener;

    public interface SearchResultChangedListener {
        void onResultChanged(int current, int count);
    }

    public void find(HighlightingEditor editText, String target) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        currentIndex = 0;
        matches.clear();
        editText.clearAdditionalFocus();

        if (target.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
        } else {
            editText.clearFocus();

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

            Editable editable = editText.getText();
            if (editable != null) {
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
            }

            resultChangedListener.onResultChanged(currentIndex + 1, matches.size());
        }

        highlightAll(editText);
    }

    private void loadMatches(Matcher matcher, int selectionStart) {
        while (matcher.find()) {
            Match match = new Match();
            match.setStart(matcher.start() + selectionStart);
            match.setEnd(matcher.end() + selectionStart);
            match.applyMatchColor();
            matches.add(match);
        }
    }

    private int getNearbyMatchIndex(Selection selection, ArrayList<Match> matchs) {
        final int size = matchs.size();
        if (size == 0) {
            return -1;
        }

        int i = 0;
        for (; i < size; i++) {
            Match match = matchs.get(i);
            if (match.getStart() > selection.getStartIndex()) {
                if (i > 0) {
                    Match lastMatch = matchs.get(i - 1);
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

    private final Selection selection = new Selection();

    public void clearSelection(HighlightingEditor editText, boolean force) {
        if (force || selection.isSelected()) {
            editText.clearAdditionalSelection();
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
                editText.addAdditionalSelection(selection.getStartIndex(), selection.getEndIndex(), selection.color);
            } else if (searchEditText != null) {
                String target = editable.subSequence(selection.getStartIndex(), selection.getEndIndex()).toString();
                if (!target.isEmpty()) {
                    searchEditText.setText(target);
                }
            }
        }

        editText.applyAdditionalSelection();
    }

    private void highlightAll(HighlightingEditor editText) {
        if (editText == null) {
            return;
        }

        // Clear old match highlight
        editText.clearSearchMatches();

        if (!matches.isEmpty()) {
            // Set new match highlight
            for (Match match : matches) {
                editText.addSearchMatch(match.getStart(), match.getEnd(), match.color);
            }
        }

        // Apply
        editText.applySearchMatches();
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

        // Clear active highlight for last match
        editText.clearAdditionalFocus();

        // Apply active highlight for current match
        currentIndex = index;
        try {
            Match match = matches.get(currentIndex).clone();
            match.applyActiveMatchColor();
            editText.addAdditionalFocus(match.getStart(), match.getEnd(), match.color);

            resultChangedListener.onResultChanged(currentIndex + 1, size);

            TextViewUtils.showSelection(editText, match.getStart());

            editText.applyAdditionalFocus();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void jumpToNearestMatch(HighlightingEditor editText) {
        int index = getNearbyMatchIndex(selection, matches);
        if (index > -1) {
            jump(editText, index);
            TextViewUtils.showSelection(editText, matches.get(index).getStart());
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

        if (currentIndex > -1 && currentIndex < size - 1) {
            // Clear active highlight for current match
            editText.clearAdditionalFocus();

            try {
                // Apply active highlight for next match
                Match match = matches.get(++currentIndex).clone();
                match.applyActiveMatchColor();
                editText.addAdditionalFocus(match.getStart(), match.getEnd(), match.color);

                resultChangedListener.onResultChanged(currentIndex + 1, size);

                TextViewUtils.showSelection(editText, match.getStart());

                editText.applyAdditionalFocus();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        } else {
            jump(editText, 0);
        }
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
            // Clear active highlight for current match
            editText.clearAdditionalFocus();

            // Apply active highlight for previous match
            Match match = matches.get(--currentIndex);
            match.applyActiveMatchColor();
            editText.addAdditionalFocus(match.getStart(), match.getEnd(), match.color);

            resultChangedListener.onResultChanged(currentIndex + 1, size);

            TextViewUtils.showSelection(editText, match.getStart());

            editText.applyAdditionalFocus();
        } else {
            jump(editText, size - 1);
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

        // Adjust matchs after replacing
        matches.remove(currentMatch);
        final int size = matches.size();
        final int offset = replacement.length() - currentMatch.getLength();
        for (int i = currentIndex; i < size; i++) {
            Match o = matches.get(i);
            // Update index by offset
            o.offsetStart(offset);
            o.offsetEnd(offset);
        }

        highlightAll(editText);

        if (size > 0) {
            if (currentIndex == size) currentIndex--;
            try {
                editText.clearAdditionalFocus();
                Match match = matches.get(currentIndex).clone();
                match.applyMatchColor();
                editText.addAdditionalFocus(match.getStart(), match.getEnd(), match.color);
                editText.applyAdditionalFocus();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        resultChangedListener.onResultChanged(currentIndex + 1, matches.size());
        return matches.size();
    }

    public void replaceAll(HighlightingEditor editText, String replacement) {
        int result = replace(editText, replacement);
        while (result != 0) {
            result = replace(editText, replacement);
        }
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
