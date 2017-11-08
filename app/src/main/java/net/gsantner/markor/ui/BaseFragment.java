package net.gsantner.markor.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Base class for custom fragments
 */
public abstract class BaseFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Get the fragment's tag
     *
     * @return the tag
     */
    public abstract String getFragmentTag();

    /**
     * Back button was pressed, return true if was handled by fragment
     *
     * @return True if back handled by fragment
     */
    public abstract boolean onBackPressed();
}
