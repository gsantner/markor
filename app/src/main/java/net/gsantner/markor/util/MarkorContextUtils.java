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

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.openeditor.OpenEditorQuickNoteActivity;
import net.gsantner.markor.activity.openeditor.OpenEditorTodoActivity;
import net.gsantner.markor.activity.openeditor.OpenFromShortcutOrWidgetActivity;
import net.gsantner.markor.activity.openeditor.OpenShareIntoActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;

public class MarkorContextUtils extends GsContextUtils {

    public MarkorContextUtils(@Nullable final Context context) {
        if (context != null) {
            setChooserTitle(context.getString(R.string.share_to_arrow));
        }
    }

    public <T extends GsContextUtils> T applySpecialLaunchersVisibility(final Context context, boolean extraLaunchersEnabled) {
        setLauncherActivityEnabled(context, OpenEditorQuickNoteActivity.class, extraLaunchersEnabled);
        setLauncherActivityEnabled(context, OpenEditorTodoActivity.class, extraLaunchersEnabled);
        setLauncherActivityEnabled(context, OpenShareIntoActivity.class, extraLaunchersEnabled);
        return thisp();
    }

    public <T extends GsContextUtils> T createLauncherDesktopShortcut(final Context context, final File file) {
        // This is only allowed to call when direct file access is possible!!
        // So basically only for java.io.File Objects. Virtual files, or content://
        // in private/restricted space won't work - because of missing permission grant when re-launching
        final String title = file != null ? GsFileUtils.getFilenameWithoutExtension(file) : null;
        if (!TextUtils.isEmpty(title)) {
            final int iconRes = file.isDirectory() ? R.mipmap.ic_shortcut_folder : R.mipmap.ic_shortcut_file;
            final Intent intent = new Intent(context, OpenFromShortcutOrWidgetActivity.class).setData(Uri.fromFile(file));
            createLauncherDesktopShortcut(context, intent, iconRes, title);
        }
        return thisp();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    public PrintJob printOrCreatePdfFromWebview(final WebView webview, Document document, boolean... landscape) {
        String jobName = String.format("%s (%s)", document.getTitle(), webview.getContext().getString(R.string.app_name_real));
        return super.print(webview, jobName, landscape);
    }

    public <T extends GsContextUtils> T showMountSdDialog(final Activity activity) {
        showMountSdDialog(activity, R.string.mount_storage, R.string.application_needs_access_to_storage_mount_it, R.drawable.mount_sdcard_help);
        return thisp();
    }

    // Get intent file
    public static File getIntentFile(final Intent intent, final File fallback) {
        if (intent == null) {
            return fallback;
        }

        // By extra path
        final File file = (File) intent.getSerializableExtra(Document.EXTRA_PATH);
        if (file != null) {
            return file;
        }

        // By url in data
        try {
            return new File(intent.getData().getPath());
        } catch (NullPointerException ignored) {
        }

        return fallback;
    }

    public static File getValidIntentDir(final Intent intent, final File fallback) {
        final File f = getIntentFile(intent, null);
        return (f != null && f.isDirectory() && f.exists()) ? f : fallback;
    }
}
