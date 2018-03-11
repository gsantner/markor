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

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import java.io.File;
import java.io.IOException;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "SameParameterValue", "unused", "deprecation", "ConstantConditions", "ObsoleteSdkInt", "SpellCheckingInspection"})
public class ShareUtil {
    protected Context _context;
    protected String _fileProviderAuthority;
    protected String _chooserTitle;

    public ShareUtil(Context context) {
        _context = context;
        _chooserTitle = "➥";
    }

    public String getFileProviderAuthority() {
        if (TextUtils.isEmpty(_fileProviderAuthority)) {
            throw new RuntimeException("Error at ShareUtil.getFileProviderAuthority(): No FileProvider authority provided");
        }
        return _fileProviderAuthority;
    }

    public ShareUtil setFileProviderAuthority(String fileProviderAuthority) {
        _fileProviderAuthority = fileProviderAuthority;
        return this;
    }


    public ShareUtil setChooserTitle(String title) {
        _chooserTitle = title;
        return this;
    }


    /**
     * Convert a {@link File} to an {@link Uri}
     *
     * @param file the file
     * @return Uri for this file
     */
    public Uri getUriByFileProviderAuthority(File file) {
        return FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
    }

    /**
     * Allow to choose a handling app for given intent
     *
     * @param intent      Thing to be shared
     * @param chooserText The title text for the chooser, or null for default
     */
    public void showChooser(Intent intent, String chooserText) {
        _context.startActivity(Intent.createChooser(intent,
                chooserText != null ? chooserText : _chooserTitle));
    }

    /**
     * Try to create a new desktop shortcut on the launcher. Add permissions:
     * <uses-permission android:name="android.permission.INSTALL_SHORTCUT" />
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     *
     * @param shortcutIntent  The intent to be invoked on tap
     * @param shortcutIconRes Icon resource for the item
     * @param shortcutTitle   Title of the item
     */
    public void createLauncherDesktopShortcut(Intent shortcutIntent, @DrawableRes int shortcutIconRes, String shortcutTitle) {
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Intent creationIntent = new Intent();
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutTitle);
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(_context, shortcutIconRes));
        creationIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        _context.sendBroadcast(creationIntent);
    }

    /**
     * Share text with given mime-type
     *
     * @param text     The text to share
     * @param mimeType MimeType or null (uses text/plain)
     */
    public void shareText(String text, @Nullable String mimeType) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType(mimeType != null ? mimeType : "text/plain");
        showChooser(intent, null);
    }

    /**
     * Share the given file as stream with given mime-type
     *
     * @param file     The file to share
     * @param mimeType The files mime type
     */
    public void shareStream(File file, String mimeType) {
        Uri fileUri = FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.setType(mimeType);
        showChooser(intent, null);
    }

    /**
     * Share the given bitmap with given format
     *
     * @param bitmap   Image
     * @param format   A {@link Bitmap.CompressFormat}, supporting JPEG,PNG, WEBP
     * @param filename Filename without ext., null or nothing supplied will default to SharedFile.
     * @return if success, true
     */
    public boolean shareImage(Bitmap bitmap, Bitmap.CompressFormat format) {
        return shareImage(bitmap, format, 95, "SharedImage");
    }


    /**
     * Share the given bitmap with given format
     *
     * @param bitmap    Image
     * @param format    A {@link Bitmap.CompressFormat}, supporting JPEG,PNG, WEBP
     * @param imageName Filename without extension
     * @param quality   Quality of the exported image [0-100]
     * @return if success, true
     */
    public boolean shareImage(Bitmap bitmap, Bitmap.CompressFormat format, int quality, String imageName) {
        try {
            String ext = format.name().toLowerCase();
            File file = File.createTempFile(imageName, "." + ext.replace("jpeg", "jpg"), _context.getExternalCacheDir());
            if (bitmap != null && new net.gsantner.opoc.util.ContextUtils(_context).writeImageToFile(file, bitmap, format, quality)) {
                shareStream(file, "image/" + ext);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Print a {@link WebView}'s contents, also allows to create a PDF
     *
     * @param webview WebView
     * @param jobName Name of the job (affects PDF name too)
     * @return {{@link PrintJob}} or null
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    public PrintJob print(WebView webview, String jobName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            PrintDocumentAdapter printAdapter;
            PrintManager printManager = (PrintManager) webview.getContext().getSystemService(Context.PRINT_SERVICE);
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


    /**
     * See {@link #print(WebView, String) print method}
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("deprecation")
    public PrintJob createPdf(WebView webview, String jobName) {
        return print(webview, jobName);
    }


    /**
     * Create a picture out of {@link WebView}'s whole content
     *
     * @param webView The WebView to get contents from
     * @return A {@link Bitmap} or null
     */
    @Nullable
    public static Bitmap getBitmapFromWebView(WebView webView) {
        try {
            //Measure WebView's content
            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            webView.measure(widthMeasureSpec, heightMeasureSpec);
            webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());

            //Build drawing cache and store its size
            webView.buildDrawingCache();
            int measuredWidth = webView.getMeasuredWidth();
            int measuredHeight = webView.getMeasuredHeight();

            //Creates the bitmap and draw WebView's content on in
            Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(bitmap, 0, bitmap.getHeight(), new Paint());

            webView.draw(canvas);
            webView.destroyDrawingCache();

            return bitmap;
        } catch (Exception | OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }


    /***
     * Replace (primary) clipboard contents with given text
     * @param text Text to be set
     */
    public boolean setClipboard(String text) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager cm = ((android.text.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null) {
                cm.setText(text);
                return true;
            }
        } else {
            android.content.ClipboardManager cm = ((android.content.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null) {
                ClipData clip = ClipData.newPlainText(_context.getPackageName(), text);
                cm.setPrimaryClip(clip);
                return true;
            }
        }
        return false;
    }

    /**
     * Share given text on a hastebin compatible server
     * (https://github.com/seejohnrun/haste-server)
     * Permission needed: Internet
     * Pastes will be deleted after 30 days without access
     *
     * @param text            The text to paste
     * @param callback        Callback after paste try
     * @param serverOrNothing Supply one or no hastebin server. If empty, the default gets taken
     */
    public void pasteOnHastebin(final String text, final Callback.a2<Boolean, String> callback, String... serverOrNothing) {
        final Handler handler = new Handler();
        final String server = (serverOrNothing != null && serverOrNothing.length > 0 && serverOrNothing[0] != null)
                ? serverOrNothing[0] : "https://hastebin.com";
        new Thread() {
            public void run() {
                // Returns a simple result, handleable without json parser {"key":"feediyujiq"}
                String ret = NetworkUtils.performCall(server + "/documents", NetworkUtils.POST, text);
                final String key = (ret.length() > 15) ? ret.split("\"")[3] : "";
                handler.post(() -> callback.callback(!key.isEmpty(), server + "/" + key));
            }
        }.start();
    }

    /**
     * Draft an email with given data. Unknown data can be supplied as null.
     * This will open a chooser with installed mail clients where the mail can be sent from
     *
     * @param subject Subject (top/title) text to be prefilled in the mail
     * @param body    Body (content) text to be prefilled in the mail
     * @param to      recipients to be prefilled in the mail
     */
    public void draftEmail(String subject, String body, String... to) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        if (subject != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        if (body != null) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
        }
        if (to != null && to.length > 0 && to[0] != null) {
            intent.putExtra(Intent.EXTRA_EMAIL, to);
        }
        showChooser(intent, null);
    }
}
