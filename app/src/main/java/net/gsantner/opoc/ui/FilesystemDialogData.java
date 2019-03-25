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

import net.gsantner.markor.R;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FilesystemDialogData {
    public interface SelectionListener extends Serializable{
        void onFsSelected(String request, File file);

        void onFsMultiSelected(String request, File... files);

        void onFsNothingSelected(String request);

        void onFsDialogConfig(final Options opt);

        void onFsDoUiUpdate(final FilesystemDialogAdapter adapter);

        void onFsLongPressed(File file, boolean doSelectMultiple);
    }

    public static class Options implements Serializable{
        public SelectionListener listener = new SelectionListenerAdapter();
        public File rootFolder = Environment.getExternalStorageDirectory();
        public String requestId = "show_dialog";

        // Dialog type
        public boolean
                doSelectFolder = true,
                doSelectFile = false,
                doSelectMultiple = false;

        public boolean mustStartWithRootFolder = true,
                folderFirst = true,
                descModtimeInsteadOfParent = false;

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
        public int primaryColor = R.color.primary;
        @ColorRes
        public int accentColor = R.color.accent;
        @ColorRes
        public int primaryTextColor = R.color.primary_text;
        @ColorRes
        public int secondaryTextColor = R.color.secondary_text;
        @ColorRes
        public int titleTextColor = primaryTextColor;

        public File[] recentFiles, popularFiles = null;
    }

    public static class SelectionListenerAdapter implements SelectionListener, Serializable {
        @Override
        public void onFsSelected(String request, File file) {
        }

        @Override
        public void onFsMultiSelected(String request, File... files) {
        }

        @Override
        public void onFsNothingSelected(String request) {
        }

        @Override
        public void onFsDialogConfig(Options opt) {

        }

        @Override
        public void onFsDoUiUpdate(FilesystemDialogAdapter adapter) {

        }

        @Override
        public void onFsLongPressed(File file, boolean doSelectMultiple) {

        }
    }
}
