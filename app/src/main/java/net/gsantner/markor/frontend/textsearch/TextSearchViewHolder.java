package net.gsantner.markor.frontend.textsearch;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatDelegate;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentEditAndViewFragment;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;

public class TextSearchViewHolder {

    private HighlightingEditor editText;
    private EditText searchEditText;
    private EditText replaceEditText;
    private TextView resultTextView;

    private boolean initialized;
    private View textSearchView;
    private TextSearchHandler textSearchHandler;

    private final int containerViewId;
    private final DocumentEditAndViewFragment parentFragment;

    public TextSearchViewHolder(DocumentEditAndViewFragment parentFragment, @IdRes int containerViewId) {
        this.containerViewId = containerViewId;
        this.parentFragment = parentFragment;
        // Lazy loading textSearchView, triggered by method show()
    }

    private void inflate(DocumentEditAndViewFragment parentFragment, @IdRes int containerViewId) {
        if (parentFragment == null) {
            return;
        }

        View parent = parentFragment.getView();
        if (parent == null) {
            return;
        }

        final int textSearchViewId = hashCode();
        textSearchView = parent.findViewById(textSearchViewId);
        if (textSearchView == null) {
            textSearchView = parentFragment.getLayoutInflater().inflate(R.layout.text_search_layout, null);
            textSearchView.setId(textSearchViewId);
            textSearchView.setVisibility(View.GONE);
            ViewGroup container = parent.findViewById(containerViewId);
            container.addView(textSearchView);
            setup(parentFragment);
        }
    }

    @SuppressLint("SetTextI18n")
    private void setup(DocumentEditAndViewFragment parentFragment) {
        editText = parentFragment.getEditor();

        if (editText == null) {
            return;
        }

        textSearchHandler = new TextSearchHandler();

        searchEditText = textSearchView.findViewById(R.id.searchEditText);
        replaceEditText = textSearchView.findViewById(R.id.replaceEditText);
        resultTextView = textSearchView.findViewById(R.id.resultTextView);

        textSearchHandler.setResultChangedListener((current, count, msg) -> {
            if (count > 0) {
                resultTextView.setText((current + 1) + "/" + count);
            } else if (count == 0) {
                resultTextView.setText(R.string.no_results);
            } else if (count == TextSearchHandler.RESULT_BAD_PATTERN) {
                SpannableString spannable = new SpannableString(parentFragment.getString(R.string.bad_pattern));
                spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                resultTextView.setText(spannable);
                Toast.makeText(parentFragment.getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        final Runnable findTask = TextViewUtils.makeDebounced(800, this::find);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                findTask.run();
            }
        });

        searchEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (event == null || event.getAction() == KeyEvent.ACTION_DOWN) {
                    textSearchHandler.next(editText);
                    return true;
                }
            }
            return false;
        });

        textSearchView.findViewById(R.id.findInSelectionImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);

                if (checked) {
                    boolean selected = textSearchHandler.setSearchRange(editText, searchEditText);
                    if (selected) {
                        find2();
                    } else { // Set selection failed
                        checked = toggleViewCheckedState(view, checked);
                    }
                } else {
                    textSearchHandler.clearSearchRange(editText);
                    find();
                }
            }
        });

        textSearchHandler.setMatchCase(false);
        textSearchView.findViewById(R.id.matchCaseImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setMatchCase(checked);
                find();
            }
        });

        textSearchHandler.setMatchWholeWord(false);
        textSearchView.findViewById(R.id.matchWholeWordImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setMatchWholeWord(checked);
                find();
            }
        });

        textSearchHandler.setUseRegex(false);
        textSearchView.findViewById(R.id.useRegexImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setUseRegex(checked);
                find();
            }
        });

        textSearchHandler.setPreserveCase(false);
        textSearchView.findViewById(R.id.preserveCaseImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setPreserveCase(checked);
            }
        });

        textSearchView.findViewById(R.id.closeImageButton).setOnClickListener(view -> close());
        textSearchView.findViewById(R.id.filterImageButton).setOnClickListener(view -> MarkorDialogFactory.showSearchDialog(parentFragment.getActivity(), editText, searchEditText.getText().toString()));
        textSearchView.findViewById(R.id.toggleImageButton).setOnClickListener(view -> toggleFindReplaceLayout());
        textSearchView.findViewById(R.id.previousImageButton).setOnClickListener(view -> textSearchHandler.previous(editText));
        textSearchView.findViewById(R.id.nextImageButton).setOnClickListener(view -> textSearchHandler.next(editText));
        textSearchView.findViewById(R.id.replaceImageButton).setOnClickListener(view -> textSearchHandler.replace(editText, replaceEditText.getText().toString()));
        textSearchView.findViewById(R.id.replaceAllImageButton).setOnClickListener(view -> textSearchHandler.replaceAll(editText, replaceEditText.getText().toString()));
        textSearchView.findViewById(R.id.clearSearchTextView).setOnClickListener(view -> searchEditText.setText(""));
        textSearchView.findViewById(R.id.clearReplaceTextView).setOnClickListener(view -> replaceEditText.setText(""));

        this.initialized = true;
    }

    private final static int BUTTON_CHECKED_COLOR = 0xFFFAB0B0;
    private final static int BUTTON_CHECKED_COLOR_DARK = 0xFFE07070;

    private boolean toggleViewCheckedState(View view, boolean checked) {
        if (checked) {
            view.getBackground().clearColorFilter();
        } else {
            int color;
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                color = BUTTON_CHECKED_COLOR_DARK;
            } else {
                color = BUTTON_CHECKED_COLOR;
            }
            view.getBackground().setColorFilter(color, PorterDuff.Mode.DARKEN);
        }
        return !checked;
    }

    private void setReplaceLayoutVisibility(boolean visible) {
        if (!initialized) {
            return;
        }

        View replaceLinearLayout = textSearchView.findViewById(R.id.replaceLinearLayout);
        ImageButton imageButton = textSearchView.findViewById(R.id.toggleImageButton);
        if (visible) {
            replaceLinearLayout.setVisibility(View.VISIBLE);
            imageButton.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);
        } else {
            replaceLinearLayout.setVisibility(View.GONE);
            imageButton.setImageResource(R.drawable.baseline_chevron_right_24);
        }
    }

    private void toggleFindReplaceLayout() {
        if (!initialized) {
            return;
        }

        View view = textSearchView.findViewById(R.id.replaceLinearLayout);
        if (view == null) {
            return;
        }

        setReplaceLayoutVisibility(view.getVisibility() != View.VISIBLE);
    }

    public void find() {
        if (initialized) {
            textSearchHandler.find(editText, searchEditText.getText().toString(), TextSearchHandler.ACTIVE_INDEX_NEARBY);
        }
    }

    public void find2() {
        if (!initialized) {
            return;
        }

        String searchText = searchEditText.getText().toString();
        if (searchText.isEmpty()) {
            return;
        }
        textSearchHandler.find(editText, searchText, TextSearchHandler.ACTIVE_INDEX_KEEP);
    }

    private final Runnable findTask2 = TextViewUtils.makeDebounced(800, this::find2);
    private final TextWatcher editTextChangedListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            findTask2.run();
        }
    };

    private void requestEditTextFocus() {
        if (initialized) {
            searchEditText.requestFocus();
            searchEditText.selectAll();
        }
    }

    public void show() {
        if (!initialized) {
            inflate(parentFragment, containerViewId);
        }

        if (initialized && textSearchView.getVisibility() != View.VISIBLE) {
            textSearchView.setVisibility(View.VISIBLE);
            requestEditTextFocus();
            find2();
            editText.addTextChangedListener(editTextChangedListener);
        }
    }

    public void clearMatches() {
        if (initialized) {
            textSearchHandler.find(editText, "", 0);
        }
    }

    /**
     * Clear matches and listeners.
     */
    private void clear() {
        if (initialized) {
            clearMatches();
            textSearchHandler.clearSearchRange(editText);
            editText.removeTextChangedListener(editTextChangedListener);
        }
    }

    public void close() {
        if (initialized) {
            textSearchView.setVisibility(View.GONE);
            clear();
        }
    }
}
