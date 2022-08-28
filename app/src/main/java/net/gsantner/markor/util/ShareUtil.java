/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.print.PrintJob;
import android.text.TextUtils;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.activity.openeditor.OpenEditorFromShortcutOrWidgetActivity;
import net.gsantner.markor.activity.openeditor.OpenEditorQuickNoteActivity;
import net.gsantner.markor.activity.openeditor.OpenEditorTodoActivity;
import net.gsantner.markor.activity.openeditor.OpenShareIntoActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.util.GsShareUtil;

import java.io.File;

public class ShareUtil extends GsShareUtil {
    public ShareUtil(Context context) {
        super(context);
        setChooserTitle(_context.getString(R.string.share_to_arrow));
    }

    public GsShareUtil applySpecialLaunchersVisibility(boolean extraLaunchersEnabled) {
        setLauncherActivityEnabled(OpenEditorQuickNoteActivity.class, extraLaunchersEnabled);
        setLauncherActivityEnabled(OpenEditorTodoActivity.class, extraLaunchersEnabled);
        setLauncherActivityEnabled(OpenShareIntoActivity.class, extraLaunchersEnabled);
        return this;
    }

    public void createLauncherDesktopShortcut(final File file) {
        // This is only allowed to call when direct file access is possible!!
        // So basically only for java.io.File Objects. Virtual files, or content://
        // in private/restricted space won't work - because of missing permission grant when re-launching
        final String title = file != null ? GsFileUtils.getFilenameWithoutExtension(file) : null;
        if (!TextUtils.isEmpty(title)) {
            final boolean isDir = file.isDirectory();
            final Class<?> klass = isDir ? MainActivity.class : OpenEditorFromShortcutOrWidgetActivity.class;
            final Intent intent = new Intent(_context, klass).setData(Uri.fromFile(file));
            final int iconRes = isDir ? R.mipmap.ic_shortcut_folder : R.mipmap.ic_shortcut_file;
            super.createLauncherDesktopShortcut(intent, iconRes, title);
            // Toast.makeText(_context, R.string.tried_to_create_shortcut_for_this_notice, Toast.LENGTH_LONG).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    public PrintJob printOrCreatePdfFromWebview(WebView webview, Document document, boolean... landscape) {
        String jobName = String.format("%s (%s)", document.getTitle(), _context.getString(R.string.app_name_real));
        return super.print(webview, jobName, landscape);
    }

    public void showMountSdDialog(Activity... activity) {
        showMountSdDialog(R.string.mount_storage, R.string.application_needs_access_to_storage_mount_it, R.drawable.mount_sdcard_help, activity);
    }
}
