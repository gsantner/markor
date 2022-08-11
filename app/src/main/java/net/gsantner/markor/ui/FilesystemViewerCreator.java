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

import android.content.Context;

import androidx.arch.core.util.Function;
import androidx.fragment.app.FragmentManager;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.ui.FilesystemViewerData;
import net.gsantner.opoc.ui.FilesystemViewerDialog;

import java.io.File;
import java.util.List;

public class FilesystemViewerCreator {
    public static Function<File, Boolean> IsMimeText = file -> file != null && ContextUtils.get().getMimeType(file).startsWith("text/");
    public static Function<File, Boolean> IsMimeImage = file -> file != null && ContextUtils.get().getMimeType(file).startsWith("image/");
    public static Function<File, Boolean> IsMimeAudio = file -> file != null && ContextUtils.get().getMimeType(file).startsWith("audio/");
    public static Function<File, Boolean> IsMimeVideo = file -> file != null && ContextUtils.get().getMimeType(file).startsWith("video/");

    public static FilesystemViewerData.Options prepareFsViewerOpts(Context context, boolean doSelectFolder, FilesystemViewerData.SelectionListener listener) {
        FilesystemViewerData.Options opts = new FilesystemViewerData.Options();
        ContextUtils cu = new ContextUtils(context);
        ShareUtil shareUtil = new ShareUtil(context);
        AppSettings appSettings = new AppSettings(context);

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
        opts.contentDescriptionFolder = R.string.folder;
        opts.contentDescriptionSelected = R.string.selected;
        opts.contentDescriptionFile = R.string.file;

        opts.accentColor = R.color.accent;
        opts.primaryColor = R.color.primary;
        opts.primaryTextColor = R.color.primary_text;
        opts.secondaryTextColor = R.color.secondary_text;
        opts.backgroundColor = R.color.background;
        opts.titleTextColor = R.color.primary_text;
        opts.fileImage = R.drawable.ic_file_white_24dp;
        opts.folderImage = R.drawable.ic_folder_white_24dp;

        opts.recentFiles = appSettings.getAsFileList(appSettings.getRecentDocuments());
        opts.popularFiles = appSettings.getAsFileList(appSettings.getPopularDocuments());
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

    private static void showDialog(FragmentManager fm, FilesystemViewerData.Options opts) {
        FilesystemViewerDialog filesystemViewerDialog = FilesystemViewerDialog.newInstance(opts);
        filesystemViewerDialog.show(fm, FilesystemViewerDialog.FRAGMENT_TAG);
    }

    public static void showFileDialog(FilesystemViewerData.SelectionListener listener, FragmentManager fm, Context context, Function<File, Boolean> fileOverallFilter) {
        final FilesystemViewerData.Options opts = prepareFsViewerOpts(context, false, listener);
        opts.fileOverallFilter = fileOverallFilter;
        showDialog(fm, opts);
    }

    public static void showFolderDialog(FilesystemViewerData.SelectionListener listener, FragmentManager fm, Context context) {
        final FilesystemViewerData.Options opts = prepareFsViewerOpts(context, true, listener);
        opts.okButtonText = R.string.select_this_folder;
        showDialog(fm, opts);
    }
}
