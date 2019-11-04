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
package net.gsantner.opoc.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.opoc.util.ContextUtils;

import butterknife.ButterKnife;

/**
 * A common base fragment to extend from
 */
public abstract class GsFragmentBase extends Fragment {
    private boolean _fragmentFirstTimeVisible = true;
    private final Object _fragmentFirstTimeVisibleSync = new Object();

    protected ContextUtils _cu;
    protected Bundle _savedInstanceState = null;
    protected Menu _fragmentMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Inflate the fragments layout. Don't override this method, just supply the needed
     * {@link LayoutRes} via abstract method {@link #getLayoutResId()}, super does the rest
     */
    @Deprecated
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _cu = new ContextUtils(inflater.getContext());
        _cu.setAppLanguage(getAppLanguage());
        _savedInstanceState = savedInstanceState;
        if (getLayoutResId() == 0) {
            Log.e(getClass().getCanonicalName(), "Error: GsFragmentbase.onCreateview: Returned 0 for getLayoutResId");
        }
        View view = inflater.inflate(getLayoutResId(), container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.postDelayed(() -> {
            synchronized (_fragmentFirstTimeVisibleSync) {
                if (getUserVisibleHint() && isVisible() && _fragmentFirstTimeVisible) {
                    _fragmentFirstTimeVisible = false;
                    onFragmentFirstTimeVisible();
                }
            }
        }, 1);
    }

    /**
     * Get a tag from the fragment, allows faster distinction
     *
     * @return This fragments tag
     */
    public abstract String getFragmentTag();


    /**
     * Get the layout to be inflated in the fragment
     *
     * @return Layout resource id
     */
    @LayoutRes
    protected abstract int getLayoutResId();

    /**
     * Event to be called when the back button was pressed
     * True should be returned when this was handled by the fragment
     * and  no further handling in the view hierarchy is needed
     *
     * @return True if back handled by fragment
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * Set the language to be used in this fragment
     * Defaults to resolve the language from sharedpreferences: pref_key__language
     *
     * @return Empty string for system language, or an android locale code
     */
    public String getAppLanguage() {
        if (getContext() != null) {
            return getContext().getSharedPreferences("app", Context.MODE_PRIVATE)
                    .getString("pref_key__language", "");
        }
        return "";
    }

    /**
     * This will be called when this fragment gets the first time visible
     */
    public void onFragmentFirstTimeVisible() {
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        synchronized (_fragmentFirstTimeVisibleSync) {
            if (isVisibleToUser && _fragmentFirstTimeVisible) {
                _fragmentFirstTimeVisible = false;
                onFragmentFirstTimeVisible();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        _fragmentMenu = menu;
    }

    public Menu getFragmentMenu() {
        return _fragmentMenu;
    }

    /**
     * Get the toolbar from activity
     * Requires id to be set to @+id/toolbar
     */
    @SuppressWarnings("ConstantConditions")
    protected Toolbar getToolbar() {
        try {
            return (Toolbar) getActivity().findViewById(new ContextUtils(getActivity()).getResId(ContextUtils.ResType.ID, "toolbar"));
        } catch (Exception e) {
            return null;
        }
    }
}
