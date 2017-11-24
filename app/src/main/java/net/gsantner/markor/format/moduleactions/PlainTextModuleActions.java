/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.moduleactions;

import android.app.Activity;
import android.view.ViewGroup;

import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.model.Document;

public class PlainTextModuleActions extends TextModuleActions {

    public PlainTextModuleActions(Activity activity, Document document, HighlightingEditor hlEditor) {
        super(activity, document, hlEditor);
    }

    @Override
    public void appendTextModuleActionsToBar(ViewGroup barLayout) {
        setBarVisible(barLayout, false);
    }
}
