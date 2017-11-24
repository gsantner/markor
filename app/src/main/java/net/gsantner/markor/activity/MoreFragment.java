/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.opoc.util.ActivityUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

// TODO: Replace this quickly added content with something better useable, e.g. RecyclerView + Adapter
public class MoreFragment extends BaseFragment {
    public static final String FRAGMENT_TAG = "MoreFragment";

    public static MoreFragment newInstance() {
        MoreFragment f = new MoreFragment();
        Bundle args = new Bundle();
        f.setArguments(args);
        return f;
    }

    private View _view;
    private Context _context;

    public MoreFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.more__fragment, container, false);
        ButterKnife.bind(this, view);
        _view = view;
        _context = view.getContext();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        _view.findViewById(R.id.more__fragment__action_donate).setVisibility(ContextUtils.get().isGooglePlayBuild() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();

        int fg = AppSettings.get().isDarkThemeEnabled() ? Color.WHITE : Color.BLACK;


        for (int resid : new int[]{R.id.more__fragment__action_about, R.id.more__fragment__action_contribute, R.id.more__fragment__action_donate, R.id.more__fragment__action_settings}) {
            LinearLayout layout = _view.findViewById(resid);
            ((ImageView) (layout.getChildAt(0))).setColorFilter(fg, android.graphics.PorterDuff.Mode.MULTIPLY);
            ((TextView) (layout.getChildAt(1))).setTextColor(fg);
        }
    }

    @OnClick({R.id.more__fragment__action_about, R.id.more__fragment__action_contribute, R.id.more__fragment__action_donate, R.id.more__fragment__action_settings})
    public void onClick(View view) {
        ActivityUtils au = new ActivityUtils(getActivity());
        switch (view.getId()) {
            case R.id.more__fragment__action_settings: {
                au.animateToActivity(SettingsActivity.class, false, 124);
                break;
            }
            case R.id.more__fragment__action_about: {
                au.animateToActivity(AboutActivity.class, false, 123);
                break;
            }
            case R.id.more__fragment__action_donate: {
                au.openWebpageInExternalBrowser(getString(R.string.url_donate));
                break;
            }
            case R.id.more__fragment__action_contribute: {
                au.openWebpageInExternalBrowser(getString(R.string.url_contribute));
                break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.more__menu, menu);
        ContextUtils cu = ContextUtils.get();

        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}