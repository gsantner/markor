/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.util;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.print.PrintJob;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.webkit.WebView;
import android.widget.Toast;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.model.Document;

public class ShareUtil extends net.gsantner.opoc.util.ShareUtil {
    public static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    public ShareUtil(Context context) {
        super(context);
        setFileProviderAuthority(FILE_PROVIDER_AUTHORITY);
        setChooserTitle(_context.getString(R.string.share_to));
    }

    public void createLauncherDesktopShortcut(Document document) {
        // This is only allowed to call when direct file access is possible!!
        // So basically only for java.io.File Objects. Virtual files, or content://
        // in private/restricted space won't work - because of missing permission grant when re-launching
        if (document != null && document.getFile() != null && !TextUtils.isEmpty(document.getTitle())) {
            Intent shortcutIntent = new Intent(_context, DocumentActivity.class);
            shortcutIntent.putExtra(DocumentActivity.EXTRA_LAUNCHER_SHORTCUT_PATH, document.getFile().getAbsolutePath());
            shortcutIntent.setType("text/plain"); // setData(Uri) -> Uri always gets null on receive
            super.createLauncherDesktopShortcut(shortcutIntent, R.drawable.ic_launcher, document.getTitle());
            Toast.makeText(_context, R.string.add_shortcut_to_launcher_homescreen_notice, Toast.LENGTH_LONG).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    public PrintJob printOrCreatePdfFromWebview(WebView webview, Document document) {
        String jobName = String.format("%s (%s)", document.getTitle(), _context.getString(R.string.app_name));
        return super.print(webview, jobName);
    }
}
