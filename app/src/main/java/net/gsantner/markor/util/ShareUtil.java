/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.model.Document;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("UnusedReturnValue")
public class ShareUtil {
    public static final String FILE_PROVIDER_AUTHORITIES = BuildConfig.APPLICATION_ID + ".provider";

    private Context _context;

    public ShareUtil(Context context) {
        _context = context;
    }

    public static void createLauncherShortcut(Context c, Document document) {
        // This is only allowed to call when direct file access is possible!!
        // So basically only for java.io.File Objects. Virtual files, or content://
        // in private/restricted space won't work - because of missing permission grant when re-launching
        if (document != null && document.getFile() != null && !TextUtils.isEmpty(document.getTitle())) {
            Intent shortcutIntent = new Intent(c, DocumentActivity.class);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            shortcutIntent.putExtra(DocumentActivity.EXTRA_LAUNCHER_SHORTCUT_PATH, document.getFile().getAbsolutePath());
            shortcutIntent.setType("text/markdown"); // setData(Uri) -> Uri always gets null on receive

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, document.getTitle());
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(c, R.drawable.ic_launcher));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            c.sendBroadcast(addIntent);

            Toast.makeText(c, R.string.add_shortcut_to_launcher_homescreen_notice, Toast.LENGTH_LONG).show();
        }
    }

    public void shareText(String text, String type) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType(type);
        showShareChooser(intent);
    }

    public void shareStream(File file, String type) {
        Uri fileUri = FileProvider.getUriForFile(_context, FILE_PROVIDER_AUTHORITIES, file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.setType(type);
        showShareChooser(intent);
    }

    public Uri getUriFromMarkorProvider(File file) {
        return FileProvider.getUriForFile(_context, FILE_PROVIDER_AUTHORITIES, file);
    }

    public boolean shareImage(Bitmap bitmap) {
        try {
            File file = File.createTempFile(_context.getString(R.string.app_name), ".png", _context.getExternalCacheDir());
            if (bitmap != null && new ContextUtils(_context).writeImageToFile(file, bitmap, Bitmap.CompressFormat.PNG, 95) != null) {
                shareStream(file, "image/png");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showShareChooser(Intent intent) {
        _context.startActivity(Intent.createChooser(intent, _context.getResources().getText(R.string.share_to)));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    public PrintJob printPdfOfWebview(Document document, WebView webview) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Context c = webview.getContext();
            PrintDocumentAdapter printAdapter;
            PrintManager printManager = (PrintManager) c.getSystemService(Context.PRINT_SERVICE);
            String jobName = String.format("%s (%s)", document.getTitle(), c.getString(R.string.app_name));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                printAdapter = webview.createPrintDocumentAdapter(jobName);
            } else {
                printAdapter = webview.createPrintDocumentAdapter();
            }
            if (printManager != null) {
                return printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
            }
        } else {
            Log.e(getClass().getName(), "ERROR: Method called on too low Android API version");
        }
        return null;
    }


    public static Bitmap getBitmapFromWebView(WebView webView) {
        try {
            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            //Measure WebView's content
            webView.measure(widthMeasureSpec, heightMeasureSpec);
            webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());

            //Build drawing cache and store its size
            webView.buildDrawingCache();

            int measuredWidth = webView.getMeasuredWidth();
            int measuredHeight = webView.getMeasuredHeight();

            //Creates the bitmap and draw WebView's content on in
            Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);

            Paint paint = new Paint();

            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(bitmap, 0, bitmap.getHeight(), paint);

            webView.draw(canvas);
            webView.destroyDrawingCache();

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
