/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
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

    public static GsFileBrowserOptions.Options prepareFsViewerOpts(
            final Context context,
            final boolean doSelectFolder,
            final GsFileBrowserOptions.SelectionListener listener
    ) {
        final GsFileBrowserOptions.Options opts = new GsFileBrowserOptions.Options();
        final MarkorContextUtils cu = new MarkorContextUtils(context);
        final AppSettings appSettings = ApplicationObject.settings();

        if (listener != null) {
            opts.listener = listener;
        }
        opts.doSelectFolder = doSelectFolder;
        opts.doSelectFile = !doSelectFolder;

        opts.okButtonEnable = opts.doSelectFolder || opts.doSelectMultiple;

        opts.searchButtonImage = R.drawable.ic_search_black_24dp;
        opts.newDirButtonImage = R.drawable.baseline_create_new_folder_24;
        opts.homeButtonImage = R.drawable.ic_home_black_24dp;
        opts.selectedItemImage = R.drawable.ic_check_black_24dp;
        opts.newDirButtonText = R.string.create_folder;
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
        opts.fileColor = R.color.file;
        opts.folderColor = R.color.folder;
        opts.fileImage = R.drawable.ic_file_white_24dp;
        opts.folderImage = R.drawable.ic_folder_white_24dp;

        opts.titleText = R.string.select;

        opts.mountedStorageFolder = cu.getStorageAccessFolder(context);

        opts.refresh = () -> {
            opts.sortFolderFirst = appSettings.isFileBrowserSortFolderFirst();
            opts.sortByType = appSettings.getFileBrowserSortByType();
            opts.sortReverse = appSettings.isFileBrowserSortReverse();
            opts.filterShowDotFiles = appSettings.isFileBrowserFilterShowDotFiles();
            opts.favouriteFiles = appSettings.getFavouriteFiles();
            opts.recentFiles = appSettings.getRecentFiles();
            opts.popularFiles = appSettings.getPopularFiles();
        };
        opts.refresh.callback();

        return opts;
    }

    public static File[] strlistToArray(List<String> strlist) {
        File[] files = new File[strlist.size()];
        for (int i = 0; i < files.length; i++) {
            files[i] = new File(strlist.get(i));
        }
        return files;
    }

    private static GsFileBrowserDialog showDialog(final FragmentManager fm, final GsFileBrowserOptions.Options opts) {
        final GsFileBrowserDialog filesystemViewerDialog = GsFileBrowserDialog.newInstance(opts);
        filesystemViewerDialog.show(fm, GsFileBrowserDialog.FRAGMENT_TAG);
        return filesystemViewerDialog;
    }

    public static GsFileBrowserDialog showFileDialog(
            final GsFileBrowserOptions.SelectionListener listener,
            final FragmentManager fm,
            final Context context,
            final GsCallback.b2<Context, File> fileOverallFilter
    ) {
        final GsFileBrowserOptions.Options opts = prepareFsViewerOpts(context, false, listener);
        opts.fileOverallFilter = fileOverallFilter;
        return showDialog(fm, opts);
    }

    public static GsFileBrowserDialog showFolderDialog(
            final GsFileBrowserOptions.SelectionListener listener,
            final FragmentManager fm,
            final Context context
    ) {
        final GsFileBrowserOptions.Options opts = prepareFsViewerOpts(context, true, listener);
        opts.okButtonText = R.string.select_this_folder;
        return showDialog(fm, opts);
    }
}
