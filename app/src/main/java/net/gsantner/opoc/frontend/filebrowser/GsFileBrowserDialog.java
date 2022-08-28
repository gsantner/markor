/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/

/*
 * Revision 001 of FilesystemViewerCreator
 * A simple filesystem dialog with file, folder and multiple selection
 * most bits (color, text, images) can be controller using FilesystemViewerData.
 * The data container contains a listener callback for results.
 * Most features are usable without any additional project files and resources
 */
package net.gsantner.opoc.frontend.filebrowser;

import android.content.Context;
import android.os.Bundle;
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

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.markor.R;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.io.File;

public class GsFileBrowserDialog extends DialogFragment implements GsFileBrowserOptions.SelectionListener {
    //########################
    //## Static
    //########################
    public static final String FRAGMENT_TAG = "FilesystemViewerCreator";

    public static GsFileBrowserDialog newInstance(GsFileBrowserOptions.Options options) {
        GsFileBrowserDialog f = new GsFileBrowserDialog();
        f.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        options.listener.onFsViewerConfig(options);
        f.setDialogOptions(options);
        return f;
    }

    //########################
    //## Member
    //########################
    private RecyclerView _recyclerList;
    public TextView _dialogTitle;
    private TextView _buttonCancel;
    private TextView _buttonOk;
    private LinearLayout _utilBar;
    private LinearLayout _buttonBar;
    private ImageView _homeButton;
    private ImageView _buttonSearch;
    private EditText _searchEdit;

    private GsFileBrowserListAdapter _filesystemViewerAdapter;
    private GsFileBrowserOptions.Options _dopt;
    private GsFileBrowserOptions.SelectionListener _callback;

    //########################
    //## Methods
    //########################
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.opoc_filesystem_dialog, container, false);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        return root;
    }

    @Override
    public void onViewCreated(View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        Context context = getContext();
        _recyclerList = root.findViewById(R.id.ui__filesystem_dialog__list);
        _dialogTitle = root.findViewById(R.id.ui__filesystem_dialog__title);
        _buttonCancel = root.findViewById(R.id.ui__filesystem_dialog__button_cancel);
        _buttonOk = root.findViewById(R.id.ui__filesystem_dialog__button_ok);
        _utilBar = root.findViewById(R.id.ui__filesystem_dialog__utilbar);
        _buttonBar = root.findViewById(R.id.ui__filesystem_dialog__buttons);
        _homeButton = root.findViewById(R.id.ui__filesystem_dialog__home);
        _buttonSearch = root.findViewById(R.id.ui__filesystem_dialog__search_button);
        _searchEdit = root.findViewById(R.id.ui__filesystem_dialog__search_edit);

        _searchEdit.addTextChangedListener(GsTextWatcherAdapter.on(this::changeAdapterFilter));
        for (final View v : new View[]{_homeButton, _buttonSearch, _buttonCancel, _buttonOk}) {
            v.setOnClickListener(this::onClicked);
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

        _filesystemViewerAdapter = new GsFileBrowserListAdapter(_dopt, context);
        _recyclerList.setAdapter(_filesystemViewerAdapter);
        _filesystemViewerAdapter.getFilter().filter("");
        onFsViewerDoUiUpdate(_filesystemViewerAdapter);
    }

    private int rcolor(@ColorRes int colorRes) {
        return ContextCompat.getColor(getContext(), colorRes);
    }

    private void setDialogOptions(GsFileBrowserOptions.Options options) {
        _dopt = options;
        _callback = _dopt.listener;
        _dopt.listener = this;
        checkOptions();
    }

    public void changeAdapterFilter(CharSequence s, int start, int before, int count) {
        if (_filesystemViewerAdapter != null) {
            _filesystemViewerAdapter.getFilter().filter(s.toString());
        }
    }

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
    public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
        if (_callback != null) {
            _callback.onFsViewerSelected(_dopt.requestId, file, lineNumber);
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
    public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
        if (_callback != null) {
            _callback.onFsViewerConfig(dopt);
        }
    }

    @Override
    public void onFsViewerDoUiUpdate(GsFileBrowserListAdapter adapter) {
        if (_dopt.doSelectMultiple && _dopt.doSelectFile) {
            _buttonOk.setVisibility(adapter.areItemsSelected() ? View.VISIBLE : View.GONE);
        }
        if (_callback != null) {
            _callback.onFsViewerDoUiUpdate(adapter);
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
