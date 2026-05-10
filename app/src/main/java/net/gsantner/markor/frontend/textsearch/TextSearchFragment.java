package net.gsantner.markor.frontend.textsearch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ReplacementTransformationMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.gsantner.markor.R;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSearchFragment extends Fragment {
    private static final String RECENT_SEARCH_STRING = "text_search_fragment__recent_search_history";
    private static final String RECENT_SEARCH_REPLACE_STRING = "text_search_fragment__recent_search_replace_history";
    private static final int MAX_RECENT_ITEMS = 5;

    private int containerViewId;
    private FragmentActivity activity;
    private FragmentManager fragmentManager;
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

    private TextSearchHandler textSearchHandler;

    private boolean initialized;
    private boolean findInSelection;
    private boolean matchCase;
    private boolean matchWholeWord;
    private boolean useRegex;
    private boolean preserveCase;
    private boolean replaceVisible;
    private int activeColor;
    private int inactiveColor;

    public static TextSearchFragment newInstance(@IdRes int containerViewId, FragmentActivity activity, HighlightingEditor editText) {
        return newInstance(containerViewId, activity, activity.getSupportFragmentManager(), editText);
    }

    public static TextSearchFragment newInstance(@IdRes int containerViewId, FragmentActivity activity, FragmentManager fragmentManager, HighlightingEditor editText) {
        Fragment fragment = fragmentManager.findFragmentByTag(String.valueOf(containerViewId));
        if (fragment instanceof TextSearchFragment) {
            TextSearchFragment existingFragment = (TextSearchFragment) fragment;
            existingFragment.containerViewId = containerViewId;
            existingFragment.activity = activity;
            existingFragment.fragmentManager = fragmentManager;
            existingFragment.editText = editText;
            if (existingFragment.textSearchHandler == null) {
                existingFragment.textSearchHandler = new TextSearchHandler();
            }
            return existingFragment;
        }

        TextSearchFragment newFragment = new TextSearchFragment();
        newFragment.containerViewId = containerViewId;
        newFragment.activity = activity;
        newFragment.fragmentManager = fragmentManager;
        newFragment.editText = editText;
        newFragment.textSearchHandler = new TextSearchHandler();

        return newFragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (initialized) {
            saveCurrentHistory();
            clearMatches();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return getLayoutInflater().inflate(R.layout.text_search_fragment, container, false);
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View fragmentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragmentView, savedInstanceState);

        initialized = !(activity == null || editText == null || textSearchHandler == null);

        if (!initialized) {
            close();
            return;
        }

        fragmentView.bringToFront();
        requestEditTextFocus(fragmentView);

        searchEditText = fragmentView.findViewById(R.id.searchEditText);
        replaceEditText = fragmentView.findViewById(R.id.replaceEditText);
        resultTextView = fragmentView.findViewById(R.id.resultTextView);
        findInSelectionImageButton = fragmentView.findViewById(R.id.findInSelectionImageButton);
        matchCaseImageButton = fragmentView.findViewById(R.id.matchCaseImageButton);
        matchWholeWordImageButton = fragmentView.findViewById(R.id.matchWholeWordImageButton);
        useRegexImageButton = fragmentView.findViewById(R.id.useRegexImageButton);
        preserveCaseImageButton = fragmentView.findViewById(R.id.preserveCaseImageButton);
        toggleImageButton = fragmentView.findViewById(R.id.toggleImageButton);
        activeColor = ContextCompat.getColor(activity, R.color.accent);
        inactiveColor = ContextCompat.getColor(activity, R.color.primary_text);

        textSearchHandler.setResultChangedListener((current, count, msg) -> {
            if (count > 0) {
                resultTextView.setText((current + 1) + "/" + count);
            } else if (count == 0) {
                resultTextView.setText(R.string.no_results);
            } else if (count == TextSearchHandler.RESULT_BAD_PATTERN) {
                SpannableString spannable = new SpannableString(getString(R.string.bad_pattern));
                spannable.setSpan(new ForegroundColorSpan(Color.RED), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                resultTextView.setText(spannable);
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                textSearchHandler.handleSearchSelection(editText, searchEditText);
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

            private boolean paused;
            private Pattern pattern = Pattern.compile("\\n");

            @Override
            public void afterTextChanged(Editable s) {
                if (paused) {
                    return;
                }

                findTask.run();

                // Highlight invisible character '\n'
                Matcher matcher = pattern.matcher(s);
                paused = true;
                while (matcher.find()) {
                    SpannableString spannable = new SpannableString("\n");
                    spannable.setSpan(new BackgroundColorSpan(Match.getMatchColor()), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    searchEditText.getText().replace(matcher.start(), matcher.end(), spannable);
                }
                paused = false;
            }
        });

        searchEditText.setTransformationMethod(new ReplacementTransformationMethod() {
            @Override
            protected char[] getOriginal() {
                return new char[]{'\n'};
            }

            @Override
            protected char[] getReplacement() {
                return new char[]{'↵'};
            }
        });

        searchEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus && textSearchHandler.isFindInSelection()) find();
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
        preserveCaseImageButton.setOnClickListener(view -> {
            setPreserveCase(!preserveCase);
        });
        setReplaceLayoutVisibility(fragmentView, replaceVisible);

        fragmentView.findViewById(R.id.filterImageButton).setOnClickListener(view -> MarkorDialogFactory.showSearchDialog(activity, editText, searchEditText.getText().toString()));
        fragmentView.findViewById(R.id.toggleImageButton).setOnClickListener(view -> toggleFindReplaceLayout(fragmentView));
        fragmentView.findViewById(R.id.previousImageButton).setOnClickListener(view -> textSearchHandler.previous(editText));
        fragmentView.findViewById(R.id.nextImageButton).setOnClickListener(view -> textSearchHandler.next(editText));
        fragmentView.findViewById(R.id.replaceImageButton).setOnClickListener(view -> {
            saveCurrentHistory();
            textSearchHandler.replace(editText, replaceEditText.getText().toString());
        });
        fragmentView.findViewById(R.id.replaceAllImageButton).setOnClickListener(view -> {
            saveCurrentHistory();
            textSearchHandler.replaceAll(editText, replaceEditText.getText().toString());
        });
        fragmentView.findViewById(R.id.clearSearchImageButton).setOnClickListener(view -> searchEditText.setText(""));
        fragmentView.findViewById(R.id.clearReplaceImageButton).setOnClickListener(view -> replaceEditText.setText(""));
        fragmentView.findViewById(R.id.historyImageButton).setOnClickListener(view -> showHistoryPopup(view));
        fragmentView.findViewById(R.id.newLineSearchTextView).setOnClickListener(view -> {
            TextViewUtils.replaceSelection(searchEditText.getText(), "\n");
        });
    }

    private void setIconActive(ImageView view, boolean active) {
        if (view == null) {
            return;
        }
        view.setColorFilter(active ? activeColor : inactiveColor, PorterDuff.Mode.SRC_IN);
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

    private void setReplaceLayoutVisibility(View parent, boolean visible) {
        if (parent == null) {
            return;
        }
        View replaceLinearLayout = parent.findViewById(R.id.replaceLinearLayout);
        if (replaceLinearLayout == null) {
            return;
        }
        replaceVisible = visible;
        if (visible) {
            replaceLinearLayout.setVisibility(View.VISIBLE);
        } else {
            replaceLinearLayout.setVisibility(View.GONE);
        }
        setIconActive(toggleImageButton, visible);
    }

    private void toggleFindReplaceLayout(View parent) {
        View view = parent.findViewById(R.id.replaceLinearLayout);
        if (view == null) {
            return;
        }
        setReplaceLayoutVisibility(parent, view.getVisibility() != View.VISIBLE);
    }

    private static void requestEditTextFocus(View parent) {
        if (parent == null) {
            return;
        }

        View view = parent.findViewById(R.id.searchEditText);
        if (view instanceof EditText) {
            view.requestFocus();
            ((EditText) view).selectAll();
        }
    }

    public void find() {
        textSearchHandler.find(editText, searchEditText.getText().toString(), TextSearchHandler.ACTIVE_INDEX_NEARBY);
    }

    public void find2() {
        if (searchEditText == null) return;
        String searchText = searchEditText.getText().toString();
        if (searchText.isEmpty()) {
            return;
        }
        textSearchHandler.find(editText, searchText, TextSearchHandler.ACTIVE_INDEX_KEEP);
    }

    public void clearMatches() {
        textSearchHandler.find(editText, "", 0);
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

    public void show() {
        FragmentManager fragmentManager = getTextSearchFragmentManager();
        if (fragmentManager == null) {
            return;
        }
        String tag = String.valueOf(this.containerViewId);
        if (fragmentManager.findFragmentByTag(tag) == null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(this.containerViewId, this, tag);
            transaction.commit();
        } else if (!this.isVisible()) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.show(this);
            transaction.commit();
            requestEditTextFocus(this.getView());
        }
        find2();
        editText.addTextChangedListener(editTextChangedListener);
    }

    public boolean isShowing() {
        return isAdded() && !isHidden();
    }

    private void clear() {
        if (initialized) {
            clearMatches();
            textSearchHandler.clearSearchSelection(editText, false);
            editText.removeTextChangedListener(editTextChangedListener);
        }
    }

    public void hide() {
        saveCurrentHistory();
        clear();
        FragmentManager fragmentManager = getTextSearchFragmentManager();
        if (fragmentManager != null) {
            fragmentManager.beginTransaction().hide(this).commit();
        }
    }

    public void close() {
        clear();
        FragmentManager fragmentManager = getTextSearchFragmentManager();
        if (fragmentManager != null) {
            fragmentManager.beginTransaction().remove(this).commit();
        }
    }

    @Nullable
    public FragmentManager getTextSearchFragmentManager() {
        if (fragmentManager != null) {
            return fragmentManager;
        }
        FragmentActivity fragmentActivity = activity != null ? activity : getActivity();
        return fragmentActivity != null ? fragmentActivity.getSupportFragmentManager() : null;
    }

    private SharedPreferences getPreferences() {
        return activity.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
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
        for (SearchHistoryEntry oldEntry : loadHistory(key)) {
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

    private List<SearchHistoryEntry> loadHistory(String key) {
        final ArrayList<SearchHistoryEntry> entries = new ArrayList<>();
        try {
            final JSONArray array = new JSONArray(getPreferences().getString(key, "[]"));
            for (int i = 0; i < array.length() && entries.size() < MAX_RECENT_ITEMS; i++) {
                SearchHistoryEntry entry = SearchHistoryEntry.fromJson(array.getJSONObject(i));
                if (!entry.search.isEmpty()) {
                    entries.add(entry);
                }
            }
        } catch (JSONException ignored) {
        }
        return entries;
    }

    private void showHistoryPopup(View anchor) {
        saveCurrentHistory();
        final boolean replaceHistory = replaceVisible;
        final List<SearchHistoryEntry> entries = loadHistory(replaceHistory ? RECENT_SEARCH_REPLACE_STRING : RECENT_SEARCH_STRING);
        if (entries.isEmpty()) {
            Toast.makeText(activity, R.string.no_results, Toast.LENGTH_SHORT).show();
            return;
        }
        final ListPopupWindow popupWindow = new ListPopupWindow(activity);
        popupWindow.setAdapter(new SearchHistoryAdapter(replaceHistory, entries));
        popupWindow.setAnchorView(anchor);
        popupWindow.setWidth(GsContextUtils.instance.convertDpToPx(activity, 300));
        popupWindow.setModal(true);
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            applyHistoryEntry(entries.get(position), replaceHistory);
            popupWindow.dismiss();
        });
        popupWindow.show();
    }

    private void applyHistoryEntry(SearchHistoryEntry entry, boolean replaceHistory) {
        searchEditText.setText(entry.search);
        if (replaceHistory) {
            replaceEditText.setText(entry.replace);
            setReplaceLayoutVisibility(getView(), true);
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
            super(activity, android.R.layout.simple_list_item_1, entries);
            this.includeReplace = includeReplace;
            padding4 = GsContextUtils.instance.convertDpToPx(activity, 4);
            padding8 = GsContextUtils.instance.convertDpToPx(activity, 8);
            iconSize = GsContextUtils.instance.convertDpToPx(activity, 24);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final SearchHistoryEntry entry = getItem(position);
            final LinearLayout row = new LinearLayout(activity);
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
            final ImageView icon = new ImageView(activity);
            icon.setImageResource(drawableRes);
            icon.setColorFilter(inactiveColor, PorterDuff.Mode.SRC_IN);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(iconSize, iconSize);
            params.setMarginStart(padding4);
            row.addView(icon, params);
        }

        private TextView makeHistoryTextView(String text) {
            final TextView textView = new TextView(activity);
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
                parts.add(getString(R.string.find_in_selection));
            }
            if (entry.matchCase) {
                parts.add(getString(R.string.match_case));
            }
            if (entry.matchWholeWord) {
                parts.add(getString(R.string.match_whole_word));
            }
            if (entry.useRegex) {
                parts.add(getString(R.string.use_regular_expression));
            }
            if (includeReplace && entry.preserveCase) {
                parts.add(getString(R.string.preserve_case));
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

        SearchHistoryEntry(
                String search,
                String replace,
                boolean matchCase,
                boolean matchWholeWord,
                boolean useRegex,
                boolean findInSelection,
                boolean preserveCase
        ) {
            this.search = search == null ? "" : search;
            this.replace = replace == null ? "" : replace;
            this.matchCase = matchCase;
            this.matchWholeWord = matchWholeWord;
            this.useRegex = useRegex;
            this.findInSelection = findInSelection;
            this.preserveCase = preserveCase;
        }

        String key(boolean includeReplace) {
            return search + "\n" + (includeReplace ? replace : "") + "\n" + matchCase + matchWholeWord + useRegex + findInSelection + preserveCase;
        }

        String getLabel(boolean includeReplace) {
            return includeReplace ? search + " ⇒ " + replace : search;
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
