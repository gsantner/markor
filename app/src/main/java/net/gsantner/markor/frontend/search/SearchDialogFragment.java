package net.gsantner.markor.frontend.search;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
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

public class SearchDialogFragment extends Fragment {

    private final static int CHECKED_COLOR = 0x70F04B4B;

    private int containerViewId;
    private FragmentActivity activity;
    private EditText editText;
    private EditText searchEditText;
    private EditText replaceEditText;
    private TextView resultTextView;
    private final OccurrenceHandler occurrenceHandler = new OccurrenceHandler();

    public static SearchDialogFragment newInstance(@IdRes int containerViewId, FragmentActivity activity, EditText editText) {
        SearchDialogFragment fragment = new SearchDialogFragment();
        fragment.containerViewId = containerViewId;
        fragment.activity = activity;
        fragment.editText = editText;

        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return getLayoutInflater().inflate(R.layout.search_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View fragmentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragmentView, savedInstanceState);

        fragmentView.post(() -> init(fragmentView));

        searchEditText = fragmentView.findViewById(R.id.searchEditText);
        replaceEditText = fragmentView.findViewById(R.id.replaceEditText);
        resultTextView = fragmentView.findViewById(R.id.resultTextView);

        occurrenceHandler.setResultChangedListener((current, count) -> {
            if (count > 0) {
                resultTextView.setText(String.format(getString(R.string.search_result), current, count));
            } else {
                resultTextView.setText(R.string.no_results);
            }
        });

        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                occurrenceHandler.handleSelection(editText, searchEditText);
            }
        });

        searchEditText.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                occurrenceHandler.handleSelection(editText, searchEditText);
            }
            return false;
        });

        searchEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                find();
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                find();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        occurrenceHandler.setFindInSelection(false);
        fragmentView.findViewById(R.id.findInSelectionImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                occurrenceHandler.setFindInSelection(checked);
                occurrenceHandler.handleSelection(editText, null);
                find();
            }
        });

        occurrenceHandler.setMatchCase(false);
        fragmentView.findViewById(R.id.matchCaseImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                occurrenceHandler.setMatchCase(checked);
                find();
            }
        });

        occurrenceHandler.setMatchWholeWord(false);
        fragmentView.findViewById(R.id.matchWholeWordImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                occurrenceHandler.setMatchWholeWord(checked);
                find();
            }
        });

        occurrenceHandler.setUseRegex(false);
        fragmentView.findViewById(R.id.useRegexImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                occurrenceHandler.setUseRegex(checked);
                find();
            }
        });

        occurrenceHandler.setPreserveCase(false);
        fragmentView.findViewById(R.id.preserveCaseImageButton).setOnClickListener(new View.OnClickListener() {
            private boolean checked = false;

            @Override
            public void onClick(View view) {
                checked = toggleViewCheckedState(view, checked);
                occurrenceHandler.setPreserveCase(checked);
            }
        });

        fragmentView.findViewById(R.id.closeImageButton).setOnClickListener(view -> hide());

        fragmentView.findViewById(R.id.toggleImageButton).setOnClickListener(view -> toggleFindReplaceLayout(fragmentView));

        fragmentView.findViewById(R.id.previousImageButton).setOnClickListener(view -> occurrenceHandler.previous(editText));

        fragmentView.findViewById(R.id.nextImageButton).setOnClickListener(view -> occurrenceHandler.next(editText));

        fragmentView.findViewById(R.id.replaceImageButton).setOnClickListener(view -> occurrenceHandler.replace(editText, replaceEditText.getText().toString()));

        fragmentView.findViewById(R.id.replaceAllImageButton).setOnClickListener(view -> occurrenceHandler.replaceAll(editText, replaceEditText.getText().toString()));
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

    private boolean toggleViewCheckedState(View view, boolean checked) {
        if (checked) {
            view.getBackground().clearColorFilter();
        } else {
            view.getBackground().setColorFilter(CHECKED_COLOR, PorterDuff.Mode.DARKEN);
        }
        return !checked;
    }

    private void setReplaceLayoutVisibility(View parent, boolean visible) {
        View replaceLinearLayout = parent.findViewById(R.id.replaceLinearLayout);
        ImageButton imageButton = parent.findViewById(R.id.toggleImageButton);
        if (visible) {
            replaceLinearLayout.setVisibility(View.VISIBLE);
            imageButton.setImageResource(R.drawable.ic_chevron_down);
        } else {
            replaceLinearLayout.setVisibility(View.GONE);
            imageButton.setImageResource(R.drawable.ic_chevron_right);
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
        occurrenceHandler.find(editText, searchEditText.getText().toString());
        occurrenceHandler.jumpNearbyOccurrence(editText);
    }

    public void clear() {
        occurrenceHandler.find(editText, "");
    }

    @Override
    public void onPause() {
        super.onPause();
        close();
    }

    public void show() {
        String tag = String.valueOf(this.containerViewId);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(this.containerViewId, this, tag);
            transaction.commit();
        } else if (!this.isVisible()) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.show(fragment);
            transaction.commit();
            requestEditTextFocus(fragment.getView());
        }
    }

    public void hide() {
        clear();
        activity.getSupportFragmentManager().beginTransaction().hide(this).commit();
    }

    public void close() {
        clear();
        activity.getSupportFragmentManager().beginTransaction().remove(this).commit();
    }
}
