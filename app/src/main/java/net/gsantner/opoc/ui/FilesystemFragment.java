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
 * Revision 001 of FilesystemDialogCreator
 * A simple filesystem dialog with file, folder and multiple selection
 * most bits (color, text, images) can be controller using FilesystemDialogData.
 * The data container contains a listener callback for results.
 * Most features are usable without any additional project files and resources
 *
 * Required: Butterknife library
 */
package net.gsantner.opoc.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import net.gsantner.markor.R;
import net.gsantner.opoc.activity.GsFragmentBase;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilesystemFragment extends GsFragmentBase
        implements FilesystemDialogData.SelectionListener {
    //########################
    //## Static
    //########################
    public static final String FRAGMENT_TAG = "FilesystemDialogCreator";

    public static FilesystemFragment newInstance(FilesystemDialogData.Options options) {
        FilesystemFragment f = new FilesystemFragment();
        options.listener.onFsDialogConfig(options);
        f.setDialogOptions(options);
        return f;
    }

    //########################
    //## Member
    //########################
    @BindView(R.id.ui__filesystem_dialog__list)
    RecyclerView _recyclerList;

    @BindView(R.id.pull_to_refresh)
    public SwipeRefreshLayout swipe;

    private FilesystemDialogAdapter _filesystemDialogAdapter;
    private FilesystemDialogData.Options _dopt;
    private FilesystemDialogData.SelectionListener _callback;

    //########################
    //## Methods
    //########################

    @Override
    public void onViewCreated(View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        Context context = getContext();
        ButterKnife.bind(this, root);

        LinearLayoutManager lam = (LinearLayoutManager) _recyclerList.getLayoutManager();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), lam.getOrientation());
        _recyclerList.addItemDecoration(dividerItemDecoration);

        _filesystemDialogAdapter = new FilesystemDialogAdapter(_dopt, context, _recyclerList);
        _recyclerList.setAdapter(_filesystemDialogAdapter);
        _filesystemDialogAdapter.getFilter().filter("");
        onFsDoUiUpdate(_filesystemDialogAdapter);

        swipe.setOnRefreshListener(() -> {
            _filesystemDialogAdapter.reloadCurrentFolder();
            swipe.setRefreshing(false);
        });

        _filesystemDialogAdapter.restoreSavedInstanceState(savedInstanceState);
    }

    @Override
    public String getFragmentTag() {
        return "FilesystemFragment";
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.opoc_filesystem_fragment;
    }

    private int rcolor(@ColorRes int colorRes) {
        return ContextCompat.getColor(getActivity(), colorRes);
    }

    private void setDialogOptions(FilesystemDialogData.Options options) {
        _dopt = options;
        _callback = _dopt.listener;
        _dopt.listener = this;
        checkOptions();
    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.ui__filesystem_dialog__button_ok:
            case R.id.ui__filesystem_dialog__home: {
                _filesystemDialogAdapter.onClick(view);
                break;
            }
            case R.id.ui__filesystem_dialog__button_cancel: {
                onFsNothingSelected(_dopt.requestId);
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
    public void onFsSelected(String request, File file) {
        if (_callback != null) {
            _callback.onFsSelected(_dopt.requestId, file);
        }
    }

    @Override
    public void onFsMultiSelected(String request, File... files) {
        if (_callback != null) {
            _callback.onFsMultiSelected(_dopt.requestId, files);
        }
    }

    @Override
    public void onFsNothingSelected(String request) {
        if (_callback != null) {
            _callback.onFsNothingSelected(_dopt.requestId);
        }
    }

    @Override
    public void onFsDialogConfig(FilesystemDialogData.Options opt) {
        if (_callback != null) {
            _callback.onFsDialogConfig(opt);
        }
    }

    @Override
    public void onFsDoUiUpdate(FilesystemDialogAdapter adapter) {
        if (_callback != null) {
            _callback.onFsDoUiUpdate(adapter);
        }
    }

    @Override
    public void onFsLongPressed(File file, boolean doSelectMultiple) {
        if (_callback != null) {
            _callback.onFsLongPressed(file, doSelectMultiple);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (_filesystemDialogAdapter.canGoUp() && !_filesystemDialogAdapter.isCurrentFolderHome()) {
            _filesystemDialogAdapter.goUp();
            return true;
        }
        return super.onBackPressed();
    }

    public File getCurrentFolder() {
        return _filesystemDialogAdapter.getCurrentFolder();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState = _filesystemDialogAdapter.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        _filesystemDialogAdapter.reloadCurrentFolder();
    }
}
