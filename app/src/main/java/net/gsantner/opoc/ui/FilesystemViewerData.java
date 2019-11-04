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
package net.gsantner.opoc.ui;

import android.arch.core.util.Function;
import android.os.Environment;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FilesystemViewerData {
    public interface SelectionListener extends Serializable {
        void onFsViewerSelected(final String request, final File file);

        void onFsViewerMultiSelected(final String request, final File... files);

        void onFsViewerNothingSelected(final String request);

        void onFsViewerConfig(final Options dopt);

        void onFsViewerDoUiUpdate(final FilesystemViewerAdapter adapter);

        void onFsViewerItemLongPressed(final File file, boolean doSelectMultiple);
    }

    public static class Options implements Serializable {
        public SelectionListener listener = new SelectionListenerAdapter();
        public File
                rootFolder = Environment.getExternalStorageDirectory(),
                mountedStorageFolder = null;
        public String requestId = "show_dialog";

        // Dialog type
        public boolean
                doSelectFolder = true,
                doSelectFile = false,
                doSelectMultiple = false;

        public boolean mustStartWithRootFolder = true,
                folderFirst = true,
                descModtimeInsteadOfParent = false,
                showDotFiles = true;

        public int itemSidePadding = 16; // dp

        // Visibility of elements
        public boolean
                titleTextEnable = true,
                utilsBarEnable = true,
                searchEnable = true,
                upButtonEnable = true,
                homeButtonEnable = true,
                cancelButtonEnable = true,
                okButtonEnable = true;

        public Comparator<File> fileComparable = null;
        public Function<File, Boolean> fileOverallFilter = input -> true;

        @StringRes
        public int cancelButtonText = android.R.string.cancel;
        @StringRes
        public int okButtonText = android.R.string.ok;
        @StringRes
        public int titleText = android.R.string.untitled;
        @StringRes
        public int searchHint = android.R.string.search_go;
        @StringRes
        public int contentDescriptionFolder = 0;
        @StringRes
        public int contentDescriptionSelected = 0;
        @StringRes
        public int contentDescriptionFile = 0;
        @DrawableRes
        public int homeButtonImage = android.R.drawable.star_big_on;
        @DrawableRes
        public int searchButtonImage = android.R.drawable.ic_menu_search;
        @DrawableRes
        public int folderImage = android.R.drawable.ic_menu_view;
        @DrawableRes
        public int selectedItemImage = android.R.drawable.checkbox_on_background;
        @DrawableRes
        public int fileImage = android.R.drawable.ic_menu_edit;
        @ColorRes
        public int backgroundColor = android.R.color.background_light;
        @ColorRes
        public int primaryColor = 0;
        @ColorRes
        public int accentColor = 0;
        @ColorRes
        public int primaryTextColor = 0;
        @ColorRes
        public int secondaryTextColor = 0;
        @ColorRes
        public int titleTextColor = 0;

        public List<File> favouriteFiles, recentFiles, popularFiles = null;
    }

    public static class SelectionListenerAdapter implements SelectionListener, Serializable {
        @Override
        public void onFsViewerSelected(String request, File file) {
        }

        @Override
        public void onFsViewerMultiSelected(String request, File... files) {
        }

        @Override
        public void onFsViewerNothingSelected(String request) {
        }

        @Override
        public void onFsViewerConfig(Options dopt) {

        }

        @Override
        public void onFsViewerDoUiUpdate(FilesystemViewerAdapter adapter) {

        }

        @Override
        public void onFsViewerItemLongPressed(File file, boolean doSelectMultiple) {

        }
    }
}
