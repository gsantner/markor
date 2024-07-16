/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
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
import java.util.Collection;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GsFileBrowserOptions {

    public interface SelectionListener {
        void onFsViewerSelected(final String request, final File file, final Integer lineNumber);

        void onFsViewerMultiSelected(final String request, final File... files);

        void onFsViewerCancel(final String request);

        void onFsViewerConfig(final Options dopt);

        void onFsViewerDoUiUpdate(final GsFileBrowserListAdapter adapter);

        void onFsViewerItemLongPressed(final File file, boolean doSelectMultiple);
    }

    public static class Options {
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
                utilsBarEnable = true,
                searchEnable = true,
                upButtonEnable = true,
                homeButtonEnable = true,
                cancelButtonEnable = true,
                okButtonEnable = true,
                newDirButtonEnable = true,
                dismissAfterCallback = true;

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
        @StringRes
        public int newDirButtonText = 0;
        @DrawableRes
        public int homeButtonImage = android.R.drawable.star_big_on;
        @DrawableRes
        public int searchButtonImage = android.R.drawable.ic_menu_search;
        @DrawableRes
        public int newDirButtonImage = android.R.drawable.ic_menu_add;
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
        @ColorRes
        public int fileColor = 0;
        @ColorRes
        public int folderColor = 0;

        public Collection<File> favouriteFiles, recentFiles, popularFiles = null;
        public GsCallback.a1<CharSequence> setTitle = null, setSubtitle = null;

        public GsCallback.a0 refresh = null;
    }

    public static class SelectionListenerAdapter implements SelectionListener, Serializable {
        @Override
        public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
        }

        @Override
        public void onFsViewerMultiSelected(String request, File... files) {
        }

        @Override
        public void onFsViewerCancel(String request) {
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
