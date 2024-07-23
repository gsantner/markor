/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.gsantner.markor.R;
import net.gsantner.opoc.frontend.GsSearchOrCustomTextDialog;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.io.File;

public class GsFileBrowserDialog extends DialogFragment implements GsFileBrowserOptions.SelectionListener {
    //########################
    //## Static
    //########################
    public static final String FRAGMENT_TAG = "FilesystemViewerCreator";

    public static GsFileBrowserDialog newInstance(final GsFileBrowserOptions.Options options) {
        final GsFileBrowserDialog f = new GsFileBrowserDialog();
        f.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        options.listener.onFsViewerConfig(options);
        f.setDialogOptions(options);
        return f;
    }

    //########################
    //## Member
    //########################
    private RecyclerView _recyclerList;
    private Toolbar _toolBar;
    private TextView _buttonCancel;
    private TextView _buttonOk;
    private FloatingActionButton _homeButton;
    private FloatingActionButton _buttonSearch;
    private FloatingActionButton _buttonNewDir;
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
        final Dialog dialog = getDialog();
        final Window window = dialog != null ? dialog.getWindow() : null;
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            setStyle(STYLE_NORMAL, R.style.AppTheme_Unified);
        }
        return root;
    }

    @Override
    public void onViewCreated(final View root, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        final Activity activity = getActivity();

        _recyclerList = root.findViewById(R.id.ui__filesystem_dialog__list);
        _toolBar = root.findViewById(R.id.ui__filesystem_dialog__title_bar);
        _buttonCancel = root.findViewById(R.id.ui__filesystem_dialog__button_cancel);
        _buttonOk = root.findViewById(R.id.ui__filesystem_dialog__button_ok);
        _homeButton = root.findViewById(R.id.ui__filesystem_dialog__home);
        _buttonNewDir = root.findViewById(R.id.ui__filesystem_dialog__new_dir);
        _buttonSearch = root.findViewById(R.id.ui__filesystem_dialog__search_button);
        _searchEdit = root.findViewById(R.id.ui__filesystem_dialog__search_edit);

        _searchEdit.addTextChangedListener(GsTextWatcherAdapter.on(this::changeAdapterFilter));
        for (final View v : new View[]{_homeButton, _buttonSearch, _buttonNewDir, _buttonCancel, _buttonOk}) {
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

        _toolBar.setTitleTextColor(rcolor(_dopt.titleTextColor));
        _toolBar.setTitle(_dopt.titleText);
        _toolBar.setSubtitleTextColor(rcolor(_dopt.secondaryTextColor));

        _homeButton.setImageResource(_dopt.homeButtonImage);
        _homeButton.setVisibility(_dopt.homeButtonEnable ? View.VISIBLE : View.GONE);
        _homeButton.setColorFilter(rcolor(_dopt.primaryTextColor), android.graphics.PorterDuff.Mode.SRC_ATOP);

        _buttonSearch.setImageResource(_dopt.searchButtonImage);
        _buttonSearch.setVisibility(_dopt.searchEnable ? View.VISIBLE : View.GONE);
        _buttonSearch.setColorFilter(rcolor(_dopt.primaryTextColor), android.graphics.PorterDuff.Mode.SRC_ATOP);

        _buttonNewDir.setImageResource(_dopt.newDirButtonImage);
        _buttonNewDir.setVisibility(_dopt.newDirButtonEnable ? View.VISIBLE : View.GONE);
        _buttonNewDir.setColorFilter(rcolor(_dopt.primaryTextColor), android.graphics.PorterDuff.Mode.SRC_ATOP);

        _searchEdit.setHint(_dopt.searchHint);
        _searchEdit.setTextColor(rcolor(_dopt.primaryTextColor));
        _searchEdit.setHintTextColor(rcolor(_dopt.secondaryTextColor));
        _searchEdit.setOnFocusChangeListener((v, isFocussed) -> {
            GsContextUtils.instance.showSoftKeyboard(getActivity(), isFocussed, _searchEdit);
        });

        root.setBackgroundColor(rcolor(_dopt.backgroundColor));

        // final LinearLayoutManager lam = (LinearLayoutManager) _recyclerList.getLayoutManager();
        // final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(activity, lam.getOrientation());
        // _recyclerList.addItemDecoration(dividerItemDecoration);
        _recyclerList.setItemViewCacheSize(20);

        _filesystemViewerAdapter = new GsFileBrowserListAdapter(_dopt, activity);
        _recyclerList.setAdapter(_filesystemViewerAdapter);
        _filesystemViewerAdapter.getFilter().filter("");
        onFsViewerDoUiUpdate(_filesystemViewerAdapter);

        // Setup callbacks
        _dopt.setSubtitle = _toolBar::setSubtitle;
        _dopt.setTitle = _toolBar::setTitle;
    }

    private int rcolor(@ColorRes int colorRes) {
        return ContextCompat.getColor(getContext(), colorRes);
    }

    private void setDialogOptions(GsFileBrowserOptions.Options options) {
        _dopt = options;
        _callback = _dopt.listener;
        _dopt.listener = this;
    }

    public void changeAdapterFilter(CharSequence s, int start, int before, int count) {
        if (_filesystemViewerAdapter != null) {
            _filesystemViewerAdapter.getFilter().filter(s.toString());
        }
    }

    public void onClicked(final View view) {
        switch (view.getId()) {
            case R.id.ui__filesystem_dialog__button_ok:
            case R.id.ui__filesystem_dialog__home: {
                _filesystemViewerAdapter.onClick(view);
                break;
            }
            case R.id.ui__filesystem_dialog__search_button: {
                if (_searchEdit.getVisibility() == View.GONE) {
                    _searchEdit.setText("");
                    _searchEdit.setVisibility(View.VISIBLE);
                    _searchEdit.requestFocus();
                } else {
                    _searchEdit.setText("");
                    _searchEdit.setVisibility(View.GONE);
                    _searchEdit.clearFocus();
                }
                break;
            }
            case R.id.ui__filesystem_dialog__button_cancel: {
                onFsViewerCancel(_dopt.requestId);
                break;
            }
            case R.id.ui__filesystem_dialog__new_dir: {
                showNewDirDialog();
                break;
            }
        }
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        GsContextUtils.instance.showSoftKeyboard(getActivity(), false, _searchEdit);
    }

    private void showNewDirDialog() {
        final Activity activity = getActivity();

        if (activity == null) {
            return;
        }

        final GsSearchOrCustomTextDialog.DialogOptions dopt = new GsSearchOrCustomTextDialog.DialogOptions();
        dopt.isDarkDialog = GsContextUtils.instance.isDarkModeEnabled(activity);
        dopt.titleText = _dopt.newDirButtonText;
        dopt.textColor = rcolor(_dopt.primaryTextColor);
        dopt.searchHintText = android.R.string.untitled;
        dopt.searchInputFilter = GsContextUtils.instance.makeFilenameInputFilter();
        dopt.callback = name -> _filesystemViewerAdapter.createDirectoryHere(name);

        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    @Override
    public void onFsViewerSelected(final String request, final File file, final Integer lineNumber) {
        if (_callback != null) {
            _callback.onFsViewerSelected(_dopt.requestId, file, lineNumber);
        }
        if (_dopt.dismissAfterCallback) {
            dismiss();
        }
    }

    @Override
    public void onFsViewerMultiSelected(String request, File... files) {
        if (_callback != null) {
            _callback.onFsViewerMultiSelected(_dopt.requestId, files);
        }
        if (_dopt.dismissAfterCallback) {
            dismiss();
        }
    }

    @Override
    public void onFsViewerCancel(String request) {
        if (_callback != null) {
            _callback.onFsViewerCancel(_dopt.requestId);
        }
        if (_dopt.dismissAfterCallback) {
            dismiss();
        }
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
        if (adapter.getCurrentFolder() != null) {
            _toolBar.setSubtitle(adapter.getCurrentFolder().getName());
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
