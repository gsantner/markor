/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
#########################################################*/
package net.gsantner.opoc.util;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.util.Pair;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A utility class to ease information sharing on Android.
 * Also allows to parse/fetch information out of shared information.
 * (M)Permissions are not checked, wrap ShareUtils methods if neccessary
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "SameParameterValue", "unused", "deprecation", "ConstantConditions", "ObsoleteSdkInt", "SpellCheckingInspection", "JavadocReference", "ConstantLocale"})
public class ShareUtil {
    public final static String EXTRA_FILEPATH = "real_file_path_2";
    public final static SimpleDateFormat DATEFORMAT_RFC3339ISH = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault());
    public final static SimpleDateFormat DATEFORMAT_IMG = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()); //20190511-230845
    public final static String MIME_TEXT_PLAIN = "text/plain";
    public final static String PREF_KEY__SAF_TREE_URI = "pref_key__saf_tree_uri";
    public final static String CONTENT_RESOLVER_FILE_PROXY_SEGMENT = "CONTENT_RESOLVER_FILE_PROXY_SEGMENT";

    public final static int REQUEST_CAMERA_PICTURE = 50001;
    public final static int REQUEST_PICK_PICTURE = 50002;
    public final static int REQUEST_SAF = 50003;
    public final static int REQUEST_STORAGE_PERMISSION_M = 50004;
    public final static int REQUEST_STORAGE_PERMISSION_R = 50005;

    public final static int MIN_OVERWRITE_LENGTH = 2;

    protected static String _lastCameraPictureFilepath;

    protected Context _context;
    protected String _chooserTitle;

    public ShareUtil(final Context context) {
        _context = context;
        _chooserTitle = "➥";
    }

    public void setContext(final Context c) {
        _context = c;
    }

    public void freeContextRef() {
        _context = null;
    }

    public String getFileProviderAuthority() {
        ContextUtils cu = new ContextUtils(_context);
        final String provider = cu.getFileProvider();
        cu.freeContextRef();
        if (TextUtils.isEmpty(provider)) {
            throw new RuntimeException("Error at ShareUtil.getFileProviderAuthority(): No FileProvider authority provided");
        }
        return provider;
    }


    public ShareUtil setChooserTitle(final String title) {
        _chooserTitle = title;
        return this;
    }

    /**
     * Convert a {@link File} to an {@link Uri}
     *
     * @param file the file
     * @return Uri for this file
     */
    public Uri getUriByFileProviderAuthority(final File file) {
        return FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
    }

    /**
     * Allow to choose a handling app for given intent
     *
     * @param intent      Thing to be shared
     * @param chooserText The title text for the chooser, or null for default
     */
    public void showChooser(final Intent intent, final String chooserText) {
        try {
            _context.startActivity(Intent.createChooser(intent, chooserText != null ? chooserText : _chooserTitle));
        } catch (Exception ignored) {
        }
    }

    /**
     * Try to create a new desktop shortcut on the launcher. Add permissions:
     * <uses-permission android:name="android.permission.INSTALL_SHORTCUT" />
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     *
     * @param intent  The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title   Title of the item
     */
    public void createLauncherDesktopShortcut(final Intent intent, @DrawableRes final int iconRes, final String title) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (intent.getAction() == null) {
            intent.setAction(Intent.ACTION_VIEW);
        }

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(_context, Long.toString(new Random().nextLong()))
                .setIntent(intent)
                .setIcon(IconCompat.createWithResource(_context, iconRes))
                .setShortLabel(title)
                .setLongLabel(title)
                .build();
        ShortcutManagerCompat.requestPinShortcut(_context, shortcut, null);
    }

    /**
     * Try to create a new desktop shortcut on the launcher. This will not work on Api > 25. Add permissions:
     * <uses-permission android:name="android.permission.INSTALL_SHORTCUT" />
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     *
     * @param intent  The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title   Title of the item
     */
    public void createLauncherDesktopShortcutLegacy(final Intent intent, @DrawableRes final int iconRes, final String title) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (intent.getAction() == null) {
            intent.setAction(Intent.ACTION_VIEW);
        }

        Intent creationIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        creationIntent.putExtra("duplicate", true);
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(_context, iconRes));
        _context.sendBroadcast(creationIntent);
    }

    /**
     * Share text with given mime-type
     *
     * @param text     The text to share
     * @param mimeType MimeType or null (uses text/plain)
     */
    public void shareText(final String text, @Nullable final String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType(mimeType != null ? mimeType : MIME_TEXT_PLAIN);
        showChooser(intent, null);
    }

    /**
     * Share the given file as stream with given mime-type
     *
     * @param file     The file to share
     * @param mimeType The files mime type
     */
    public boolean shareStream(final File file, final String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(EXTRA_FILEPATH, file.getAbsolutePath());
        intent.setType(mimeType);

        try {
            Uri fileUri = FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            showChooser(intent, null);
            return true;
        } catch (Exception ignored) { // FileUriExposed(API24) / IllegalArgument
        }
        return false;
    }

    /**
     * Share the given files as stream with given mime-type
     *
     * @param files    The files to share
     * @param mimeType The files mime type. Usally * / * is the best option
     */
    public boolean shareStreamMultiple(final Collection<File> files, final String mimeType) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : files) {
            File uri = new File(file.toString());
            uris.add(FileProvider.getUriForFile(_context, getFileProviderAuthority(), file));
        }

        try {
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType(mimeType);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            showChooser(intent, null);
            return true;
        } catch (Exception e) { // FileUriExposed(API24) / IllegalArgument
            return false;
        }
    }

    /**
     * Start calendar application to add new event, with given details prefilled
     */
    public boolean createCalendarAppointment(@Nullable final String title, @Nullable final String description, @Nullable final String location, @Nullable final Long... startAndEndTime) {
        Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
        if (title != null) {
            intent.putExtra(CalendarContract.Events.TITLE, title);
        }
        if (description != null) {
            intent.putExtra(CalendarContract.Events.DESCRIPTION, (description.length() > 800 ? description.substring(0, 800) : description));
        }
        if (location != null) {
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location);
        }
        if (startAndEndTime != null) {
            if (startAndEndTime.length > 0 && startAndEndTime[0] > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startAndEndTime[0]);
            }
            if (startAndEndTime.length > 1 && startAndEndTime[1] > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startAndEndTime[1]);
            }
        }

        try {
            _context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    /**
     * Open a View intent for given file
     *
     * @param file The file to share
     */
    public boolean viewFileInOtherApp(final File file, @Nullable final String type) {
        // On some specific devices the first won't work
        Uri fileUri = null;
        try {
            fileUri = FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
        } catch (Exception ignored) {
            try {
                fileUri = Uri.fromFile(file);
            } catch (Exception ignored2) {
            }
        }

        if (fileUri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setData(fileUri);
            intent.putExtra(EXTRA_FILEPATH, file.getAbsolutePath());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(fileUri, type);
            showChooser(intent, null);
            return true;
        }
        return false;
    }


    /**
     * Request installation of APK specified by file
     * Permission required:
     * <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
     *
     * @param file The apk file to install
     */
    public boolean requestApkInstallation(final File file) {
        if (file == null || !file.getName().toLowerCase().endsWith(".apk")) {
            return false;
        }

        Uri fileUri = null;
        try {
            fileUri = FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
        } catch (Exception ignored) {
            try {
                fileUri = Uri.fromFile(file);
            } catch (Exception ignored2) {
            }
        }

        if (fileUri != null) {
            final Intent intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? Intent.ACTION_INSTALL_PACKAGE : Intent.ACTION_VIEW)
                    .setFlags(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? Intent.FLAG_GRANT_READ_URI_PERMISSION : Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setDataAndType(fileUri, "application/vnd.android.package-archive");
            _context.startActivity(intent);
            return true;
        }
        return false;
    }

    /**
     * Share the given bitmap with given format
     *
     * @param bitmap    Image
     * @param format    A {@link Bitmap.CompressFormat}, supporting JPEG,PNG,WEBP
     * @param imageName Filename without extension
     * @param quality   Quality of the exported image [0-100]
     * @return if success, true
     */
    public boolean shareImage(final Bitmap bitmap, final Integer... quality) {
        try {
            File file = new File(_context.getCacheDir(), getFilenameWithTimestamp());
            if (bitmap != null && new ContextUtils(_context).writeImageToFile(file, bitmap, quality)) {
                String x = FileUtils.getMimeType(file);
                shareStream(file, FileUtils.getMimeType(file));
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Generate a filename based off current datetime in filename (year, month, day, hour, minute, second)
     * Examples: Screenshot_20210208-184301_Trebuchet.png IMG_20190511-230845.jpg
     *
     * @param A0prefixA1postfixA2ext All arguments are optional and default values are taken for null
     *                               [0] = Prefix [Screenshot/IMG]
     *                               [1] = Postfix [Trebuchet]
     *                               [2] = File extensions [jpg/png/txt]
     * @return Filename
     */
    public static String getFilenameWithTimestamp(String... A0prefixA1postfixA2ext) {
        final String prefix = (((A0prefixA1postfixA2ext != null && A0prefixA1postfixA2ext.length > 0 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[0])) ? A0prefixA1postfixA2ext[0] : "Screenshot") + "_").trim().replaceFirst("^_$", "");
        final String postfix = ("_" + ((A0prefixA1postfixA2ext != null && A0prefixA1postfixA2ext.length > 1 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[1])) ? A0prefixA1postfixA2ext[1] : "")).trim().replaceFirst("^_$", "");
        final String ext = (A0prefixA1postfixA2ext != null && A0prefixA1postfixA2ext.length > 2 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[2])) ? A0prefixA1postfixA2ext[2] : "jpg";
        return String.format("%s%s%s.%s", prefix.trim(), DATEFORMAT_IMG.format(new Date()), postfix.trim(), ext.toLowerCase().replace(".", "").replace("jpeg", "jpg"));
    }

    /**
     * Print a {@link WebView}'s contents, also allows to create a PDF
     *
     * @param webview WebView
     * @param jobName Name of the job (affects PDF name too)
     * @return {{@link PrintJob}} or null
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public PrintJob print(final WebView webview, final String jobName, final boolean... landscape) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final PrintDocumentAdapter printAdapter;
            final PrintManager printManager = (PrintManager) _context.getSystemService(Context.PRINT_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                printAdapter = webview.createPrintDocumentAdapter(jobName);
            } else {
                printAdapter = webview.createPrintDocumentAdapter();
            }
            final PrintAttributes.Builder attrib = new PrintAttributes.Builder();
            if (landscape != null && landscape.length > 0 && landscape[0]) {
                attrib.setMediaSize(new PrintAttributes.MediaSize("ISO_A4", "android", 11690, 8270));
                attrib.setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0));
            }
            if (printManager != null) {
                try {
                    return printManager.print(jobName, printAdapter, attrib.build());
                } catch (Exception ignored) {
                }
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
    public PrintJob createPdf(final WebView webview, final String jobName) {
        return print(webview, jobName);
    }


    /**
     * Create a picture out of {@link WebView}'s whole content
     *
     * @param webView The WebView to get contents from
     * @return A {@link Bitmap} or null
     */
    @Nullable
    public static Bitmap getBitmapFromWebView(final WebView webView, final boolean... a0fullpage) {
        try {
            //Measure WebView's content
            if (a0fullpage != null && a0fullpage.length > 0 && a0fullpage[0]) {
                int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                webView.measure(widthMeasureSpec, heightMeasureSpec);
                webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());
            }

            //Build drawing cache and store its size
            webView.buildDrawingCache();

            //Creates the bitmap and draw WebView's content on in
            Bitmap bitmap = Bitmap.createBitmap(webView.getMeasuredWidth(), webView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
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
     * Replace (primary) clipboard contents with given {@code text}
     * @param text Text to be set
     */
    public boolean setClipboard(final CharSequence text) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager cm = ((android.text.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null) {
                cm.setText(text);
                return true;
            }
        } else {
            android.content.ClipboardManager cm = ((android.content.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null) {
                ClipData clip = ClipData.newPlainText(_context.getPackageName(), text);
                try {
                    cm.setPrimaryClip(clip);
                } catch (Exception ignored) {
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Get clipboard contents, very failsafe and compat to older android versions
     */
    public List<String> getClipboard() {
        List<String> clipper = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager cm = ((android.text.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null && !TextUtils.isEmpty(cm.getText())) {
                clipper.add(cm.getText().toString());
            }
        } else {
            android.content.ClipboardManager cm = ((android.content.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData data = cm.getPrimaryClip();
                for (int i = 0; data != null && i < data.getItemCount() && i < data.getItemCount(); i++) {
                    ClipData.Item item = data.getItemAt(i);
                    if (item != null && !TextUtils.isEmpty(item.getText())) {
                        clipper.add(data.getItemAt(i).getText().toString());
                    }
                }
            }
        }
        return clipper;
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
    public void pasteOnHastebin(final String text, final Callback.a2<Boolean, String> callback, final String... serverOrNothing) {
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
    public void draftEmail(final String subject, final String body, final String... to) {
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

    /**
     * Try to force extract a absolute filepath from an intent
     *
     * @param receivingIntent The intent from {@link Activity#getIntent()}
     * @return A file or null if extraction did not succeed
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public File extractFileFromIntent(final Intent receivingIntent) {
        String action = receivingIntent.getAction();
        String type = receivingIntent.getType();
        String tmps;
        String fileStr;
        String[] sarr;
        final ArrayList<String> probeFiles = new ArrayList<>();

        // Filter non existing files out
        Callback.a0 filterNe = () -> {
            for (String fp : new ArrayList<>(probeFiles)) {
                boolean ok = false;
                if (!TextUtils.isEmpty(fp)) {
                    File f = new File(fp);
                    ok = f.exists() && f.canRead();
                }
                if (!ok) {
                    probeFiles.remove(fp);
                }
            }
        };

        if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) || Intent.ACTION_SEND.equals(action)) {
            // Markor, S.M.T FileManager
            if (receivingIntent.hasExtra((tmps = EXTRA_FILEPATH))) {
                probeFiles.add(receivingIntent.getStringExtra(tmps));
            }

            // Analyze data/Uri
            Uri fileUri = receivingIntent.getData();
            fileUri = (fileUri != null ? fileUri : receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM));
            if (fileUri != null && (fileStr = fileUri.toString()) != null) {
                // Uri contains file
                if (fileStr.startsWith("file://")) {
                    probeFiles.add(fileUri.getPath());
                }
                if (fileStr.startsWith((tmps = "content://"))) {
                    fileStr = fileStr.substring(tmps.length());
                    String fileProvider = fileStr.substring(0, fileStr.indexOf("/"));
                    fileStr = fileStr.substring(fileProvider.length() + 1);

                    // Some file managers dont add leading slash
                    if (fileStr.startsWith("storage/")) {
                        fileStr = "/" + fileStr;
                    }
                    // Some do add some custom prefix
                    for (String prefix : new String[]{"file", "document", "root_files", "name"}) {
                        if (fileStr.startsWith(prefix)) {
                            fileStr = fileStr.substring(prefix.length());
                        }
                    }

                    // prefix for External storage (/storage/emulated/0  ///  /sdcard/) --> e.g. "content://com.amaze.filemanager/storage_root/file.txt" = "/sdcard/file.txt"
                    for (String prefix : new String[]{"external/", "media/", "storage_root/"}) {
                        if (fileStr.startsWith((tmps = prefix))) {
                            probeFiles.add(Uri.decode(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileStr.substring(tmps.length())));
                        }
                    }

                    // Next/OwnCloud Fileprovider
                    for (String fp : new String[]{"org.nextcloud.files", "org.nextcloud.beta.files", "org.owncloud.files"}) {
                        if (fileProvider.equals(fp) && fileStr.startsWith(tmps = "external_files/")) {
                            probeFiles.add(Uri.decode("/storage/" + fileStr.substring(tmps.length()).trim()));
                        }
                    }
                    // AOSP File Manager/Documents
                    if (fileProvider.equals("com.android.externalstorage.documents") && fileStr.startsWith(tmps = "/primary%3A")) {
                        probeFiles.add(Uri.decode(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileStr.substring(tmps.length())));
                    }
                    // Mi File Explorer
                    if (fileProvider.equals("com.mi.android.globalFileexplorer.myprovider") && fileStr.startsWith(tmps = "external_files")) {
                        probeFiles.add(Uri.decode(Environment.getExternalStorageDirectory().getAbsolutePath() + fileStr.substring(tmps.length())));
                    }

                    if (fileStr.startsWith(tmps = "external_files/")) {
                        for (String prefix : new String[]{Environment.getExternalStorageDirectory().getAbsolutePath(), "/storage", ""}) {
                            probeFiles.add(Uri.decode(prefix + "/" + fileStr.substring(tmps.length())));
                        }

                    }

                    // URI Encoded paths with full path after content://package/
                    if (fileStr.startsWith("/") || fileStr.startsWith("%2F")) {
                        probeFiles.add(Uri.decode(fileStr));
                        probeFiles.add(fileStr);
                    }
                }
            }
            fileUri = receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null && !TextUtils.isEmpty(tmps = fileUri.getPath()) && tmps.startsWith("/")) {
                probeFiles.add(tmps);
            }

            // Scan MediaStore.MediaColumns
            sarr = resolveDataColumns(_context, receivingIntent, MediaStore.MediaColumns.DATA, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.MediaColumns.DATA : null));
            if (sarr[0] != null) {
                probeFiles.add(sarr[0]);
            }
            if (sarr[1] != null) {
                probeFiles.add(Environment.getExternalStorageDirectory() + "/" + sarr[1]);
            }
        }
        filterNe.callback();

        // Try build proxy by contentresolver if no file found
        if (probeFiles.isEmpty()) {
            Uri uri = new ShareCompat.IntentReader(_context, receivingIntent).getStream();
            uri = (uri != null ? uri : receivingIntent.getData());
            try {
                File f = new File(_context.getCacheDir(), CONTENT_RESOLVER_FILE_PROXY_SEGMENT + "/" + uri.getLastPathSegment());
                f.getParentFile().mkdirs();
                byte[] data = FileUtils.readCloseBinaryStream(_context.getContentResolver().openInputStream(uri));
                FileUtils.writeFile(f, data, null);
                f.setReadable(true);
                f.setWritable(true);
                probeFiles.add(f.getAbsolutePath());
            } catch (Exception ignored) {
            }
        }

        return probeFiles.isEmpty() ? null : new File(probeFiles.get(0));
    }

    public static String[] resolveDataColumns(final Context context, final Intent intent, final String... columns) {
        final String[] out = (new String[columns.length]);
        final int INVALID = -1;
        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(intent.getData(), columns, null, null, null);
        } catch (Exception ignored) {
            cursor = null;
        }
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < columns.length; i++) {
                final int coli = TextUtils.isEmpty(columns[i]) ? INVALID : cursor.getColumnIndex(columns[i]);
                out[i] = (coli == INVALID ? null : cursor.getString(coli));
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return out;
    }

    /**
     * Request a picture from gallery
     * Result will be available from {@link Activity#onActivityResult(int, int, Intent)}.
     * It will return the path to the image if locally stored. If retrieved from e.g. a cloud
     * service, the image will get copied to app-cache folder and it's path returned.
     */
    public void requestGalleryPicture() {
        if (!(_context instanceof Activity)) {
            throw new RuntimeException("Error: ShareUtil.requestGalleryPicture needs an Activity Context.");
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            ((Activity) _context).startActivityForResult(intent, REQUEST_PICK_PICTURE);
        } catch (Exception ex) {
            Toast.makeText(_context, "No gallery app installed!", Toast.LENGTH_SHORT).show();
        }
    }

    public String extractFileFromIntentStr(final Intent receivingIntent) {
        File f = extractFileFromIntent(receivingIntent);
        return f != null ? f.getAbsolutePath() : null;
    }

    /**
     * Request a picture from camera-like apps
     * Result ({@link String}) will be available from {@link Activity#onActivityResult(int, int, Intent)}.
     * It has set resultCode to {@link Activity#RESULT_OK} with same requestCode, if successfully
     * The requested image savepath has to be stored at caller side (not contained in intent),
     * it can be retrieved using {@link #extractResultFromActivityResult(int, int, Intent, Activity...)}
     * returns null if an error happened.
     *
     * @param target Path to file to write to, if folder the filename gets app_name + millis + random filename. If null DCIM folder is used.
     */
    @SuppressWarnings("RegExpRedundantEscape")
    public String requestCameraPicture(final File target) {
        if (!(_context instanceof Activity)) {
            throw new RuntimeException("Error: ShareUtil.requestCameraPicture needs an Activity Context.");
        }
        final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final String timestampedFilename = getFilenameWithTimestamp("IMG", "", "jpg");
        final File storageDir = target != null ? target : new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");

        String cameraPictureFilepath = null;
        if (takePictureIntent.resolveActivity(_context.getPackageManager()) != null) {
            File photoFile;
            try {
                // Create an image file name
                if (target != null && !target.isDirectory()) {
                    photoFile = target;
                } else {
                    photoFile = new File(storageDir, timestampedFilename);
                    if (!photoFile.getParentFile().exists() && !photoFile.getParentFile().mkdirs()) {
                        photoFile = File.createTempFile(timestampedFilename.replace(".jpg", "_"), ".jpg", storageDir);
                    }
                }

                //noinspection StatementWithEmptyBody
                if (!photoFile.getParentFile().exists() && photoFile.getParentFile().mkdirs()) ;

                // Save a file: path for use with ACTION_VIEW intents
                cameraPictureFilepath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                return null;
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(_context, getFileProviderAuthority(), photoFile));
                } else {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }
                ((Activity) _context).startActivityForResult(takePictureIntent, REQUEST_CAMERA_PICTURE);
            }
        }
        _lastCameraPictureFilepath = cameraPictureFilepath;
        return cameraPictureFilepath;
    }

    /**
     * Extract result data from {@link Activity#onActivityResult(int, int, Intent)}.
     * Forward all arguments from activity. Only requestCodes from {@link ShareUtil} get analyzed.
     * Also may forward results via local broadcast
     */
    @SuppressLint("ApplySharedPref")
    public Object extractResultFromActivityResult(final int requestCode, final int resultCode, final Intent data, final Activity... activityOrNull) {
        Activity activity = greedyGetActivity(activityOrNull);
        switch (requestCode) {
            case REQUEST_CAMERA_PICTURE: {
                String picturePath = (resultCode == RESULT_OK) ? _lastCameraPictureFilepath : null;
                if (picturePath != null) {
                    sendLocalBroadcastWithStringExtra(REQUEST_CAMERA_PICTURE + "", EXTRA_FILEPATH, picturePath);
                }
                return picturePath;
            }
            case REQUEST_PICK_PICTURE: {
                if (resultCode == RESULT_OK && data != null) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    String picturePath = null;

                    Cursor cursor = _context.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        for (String column : filePathColumn) {
                            int curColIndex = cursor.getColumnIndex(column);
                            if (curColIndex == -1) {
                                continue;
                            }
                            picturePath = cursor.getString(curColIndex);
                            if (!TextUtils.isEmpty(picturePath)) {
                                break;
                            }
                        }
                        cursor.close();
                    }

                    // Try to grab via file extraction method
                    data.setAction(Intent.ACTION_VIEW);
                    picturePath = picturePath != null ? picturePath : extractFileFromIntentStr(data);

                    // Retrieve image from file descriptor / Cloud, e.g.: Google Drive, Picasa
                    if (picturePath == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try {
                            ParcelFileDescriptor parcelFileDescriptor = _context.getContentResolver().openFileDescriptor(selectedImage, "r");
                            if (parcelFileDescriptor != null) {
                                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                                FileInputStream input = new FileInputStream(fileDescriptor);

                                // Create temporary file in cache directory
                                picturePath = File.createTempFile("image", "tmp", _context.getCacheDir()).getAbsolutePath();
                                FileUtils.writeFile(new File(picturePath), FileUtils.readCloseBinaryStream(input), null);
                            }
                        } catch (IOException ignored) {
                            // nothing we can do here, null value will be handled below
                        }
                    }

                    // Return path to picture on success, else null
                    if (picturePath != null) {
                        sendLocalBroadcastWithStringExtra(REQUEST_CAMERA_PICTURE + "", EXTRA_FILEPATH, picturePath);
                    }
                    return picturePath;
                }
                break;
            }

            case REQUEST_SAF: {
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    Uri treeUri = data.getData();
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString(PREF_KEY__SAF_TREE_URI, treeUri.toString()).commit();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        activity.getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    }
                    return treeUri;
                }
                break;
            }

            case REQUEST_STORAGE_PERMISSION_M:
            case REQUEST_STORAGE_PERMISSION_R: {
                return checkExternalStoragePermission(false);
            }
        }
        return null;
    }

    /**
     * Send a local broadcast (to receive within app), with given action and string-extra+value.
     * This is a convenience method for quickly sending just one thing.
     */
    public void sendLocalBroadcastWithStringExtra(final String action, final String extra, final CharSequence value) {
        Intent intent = new Intent(action);
        intent.putExtra(extra, value);
        LocalBroadcastManager.getInstance(_context).sendBroadcast(intent);
    }

    /**
     * Receive broadcast results via a callback method
     *
     * @param callback       Function to call with received {@link Intent}
     * @param autoUnregister wether or not to automatically unregister receiver after first match
     * @param filterActions  All {@link IntentFilter} actions to filter for
     * @return The created instance. Has to be unregistered on {@link Activity} lifecycle events.
     */
    public BroadcastReceiver receiveResultFromLocalBroadcast(final Callback.a2<Intent, BroadcastReceiver> callback, final boolean autoUnregister, final String... filterActions) {
        IntentFilter intentFilter = new IntentFilter();
        for (String filterAction : filterActions) {
            intentFilter.addAction(filterAction);
        }
        final BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if (autoUnregister) {
                        LocalBroadcastManager.getInstance(_context).unregisterReceiver(this);
                    }
                    try {
                        callback.callback(intent, this);
                    } catch (Exception ignored) {
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(_context).registerReceiver(br, intentFilter);
        return br;
    }

    /**
     * Request edit of image (by image editor/viewer - for example to crop image)
     *
     * @param file File that should be edited
     */
    public void requestPictureEdit(final File file) {
        Uri uri = getUriByFileProviderAuthority(file);
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra(EXTRA_FILEPATH, file.getAbsolutePath());
        _context.startActivity(Intent.createChooser(intent, null));
    }

    /**
     * Get content://media/ Uri for given file, or null if not indexed
     *
     * @param file Target file
     * @param mode 1 for picture, 2 for video, anything else for other
     * @return Media URI
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public Uri getMediaUri(final File file, final int mode) {
        Uri uri = MediaStore.Files.getContentUri("external");
        uri = (mode != 0) ? (mode == 1 ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI) : uri;

        Cursor cursor = null;
        try {
            cursor = _context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "= ?", new String[]{file.getAbsolutePath()}, null);
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range")
                int mediaid = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                return Uri.withAppendedPath(uri, mediaid + "");
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * By default Chrome Custom Tabs only uses Chrome Stable to open links
     * There are also other packages (like Chrome Beta, Chromium, Firefox, ..)
     * which implement the Chrome Custom Tab interface. This method changes
     * the customtab intent to use an available compatible browser, if available.
     */
    public void enableChromeCustomTabsForOtherBrowsers(final Intent customTabIntent) {
        String[] checkpkgs = new String[]{
                "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.google.android.apps.chrome", "org.chromium.chrome",
                "org.mozilla.fennec_fdroid", "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fennec_aurora",
                "org.mozilla.klar", "org.mozilla.focus",
        };

        // Get all intent handlers for web links
        PackageManager pm = _context.getPackageManager();
        Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"));
        List<String> browsers = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(urlIntent, 0)) {
            Intent i = new Intent("android.support.customtabs.action.CustomTabsService");
            i.setPackage(ri.activityInfo.packageName);
            if (pm.resolveService(i, 0) != null) {
                browsers.add(ri.activityInfo.packageName);
            }
        }

        // Check if the user has a "default browser" selected
        ResolveInfo ri = pm.resolveActivity(urlIntent, 0);
        String userDefaultBrowser = (ri == null) ? null : ri.activityInfo.packageName;

        // Select which browser to use out of all installed customtab supporting browsers
        String pkg = null;
        if (browsers.isEmpty()) {
            pkg = null;
        } else if (browsers.size() == 1) {
            pkg = browsers.get(0);
        } else if (!TextUtils.isEmpty(userDefaultBrowser) && browsers.contains(userDefaultBrowser)) {
            pkg = userDefaultBrowser;
        } else {
            for (String checkpkg : checkpkgs) {
                if (browsers.contains(checkpkg)) {
                    pkg = checkpkg;
                    break;
                }
            }
            if (pkg == null && !browsers.isEmpty()) {
                pkg = browsers.get(0);
            }
        }
        if (pkg != null && customTabIntent != null) {
            customTabIntent.setPackage(pkg);
        }
    }

    public boolean openWebpageInChromeCustomTab(final String url) {
        boolean ok = false;
        ContextUtils cu = new ContextUtils(_context);
        try {
            // Use a CustomTabsIntent.Builder to configure CustomTabsIntent.
            // Once ready, call CustomTabsIntent.Builder.build() to create a CustomTabsIntent
            // and launch the desired Url with CustomTabsIntent.launchUrl()
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(_context, cu.getResId(ContextUtils.ResType.COLOR, "primary")));
            builder.setSecondaryToolbarColor(ContextCompat.getColor(_context, cu.getResId(ContextUtils.ResType.COLOR, "primary_dark")));
            builder.addDefaultShareMenuItem();
            CustomTabsIntent customTabsIntent = builder.build();
            enableChromeCustomTabsForOtherBrowsers(customTabsIntent.intent);
            customTabsIntent.launchUrl(_context, Uri.parse(url));
            ok = true;
        } catch (Exception ignored) {
        }
        cu.freeContextRef();
        return ok;
    }

    /***
     * Request storage access. The user needs to press "Select storage" at the correct storage.
     * @param activity The activity which will receive the result from startActivityForResult
     */
    public void requestStorageAccessFramework(final Activity... activity) {
        Activity a = greedyGetActivity(activity);
        if (a != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            );
            a.startActivityForResult(intent, REQUEST_SAF);
        }
    }

    /**
     * Get storage access framework tree uri. The user must have granted access via {@link #requestStorageAccessFramework(Activity...)}
     *
     * @return Uri or null if not granted yet
     */
    public Uri getStorageAccessFrameworkTreeUri() {
        String treeStr = PreferenceManager.getDefaultSharedPreferences(_context).getString(PREF_KEY__SAF_TREE_URI, null);
        if (!TextUtils.isEmpty(treeStr)) {
            try {
                return Uri.parse(treeStr);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Get mounted storage folder root (by tree uri). The user must have granted access via {@link #requestStorageAccessFramework(Activity...)}
     *
     * @return File or null if SD not mounted
     */
    public File getStorageAccessFolder() {
        Uri safUri = getStorageAccessFrameworkTreeUri();
        if (safUri != null) {
            String safUriStr = safUri.toString();
            ContextUtils cu = new ContextUtils(_context);
            for (Pair<File, String> storage : cu.getStorages(false, true)) {
                @SuppressWarnings("ConstantConditions") String storageFolderName = storage.first.getName();
                if (safUriStr.contains(storageFolderName)) {
                    return storage.first;
                }
            }
            cu.freeContextRef();
        }
        return null;
    }

    /**
     * Check whether or not a file is under a storage access folder (external storage / SD)
     *
     * @param file The file object (file/folder)
     * @return Wether or not the file is under storage access folder
     */
    public boolean isUnderStorageAccessFolder(final File file) {
        if (file != null) {
            // When file writeable as is, it's the fastest way to learn SAF isn't required
            if (file.canWrite()) {
                return false;
            }
            ContextUtils cu = new ContextUtils(_context);
            for (Pair<File, String> storage : cu.getStorages(false, true)) {
                if (file.getAbsolutePath().startsWith(storage.first.getAbsolutePath())) {
                    cu.freeContextRef();
                    return true;
                }
            }
            cu.freeContextRef();
        }
        return false;
    }

    public boolean isContentResolverProxyFile(final File file) {
        return file != null && CONTENT_RESOLVER_FILE_PROXY_SEGMENT.equals(file.getParentFile().getName());
    }

    /**
     * Greedy extract Activity from parameter or convert context if it's a activity
     */
    private Activity greedyGetActivity(final Activity... activity) {
        if (activity != null && activity.length != 0 && activity[0] != null) {
            return activity[0];
        }
        if (_context instanceof Activity) {
            return (Activity) _context;
        }
        return null;
    }

    /**
     * Check whether or not a file can be written.
     * Requires storage access framework permission for external storage (SD)
     *
     * @param file  The file object (file/folder)
     * @param isDir Wether or not the given file parameter is a directory
     * @return Wether or not the file can be written
     */
    public boolean canWriteFile(final File file, final boolean isDir) {
        if (file == null) {
            return false;
        } else if (file.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())
                || file.getAbsolutePath().startsWith(_context.getFilesDir().getAbsolutePath())) {
            boolean s1 = isDir && file.getParentFile().canWrite();
            return !isDir && file.getParentFile() != null ? file.getParentFile().canWrite() : file.canWrite();
        } else {
            DocumentFile dof = getDocumentFile(file, isDir);
            return dof != null && dof.canWrite();
        }
    }

    /**
     * Get a {@link DocumentFile} object out of a normal java {@link File}.
     * When used on a external storage (SD), use {@link #requestStorageAccessFramework(Activity...)}
     * first to get access. Otherwise this will fail.
     *
     * @param file  The file/folder to convert
     * @param isDir Wether or not file is a directory. For non-existing (to be created) files this info is not known hence required.
     * @return A {@link DocumentFile} object or null if file cannot be converted
     */
    @SuppressWarnings("RegExpRedundantEscape")
    public DocumentFile getDocumentFile(final File file, final boolean isDir) {
        // On older versions use fromFile
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return DocumentFile.fromFile(file);
        }

        // Get ContextUtils to find storageRootFolder
        ContextUtils cu = new ContextUtils(_context);
        File baseFolderFile = cu.getStorageRootFolder(file);
        cu.freeContextRef();

        String baseFolder = baseFolderFile == null ? null : baseFolderFile.getAbsolutePath();
        boolean originalDirectory = false;
        if (baseFolder == null) {
            return null;
        }

        String relPath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath)) {
                relPath = fullPath.substring(baseFolder.length() + 1);
            } else {
                originalDirectory = true;
            }
        } catch (IOException e) {
            return null;
        } catch (Exception ignored) {
            originalDirectory = true;
        }
        Uri treeUri;
        if ((treeUri = getStorageAccessFrameworkTreeUri()) == null) {
            return null;
        }
        DocumentFile dof = DocumentFile.fromTreeUri(_context, treeUri);
        if (originalDirectory) {
            return dof;
        }
        String[] parts = relPath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDof = dof.findFile(parts[i]);
            if (nextDof == null) {
                try {
                    nextDof = ((i < parts.length - 1) || isDir) ? dof.createDirectory(parts[i]) : dof.createFile("image", parts[i]);
                } catch (Exception ignored) {
                    nextDof = null;
                }
            }
            dof = nextDof;
        }
        return dof;
    }

    public void showMountSdDialog(@StringRes final int title, @StringRes final int description, @DrawableRes final int mountDescriptionGraphic, final Activity... activityOrNull) {
        Activity activity = greedyGetActivity(activityOrNull);
        if (activity == null) {
            return;
        }

        // Image viewer
        ImageView imv = new ImageView(activity);
        imv.setImageResource(mountDescriptionGraphic);
        imv.setAdjustViewBounds(true);

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setView(imv);
        dialog.setTitle(title);
        dialog.setMessage(_context.getString(description) + "\n\n");
        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> requestStorageAccessFramework(activity));
        AlertDialog dialogi = dialog.create();
        dialogi.show();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "StatementWithEmptyBody"})
    public void writeFile(final File file, final boolean isDirectory, final Callback.a2<Boolean, OutputStream> writeFileCallback) {
        try {
            OutputStream fileOutputStream = null;
            ParcelFileDescriptor pfd = null;
            final boolean existingEmptyFile = file.canWrite() && file.length() < MIN_OVERWRITE_LENGTH;
            final boolean nonExistingCreatableFile = !file.exists() && file.getParentFile().canWrite();
            if (existingEmptyFile || nonExistingCreatableFile) {
                if (isDirectory) {
                    file.mkdirs();
                } else {
                    fileOutputStream = new FileOutputStream(file);
                }
            } else if (isContentResolverProxyFile(file)) {
                // File initially read from Activity, Intent & ContentResolver -> write back to it
                try {
                    Intent intent = greedyGetActivity().getIntent();
                    Uri uri = new ShareCompat.IntentReader(_context, intent).getStream();
                    uri = (uri != null ? uri : intent.getData());
                    fileOutputStream = _context.getContentResolver().openOutputStream(uri);
                } catch (Exception ignored) {
                }
            } else {
                DocumentFile dof = getDocumentFile(file, isDirectory);
                if (dof != null && dof.getUri() != null && dof.canWrite()) {
                    if (isDirectory) {
                        // Nothing to do
                    } else {
                        pfd = _context.getContentResolver().openFileDescriptor(dof.getUri(), "rwt");
                        fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                    }
                }
            }
            if (writeFileCallback != null) {
                writeFileCallback.callback(fileOutputStream != null || (isDirectory && file.exists()), fileOutputStream);
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception ignored) {
                }
            }
            if (pfd != null) {
                pfd.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Call telephone number.
     * Non direct call, opens up the dialer and pre-sets the telephone number. User needs to press manually.
     * Direct call requires M permission granted, also add permissions to manifest:
     * <uses-permission android:name="android.permission.CALL_PHONE" />
     *
     * @param telNo      The telephone number to call
     * @param directCall Direct call number if possible
     */
    @SuppressWarnings("SimplifiableConditionalExpression")
    public void callTelephoneNumber(final String telNo, final boolean... directCall) {
        Activity activity = greedyGetActivity();
        if (activity == null) {
            throw new RuntimeException("Error: ShareUtil::callTelephoneNumber needs to be contstructed with activity context");
        }
        boolean ldirectCall = (directCall != null && directCall.length > 0) ? directCall[0] : true;


        if (android.os.Build.VERSION.SDK_INT >= 23 && ldirectCall && activity != null) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE}, 4001);
                ldirectCall = false;
            } else {
                try {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + telNo));
                    activity.startActivity(callIntent);
                } catch (Exception ignored) {
                    ldirectCall = false;
                }
            }
        }
        // Show dialer up with telephone number pre-inserted
        if (!ldirectCall) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", telNo, null));
            activity.startActivity(intent);
        }
    }

    /**
     * @param locale   {@link Locale} locale
     * @param format   {@link String} text which 'll be used as format for {@link SimpleDateFormat}
     * @param datetime {@link Long}   requested time miliseconds
     * @param fallback {@link String} default fallback value. If the format is incorrect and a default is not provided, return the specified format
     * @return formatted string
     */
    public static String formatDateTime(@Nullable final Locale locale, @NonNull final String format, @Nullable final Long datetime, @Nullable final String... fallback) {
        try {
            final Locale l = locale != null ? locale : Locale.getDefault();
            final long t = datetime != null ? datetime : System.currentTimeMillis();
            return new SimpleDateFormat(StringUtils.unescapeString(format), l).format(t);
        } catch (Exception err) {
            return (fallback != null && fallback.length > 0) ? fallback[0] : format;
        }
    }

    public static String formatDateTime(@NonNull final Context context, @NonNull final String format, @Nullable final Long datetime, @Nullable final String... def) {
        final Locale locale = ConfigurationCompat.getLocales(context.getResources().getConfiguration()).get(0);
        return formatDateTime(locale, format, datetime, def);
    }

    @SuppressWarnings("ConstantConditions")
    public boolean checkExternalStoragePermission(final boolean doRequest, String... optionalDescription) {
        final Activity activity = greedyGetActivity();
        final int v = android.os.Build.VERSION.SDK_INT;
        final AtomicReference<Callback.a0> permissionRequest = new AtomicReference<>();

        // On Android R+ - check externalStorageManager is granted, otherwise request it
        if (v >= android.os.Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            permissionRequest.set(() -> {
                ContextUtils cu = new ContextUtils(activity.getApplicationContext());
                try {
                    Uri uri = Uri.parse("package:" + cu.getPackageIdReal());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    activity.startActivityForResult(intent, REQUEST_STORAGE_PERMISSION_R);
                } catch (Exception ex) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivityForResult(intent, REQUEST_STORAGE_PERMISSION_R);
                }
                cu.freeContextRef();
            });
        }

        // On Android M-Q - request M permission
        if (v >= android.os.Build.VERSION_CODES.M && v < android.os.Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionRequest.set(() -> ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION_M));
        }

        if (doRequest && permissionRequest.get() != null) {
            if (optionalDescription == null || optionalDescription.length == 0 || TextUtils.isEmpty(optionalDescription[0])) {
                permissionRequest.get().callback();
            } else {
                final AlertDialog d = new AlertDialog.Builder(activity)
                        .setMessage(optionalDescription[0])
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> permissionRequest.get().callback())
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                d.setCanceledOnTouchOutside(false);
            }
        }

        // Android R Manage-All-Files permission
        if (v >= android.os.Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }

        // Android M permissions
        if (v >= android.os.Build.VERSION_CODES.M && v < android.os.Build.VERSION_CODES.R) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        // In case unsure, check if anything is writable at external storage
        for (final File f : Environment.getExternalStorageDirectory() != null ? Environment.getExternalStorageDirectory().listFiles() : new File[0]) {
            if (f.canWrite()) {
                return true;
            }
        }
        return false;
    }
}
