/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.app.Activity;
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
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v4.graphics.drawable.IconCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static android.app.Activity.RESULT_OK;

/**
 * A utility class to ease information sharing on Android.
 * Also allows to parse/fetch information out of shared information.
 * (M)Permissions are not checked, wrap ShareUtils methods if neccessary
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "SameParameterValue", "unused", "deprecation", "ConstantConditions", "ObsoleteSdkInt", "SpellCheckingInspection"})
public class ShareUtil {
    public final static String EXTRA_FILEPATH = "real_file_path_2";
    public final static SimpleDateFormat SDF_RFC3339_ISH = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm", Locale.getDefault());
    public final static SimpleDateFormat SDF_SHORT = new SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault());
    public final static String MIME_TEXT_PLAIN = "text/plain";

    public final static int REQUEST_CAMERA_PICTURE = 50001;
    public final static int REQUEST_PICK_PICTURE = 50002;

    protected static String _lastCameraPictureFilepath;

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
     * @param intent  The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title   Title of the item
     */
    public void createLauncherDesktopShortcut(Intent intent, @DrawableRes int iconRes, String title) {
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
    public void createLauncherDesktopShortcutLegacy(Intent intent, @DrawableRes int iconRes, String title) {
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
    public void shareText(String text, @Nullable String mimeType) {
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
    public boolean shareStream(File file, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(EXTRA_FILEPATH, file.getAbsolutePath());
        intent.setType(mimeType);

        try {
            Uri fileUri = FileProvider.getUriForFile(_context, getFileProviderAuthority(), file);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            showChooser(intent, null);
            return true;
        } catch (Exception e) { // FileUriExposed(API24) / IllegalArgument
            return false;
        }
    }

    /**
     * Start calendar application to add new event, with given details prefilled
     */
    public void createCalendarAppointment(@Nullable String title, @Nullable String description, @Nullable String location, @Nullable Long... startAndEndTime) {
        Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
        if (title != null) {
            intent.putExtra(CalendarContract.Events.TITLE, title);
        }
        if (description != null) {
            description = description.length() > 800 ? description.substring(0, 800) : description;
            intent.putExtra(CalendarContract.Events.DESCRIPTION, description);
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
        _context.startActivity(intent);
    }

    /**
     * Open a View intent for given file
     *
     * @param file The file to share
     */
    public boolean viewFileInOtherApp(File file, @Nullable String type) {
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
     * Share the given bitmap with given format
     *
     * @param bitmap Image
     * @param format A {@link Bitmap.CompressFormat}, supporting JPEG,PNG,WEBP
     * @return if success, true
     */
    public boolean shareImage(Bitmap bitmap, Bitmap.CompressFormat format) {
        return shareImage(bitmap, format, 95, "SharedImage");
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
    public boolean shareImage(Bitmap bitmap, Bitmap.CompressFormat format, int quality, String imageName) {
        try {
            String ext = format.name().toLowerCase();
            File file = File.createTempFile(imageName, "." + ext.replace("jpeg", "jpg"), _context.getExternalCacheDir());
            if (bitmap != null && new ContextUtils(_context).writeImageToFile(file, bitmap, format, quality)) {
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
     * Replace (primary) clipboard contents with given {@code text}
     * @param text Text to be set
     */
    public boolean setClipboard(CharSequence text) {
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
                cm.setPrimaryClip(clip);
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

    /**
     * Try to force extract a absolute filepath from an intent
     *
     * @param receivingIntent The intent from {@link Activity#getIntent()}
     * @return A file or null if extraction did not succeed
     */
    public File extractFileFromIntent(Intent receivingIntent) {
        String action = receivingIntent.getAction();
        String type = receivingIntent.getType();
        File tmpf;
        String tmps;
        String fileStr;

        if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) || Intent.ACTION_SEND.equals(action)) {
            // Markor, S.M.T FileManager
            if (receivingIntent.hasExtra((tmps = EXTRA_FILEPATH))) {
                return new File(receivingIntent.getStringExtra(tmps));
            }

            // Analyze data/Uri
            Uri fileUri = receivingIntent.getData();
            if (fileUri != null && (fileStr = fileUri.toString()) != null) {
                // Uri contains file
                if (fileStr.startsWith("file://")) {
                    return new File(fileUri.getPath());
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
                    // Next/OwnCloud Fileprovider
                    for (String fp : new String[]{"org.nextcloud.files", "org.nextcloud.beta.files", "org.owncloud.files"}) {
                        if (fileProvider.equals(fp) && fileStr.startsWith(tmps = "external_files/")) {
                            return new File(Uri.decode("/storage/" + fileStr.substring(tmps.length())));
                        }
                    }
                    // AOSP File Manager/Documents
                    if (fileProvider.equals("com.android.externalstorage.documents") && fileStr.startsWith(tmps = "/primary%3A")) {
                        return new File(Uri.decode(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileStr.substring(tmps.length())));
                    }
                    // Mi File Explorer
                    if (fileProvider.equals("com.mi.android.globalFileexplorer.myprovider") && fileStr.startsWith(tmps = "external_files")) {
                        return new File(Uri.decode(Environment.getExternalStorageDirectory().getAbsolutePath() + fileStr.substring(tmps.length())));
                    }
                    // URI Encoded paths with full path after content://package/
                    if (fileStr.startsWith("/") || fileStr.startsWith("%2F")) {
                        tmpf = new File(Uri.decode(fileStr));
                        if (tmpf.exists()) {
                            return tmpf;
                        } else if ((tmpf = new File(fileStr)).exists()) {
                            return tmpf;
                        }
                    }
                }
            }
            fileUri = receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null && !TextUtils.isEmpty(tmps = fileUri.getPath()) && tmps.startsWith("/") && (tmpf = new File(tmps)).exists()) {
                return tmpf;
            }
        }
        return null;
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
        ((Activity) _context).startActivityForResult(intent, REQUEST_PICK_PICTURE);
    }

    /**
     * Request a picture from camera-like apps
     * Result ({@link String}) will be available from {@link Activity#onActivityResult(int, int, Intent)}.
     * It has set resultCode to {@link Activity#RESULT_OK} with same requestCode, if successfully
     * The requested image savepath has to be stored at caller side (not contained in intent),
     * it can be retrieved using {@link #extractResultFromActivityResult(int, int, Intent)},
     * returns null if an error happened.
     *
     * @param target Path to file to write to, if folder the filename gets app_name + millis + random filename. If null DCIM folder is used.
     */
    public String requestCameraPicture(File target) {
        if (!(_context instanceof Activity)) {
            throw new RuntimeException("Error: ShareUtil.requestCameraPicture needs an Activity Context.");
        }
        String cameraPictureFilepath = null;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(_context.getPackageManager()) != null) {
            File photoFile;
            try {
                // Create an image file name
                if (target != null && !target.isDirectory()) {
                    photoFile = target;
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault());
                    File storageDir = target != null ? target : new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
                    String imageFileName = ((new ContextUtils(_context).rstr("app_name")).replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + "_").replace("__", "_") + sdf.format(new Date());
                    photoFile = new File(storageDir, imageFileName + ".jpg");
                    if (!photoFile.getParentFile().exists() && !photoFile.getParentFile().mkdirs()) {
                        photoFile = File.createTempFile(imageFileName + "_", ".jpg", storageDir);
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
                    Uri uri = FileProvider.getUriForFile(_context, getFileProviderAuthority(), photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
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
    public Object extractResultFromActivityResult(int requestCode, int resultCode, Intent data) {
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

                    // Retrieve image from file descriptor / Cloud, e.g.: Google Drive, Picasa
                    if (picturePath == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try {
                            ParcelFileDescriptor parcelFileDescriptor = _context.getContentResolver().openFileDescriptor(selectedImage, "r");
                            if (parcelFileDescriptor != null) {
                                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                                FileInputStream input = new FileInputStream(fileDescriptor);

                                // Create temporary file in cache directory
                                picturePath = File.createTempFile("image", "tmp", _context.getCacheDir()).getAbsolutePath();
                                FileUtils.writeFile(new File(picturePath), FileUtils.readCloseBinaryStream(input));
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
        }
        return null;
    }

    /**
     * Send a local broadcast (to receive within app), with given action and string-extra+value.
     * This is a convenience method for quickly sending just one thing.
     */
    public void sendLocalBroadcastWithStringExtra(String action, String extra, CharSequence value) {
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
    public BroadcastReceiver receiveResultFromLocalBroadcast(Callback.a2<Intent, BroadcastReceiver> callback, boolean autoUnregister, String... filterActions) {
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
    public void requestPictureEdit(File file) {
        Uri uri = getUriByFileProviderAuthority(file);
        int flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION;

        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(flags);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra(EXTRA_FILEPATH, file.getAbsolutePath());

        for (ResolveInfo resolveInfo : _context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            String packageName = resolveInfo.activityInfo.packageName;
            _context.grantUriPermission(packageName, uri, flags);
        }
        _context.startActivity(Intent.createChooser(intent, null));
    }

    /**
     * Get content://media/ Uri for given file, or null if not indexed
     *
     * @param file Target file
     * @param mode 1 for picture, 2 for video, anything else for other
     * @return
     */
    public Uri getMediaUri(File file, int mode) {
        Uri uri = MediaStore.Files.getContentUri("external");
        uri = (mode != 0) ? (mode == 1 ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI) : uri;

        Cursor cursor = null;
        try {
            cursor = _context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "= ?", new String[]{file.getAbsolutePath()}, null);
            if (cursor != null && cursor.moveToFirst()) {
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
}
