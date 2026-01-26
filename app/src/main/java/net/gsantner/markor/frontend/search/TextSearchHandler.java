package net.gsantner.markor.frontend.search;

import android.text.Editable;
import android.widget.EditText;

import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSearchHandler {
    private final ArrayList<Occurrence> occurrences = new ArrayList<>();
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
        occurrences.clear();
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
                        loadOccurrences(matcher, selection.getStartIndex());
                    }
                } else {
                    Matcher matcher = pattern.matcher(editable);
                    loadOccurrences(matcher, 0);
                }
            }

            resultChangedListener.onResultChanged(currentIndex + 1, occurrences.size());
        }

        highlightAll(editText);
    }

    private void loadOccurrences(Matcher matcher, int selectionStart) {
        while (matcher.find()) {
            Occurrence occurrence = new Occurrence();
            occurrence.setStart(matcher.start() + selectionStart);
            occurrence.setEnd(matcher.end() + selectionStart);
            occurrence.applyNormalColor();
            occurrences.add(occurrence);
        }
    }

    private int getNearbyOccurrenceIndex(Selection selection, ArrayList<Occurrence> occurrences) {
        final int size = occurrences.size();
        if (size == 0) {
            return -1;
        }

        int i = 0;
        for (; i < size; i++) {
            Occurrence occurrence = occurrences.get(i);
            if (occurrence.getStart() > selection.getStartIndex()) {
                if (i > 0) {
                    Occurrence lastOccurrence = occurrences.get(i - 1);
                    if (selection.getStartIndex() - lastOccurrence.getEnd() < occurrence.getStart() - selection.getEndIndex()) {
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

        // Clear old occurrence highlight
        editText.clearAdditionalOccurrences();

        if (!occurrences.isEmpty()) {
            // Set new occurrence highlight
            for (Occurrence occurrence : occurrences) {
                editText.addAdditionalOccurrence(occurrence.getStart(), occurrence.getEnd(), occurrence.color);
            }
        }

        // Apply
        editText.applyAdditionalOccurrences();
    }

    public void jump(HighlightingEditor editText, int index) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        int size = occurrences.size();
        if (size == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        if (index < 0 || index > size - 1) {
            return;
        }

        // Clear focused highlight for last occurrence
        editText.clearAdditionalFocus();

        // Apply focused highlight for current occurrence
        currentIndex = index;
        try {
            Occurrence occurrence = occurrences.get(currentIndex).clone();
            occurrence.applyFocusedColor();
            editText.addAdditionalFocus(occurrence.getStart(), occurrence.getEnd(), occurrence.color);

            resultChangedListener.onResultChanged(currentIndex + 1, size);

            TextViewUtils.showSelection(editText, occurrence.getStart());

            editText.applyAdditionalFocus();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void jumpToNearestOccurrence(HighlightingEditor editText) {
        int index = getNearbyOccurrenceIndex(selection, occurrences);
        if (index > -1) {
            jump(editText, index);
            TextViewUtils.showSelection(editText, occurrences.get(index).getStart());
        }
    }

    public void next(HighlightingEditor editText) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        int size = occurrences.size();
        if (size == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        if (currentIndex > -1 && currentIndex < size - 1) {
            // Clear focused highlight for current occurrence
            editText.clearAdditionalFocus();

            try {
                // Apply focused highlight for next occurrence
                Occurrence occurrence = occurrences.get(++currentIndex).clone();
                occurrence.applyFocusedColor();
                editText.addAdditionalFocus(occurrence.getStart(), occurrence.getEnd(), occurrence.color);

                resultChangedListener.onResultChanged(currentIndex + 1, size);

                TextViewUtils.showSelection(editText, occurrence.getStart());

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

        int size = occurrences.size();
        if (size == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        if (currentIndex > 0 && currentIndex < size) {
            // Clear focused highlight for current occurrence
            editText.clearAdditionalFocus();

            // Apply focused highlight for previous occurrence
            Occurrence occurrence = occurrences.get(--currentIndex);
            occurrence.applyFocusedColor();
            editText.addAdditionalFocus(occurrence.getStart(), occurrence.getEnd(), occurrence.color);

            resultChangedListener.onResultChanged(currentIndex + 1, size);

            TextViewUtils.showSelection(editText, occurrence.getStart());

            editText.applyAdditionalFocus();
        } else {
            jump(editText, size - 1);
        }
    }

    public int replace(HighlightingEditor editText, String replacement) {
        if (editText == null || occurrences.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
            return 0;
        }

        if (isPreserveCase() && !replacement.isEmpty()) {
            char c = replacement.charAt(0);
            if (Character.isLetter(c)) {
                replacement = Character.toLowerCase(c) + (replacement.substring(1));
            }
        }

        Occurrence currentOccurrence = occurrences.get(currentIndex);

        // Replace
        Editable editable = editText.getText();
        if (editable != null) {
            editable.replace(currentOccurrence.getStart(), currentOccurrence.getEnd(), replacement);
        }

        // Adjust occurrences after replacing
        occurrences.remove(currentOccurrence);
        final int size = occurrences.size();
        final int offset = replacement.length() - currentOccurrence.getLength();
        for (int i = currentIndex; i < size; i++) {
            Occurrence o = occurrences.get(i);
            // Update index by offset
            o.offsetStart(offset);
            o.offsetEnd(offset);
        }

        highlightAll(editText);

        if (size > 0) {
            if (currentIndex == size) currentIndex--;
            try {
                editText.clearAdditionalFocus();
                Occurrence occurrence = occurrences.get(currentIndex).clone();
                occurrence.applyFocusedColor();
                editText.addAdditionalFocus(occurrence.getStart(), occurrence.getEnd(), occurrence.color);
                editText.applyAdditionalFocus();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        resultChangedListener.onResultChanged(currentIndex + 1, occurrences.size());
        return occurrences.size();
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
