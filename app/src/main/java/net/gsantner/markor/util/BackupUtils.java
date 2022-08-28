package net.gsantner.markor.util;

import android.app.Activity;
import android.content.Context;

import androidx.fragment.app.FragmentManager;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.util.GsBackupUtils;

import java.io.File;
import java.util.List;

public class BackupUtils extends GsBackupUtils {

    public static void showBackupSelectFromDialog(final Context context, final FragmentManager manager) {
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;

            FilesystemViewerCreator.showFileDialog(
                    new GsFileBrowserOptions.SelectionListenerAdapter() {
                        @Override
                        public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                            dopt.rootFolder = new AppSettings(context).getNotebookDirectory();
                            dopt.titleText = R.string.select;
                        }

                        @Override
                        public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
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
                    new GsFileBrowserOptions.SelectionListenerAdapter() {
                        @Override
                        public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                            dopt.rootFolder = new AppSettings(context).getNotebookDirectory();
                            dopt.titleText = R.string.select_folder;
                        }

                        @Override
                        public void onFsViewerSelected(String request, File dir, final Integer lineNumber) {
                            makeBackup(context, getPrefNamesToBackup(), generateBackupFilepath(context, dir));
                        }
                    }, manager, activity
            );
        }
    }

    public static List<String> getPrefNamesToBackup() {
        List<String> prefs = GsBackupUtils.getPrefNamesToBackup();
        prefs.add(TextActions.ACTION_ORDER_PREF_NAME);
        return prefs;
    }
}
