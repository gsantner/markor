/*
 * Copyright (c) 2017-2018 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.moduleactions;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.view.KeyEvent;

import net.gsantner.markor.R;
import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;

public class CommonTextModuleActions {
    public static final int ACTION_TEXT_NAVIGATION__ICON = R.drawable.ic_unfold_more_black_24dp;
    public static final String ACTION_TEXT_NAVIGATION = "text_navigation";

    private final Activity _activity;
    private final Document _document;
    private final HighlightingEditor _hlEditor;

    public CommonTextModuleActions(Activity activity, Document document, HighlightingEditor hlEditor) {
        _activity = activity;
        _document = document;
        _hlEditor = hlEditor;
    }

    private String rstr(@StringRes int resKey) {
        return _activity.getString(resKey);
    }

    // Returns true when handled
    public boolean runAction(String action) {
        switch (action) {
            case ACTION_TEXT_NAVIGATION: {
                SearchOrCustomTextDialogCreator.showTextNavigationDialog(_activity, (callbackPayload) -> {
                    if (!_hlEditor.hasSelection() && _hlEditor.length() > 0) {
                        _hlEditor.requestFocus();
                        _hlEditor.setSelection(0);
                    }
                    if (callbackPayload.equals(rstr(R.string.key_page_down))) {
                        _hlEditor.pressKeyOnce(KeyEvent.KEYCODE_PAGE_DOWN);
                    } else if (callbackPayload.equals(rstr(R.string.key_page_up))) {
                        _hlEditor.pressKeyOnce(KeyEvent.KEYCODE_PAGE_UP);
                    } else if (callbackPayload.equals(rstr(R.string.key_pos_1))) {
                        _hlEditor.pressKeyOnce(KeyEvent.KEYCODE_MOVE_HOME);
                    } else if (callbackPayload.equals(rstr(R.string.key_pos_end))) {
                        //_hlEditor.pressKeyOnce(KeyEvent.KEYCODE_MOVE_END);
                        int targetpos = _hlEditor.length() - 1;
                        if (targetpos > 0) {
                            _hlEditor.setSelection(targetpos);
                        }
                    }
                });
                return true;
            }
        }
        return false;
    }
}
