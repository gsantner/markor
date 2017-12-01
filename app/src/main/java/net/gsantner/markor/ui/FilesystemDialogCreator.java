/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.net> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.markor.ui;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.opoc.ui.FilesystemDialog;
import net.gsantner.opoc.ui.FilesystemDialogData;

public class FilesystemDialogCreator {
    private static FilesystemDialogData.Options prepareFsDialogOpts
            (Context context, boolean doSelectFolder, FilesystemDialogData.SelectionListener listener) {
        FilesystemDialogData.Options opts = new FilesystemDialogData.Options();
        ContextUtils cu = new ContextUtils(context);
        boolean titleLight = cu.shouldColorOnTopBeLight(cu.color(opts.primaryColor));
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();

        if (listener != null) {
            opts.listener = listener;
        }
        opts.doSelectFolder = doSelectFolder;
        opts.doSelectFile = !doSelectFolder;

        opts.searchHint = R.string.search_documents;
        opts.searchButtonImage = R.drawable.ic_action_search;
        opts.homeButtonImage = R.drawable.ic_home_black_24dp;
        opts.upButtonImage = R.drawable.ic_arrow_back_white_24dp;
        opts.selectedItemImage = R.drawable.ic_check_black_24dp;
        opts.upButtonEnable = true;
        opts.homeButtonEnable = true;

        opts.primaryTextColor = darkTheme ? R.color.dark__primary_text : R.color.light__primary_text;
        opts.secondaryTextColor = darkTheme ? R.color.dark__secondary_text : R.color.light__secondary_text;
        opts.backgroundColor = darkTheme ? R.color.dark__background : R.color.light__background;
        opts.titleTextColor = titleLight ? R.color.dark__primary_text : R.color.light__primary_text;
        opts.fileImage = R.drawable.ic_file_white_24dp;
        opts.folderImage = R.drawable.ic_folder_white_24dp;

        opts.titleText = R.string.select;

        return opts;
    }

    private static void showDialog(FragmentManager fm, FilesystemDialogData.Options opts) {
        FilesystemDialog filesystemDialog = FilesystemDialog.newInstance(opts);
        filesystemDialog.show(fm, FilesystemDialog.FRAGMENT_TAG);
    }

    public static void showFileDialog(FilesystemDialogData.SelectionListener listener, FragmentManager fm, Context context) {
        final FilesystemDialogData.Options opts = prepareFsDialogOpts(context, false, listener);
        showDialog(fm, opts);
    }

    public static void showFolderDialog(FilesystemDialogData.SelectionListener listener, FragmentManager fm, Context context) {
        final FilesystemDialogData.Options opts = prepareFsDialogOpts(context, true, listener);
        opts.okButtonText = R.string.select_this_folder;
        showDialog(fm, opts);
    }
}
