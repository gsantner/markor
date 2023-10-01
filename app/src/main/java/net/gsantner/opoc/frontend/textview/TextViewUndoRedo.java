/*
 * THIS CLASS IS PROVIDED TO THE PUBLIC DOMAIN FOR FREE WITHOUT ANY
 * RESTRICTIONS OR ANY WARRANTY.
 */

/*

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button redoBtn = (Button) findViewById(R.id.redo);
        Button undoBtn = (Button) findViewById(R.id.undo);
        EditText editText = (EditText) findViewById(R.id.edittext);
      
        // pass edittext object to TextViewUndoRedo class
        TextViewUndoRedo helper = new TextViewUndoRedo(edittext);
        
        // call the method from TextViewUndoRedo class
        redoBtn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            helper.redo(); // perform redo
          }
        });
        undoBtn.setOnClickLisener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            helper.undo(); // perform undo
          }
        });
    }
}

*/

package net.gsantner.opoc.frontend.textview;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

import net.gsantner.markor.frontend.textview.TextViewUtils;

import java.io.File;
import java.util.LinkedList;

/**
 * A generic undo/redo implementation for TextViews.
 */
public class TextViewUndoRedo {

    /**
     * Is undo/redo being performed? This member signals if an undo/redo
     * operation is currently being performed. Changes in the text during
     * undo/redo are not recorded because it would mess up the undo history.
     */
    private boolean mIsUndoOrRedo = false;

    /**
     * The edit history.
     */
    private final EditHistory mEditHistory;

    /**
     * The change listener.
     */
    private final EditTextChangeListener mChangeListener;

    /**
     * The edit text.
     */
    private TextView mTextView;

    // =================================================================== //

    /**
     * Create a new TextViewUndoRedo and attach it to the specified TextView.
     *
     * @param textView The text view for which the undo/redo is implemented.
     */
    public TextViewUndoRedo(TextView textView) {
        mTextView = textView;
        mEditHistory = new EditHistory();
        mChangeListener = new EditTextChangeListener();
        mTextView.addTextChangedListener(mChangeListener);
    }

    public void setTextView(TextView textView) {
        disconnect();
        mTextView = textView;
        mTextView.addTextChangedListener(mChangeListener);
    }

    public String undoRedoPrefKeyForFile(File file) {
        return "file-" + file.getAbsolutePath().replace("/", "-");
    }

    // =================================================================== //

    /**
     * Disconnect this undo/redo from the text view.
     */
    public void disconnect() {
        if (mTextView != null) {
            mTextView.removeTextChangedListener(mChangeListener);
        }
    }

    /**
     * Set the maximum history size. If size is negative, then history size is
     * only limited by the device memory.
     */
    public void setMaxHistorySize(int maxHistorySize) {
        mEditHistory.setMaxHistorySize(maxHistorySize);
    }

    /**
     * Clear history.
     */
    public void clearHistory() {
        mEditHistory.clear();
    }

    /**
     * Can undo be performed?
     */
    public boolean getCanUndo() {
        return (mEditHistory.position > 0);
    }

    /**
     * Perform undo.
     */
    public void undo() {
        final EditItem edit = mEditHistory.getPrevious();
        if (edit == null) {
            return;
        }

        final Editable text = mTextView.getEditableText();
        final int start = edit.start;
        final int end = start + (edit.after != null ? edit.after.length() : 0);

        mIsUndoOrRedo = true;
        text.replace(start, end, edit.before);
        mIsUndoOrRedo = false;

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
            text.removeSpan(o);
        }

        if (edit.selBefore >= 0 && edit.selBefore <= text.length()) {
            Selection.setSelection(text, edit.selBefore);
        }
    }

    /**
     * Can redo be performed?
     */
    public boolean getCanRedo() {
        return (mEditHistory.position < mEditHistory.history.size());
    }

    /**
     * Perform redo.
     */
    public void redo() {
        final EditItem edit = mEditHistory.getNext();
        if (edit == null) {
            return;
        }

        final Editable text = mTextView.getEditableText();
        final int start = edit.start;
        final int end = start + (edit.before != null ? edit.before.length() : 0);

        mIsUndoOrRedo = true;
        text.replace(start, end, edit.after);
        mIsUndoOrRedo = false;

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
            text.removeSpan(o);
        }

        if (edit.selAfter >= 0 && edit.selAfter <= text.length()) {
            Selection.setSelection(text, edit.selAfter);
        }
    }

    /**
     * Store preferences.
     */
    public void storePersistentState(Editor editor, String prefix) {
        // Store hash code of text in the editor so that we can check if the
        // editor contents has changed.
        editor.putString(prefix + ".hash",
                String.valueOf(mTextView.getText().toString().hashCode()));
        editor.putInt(prefix + ".maxSize", mEditHistory._maxHistorySize);
        editor.putInt(prefix + ".position", mEditHistory.position);
        editor.putInt(prefix + ".size", mEditHistory.history.size());

        int i = 0;
        for (EditItem ei : mEditHistory.history) {
            String pre = prefix + "." + i;

            editor.putInt(pre + ".start", ei.start);
            editor.putString(pre + ".before", ei.before.toString());
            editor.putString(pre + ".after", ei.after.toString());

            i++;
        }
    }

    /**
     * Restore preferences.
     *
     * @param prefix The preference key prefix used when state was stored.
     * @return did restore succeed? If this is false, the undo history will be
     * empty.
     */
    public boolean restorePersistentState(SharedPreferences sp, String prefix)
            throws IllegalStateException {

        boolean ok = doRestorePersistentState(sp, prefix);
        if (!ok) {
            mEditHistory.clear();
        }

        return ok;
    }

    private boolean doRestorePersistentState(SharedPreferences sp, String prefix) {

        String hash = sp.getString(prefix + ".hash", null);
        if (hash == null) {
            // No state to be restored.
            return true;
        }

        if (Integer.parseInt(hash) != mTextView.getText().toString().hashCode()) {
            return false;
        }

        mEditHistory.clear();
        mEditHistory._maxHistorySize = sp.getInt(prefix + ".maxSize", -1);

        int count = sp.getInt(prefix + ".size", -1);
        if (count == -1) {
            return false;
        }

        for (int i = 0; i < count; i++) {
            String pre = prefix + "." + i;

            int start = sp.getInt(pre + ".start", -1);
            String before = sp.getString(pre + ".before", null);
            String after = sp.getString(pre + ".after", null);

            if (start == -1 || before == null || after == null) {
                return false;
            }
            mEditHistory.add(new EditItem(start, before, after, -1, -1));
        }

        mEditHistory.position = sp.getInt(prefix + ".position", -1);
        return mEditHistory.position != -1;
    }

    // =================================================================== //

    /**
     * Keeps track of all the edit history of a text.
     */
    private static final class EditHistory {

        /**
         * The position from which an EditItem will be retrieved when getNext()
         * is called. If getPrevious() has not been called, this has the same
         * value as mmHistory.size().
         */
        private int position = 0;

        /**
         * Maximum undo history size.
         */
        private int _maxHistorySize = -1;

        /**
         * The list of edits in chronological order.
         */
        private final LinkedList<EditItem> history = new LinkedList<>();

        /**
         * Clear history.
         */
        private void clear() {
            position = 0;
            history.clear();
        }

        /**
         * Adds a new edit operation to the history at the current position. If
         * executed after a call to getPrevious() removes all the future history
         * (elements with positions >= current history position).
         */
        private void add(final EditItem item) {
            if (item == null || item.zeroChange()) {
                return;
            }

            while (history.size() > position) {
                history.removeLast();
            }
            history.add(item);
            position++;

            if (_maxHistorySize >= 0) {
                trimHistory();
            }
        }

        /**
         * Set the maximum history size. If size is negative, then history size
         * is only limited by the device memory.
         */
        private void setMaxHistorySize(int maxHistorySize) {
            _maxHistorySize = maxHistorySize;
            if (_maxHistorySize >= 0) {
                trimHistory();
            }
        }

        /**
         * Trim history when it exceeds max history size.
         */
        private void trimHistory() {
            while (history.size() > _maxHistorySize) {
                history.removeFirst();
                position--;
            }

            if (position < 0) {
                position = 0;
            }
        }

        /**
         * Traverses the history backward by one position, returns and item at
         * that position.
         */
        private EditItem getPrevious() {
            if (position == 0) {
                return null;
            }
            position--;
            return history.get(position);
        }

        /**
         * Traverses the history forward by one position, returns and item at
         * that position.
         */
        private EditItem getNext() {
            if (position >= history.size()) {
                return null;
            }

            EditItem item = history.get(position);
            position++;
            return item;
        }
    }

    /**
     * Represents the changes performed by a single edit operation.
     */
    private static final class EditItem {
        private final int start;
        private final String before;
        private final String after;

        private final int selBefore, selAfter;

        /**
         * Constructs EditItem of a modification that was applied at position
         * start and replaced CharSequence before with CharSequence after.
         */
        public EditItem(
                final int start,
                final String before,
                final String after,
                final int selBefore,
                final int selAfter
        ) {
            // Made change minimal
            final int[] diff = TextViewUtils.findDiff(before, after, 0, 0);
            this.start = start + diff[0];
            this.before = before.substring(diff[0], diff[1]);
            this.after = after.substring(diff[0], diff[2]);

            this.selBefore = selBefore;
            this.selAfter = selAfter;
        }

        public boolean zeroChange() {
            return before.equals(after);
        }

        public boolean equals(final EditItem other) {
            return other != null && start == other.start && before.equals(other.before) && after.equals(other.after);
        }
    }

    /**
     * Class that listens to changes in the text.
     */
    private final class EditTextChangeListener implements TextWatcher {

        /**
         * The text that will be removed by the change event.
         */
        private String beforeChange, afterChange;
        private boolean isInChain = false;
        private long lastTime = 0;
        private int selBefore = -1;
        private int changeStart = -1;


        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (mIsUndoOrRedo) {
                return;
            }

            beforeChange = TextViewUtils.toString(s, start, start + count);
            selBefore = Selection.getSelectionStart(s);
        }

        public void onTextChanged(final CharSequence s, int start, int before, int count) {
            if (mIsUndoOrRedo) {
                return;
            }
            afterChange = TextViewUtils.toString(s, start, start + count);
            changeStart = start;
        }

        public void afterTextChanged(final Editable s) {
            if (mIsUndoOrRedo) {
                return;
            }

            final int selAfter = Selection.getSelectionStart(s);
            final EditItem cur = new EditItem(changeStart, beforeChange, afterChange, selBefore, selAfter);

            // Sometimes autocorrect inserts a zero change. Ignore this.
            if (cur.zeroChange()) {
                return;
            }

            // Get previous and new items
            final EditItem prev;
            if (mEditHistory.position == mEditHistory.history.size()) {
                prev = mEditHistory.getPrevious();
            } else {
                prev = null;
            }

            // Sometimes the text watcher is called for the same change 2x. Ignore this.
            if (cur.equals(prev)) {
                mEditHistory.add(prev);
                return;
            }

            // 5 second time within which we will combine
            final long delta = System.currentTimeMillis() - lastTime;
            lastTime += delta;

            // Attempt to combine if conditions are met
            if (delta < 5000 && prev != null && !prev.zeroChange()) {

                final int pbl = prev.before.length();
                final int pal = prev.after.length();
                final int cbl = cur.before.length();
                final int cal = cur.after.length();

                final int newStart = Math.min(prev.start, cur.start);

                final boolean insChain = pbl == 0 && (isInChain || pal == 1);
                final boolean singleIns = cbl == 0 && cal == 1;
                final boolean insFollows = cur.start == (prev.start + pal);

                // Combine if adding multiple of same class or trailing space
                if (singleIns && insChain && insFollows) {
                    final int chainType = typeOf(prev.after.charAt(pal - 1));
                    final int insType = typeOf(cur.after.charAt(0));
                    if (chainType == insType || (chainType == CHAR && insType == SPACE)) {
                        // Not in chain if char chain followed by space
                        isInChain = chainType == insType;
                        mEditHistory.add(new EditItem(newStart, "", prev.after + cur.after, prev.selBefore, cur.selAfter));
                        return;
                    }
                }

                final boolean delChain = pal == 0 && (isInChain || pbl == 1);
                final boolean singleDel = cal == 0 && cbl == 1;
                final boolean delFollows = cur.start == (prev.start - cbl);

                // Combine if removing multiple of same class or trailing space
                if (singleDel && delChain && delFollows) {
                    final int chainType = typeOf(prev.before.charAt(0));
                    final int delType = typeOf(cur.before.charAt(0));
                    if (chainType == delType || (chainType == CHAR && delType == SPACE)) {
                        isInChain = chainType == delType;
                        mEditHistory.add(new EditItem(newStart, cur.before + prev.before, "", prev.selBefore, cur.selAfter));
                        return;
                    }
                }
            }

            // Else add both prev and cur back - null and empty handled automatically
            isInChain = false;
            mEditHistory.add(prev);
            mEditHistory.add(cur);
        }

        final int CHAR = 0, SPACE = 1, NL = 2;

        private int typeOf(final char c) {
            switch (c) {
                case '\n':
                    return NL;
                case ' ':
                    return SPACE;
                default:
                    return CHAR;
            }
        }
    }
}
