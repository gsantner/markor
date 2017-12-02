/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.net> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.opoc.util;

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
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public abstract class ShareUtilBase {
    protected Context _context;

    public ShareUtilBase(Context context) {
        _context = context;
    }

    public abstract String getFileProviderAuthority();

    public abstract void showShareChooser(Intent intent);


    public Uri getUriByFileProviderAuthority(File file) {
        return FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
    }

    public void createLauncherDesktopShortcut(Intent shortcutIntent, @DrawableRes int shortcutIconRes, String shortcutTitle, String shortcutCreateToast) {
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutTitle);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(_context, shortcutIconRes));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        _context.sendBroadcast(addIntent);

        Toast.makeText(_context, shortcutCreateToast, Toast.LENGTH_LONG).show();
    }

    public void shareText(String text, String mimeType) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType(mimeType);
        showShareChooser(intent);
    }

    public void shareStream(File file, String mimeType) {
        Uri fileUri = FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.setType(mimeType);
        showShareChooser(intent);
    }

    public boolean shareImage(Bitmap bitmap) {
        try {
            File file = File.createTempFile("SharedFile", ".png", _context.getExternalCacheDir());
            if (bitmap != null && new net.gsantner.markor.util.ContextUtils(_context).writeImageToFile(file, bitmap, Bitmap.CompressFormat.PNG, 95) != null) {
                shareStream(file, "image/png");
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public void showShareChooser(Intent intent, String chooserText) {
        _context.startActivity(Intent.createChooser(intent, chooserText));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    public PrintJob printOrCreatePdfFromWebview(WebView webview, String jobName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Context c = webview.getContext();
            PrintDocumentAdapter printAdapter;
            PrintManager printManager = (PrintManager) c.getSystemService(Context.PRINT_SERVICE);
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


    @Nullable
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
