package net.gsantner.markor.util;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.FragmentManager;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.DatetimeFormatDialog;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.ui.SearchReplaceDialog;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.opoc.ui.FilesystemViewerData;

import java.io.File;
import java.util.List;

public class BackupUtils extends net.gsantner.opoc.util.BackupUtils {

    public static void showBackupSelectFromDialog(final Context context, final FragmentManager manager) {
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;

            FilesystemViewerCreator.showFileDialog(
                    new FilesystemViewerData.SelectionListenerAdapter() {
                        @Override
                        public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                            dopt.rootFolder = new AppSettings(context).getNotebookDirectory();
                            dopt.titleText = R.string.select;
                        }

                        @Override
                        public void onFsViewerSelected(String request, File file) {
                            loadBackup(context, file);
                        }
                    }, manager, activity,
                    input -> input != null && input.exists() && input.toString().trim().toLowerCase().endsWith(".json")
            );
        }
    }

    public static void showBackupWriteToDialog(final Context context, final FragmentManager manager) {
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;

            FilesystemViewerCreator.showFolderDialog(
                    new FilesystemViewerData.SelectionListenerAdapter() {
                        @Override
                        public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                            dopt.rootFolder = new AppSettings(context).getNotebookDirectory();
                            dopt.titleText = R.string.select_folder;
                        }

                        @Override
                        public void onFsViewerSelected(String request, File dir) {
                            makeBackup(context, getPrefNamesToBackup(), generateBackupFilepath(context, dir));
                        }
                    }, manager, activity
            );
        }
    }

    public static List<String> getPrefNamesToBackup() {
        List<String> prefs = net.gsantner.opoc.util.BackupUtils.getPrefNamesToBackup();
        prefs.add(DatetimeFormatDialog.DATETIME_SETTINGS);
        prefs.add(TextActions.ACTION_ORDER_PREF_NAME);
        prefs.add(SearchReplaceDialog.SEARCH_REPLACE_SETTINGS);
        return prefs;
    }
}
