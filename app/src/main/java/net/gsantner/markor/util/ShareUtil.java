package net.gsantner.markor.util;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.print.PrintJob;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.webkit.WebView;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.ShareUtilBase;

/**
 * Created by gregor on 02.12.17.
 */

public class ShareUtil extends ShareUtilBase {
    public static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    public ShareUtil(Context context) {
        super(context);
    }

    @Override
    public String getFileProviderAuthority() {
        return FILE_PROVIDER_AUTHORITY;
    }

    public void createLauncherDesktopShortcut(Document document) {
        // This is only allowed to call when direct file access is possible!!
        // So basically only for java.io.File Objects. Virtual files, or content://
        // in private/restricted space won't work - because of missing permission grant when re-launching
        if (document != null && document.getFile() != null && !TextUtils.isEmpty(document.getTitle())) {
            Intent shortcutIntent = new Intent(_context, DocumentActivity.class);
            shortcutIntent.putExtra(DocumentActivity.EXTRA_LAUNCHER_SHORTCUT_PATH, document.getFile().getAbsolutePath());
            shortcutIntent.setType("text/markdown"); // setData(Uri) -> Uri always gets null on receive
            super.createLauncherDesktopShortcut(shortcutIntent, R.drawable.ic_launcher, document.getTitle(), _context.getString(R.string.add_shortcut_to_launcher_homescreen_notice));
        }
    }

    @Override
    public void showShareChooser(Intent intent) {
        super.showShareChooser(intent, _context.getString(R.string.share_to));
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    public PrintJob printOrCreatePdfFromWebview(WebView webview, Document document) {
        String jobName = String.format("%s (%s)", document.getTitle(), _context.getString(R.string.app_name));
        return super.printOrCreatePdfFromWebview(webview, jobName);
    }
}
