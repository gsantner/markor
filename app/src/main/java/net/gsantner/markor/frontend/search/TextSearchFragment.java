package net.gsantner.markor.frontend.search;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import net.gsantner.markor.R;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;

public class TextSearchFragment extends Fragment {
    private int containerViewId;
    private FragmentActivity activity;
    private HighlightingEditor editText;
    private EditText searchEditText;
    private EditText replaceEditText;
    private TextView resultTextView;
    private final TextSearchHandler textSearchHandler = new TextSearchHandler();

    public static TextSearchFragment newInstance(@IdRes int containerViewId, FragmentActivity activity, HighlightingEditor editText) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(String.valueOf(containerViewId));
        if (fragment instanceof TextSearchFragment) {
            return (TextSearchFragment) fragment;
        }

        TextSearchFragment newFragment = new TextSearchFragment();
        newFragment.containerViewId = containerViewId;
        newFragment.activity = activity;
        newFragment.editText = editText;

        return newFragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearMatches();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return getLayoutInflater().inflate(R.layout.text_search_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View fragmentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragmentView, savedInstanceState);

        fragmentView.post(() -> init(fragmentView));

        searchEditText = fragmentView.findViewById(R.id.searchEditText);
        replaceEditText = fragmentView.findViewById(R.id.replaceEditText);
        resultTextView = fragmentView.findViewById(R.id.resultTextView);

        textSearchHandler.setResultChangedListener((current, count) -> {
            if (count > 0) {
                resultTextView.setText(current + "/" + count);
            } else {
                resultTextView.setText(R.string.no_results);
            }
        });

        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                textSearchHandler.handleSelection(editText, searchEditText);
            }
        });

        searchEditText.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                textSearchHandler.handleSelection(editText, searchEditText);
            }
            return false;
        });

        searchEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                find();
            }
        });

        final Runnable findTask = this::find;
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Handler handler = editText.getHandler();
                handler.removeCallbacks(findTask);
                handler.postDelayed(findTask, 800);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        textSearchHandler.setFindInSelection(false);
        fragmentView.findViewById(R.id.findInSelectionImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setFindInSelection(checked);
                textSearchHandler.handleSelection(editText, null);
                find();
            }
        });

        textSearchHandler.setMatchCase(false);
        fragmentView.findViewById(R.id.matchCaseImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setMatchCase(checked);
                find();
            }
        });

        textSearchHandler.setMatchWholeWord(false);
        fragmentView.findViewById(R.id.matchWholeWordImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setMatchWholeWord(checked);
                find();
            }
        });

        textSearchHandler.setUseRegex(false);
        fragmentView.findViewById(R.id.useRegexImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setUseRegex(checked);
                find();
            }
        });

        textSearchHandler.setPreserveCase(false);
        fragmentView.findViewById(R.id.preserveCaseImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                textSearchHandler.setPreserveCase(checked);
            }
        });

        fragmentView.findViewById(R.id.closeImageButton).setOnClickListener(view -> hide());
        fragmentView.findViewById(R.id.filterImageButton).setOnClickListener(view -> MarkorDialogFactory.showSearchDialog(activity, editText, searchEditText.getText().toString()));
        fragmentView.findViewById(R.id.toggleImageButton).setOnClickListener(view -> toggleFindReplaceLayout(fragmentView));
        fragmentView.findViewById(R.id.previousImageButton).setOnClickListener(view -> textSearchHandler.previous(editText));
        fragmentView.findViewById(R.id.nextImageButton).setOnClickListener(view -> textSearchHandler.next(editText));
        fragmentView.findViewById(R.id.replaceImageButton).setOnClickListener(view -> textSearchHandler.replace(editText, replaceEditText.getText().toString()));
        fragmentView.findViewById(R.id.replaceAllImageButton).setOnClickListener(view -> textSearchHandler.replaceAll(editText, replaceEditText.getText().toString()));
    }

    public void init(@NonNull View view) {
        Context context = getContext();
        int displayWidth = 1040;
        int width = displayWidth;
        if (context != null) {
            displayWidth = context.getResources().getDisplayMetrics().widthPixels;
            if (displayWidth < 1080) {
                width = displayWidth - 20;
            }
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.MATCH_PARENT);
        layoutParams.topMargin = 10;
        layoutParams.bottomMargin = 10;
        view.setLayoutParams(layoutParams);
        view.setX((displayWidth - width) / 2f);
        view.setY(0);
        view.bringToFront();

        requestEditTextFocus(view);
    }

    private final static int BUTTON_CHECKED_COLOR = 0xA0EE4B4B;

    private boolean toggleViewCheckedState(View view, boolean checked) {
        if (checked) {
            view.getBackground().clearColorFilter();
        } else {
            view.getBackground().setColorFilter(BUTTON_CHECKED_COLOR, PorterDuff.Mode.DARKEN);
        }
        return !checked;
    }

    private void setReplaceLayoutVisibility(View parent, boolean visible) {
        View replaceLinearLayout = parent.findViewById(R.id.replaceLinearLayout);
        ImageButton imageButton = parent.findViewById(R.id.toggleImageButton);
        if (visible) {
            replaceLinearLayout.setVisibility(View.VISIBLE);
            imageButton.setImageResource(R.drawable.ic_chevron_down_24dp);
        } else {
            replaceLinearLayout.setVisibility(View.GONE);
            imageButton.setImageResource(R.drawable.ic_chevron_right_24dp);
        }
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
        textSearchHandler.find(editText, searchEditText.getText().toString());
        textSearchHandler.jumpToNearestMatch(editText);
    }

    public void clearMatches() {
        textSearchHandler.find(editText, "");
    }

    public void show() {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
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
    }

    public void hide() {
        clearMatches();
        activity.getSupportFragmentManager().beginTransaction().hide(this).commit();
    }

    public void close() {
        clearMatches();
        activity.getSupportFragmentManager().beginTransaction().remove(this).commit();
    }
}
