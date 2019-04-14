/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui;

import android.arch.core.util.Function;
import android.content.Context;
import android.support.v4.app.FragmentManager;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.ui.FilesystemDialog;
import net.gsantner.opoc.ui.FilesystemDialogData;

import java.io.File;
import java.util.List;

public class FilesystemDialogCreator {
    public static Function<File, Boolean> IsMimeText = file -> file != null && ContextUtils.get().getMimeType(file).startsWith("text/");
    public static Function<File, Boolean> IsMimeImage = file -> file != null && ContextUtils.get().getMimeType(file).startsWith("image/");

    public static FilesystemDialogData.Options prepareFsDialogOpts(Context context, boolean doSelectFolder, FilesystemDialogData.SelectionListener listener) {
        FilesystemDialogData.Options opts = new FilesystemDialogData.Options();
        ContextUtils cu = new ContextUtils(context);
        ShareUtil shareUtil = new ShareUtil(context);
        AppSettings appSettings = new AppSettings(context);
        boolean titleLight = cu.shouldColorOnTopBeLight(cu.rcolor(opts.primaryColor));
        boolean darkTheme = appSettings.isDarkThemeEnabled();

        if (listener != null) {
            opts.listener = listener;
        }
        opts.doSelectFolder = doSelectFolder;
        opts.doSelectFile = !doSelectFolder;

        opts.searchHint = R.string.search_documents;
        opts.searchButtonImage = R.drawable.ic_search_black_24dp;
        opts.homeButtonImage = R.drawable.ic_home_black_24dp;
        opts.selectedItemImage = R.drawable.ic_check_black_24dp;
        opts.upButtonEnable = true;
        opts.homeButtonEnable = true;
        opts.mustStartWithRootFolder = false;

        opts.fileComparable = (o1, o2) -> {
            String m1 = ContextUtils.get().getMimeType(o1);
            String m2 = ContextUtils.get().getMimeType(o2);
            if (m1.startsWith("text/") || m2.startsWith("text")) {
                if (m1.startsWith("text/") && !m2.startsWith("text/")) {
                    return -1;
                } else if (m2.startsWith("text/") && !m1.startsWith("text/")) {
                    return 1;
                }
            }
            return 0;
        };

        opts.primaryTextColor = darkTheme ? R.color.dark__primary_text : R.color.light__primary_text;
        opts.secondaryTextColor = darkTheme ? R.color.dark__secondary_text : R.color.light__secondary_text;
        opts.backgroundColor = darkTheme ? R.color.dark__background : R.color.light__background;
        opts.titleTextColor = titleLight ? R.color.dark__primary_text : R.color.light__primary_text;
        opts.fileImage = R.drawable.ic_file_white_24dp;
        opts.folderImage = R.drawable.ic_folder_white_24dp;

        opts.recentFiles = strlistToArray(appSettings.getRecentDocuments());
        opts.popularFiles = strlistToArray(appSettings.getPopularDocuments());
        opts.favouriteFiles = appSettings.getFavouriteFiles();

        opts.titleText = R.string.select;

        opts.mountedStorageFolder = shareUtil.getStorageAccessFolder();

        shareUtil.freeContextRef();
        cu.freeContextRef();
        return opts;
    }

    public static File[] strlistToArray(List<String> strlist) {
        File[] files = new File[strlist.size()];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(strlist.get(i));
        }
        return files;
    }

    private static void showDialog(FragmentManager fm, FilesystemDialogData.Options opts) {
        FilesystemDialog filesystemDialog = FilesystemDialog.newInstance(opts);
        filesystemDialog.show(fm, FilesystemDialog.FRAGMENT_TAG);
    }

    public static void showFileDialog(FilesystemDialogData.SelectionListener listener, FragmentManager fm, Context context, Function<File, Boolean> fileOverallFilter) {
        final FilesystemDialogData.Options opts = prepareFsDialogOpts(context, false, listener);
        opts.fileOverallFilter = fileOverallFilter;
        showDialog(fm, opts);
    }

    public static void showFolderDialog(FilesystemDialogData.SelectionListener listener, FragmentManager fm, Context context) {
        final FilesystemDialogData.Options opts = prepareFsDialogOpts(context, true, listener);
        opts.okButtonText = R.string.select_this_folder;
        showDialog(fm, opts);
    }
}
