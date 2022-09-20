/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.filebrowser;

import android.content.Context;

import androidx.fragment.app.FragmentManager;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserDialog;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.util.List;

public class MarkorFileBrowserFactory {
    public static GsCallback.b2<Context, File> IsMimeText = (context, file) -> file != null && GsContextUtils.instance.getMimeType(context, file).startsWith("text/");
    public static GsCallback.b2<Context, File> IsMimeImage = (context, file) -> file != null && GsContextUtils.instance.getMimeType(context, file).startsWith("image/");
    public static GsCallback.b2<Context, File> IsMimeAudio = (context, file) -> file != null && GsContextUtils.instance.getMimeType(context, file).startsWith("audio/");
    public static GsCallback.b2<Context, File> IsMimeVideo = (context, file) -> file != null && GsContextUtils.instance.getMimeType(context, file).startsWith("video/");

    public static GsFileBrowserOptions.Options prepareFsViewerOpts(Context context, boolean doSelectFolder, GsFileBrowserOptions.SelectionListener listener) {
        final GsFileBrowserOptions.Options opts = new GsFileBrowserOptions.Options();
        final MarkorContextUtils cu = new MarkorContextUtils(context);
        final AppSettings appSettings = ApplicationObject.settings();

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

        opts.sortFolderFirst = appSettings.isFileBrowserSortFolderFirst();
        opts.sortByType = appSettings.getFileBrowserSortByType();
        opts.sortReverse = appSettings.isFileBrowserSortReverse();
        opts.filterShowDotFiles = appSettings.isFileBrowserFilterShowDotFiles();

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

        opts.mountedStorageFolder = cu.getStorageAccessFolder(context);
        return opts;
    }

    public static File[] strlistToArray(List<String> strlist) {
        File[] files = new File[strlist.size()];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(strlist.get(i));
        }
        return files;
    }

    private static void showDialog(FragmentManager fm, GsFileBrowserOptions.Options opts) {
        GsFileBrowserDialog filesystemViewerDialog = GsFileBrowserDialog.newInstance(opts);
        filesystemViewerDialog.show(fm, GsFileBrowserDialog.FRAGMENT_TAG);
    }

    public static void showFileDialog(GsFileBrowserOptions.SelectionListener listener, FragmentManager fm, Context context, GsCallback.b2<Context, File> fileOverallFilter) {
        final GsFileBrowserOptions.Options opts = prepareFsViewerOpts(context, false, listener);
        opts.fileOverallFilter = fileOverallFilter;
        showDialog(fm, opts);
    }

    public static void showFolderDialog(GsFileBrowserOptions.SelectionListener listener, FragmentManager fm, Context context) {
        final GsFileBrowserOptions.Options opts = prepareFsViewerOpts(context, true, listener);
        opts.okButtonText = R.string.select_this_folder;
        showDialog(fm, opts);
    }
}
