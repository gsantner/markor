/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.model;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Document implements Serializable {
    private static final int MAX_TITLE_EXTRACTION_LENGTH = 25;

    public static final String EXTRA_DOCUMENT = "EXTRA_DOCUMENT"; // Document
    public static final String EXTRA_PATH = "EXTRA_PATH"; // java.io.File
    public static final String EXTRA_PATH_IS_FOLDER = "EXTRA_PATH_IS_FOLDER"; // boolean
    public static final String EXTRA_FILE_LINE_NUMBER = "EXTRA_FILE_LINE_NUMBER"; // int

    private final File _file;
    private final String _fileExtension;
    private int _format = TextFormat.FORMAT_UNKNOWN;
    private String _title = "";
    private int _hash = 0;                            // Hash of last processed content
    private long _modTime = 0;                        // Modtime of last loaded content
    private int _initialLineNumber = -1;

    public Document(File file) {
        _file = file;

        _modTime = file.lastModified();

        final String name = _file.getName();
        final int doti = name.lastIndexOf(".");
        if (doti < 0) {
            _fileExtension = "";
            _title = name;
        } else {
            _fileExtension = name.substring(doti).toLowerCase();
            _title = name.substring(0, doti);
        }

        // Set initial format
        final String fnlower = getFile().getName().toLowerCase();
        if (TextFormat.CONVERTER_TODOTXT.isFileOutOfThisFormat(fnlower)) {
            setFormat(TextFormat.FORMAT_TODOTXT);
        } else if (TextFormat.CONVERTER_KEYVALUE.isFileOutOfThisFormat(fnlower)) {
            setFormat(TextFormat.FORMAT_KEYVALUE);
        } else if (TextFormat.CONVERTER_MARKDOWN.isFileOutOfThisFormat(fnlower)) {
            setFormat(TextFormat.FORMAT_MARKDOWN);
        } else if (TextFormat.CONVERTER_ZIMWIKI.isFileOutOfThisFormat(getPath())) {
            setFormat(TextFormat.FORMAT_ZIMWIKI);
        } else {
            setFormat(TextFormat.FORMAT_PLAIN);
        }
    }

    public String getPath() {
        return getPath(this);
    }

    public static String getPath(final Document document) {
        if (document != null) {
            final File file = document.getFile();
            if (file != null) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public boolean makePath() {
        try {
            final File parent = _file.getParentFile();
            parent.mkdirs();
            return parent.exists();
        } catch (NullPointerException | SecurityException ignored){};
        return false;
    }

    public File getFile() {
        return _file;
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        _title = title == null ? "" : title;
    }

    public int getLastHash() {
        return _hash;
    }

    public boolean contentChanged(final CharSequence data) {
        return (data != null) && _hash != data.hashCode();
    }

    public String getName() {
        return getFile().getName();
    }

    public void setInitialLineNumber(int num) {
        _initialLineNumber = num;
    }

    public int getInitialLineNumber() {
        return _initialLineNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Document) {
            Document other = ((Document) obj);
            return equalsc(getFile(), other.getFile())
                    && equalsc(getTitle(), other.getTitle())
                    && (getFormat() == other.getFormat())
                    && (getLastHash() == other.getLastHash());
        }
        return super.equals(obj);
    }

    private static boolean equalsc(Object o1, Object o2) {
        return (o1 == null && o2 == null) || o1 != null && o1.equals(o2);
    }

    //
    //
    //

    public String getFileExtension() {
        return _fileExtension;
    }

    public int getFormat() {
        return _format;
    }

    public void setFormat(int format) {
        _format = format;
    }

    public long getModTime() {
        return _modTime;
    }

    public void setModTime(long modTime) {
        _modTime = modTime;
    }

    public static boolean isEncrypted(File file) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && file.getName().endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
    }
    
    public boolean isEncrypted() {
        return isEncrypted(getFile());
    }

    private static File getValidFile(Context context, Bundle arguments) {
        File file = (File) arguments.getSerializable(EXTRA_PATH);

        // Don't modify a valid file
        if (file != null && file.isFile() && file.exists()) {
            return file;
        }

        // Default to notebook if null
        file = (file == null) ? new AppSettings(context).getNotebookDirectory() : file;

        // Get a file in the directory if directory is specified
        if (arguments.getBoolean(EXTRA_PATH_IS_FOLDER, false) || file.isDirectory()) {
            file.mkdirs(); // Make folder exist
            final String content = arguments.getString(Intent.EXTRA_TEXT);
            File temp;
            do {
                // filenameFromContent will fallback to uuid
                temp = new File(file, String.format(filenameFromContent(content) + MarkdownTextConverter.EXT_MARKDOWN__MD));
            } while (temp.exists());
            file = temp;
        }

        return file;
    }

    public static Document fromArguments(Context context, Bundle arguments) {

        // When called directly with a document
        if (arguments.containsKey(EXTRA_DOCUMENT)) {
            return (Document) arguments.getSerializable(EXTRA_DOCUMENT);
        }

        Document document = new Document(getValidFile(context, arguments));

        if (arguments.containsKey(EXTRA_FILE_LINE_NUMBER)) {
            final int lineNumber = arguments.getInt(EXTRA_FILE_LINE_NUMBER);
            document.setInitialLineNumber(lineNumber);
        }

        return document;
    }

    public synchronized String loadContent(final Context context) {

        String content;
        final char[] pw;
        if (isEncrypted() && (pw = getPasswordWithWarning(context)) != null) {
            try {
                final byte[] encryptedContext = FileUtils.readCloseStreamWithSize(new FileInputStream(getFile()), (int) getFile().length());
                if (encryptedContext.length > JavaPasswordbasedCryption.Version.NAME_LENGTH) {
                    content = JavaPasswordbasedCryption.getDecryptedText(encryptedContext, pw);
                } else {
                    content = new String(encryptedContext, StandardCharsets.UTF_8);
                }
            } catch (FileNotFoundException e) {
                Log.e(Document.class.getName(), "loadDocument:  File " + getFile() + " not found.");
                content = "";
            } catch (JavaPasswordbasedCryption.EncryptionFailedException | IllegalArgumentException e) {
                Toast.makeText(context, R.string.could_not_decrypt_file_content_wrong_password_or_is_the_file_maybe_not_encrypted, Toast.LENGTH_LONG).show();
                Log.e(Document.class.getName(), "loadDocument:  decrypt failed for File " + getFile() + ". " + e.getMessage(), e);
                content = "";
            }
        } else {
            content = FileUtils.readTextFileFast(getFile());
        }

        if (MainActivity.IS_DEBUG_ENABLED) {
            AppSettings.appendDebugLog(
                    "\n\n\n--------------\nLoaded document, filepattern "
                            + getName().replaceAll(".*\\.", "-")
                            + ", chars: " + content.length() + " bytes:" + content.getBytes().length
                            + "(" + FileUtils.getReadableFileSize(content.getBytes().length, true) +
                            "). Language >" + Locale.getDefault().toString()
                            + "<, Language override >" + AppSettings.get().getLanguage() + "<");
        }

        _hash = content.hashCode();
        _modTime = _file.lastModified();

        return content;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static char[] getPasswordWithWarning(final Context context) {
        final char[] pw = new AppSettings(context).getDefaultPassword();
        if (pw == null || pw.length == 0) {
            final String warningText = context.getString(R.string.no_password_set_cannot_encrypt_decrypt);
            Toast.makeText(context, warningText, Toast.LENGTH_LONG).show();
            Log.w(Document.class.getName(), warningText);
            return null;
        }
        return pw;
    }

    public boolean saveContent(final Context context, final String content) {
        return saveContent(context, content, null, false);
    }

    public synchronized boolean saveContent(final Context context, final String content, ShareUtil shareUtil, final boolean ignoreEmpty) {
        if (content == null || (!ignoreEmpty && content.trim().isEmpty() && content.length() < ShareUtil.MIN_OVERWRITE_LENGTH)) {
            return false;
        }

        if (shareUtil == null) shareUtil = new ShareUtil(context);

        // Create parent (=folder of file) if not exists
        if (!_file.getParentFile().exists()) {
            _file.getParentFile().mkdirs();
        }

        boolean ret;
        try {
            final char[] pw;
            final byte[] contentAsBytes;
            if (isEncrypted() && (pw = getPasswordWithWarning(context)) != null) {
                contentAsBytes = new JavaPasswordbasedCryption(Build.VERSION.SDK_INT, new SecureRandom()).encrypt(content, pw);
            } else {
                contentAsBytes = content.getBytes();
            }

            if (shareUtil.isUnderStorageAccessFolder(_file)) {
                shareUtil.writeFile(_file, false, (fileOpened, fos) -> {
                    try {
                        fos.write(contentAsBytes);
                        fos.flush();
                    } catch (Exception ignored) {
                    }
                });
                ret = true;
            } else {
                ret = FileUtils.writeFile(getFile(), contentAsBytes);
            }
        } catch (JavaPasswordbasedCryption.EncryptionFailedException e) {
            Log.e(Document.class.getName(), "writeContent:  encrypt failed for File " + getPath() + ". " + e.getMessage(), e);
            Toast.makeText(context, R.string.could_not_encrypt_file_content_the_file_was_not_saved, Toast.LENGTH_LONG).show();
            ret = false;
        }

        return ret;
    }

    public static String getMaskedContent(final String text) {
        final String httpToken = "§$§$§$§$";
        return text
                .replace("http://", httpToken)
                .replace("https://", httpToken)
                .replaceAll("\\w", "a")
                .replace(httpToken, "https://");
    }

    public static String normalizeFilename(final String name) {
        if (TextUtils.isEmpty(name.trim())) {
            return "Note " + UUID.randomUUID().toString();
        } else {
            return name.replaceAll("[\\\\/:\"´`'*$?<>\n\r@|#]+", "").trim();
        }
    }

    public static String filenameFromContent(final String content) {
        if (!TextUtils.isEmpty(content)) {
            final String contentL1 = content.split("\n")[0];
            if (contentL1.length() < MAX_TITLE_EXTRACTION_LENGTH) {
                return contentL1;
            } else {
                return contentL1.substring(0, MAX_TITLE_EXTRACTION_LENGTH);
            }
        } else {
            return "Note " + UUID.randomUUID().toString();
        }
    }
}
