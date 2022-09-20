/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend.filebrowser;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.io.Serializable;
import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GsFileBrowserOptions {
    public interface SelectionListener extends Serializable {
        void onFsViewerSelected(final String request, final File file, final Integer lineNumber);

        void onFsViewerMultiSelected(final String request, final File... files);

        void onFsViewerNothingSelected(final String request);

        void onFsViewerConfig(final Options dopt);

        void onFsViewerDoUiUpdate(final GsFileBrowserListAdapter adapter);

        void onFsViewerItemLongPressed(final File file, boolean doSelectMultiple);
    }

    public static class Options implements Serializable {
        public SelectionListener listener = new SelectionListenerAdapter();
        public File
                rootFolder = Environment.getExternalStorageDirectory(),
                mountedStorageFolder = null,
                startFolder = null;
        public String requestId = "show_dialog";
        public String sortByType = GsFileUtils.SORT_BY_NAME;

        // Dialog type
        public boolean
                doSelectFolder = true,
                doSelectFile = false,
                doSelectMultiple = false;

        public boolean mustStartWithRootFolder = true,
                sortFolderFirst = true,
                sortReverse = false,
                descModtimeInsteadOfParent = false,
                filterShowDotFiles = true;

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

        public GsCallback.b2<Context, File> fileOverallFilter = (context, file) -> true;

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
        public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
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
        public void onFsViewerDoUiUpdate(GsFileBrowserListAdapter adapter) {

        }

        @Override
        public void onFsViewerItemLongPressed(File file, boolean doSelectMultiple) {

        }
    }
}
