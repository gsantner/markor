package net.gsantner.markor.frontend.textsearch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentEditAndViewFragment;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.GsContextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextSearchViewHolder {
    private static final String RECENT_SEARCH_STRING = "text_search_view_holder__recent_search_history";
    private static final String RECENT_SEARCH_REPLACE_STRING = "text_search_view_holder__recent_search_replace_history";
    private static final int MAX_RECENT_ITEMS = 10;

    private HighlightingEditor editText;
    private EditText searchEditText;
    private EditText replaceEditText;
    private TextView resultTextView;
    private ImageButton findInSelectionImageButton;
    private ImageButton matchCaseImageButton;
    private ImageButton matchWholeWordImageButton;
    private ImageButton useRegexImageButton;
    private ImageButton preserveCaseImageButton;
    private ImageButton toggleImageButton;

    private boolean initialized;
    private boolean findInSelection;
    private boolean matchCase;
    private boolean matchWholeWord;
    private boolean useRegex;
    private boolean preserveCase;
    private boolean replaceVisible;
    private int activeColor;
    private int inactiveColor;
    private View textSearchView;
    private TextSearchHandler textSearchHandler;

    private final int containerViewId;
    private final DocumentEditAndViewFragment parentFragment;

    public TextSearchViewHolder(DocumentEditAndViewFragment parentFragment, @IdRes int containerViewId) {
        this.containerViewId = containerViewId;
        this.parentFragment = parentFragment;
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
        activeColor = ContextCompat.getColor(parentFragment.requireContext(), R.color.accent);
        inactiveColor = ContextCompat.getColor(parentFragment.requireContext(), R.color.primary_text);

        searchEditText = textSearchView.findViewById(R.id.searchEditText);
        replaceEditText = textSearchView.findViewById(R.id.replaceEditText);
        resultTextView = textSearchView.findViewById(R.id.resultTextView);
        findInSelectionImageButton = textSearchView.findViewById(R.id.findInSelectionImageButton);
        matchCaseImageButton = textSearchView.findViewById(R.id.matchCaseImageButton);
        matchWholeWordImageButton = textSearchView.findViewById(R.id.matchWholeWordImageButton);
        useRegexImageButton = textSearchView.findViewById(R.id.useRegexImageButton);
        preserveCaseImageButton = textSearchView.findViewById(R.id.preserveCaseImageButton);
        toggleImageButton = textSearchView.findViewById(R.id.toggleImageButton);

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
            if (actionId == EditorInfo.IME_ACTION_SEARCH && (event == null || event.getAction() == KeyEvent.ACTION_DOWN)) {
                saveCurrentHistory();
                textSearchHandler.next(editText);
                return true;
            }
            return false;
        });

        setFindInSelection(findInSelection);
        findInSelectionImageButton.setOnClickListener(view -> {
            setFindInSelection(!findInSelection);
            textSearchHandler.handleSearchSelection(editText, null);
            find();
        });

        setMatchCase(matchCase);
        matchCaseImageButton.setOnClickListener(view -> {
            setMatchCase(!matchCase);
            find();
        });

        setMatchWholeWord(matchWholeWord);
        matchWholeWordImageButton.setOnClickListener(view -> {
            setMatchWholeWord(!matchWholeWord);
            find();
        });

        setUseRegex(useRegex);
        useRegexImageButton.setOnClickListener(view -> {
            setUseRegex(!useRegex);
            find();
        });

        setPreserveCase(preserveCase);
        preserveCaseImageButton.setOnClickListener(view -> setPreserveCase(!preserveCase));
        setReplaceLayoutVisibility(replaceVisible);

        textSearchView.findViewById(R.id.filterImageButton).setOnClickListener(view -> MarkorDialogFactory.showSearchDialog(parentFragment.getActivity(), editText, searchEditText.getText().toString()));
        toggleImageButton.setOnClickListener(view -> toggleFindReplaceLayout());
        textSearchView.findViewById(R.id.previousImageButton).setOnClickListener(view -> {
            saveCurrentHistory();
            textSearchHandler.previous(editText);
        });
        textSearchView.findViewById(R.id.nextImageButton).setOnClickListener(view -> {
            saveCurrentHistory();
            textSearchHandler.next(editText);
        });
        textSearchView.findViewById(R.id.replaceImageButton).setOnClickListener(view -> {
            textSearchHandler.replace(editText, replaceEditText.getText().toString());
            saveCurrentHistory();
        });
        textSearchView.findViewById(R.id.replaceAllImageButton).setOnClickListener(view -> {
            textSearchHandler.replaceAll(editText, replaceEditText.getText().toString());
            saveCurrentHistory();
        });
        textSearchView.findViewById(R.id.clearSearchImageButton).setOnClickListener(view -> searchEditText.setText(""));
        textSearchView.findViewById(R.id.clearReplaceImageButton).setOnClickListener(view -> replaceEditText.setText(""));
        textSearchView.findViewById(R.id.historyImageButton).setOnClickListener(this::showHistoryPopup);
        textSearchView.findViewById(R.id.newLineSearchTextView).setOnClickListener(view -> TextViewUtils.replaceSelection(searchEditText.getText(), "\n"));

        initialized = true;
    }

    private void setIconActive(ImageView view, boolean active) {
        if (view != null) {
            view.setColorFilter(active ? activeColor : inactiveColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private void setFindInSelection(boolean checked) {
        findInSelection = checked;
        textSearchHandler.setFindInSelection(checked);
        setIconActive(findInSelectionImageButton, checked);
    }

    private void setMatchCase(boolean checked) {
        matchCase = checked;
        textSearchHandler.setMatchCase(checked);
        setIconActive(matchCaseImageButton, checked);
    }

    private void setMatchWholeWord(boolean checked) {
        matchWholeWord = checked;
        textSearchHandler.setMatchWholeWord(checked);
        setIconActive(matchWholeWordImageButton, checked);
    }

    private void setUseRegex(boolean checked) {
        useRegex = checked;
        textSearchHandler.setUseRegex(checked);
        setIconActive(useRegexImageButton, checked);
    }

    private void setPreserveCase(boolean checked) {
        preserveCase = checked;
        textSearchHandler.setPreserveCase(checked);
        setIconActive(preserveCaseImageButton, checked);
    }

    private void setReplaceLayoutVisibility(boolean visible) {
        if (!initialized && textSearchView == null) {
            replaceVisible = visible;
            return;
        }
        View replaceLinearLayout = textSearchView.findViewById(R.id.replaceLinearLayout);
        if (replaceLinearLayout == null) {
            return;
        }
        replaceVisible = visible;
        replaceLinearLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        setIconActive(toggleImageButton, visible);
    }

    private void toggleFindReplaceLayout() {
        if (!initialized) {
            return;
        }
        View view = textSearchView.findViewById(R.id.replaceLinearLayout);
        if (view != null) {
            setReplaceLayoutVisibility(view.getVisibility() != View.VISIBLE);
        }
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
        if (!searchText.isEmpty()) {
            textSearchHandler.find(editText, searchText, TextSearchHandler.ACTIVE_INDEX_KEEP);
        }
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
            textSearchHandler.handleSearchSelection(editText, searchEditText);
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
            parentFragment.requireActivity().invalidateOptionsMenu();
        }
    }

    public boolean isShow() {
        return textSearchView != null && textSearchView.getVisibility() == View.VISIBLE;
    }

    public void clearMatches() {
        if (initialized) {
            textSearchHandler.find(editText, "", 0);
        }
    }

    private void clear() {
        if (initialized) {
            clearMatches();
            textSearchHandler.clearSearchSelection(editText, false);
            editText.removeTextChangedListener(editTextChangedListener);
        }
    }

    public void close() {
        if (initialized) {
            saveCurrentHistory();
            textSearchView.setVisibility(View.GONE);
            clear();
            parentFragment.requireActivity().invalidateOptionsMenu();
        }
    }

    private SharedPreferences getPreferences() {
        return parentFragment.requireActivity().getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
    }

    private void saveCurrentHistory() {
        if (!initialized || searchEditText == null || searchEditText.length() == 0) {
            return;
        }
        final SearchHistoryEntry entry = new SearchHistoryEntry(
                searchEditText.getText().toString(),
                replaceEditText.getText().toString(),
                matchCase,
                matchWholeWord,
                useRegex,
                findInSelection,
                preserveCase
        );
        saveHistoryEntry(RECENT_SEARCH_STRING, entry, false);
        if (replaceVisible) {
            saveHistoryEntry(RECENT_SEARCH_REPLACE_STRING, entry, true);
        }
    }

    private void saveHistoryEntry(String key, SearchHistoryEntry entry, boolean includeReplace) {
        final ArrayList<SearchHistoryEntry> entries = new ArrayList<>();
        final Set<String> known = new HashSet<>();
        entries.add(entry);
        known.add(entry.key(includeReplace));
        for (SearchHistoryEntry oldEntry : loadHistory(key, includeReplace)) {
            if (entries.size() >= MAX_RECENT_ITEMS) {
                break;
            }
            final String oldKey = oldEntry.key(includeReplace);
            if (!known.contains(oldKey)) {
                entries.add(oldEntry);
                known.add(oldKey);
            }
        }
        final JSONArray array = new JSONArray();
        for (SearchHistoryEntry historyEntry : entries) {
            array.put(historyEntry.toJson());
        }
        getPreferences().edit().putString(key, array.toString()).apply();
    }

    private List<SearchHistoryEntry> loadHistory(String key, boolean includeReplace) {
        final ArrayList<SearchHistoryEntry> entries = new ArrayList<>();
        final HashSet<String> known = new HashSet<>();
        try {
            final JSONArray array = new JSONArray(getPreferences().getString(key, "[]"));
            for (int i = 0; i < array.length() && entries.size() < MAX_RECENT_ITEMS; i++) {
                SearchHistoryEntry entry = SearchHistoryEntry.fromJson(array.getJSONObject(i));
                final String entryKey = entry.key(includeReplace);
                if (!entry.search.isEmpty() && !known.contains(entryKey)) {
                    entries.add(entry);
                    known.add(entryKey);
                }
            }
        } catch (JSONException ignored) {
        }
        return entries;
    }

    private void showHistoryPopup(View anchor) {
        final boolean replaceHistory = replaceVisible;
        final List<SearchHistoryEntry> entries = loadHistory(replaceHistory ? RECENT_SEARCH_REPLACE_STRING : RECENT_SEARCH_STRING, replaceHistory);
        final ListPopupWindow popupWindow = new ListPopupWindow(parentFragment.requireContext());
        popupWindow.setAdapter(new SearchHistoryAdapter(replaceHistory, entries));
        popupWindow.setAnchorView(anchor);
        popupWindow.setWidth(GsContextUtils.instance.convertDpToPx(parentFragment.requireContext(), 300));
        if (entries.isEmpty()) {
            popupWindow.setHeight(GsContextUtils.instance.convertDpToPx(parentFragment.requireContext(), 48));
        }
        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            if (!entries.isEmpty()) {
                applyHistoryEntry(entries.get(position), replaceHistory);
                popupWindow.dismiss();
            }
        });
        popupWindow.show();
    }

    private void applyHistoryEntry(SearchHistoryEntry entry, boolean replaceHistory) {
        searchEditText.setText(entry.search);
        if (replaceHistory) {
            replaceEditText.setText(entry.replace);
            setReplaceLayoutVisibility(true);
        }
        setFindInSelection(entry.findInSelection);
        setMatchCase(entry.matchCase);
        setMatchWholeWord(entry.matchWholeWord);
        setUseRegex(entry.useRegex);
        setPreserveCase(entry.preserveCase);
        find();
    }

    private class SearchHistoryAdapter extends ArrayAdapter<SearchHistoryEntry> {
        private final boolean includeReplace;
        private final int padding4;
        private final int padding8;
        private final int iconSize;

        SearchHistoryAdapter(boolean includeReplace, List<SearchHistoryEntry> entries) {
            super(parentFragment.requireContext(), android.R.layout.simple_list_item_1, entries);
            this.includeReplace = includeReplace;
            padding4 = GsContextUtils.instance.convertDpToPx(parentFragment.requireContext(), 4);
            padding8 = GsContextUtils.instance.convertDpToPx(parentFragment.requireContext(), 8);
            iconSize = GsContextUtils.instance.convertDpToPx(parentFragment.requireContext(), 24);
        }

        @Override
        public int getCount() {
            return super.getCount() == 0 ? 1 : super.getCount();
        }

        @Override
        public boolean isEnabled(int position) {
            return super.getCount() > 0;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final SearchHistoryEntry entry = super.getCount() == 0 ? null : getItem(position);
            final LinearLayout row = new LinearLayout(parentFragment.requireContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(padding8, padding4, padding8, padding4);

            final TextView label = makeHistoryTextView(entry == null ? "" : entry.getLabel(includeReplace));
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            if (entry != null) {
                addHistoryIcon(row, entry.findInSelection, R.drawable.ic_list_selection_24dp);
                addHistoryIcon(row, entry.matchCase, R.drawable.outline_match_case_24);
                addHistoryIcon(row, entry.matchWholeWord, R.drawable.outline_match_word_24);
                addHistoryIcon(row, entry.useRegex, R.drawable.ic_regex_24dp);
                addHistoryIcon(row, includeReplace && entry.preserveCase, R.drawable.ic_preserve_case_24dp);
                row.setContentDescription(getAccessibilityLabel(entry));
            }
            return row;
        }

        private void addHistoryIcon(LinearLayout row, boolean visible, int drawableRes) {
            if (!visible) {
                return;
            }
            final ImageView icon = new ImageView(parentFragment.requireContext());
            icon.setImageResource(drawableRes);
            icon.setColorFilter(inactiveColor, PorterDuff.Mode.SRC_IN);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(iconSize, iconSize);
            params.setMarginStart(padding4);
            row.addView(icon, params);
        }

        private TextView makeHistoryTextView(String text) {
            final TextView textView = new TextView(parentFragment.requireContext());
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setText(text);
            textView.setTextColor(inactiveColor);
            return textView;
        }

        private String getAccessibilityLabel(SearchHistoryEntry entry) {
            final ArrayList<String> parts = new ArrayList<>();
            parts.add(entry.getLabel(includeReplace));
            if (entry.findInSelection) {
                parts.add(parentFragment.getString(R.string.find_in_selection));
            }
            if (entry.matchCase) {
                parts.add(parentFragment.getString(R.string.match_case));
            }
            if (entry.matchWholeWord) {
                parts.add(parentFragment.getString(R.string.match_whole_word));
            }
            if (entry.useRegex) {
                parts.add(parentFragment.getString(R.string.use_regular_expression));
            }
            if (includeReplace && entry.preserveCase) {
                parts.add(parentFragment.getString(R.string.preserve_case));
            }
            return TextUtils.join(", ", parts);
        }
    }

    private static class SearchHistoryEntry {
        final String search;
        final String replace;
        final boolean matchCase;
        final boolean matchWholeWord;
        final boolean useRegex;
        final boolean findInSelection;
        final boolean preserveCase;

        private static final String SEARCH_KEY = "search";
        private static final String REPLACE_KEY = "replace";
        private static final String MATCH_CASE_KEY = "match_case";
        private static final String MATCH_WHOLE_WORD_KEY = "match_whole_word";
        private static final String USE_REGEX_KEY = "use_regex";
        private static final String FIND_IN_SELECTION_KEY = "find_in_selection";
        private static final String PRESERVE_CASE_KEY = "preserve_case";

        SearchHistoryEntry(String search, String replace, boolean matchCase, boolean matchWholeWord, boolean useRegex, boolean findInSelection, boolean preserveCase) {
            this.search = search == null ? "" : search;
            this.replace = replace == null ? "" : replace;
            this.matchCase = matchCase;
            this.matchWholeWord = matchWholeWord;
            this.useRegex = useRegex;
            this.findInSelection = findInSelection;
            this.preserveCase = preserveCase;
        }

        String key(boolean includeReplace) {
            return includeReplace ? search + "\n" + replace : search;
        }

        String getLabel(boolean includeReplace) {
            return includeReplace ? search + " => " + replace : search;
        }

        JSONObject toJson() {
            final JSONObject object = new JSONObject();
            try {
                object.put(SEARCH_KEY, search);
                object.put(REPLACE_KEY, replace);
                object.put(MATCH_CASE_KEY, matchCase);
                object.put(MATCH_WHOLE_WORD_KEY, matchWholeWord);
                object.put(USE_REGEX_KEY, useRegex);
                object.put(FIND_IN_SELECTION_KEY, findInSelection);
                object.put(PRESERVE_CASE_KEY, preserveCase);
            } catch (JSONException ignored) {
            }
            return object;
        }

        static SearchHistoryEntry fromJson(JSONObject object) {
            return new SearchHistoryEntry(
                    object.optString(SEARCH_KEY, ""),
                    object.optString(REPLACE_KEY, ""),
                    object.optBoolean(MATCH_CASE_KEY, false),
                    object.optBoolean(MATCH_WHOLE_WORD_KEY, false),
                    object.optBoolean(USE_REGEX_KEY, false),
                    object.optBoolean(FIND_IN_SELECTION_KEY, false),
                    object.optBoolean(PRESERVE_CASE_KEY, false)
            );
        }
    }
}
