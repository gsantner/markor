/*#######################################################
 *
 *   Maintained 2017-2026 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 #########################################################*/
package net.gsantner.markor.frontend.textview;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gsantner.opoc.wrapper.GsCallback;

import java.util.List;

/**
 * Common interface for all Markor text editors (HighlightingEditor, RecyclerTextEditor, etc.).
 * This allows format-specific features (action buttons, search, highlighting) to work with
 * any editor implementation interchangeably.
 */
public interface MarkorEditor {

    String PLACE_CURSOR_HERE_TOKEN = "%%PLACE_CURSOR_HERE%%";
    String INSERT_SELECTION_HERE_TOKEN = "%%INSERT_SELECTION_HERE%%";

    // ---- Text content ----

    @Nullable
    Editable getText();

    void setText(@Nullable CharSequence text);

    int length();

    // ---- Selection / cursor ----

    void setSelection(int index);

    void setSelection(int start, int stop);

    int getSelectionStart();

    int getSelectionEnd();

    boolean hasSelection();

    void selectLines();

    // ---- Text manipulation ----

    void insertOrReplaceTextOnCursor(String newText);

    void simulateKeyPress(int keyEvent_KEYCODE_SOMETHING);

    // ---- Auto-format ----

    void setAutoFormatters(@Nullable InputFilter inputFilter, @Nullable TextWatcher modifier);

    boolean getAutoFormatEnabled();

    void setAutoFormatEnabled(boolean enable);

    void withAutoFormatDisabled(@NonNull GsCallback.a0 callback);

    // ---- Syntax highlighting ----

    void setHighlighter(@Nullable SyntaxHighlighterBase highlighter);

    @Nullable
    SyntaxHighlighterBase getHighlighter();

    boolean setHighlightingEnabled(boolean enable);

    boolean getHighlightingEnabled();

    void recomputeHighlighting();

    void initHighlighter();

    // ---- Search highlight ----

    void setSearchMatches(@Nullable List<SyntaxHighlighterBase.SpanGroup> spanGroups);

    void removeSearchMatch(@Nullable SyntaxHighlighterBase.SpanGroup spanGroup);

    void clearSearchMatches();

    void applyDynamicHighlight();

    void addSearchSelection(int start, int end, @ColorInt int color);

    void clearSearchSelection();

    // ---- Focus & view ----

    boolean requestFocus();

    boolean hasFocus();

    /**
     * Returns the underlying Android View for this editor.
     * For HighlightingEditor this is the editor itself (it extends EditText).
     * For RecyclerTextEditor-based implementations, this is the RecyclerView.
     */
    @NonNull
    View getView();

    void setOnFocusChangeListener(@Nullable View.OnFocusChangeListener listener);

    // ---- TextWatcher support (for search fragment) ----

    void addTextChangedListener(@NonNull TextWatcher watcher);

    void removeTextChangedListener(@NonNull TextWatcher watcher);

    // ---- Miscellaneous ----

    int getTextChangedNumber();

    void setSaveInstanceState(boolean save);

    boolean indexesValid(int... indexes);

    // ---- Cursor movement ----

    int moveCursorToEndOfLine(int offset);

    int moveCursorToBeginOfLine(int offset);
}
