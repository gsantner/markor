package net.gsantner.markor.frontend.search;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OccurrenceHandler {
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

    public void find(EditText editText, String target) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        currentIndex = 0;
        occurrences.clear();

        if (target.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
        } else {
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
                    CharSequence subCharSequence = editText.getText().subSequence(selection.getStartIndex(), selection.getEndIndex());
                    Matcher matcher = pattern.matcher(subCharSequence);
                    loadOccurrences(matcher, selection.getStartIndex());
                }
            } else {
                Matcher matcher = pattern.matcher(editText.getText());
                loadOccurrences(matcher, 0);
            }

            resultChangedListener.onResultChanged(currentIndex + 1, occurrences.size());
        }

        highlightAll(editText);
        jump(editText, 0);
    }

    private void loadOccurrences(Matcher matcher, int selectionStart) {
        while (matcher.find()) {
            Occurrence occurrence = new Occurrence();
            occurrence.setStartIndex(matcher.start() + selectionStart);
            occurrence.setEndIndex(matcher.end() + selectionStart);
            occurrences.add(occurrence);
        }
    }

    private void clearOccurrenceSpans(SpannableStringBuilder spannableStringBuilder) {
        Occurrence.BackgroundColorSpan[] styleSpans = spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), Occurrence.BackgroundColorSpan.class);
        for (Occurrence.BackgroundColorSpan span : styleSpans) {
            spannableStringBuilder.removeSpan(span);
        }
    }

    private final Selection selection = new Selection();

    private SpannableStringBuilder clearSelection(EditText editText, boolean force) {
        SpannableStringBuilder spannableStringBuilder = null;
        if (force) {
            spannableStringBuilder = new SpannableStringBuilder(editText.getText());
            Selection.BackgroundColorSpan[] styleSpans = spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), Selection.BackgroundColorSpan.class);
            for (Selection.BackgroundColorSpan span : styleSpans) {
                spannableStringBuilder.removeSpan(span);
            }
            selection.reset();
        } else if (selection.isSelected()) {
            spannableStringBuilder = new SpannableStringBuilder(editText.getText());
            spannableStringBuilder.removeSpan(selection.getBackgroundColorSpan());
            selection.reset();
        }

        return spannableStringBuilder;
    }

    public void handleSelection(EditText editText, EditText searchEditText) {
        SpannableStringBuilder spannableStringBuilder = clearSelection(editText, false);

        selection.setStartIndex(editText.getSelectionStart());
        selection.setEndIndex(editText.getSelectionEnd());

        if (selection.isSelected()) {
            if (isFindInSelection()) {
                if (spannableStringBuilder == null) {
                    spannableStringBuilder = new SpannableStringBuilder(editText.getText());
                }
                spannableStringBuilder.setSpan(selection.createBackgroundColorSpan(), selection.getStartIndex(), selection.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (searchEditText != null) {
                String target = editText.getText().subSequence(selection.getStartIndex(), selection.getEndIndex()).toString();
                if (!target.isEmpty()) {
                    searchEditText.setText(target);
                }
            }
        }

        if (spannableStringBuilder != null) {
            editText.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE);
        }
    }

    private void highlightAll(EditText editText) {
        if (editText == null) {
            return;
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editText.getText());

        // Clear old occurrence spans
        clearOccurrenceSpans(spannableStringBuilder);

        if (!occurrences.isEmpty()) {
            // Set new occurrence spans
            for (Occurrence occurrence : occurrences) {
                spannableStringBuilder.setSpan(occurrence.createBackgroundColorSpan(), occurrence.getStartIndex(), occurrence.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        editText.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE);
    }

    public void jump(EditText editText, int index) {
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

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editText.getText());

        // Remove special highlight for current occurrence
        Occurrence currentOccurrence = occurrences.get(currentIndex);
        Occurrence.BackgroundColorSpan span = currentOccurrence.getBackgroundColorSpan();
        if (span != null) {
            spannableStringBuilder.removeSpan(span);
        }

        // Set special highlight for specified occurrence
        currentIndex = index;
        Occurrence occurrence = occurrences.get(currentIndex);
        spannableStringBuilder.setSpan(occurrence.createSpecialBackgroundColorSpan(), occurrence.getStartIndex(), occurrence.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        editText.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE);

        resultChangedListener.onResultChanged(currentIndex + 1, size);
    }

    public void next(EditText editText) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        int size = occurrences.size();
        if (size == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        if (currentIndex > size - 2) {
            return;
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editText.getText());

        // 1. Get current occurrence
        Occurrence currentOccurrence = occurrences.get(currentIndex);
        // 1.1 Remove special highlight for current occurrence
        spannableStringBuilder.removeSpan(currentOccurrence.getBackgroundColorSpan());
        // 1.2 Set normal highlight for current occurrence
        spannableStringBuilder.setSpan(currentOccurrence.createBackgroundColorSpan(), currentOccurrence.getStartIndex(), currentOccurrence.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 2. Get next occurrence
        Occurrence occurrence = occurrences.get(++currentIndex);
        // 2.1 Remove normal highlight for next occurrence
        spannableStringBuilder.removeSpan(occurrence.getBackgroundColorSpan());
        // 2.2 Set special highlight for next occurrence
        spannableStringBuilder.setSpan(occurrence.createSpecialBackgroundColorSpan(), occurrence.getStartIndex(), occurrence.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        editText.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE);

        resultChangedListener.onResultChanged(currentIndex + 1, size);
    }

    public void previous(EditText editText) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }

        int size = occurrences.size();
        if (size == 0) {
            resultChangedListener.onResultChanged(0, 0);
            return;
        }
        if (currentIndex < 1) {
            return;
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editText.getText());

        // 1. Get current occurrence
        Occurrence currentOccurrence = occurrences.get(currentIndex);
        // 1.1 Remove special highlight for current occurrence
        spannableStringBuilder.removeSpan(currentOccurrence.getBackgroundColorSpan());
        // 1.2 Set normal highlight for current occurrence
        spannableStringBuilder.setSpan(currentOccurrence.createBackgroundColorSpan(), currentOccurrence.getStartIndex(), currentOccurrence.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 2. Get previous occurrence
        Occurrence occurrence = occurrences.get(--currentIndex);
        // 2.1 Remove normal highlight for previous occurrence
        spannableStringBuilder.removeSpan(occurrence.getBackgroundColorSpan());
        // 2.2 Set special highlight for previous occurrence
        spannableStringBuilder.setSpan(occurrence.createSpecialBackgroundColorSpan(), occurrence.getStartIndex(), occurrence.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        editText.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE);

        resultChangedListener.onResultChanged(currentIndex + 1, size);
    }

    public int replace(EditText editText, String replacement) {
        if (editText == null) {
            resultChangedListener.onResultChanged(0, 0);
            return 0;
        }

        if (occurrences.isEmpty()) {
            resultChangedListener.onResultChanged(0, 0);
            return 0;
        }

        if (isPreserveCase() && !replacement.isEmpty()) {
            char c = replacement.charAt(0);
            if (Character.isLetter(c)) {
                replacement = Character.toLowerCase(c) + (replacement.substring(1));
            }
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(editText.getText());
        Occurrence currentOccurrence = occurrences.get(currentIndex);

        // Replace
        spannableStringBuilder.replace(currentOccurrence.getStartIndex(), currentOccurrence.getEndIndex(), replacement);

        // Remove target occurrence span
        spannableStringBuilder.removeSpan(currentOccurrence.getBackgroundColorSpan());

        // Adjust occurrences after replacement
        for (int i = currentIndex; i < occurrences.size(); i++) {
            Occurrence o = occurrences.get(i);
            int offset = replacement.length() - currentOccurrence.getLength();
            o.offsetStartIndex(offset);
            o.offsetEndIndex(offset);
        }
        occurrences.remove(currentOccurrence);

        // Highlight next
        int size = occurrences.size();
        if (size != 0) {
            if (currentIndex == size) {
                currentIndex--;
            }

            Occurrence nextOccurrence = occurrences.get(currentIndex);

            // Clear occurrence highlight
            Occurrence.BackgroundColorSpan span = nextOccurrence.getBackgroundColorSpan();
            if (span != null) {
                spannableStringBuilder.removeSpan(span);
            }

            // Set special highlight
            spannableStringBuilder.setSpan(nextOccurrence.createSpecialBackgroundColorSpan(), nextOccurrence.getStartIndex(), nextOccurrence.getEndIndex(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        editText.setText(spannableStringBuilder);

        resultChangedListener.onResultChanged(currentIndex + 1, size);
        return size;
    }

    public void replaceAll(EditText editText, String replacement) {
        int result = replace(editText, replacement);
        while (result != 0) {
            result = replace(editText, replacement);
        }
    }

    // Getters
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

    // Setters
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
