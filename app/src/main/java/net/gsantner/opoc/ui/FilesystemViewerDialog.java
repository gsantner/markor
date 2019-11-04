/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
/*
 * Revision 001 of FilesystemViewerCreator
 * A simple filesystem dialog with file, folder and multiple selection
 * most bits (color, text, images) can be controller using FilesystemViewerData.
 * The data container contains a listener callback for results.
 * Most features are usable without any additional project files and resources
 *
 * Required: Butterknife library
 */
package net.gsantner.opoc.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.gsantner.markor.R;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

public class FilesystemViewerDialog extends DialogFragment
        implements FilesystemViewerData.SelectionListener {
    //########################
    //## Static
    //########################
    public static final String FRAGMENT_TAG = "FilesystemViewerCreator";

    public static FilesystemViewerDialog newInstance(FilesystemViewerData.Options options) {
        FilesystemViewerDialog f = new FilesystemViewerDialog();
        f.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        options.listener.onFsViewerConfig(options);
        f.setDialogOptions(options);
        return f;
    }

    //########################
    //## Member
    //########################
    @BindView(R.id.ui__filesystem_dialog__list)
    RecyclerView _recyclerList;

    @BindView(R.id.ui__filesystem_dialog__title)
    TextView _dialogTitle;

    @BindView(R.id.ui__filesystem_dialog__button_cancel)
    TextView _buttonCancel;

    @BindView(R.id.ui__filesystem_dialog__button_ok)
    TextView _buttonOk;

    @BindView(R.id.ui__filesystem_dialog__utilbar)
    LinearLayout _utilBar;

    @BindView(R.id.ui__filesystem_dialog__buttons)
    LinearLayout _buttonBar;

    @BindView(R.id.ui__filesystem_dialog__home)
    ImageView _homeButton;

    @BindView(R.id.ui__filesystem_dialog__search_button)
    ImageView _buttonSearch;

    @BindView(R.id.ui__filesystem_dialog__search_edit)
    EditText _searchEdit;

    private FilesystemViewerAdapter _filesystemViewerAdapter;
    private FilesystemViewerData.Options _dopt;
    private FilesystemViewerData.SelectionListener _callback;

    //########################
    //## Methods
    //########################
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.opoc_filesystem_dialog, container, false);
        ButterKnife.bind(this, root);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return root;
    }

    @Override
    public void onViewCreated(View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        Context context = getContext();
        if (_buttonCancel == null) {
            ButterKnife.bind(this, root);
            if (_buttonCancel == null) {
                System.err.println("Error: at " + getClass().getName() + " :: Could not bind UI");
            }
        }

        if (_dopt == null || _buttonCancel == null) {
            dismiss();
            return;
        }

        _buttonCancel.setVisibility(_dopt.cancelButtonEnable ? View.VISIBLE : View.GONE);
        _buttonCancel.setTextColor(rcolor(_dopt.accentColor));
        _buttonCancel.setText(_dopt.cancelButtonText);

        _buttonOk.setVisibility(_dopt.okButtonEnable ? View.VISIBLE : View.GONE);
        _buttonOk.setTextColor(rcolor(_dopt.accentColor));
        _buttonOk.setText(_dopt.okButtonText);

        _dialogTitle.setTextColor(rcolor(_dopt.titleTextColor));
        _dialogTitle.setBackgroundColor(rcolor(_dopt.primaryColor));
        _dialogTitle.setText(_dopt.titleText);
        _dialogTitle.setVisibility(_dopt.titleTextEnable ? View.VISIBLE : View.GONE);

        _homeButton.setImageResource(_dopt.homeButtonImage);
        _homeButton.setVisibility(_dopt.homeButtonEnable ? View.VISIBLE : View.GONE);
        _homeButton.setColorFilter(rcolor(_dopt.primaryTextColor), android.graphics.PorterDuff.Mode.SRC_ATOP);

        _buttonSearch.setImageResource(_dopt.searchButtonImage);
        _buttonSearch.setVisibility(_dopt.searchEnable ? View.VISIBLE : View.GONE);
        _buttonSearch.setColorFilter(rcolor(_dopt.primaryTextColor), android.graphics.PorterDuff.Mode.SRC_ATOP);

        _searchEdit.setHint(_dopt.searchHint);
        _searchEdit.setTextColor(rcolor(_dopt.primaryTextColor));
        _searchEdit.setHintTextColor(rcolor(_dopt.secondaryTextColor));

        root.setBackgroundColor(rcolor(_dopt.backgroundColor));

        LinearLayoutManager lam = (LinearLayoutManager) _recyclerList.getLayoutManager();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), lam.getOrientation());
        _recyclerList.addItemDecoration(dividerItemDecoration);

        _filesystemViewerAdapter = new FilesystemViewerAdapter(_dopt, context);
        _recyclerList.setAdapter(_filesystemViewerAdapter);
        _filesystemViewerAdapter.getFilter().filter("");
        onFsViewerDoUiUpdate(_filesystemViewerAdapter);
    }

    private int rcolor(@ColorRes int colorRes) {
        return ContextCompat.getColor(getActivity(), colorRes);
    }

    private void setDialogOptions(FilesystemViewerData.Options options) {
        _dopt = options;
        _callback = _dopt.listener;
        _dopt.listener = this;
        checkOptions();
    }

    @OnTextChanged(value = R.id.ui__filesystem_dialog__search_edit, callback = OnTextChanged.Callback.TEXT_CHANGED)
    public void changeAdapterFilter(CharSequence s, int start, int before, int count) {
        if (_filesystemViewerAdapter != null) {
            _filesystemViewerAdapter.getFilter().filter(s.toString());
        }
    }

    @OnClick({R.id.ui__filesystem_dialog__home, R.id.ui__filesystem_dialog__search_button, R.id.ui__filesystem_dialog__button_cancel, R.id.ui__filesystem_dialog__button_ok})
    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.ui__filesystem_dialog__button_ok:
            case R.id.ui__filesystem_dialog__home: {
                _filesystemViewerAdapter.onClick(view);
                break;
            }
            case R.id.ui__filesystem_dialog__search_button: {
                _buttonSearch.setVisibility(View.GONE);
                _searchEdit.setVisibility(View.VISIBLE);
                _searchEdit.requestFocus();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(_searchEdit, InputMethodManager.SHOW_IMPLICIT);
                break;
            }
            case R.id.ui__filesystem_dialog__button_cancel: {
                onFsViewerNothingSelected(_dopt.requestId);
                break;
            }

        }
    }

    private void checkOptions() {
        if (_dopt.doSelectFile && !_dopt.doSelectMultiple) {
            _dopt.okButtonEnable = false;
        }
    }

    @Override
    public void onFsViewerSelected(String request, File file) {
        if (_callback != null) {
            _callback.onFsViewerSelected(_dopt.requestId, file);
        }
        dismiss();
    }

    @Override
    public void onFsViewerMultiSelected(String request, File... files) {
        if (_callback != null) {
            _callback.onFsViewerMultiSelected(_dopt.requestId, files);
        }
        dismiss();
    }

    @Override
    public void onFsViewerNothingSelected(String request) {
        if (_callback != null) {
            _callback.onFsViewerNothingSelected(_dopt.requestId);
        }
        dismiss();
    }

    @Override
    public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
        if (_callback != null) {
            _callback.onFsViewerConfig(dopt);
        }
    }

    @Override
    public void onFsViewerDoUiUpdate(FilesystemViewerAdapter adapter) {
        if (_dopt.doSelectMultiple && _dopt.doSelectFile) {
            _buttonOk.setVisibility(adapter.areItemsSelected() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onFsViewerItemLongPressed(File file, boolean doSelectMultiple) {
        if (_callback != null) {
            _callback.onFsViewerItemLongPressed(file, doSelectMultiple);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Window w;
        if (getDialog() != null && (w = getDialog().getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }
}
