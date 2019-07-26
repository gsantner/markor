package net.gsantner.opoc.util;


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


/*
 * THIS CLASS IS PROVIDED TO THE PUBLIC DOMAIN FOR FREE WITHOUT ANY
 * RESTRICTIONS OR ANY WARRANTY.
 */

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

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
    private EditHistory mEditHistory;

    /**
     * The change listener.
     */
    private EditTextChangeListener mChangeListener;

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
        if (mTextView != null) {
            mTextView.removeTextChangedListener(mChangeListener);
        }
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
        mTextView.removeTextChangedListener(mChangeListener);
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
        return (mEditHistory.mmPosition > 0);
    }

    /**
     * Perform undo.
     */
    public void undo() {
        EditItem edit = mEditHistory.getPrevious();
        if (edit == null) {
            return;
        }

        Editable text = mTextView.getEditableText();
        int start = edit.mmStart;
        int end = start + (edit.mmAfter != null ? edit.mmAfter.length() : 0);

        mIsUndoOrRedo = true;
        text.replace(start, end, edit.mmBefore);
        mIsUndoOrRedo = false;

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
            text.removeSpan(o);
        }

        Selection.setSelection(text, edit.mmBefore == null ? start
                : (start + edit.mmBefore.length()));
    }

    /**
     * Can redo be performed?
     */
    public boolean getCanRedo() {
        return (mEditHistory.mmPosition < mEditHistory.mmHistory.size());
    }

    /**
     * Perform redo.
     */
    public void redo() {
        EditItem edit = mEditHistory.getNext();
        if (edit == null) {
            return;
        }

        Editable text = mTextView.getEditableText();
        int start = edit.mmStart;
        int end = start + (edit.mmBefore != null ? edit.mmBefore.length() : 0);

        mIsUndoOrRedo = true;
        text.replace(start, end, edit.mmAfter);
        mIsUndoOrRedo = false;

        // This will get rid of underlines inserted when editor tries to come
        // up with a suggestion.
        for (Object o : text.getSpans(0, text.length(), UnderlineSpan.class)) {
            text.removeSpan(o);
        }

        Selection.setSelection(text, edit.mmAfter == null ? start
                : (start + edit.mmAfter.length()));
    }

    /**
     * Store preferences.
     */
    public void storePersistentState(Editor editor, String prefix) {
        // Store hash code of text in the editor so that we can check if the
        // editor contents has changed.
        editor.putString(prefix + ".hash",
                String.valueOf(mTextView.getText().toString().hashCode()));
        editor.putInt(prefix + ".maxSize", mEditHistory.mmMaxHistorySize);
        editor.putInt(prefix + ".position", mEditHistory.mmPosition);
        editor.putInt(prefix + ".size", mEditHistory.mmHistory.size());

        int i = 0;
        for (EditItem ei : mEditHistory.mmHistory) {
            String pre = prefix + "." + i;

            editor.putInt(pre + ".start", ei.mmStart);
            editor.putString(pre + ".before", ei.mmBefore.toString());
            editor.putString(pre + ".after", ei.mmAfter.toString());

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

        if (Integer.valueOf(hash) != mTextView.getText().toString().hashCode()) {
            return false;
        }

        mEditHistory.clear();
        mEditHistory.mmMaxHistorySize = sp.getInt(prefix + ".maxSize", -1);

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
            mEditHistory.add(new EditItem(start, before, after));
        }

        mEditHistory.mmPosition = sp.getInt(prefix + ".position", -1);
        return mEditHistory.mmPosition != -1;
    }

    // =================================================================== //

    /**
     * Keeps track of all the edit history of a text.
     */
    private final class EditHistory {

        /**
         * The position from which an EditItem will be retrieved when getNext()
         * is called. If getPrevious() has not been called, this has the same
         * value as mmHistory.size().
         */
        private int mmPosition = 0;

        /**
         * Maximum undo history size.
         */
        private int mmMaxHistorySize = -1;

        /**
         * The list of edits in chronological order.
         */
        private final LinkedList<EditItem> mmHistory = new LinkedList<EditItem>();

        /**
         * Clear history.
         */
        private void clear() {
            mmPosition = 0;
            mmHistory.clear();
        }

        /**
         * Adds a new edit operation to the history at the current position. If
         * executed after a call to getPrevious() removes all the future history
         * (elements with positions >= current history position).
         */
        private void add(EditItem item) {
            while (mmHistory.size() > mmPosition) {
                mmHistory.removeLast();
            }
            mmHistory.add(item);
            mmPosition++;

            if (mmMaxHistorySize >= 0) {
                trimHistory();
            }
        }

        /**
         * Set the maximum history size. If size is negative, then history size
         * is only limited by the device memory.
         */
        private void setMaxHistorySize(int maxHistorySize) {
            mmMaxHistorySize = maxHistorySize;
            if (mmMaxHistorySize >= 0) {
                trimHistory();
            }
        }

        /**
         * Trim history when it exceeds max history size.
         */
        private void trimHistory() {
            while (mmHistory.size() > mmMaxHistorySize) {
                mmHistory.removeFirst();
                mmPosition--;
            }

            if (mmPosition < 0) {
                mmPosition = 0;
            }
        }

        /**
         * Traverses the history backward by one position, returns and item at
         * that position.
         */
        private EditItem getPrevious() {
            if (mmPosition == 0) {
                return null;
            }
            mmPosition--;
            return mmHistory.get(mmPosition);
        }

        /**
         * Traverses the history forward by one position, returns and item at
         * that position.
         */
        private EditItem getNext() {
            if (mmPosition >= mmHistory.size()) {
                return null;
            }

            EditItem item = mmHistory.get(mmPosition);
            mmPosition++;
            return item;
        }
    }

    /**
     * Represents the changes performed by a single edit operation.
     */
    private final class EditItem {
        private final int mmStart;
        private final CharSequence mmBefore;
        private final CharSequence mmAfter;

        /**
         * Constructs EditItem of a modification that was applied at position
         * start and replaced CharSequence before with CharSequence after.
         */
        public EditItem(int start, CharSequence before, CharSequence after) {
            mmStart = start;
            mmBefore = before;
            mmAfter = after;
        }
    }

    /**
     * Class that listens to changes in the text.
     */
    private final class EditTextChangeListener implements TextWatcher {

        /**
         * The text that will be removed by the change event.
         */
        private CharSequence mBeforeChange;

        /**
         * The text that was inserted by the change event.
         */
        private CharSequence mAfterChange;

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            if (mIsUndoOrRedo) {
                return;
            }

            mBeforeChange = s.subSequence(start, start + count);
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            if (mIsUndoOrRedo) {
                return;
            }

            mAfterChange = s.subSequence(start, start + count);
            mEditHistory.add(new EditItem(start, mBeforeChange, mAfterChange));
        }

        public void afterTextChanged(Editable s) {
        }
    }
}
