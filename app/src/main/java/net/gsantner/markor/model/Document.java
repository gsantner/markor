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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Document implements Serializable {
    private static final int MAX_TITLE_EXTRACTION_LENGTH = 25;

    public static final String EXTRA_DOCUMENT = "EXTRA_DOCUMENT"; // Document
    public static final String EXTRA_PATH = "EXTRA_PATH"; // java.io.File
    public static final String EXTRA_FILE_LINE_NUMBER = "EXTRA_FILE_LINE_NUMBER"; // int

    private final File _file;
    private final String _fileExtension;
    private String _title = "";
    private String _path = "";
    private long _modTime = 0;
    private boolean _withBom = false;
    @StringRes
    private int _format = TextFormat.FORMAT_UNKNOWN;

    // Used to check if string changed
    private long _lastHash = 0;
    private int _lastLength = -1;

    public Document(@NonNull final File file) {
        _file = file;
        _path = _file.getAbsolutePath();
        _title = FileUtils.getFilenameWithoutExtension(_file);
        _fileExtension = FileUtils.getFilenameExtension(_file);

        // Set initial format
        final String fnlower = _file.getName().toLowerCase();
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

    // Get a default file
    public static Document getDefault(final Context context) {
        final File notebook = new AppSettings(context).getNotebookDirectory();
        final File random = new File(notebook, getFileNameWithTimestamp(true));
        return new Document(random);
    }

    public String getPath() {
        return _path;
    }

    @NonNull
    public File getFile() {
        return _file;
    }

    public long lastModified() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Files.readAttributes(_file.toPath(), BasicFileAttributes.class).lastModifiedTime().toMillis();
            }
        } catch (IOException ignored) {
        }
        return _file.lastModified();
    }

    public String getTitle() {
        return _title;
    }

    public String getName() {
        return _file.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Document) {
            Document other = ((Document) obj);
            return equalsc(_file, other._file)
                    && equalsc(getTitle(), other.getTitle())
                    && (getFormat() == other.getFormat());
        }
        return super.equals(obj);
    }

    private static boolean equalsc(Object o1, Object o2) {
        return (o1 == null && o2 == null) || o1 != null && o1.equals(o2);
    }

    public String getFileExtension() {
        return _fileExtension;
    }

    @StringRes
    public int getFormat() {
        return _format;
    }

    public void setFormat(int format) {
        _format = format;
    }

    public static boolean isEncrypted(File file) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && file.getName().endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
    }

    public boolean isEncrypted() {
        return isEncrypted(_file);
    }

    private void setContentHash(final CharSequence s) {
        _lastLength = s.length();
        _lastHash = FileUtils.crc32(s);
    }

    public boolean isContentSame(final CharSequence s) {
        return s != null && s.length() == _lastLength && _lastHash == FileUtils.crc32(s);
    }

    public synchronized String loadContent(final Context context) {
        String content;
        final char[] pw;
        if (isEncrypted() && (pw = getPasswordWithWarning(context)) != null) {
            try {
                final byte[] encryptedContext = FileUtils.readCloseStreamWithSize(new FileInputStream(_file), (int) _file.length());
                if (encryptedContext.length > JavaPasswordbasedCryption.Version.NAME_LENGTH) {
                    content = JavaPasswordbasedCryption.getDecryptedText(encryptedContext, pw);
                } else {
                    content = new String(encryptedContext, StandardCharsets.UTF_8);
                }
            } catch (FileNotFoundException e) {
                Log.e(Document.class.getName(), "loadDocument:  File " + _file + " not found.");
                content = "";
            } catch (JavaPasswordbasedCryption.EncryptionFailedException | IllegalArgumentException e) {
                Toast.makeText(context, R.string.could_not_decrypt_file_content_wrong_password_or_is_the_file_maybe_not_encrypted, Toast.LENGTH_LONG).show();
                Log.e(Document.class.getName(), "loadDocument:  decrypt failed for File " + _file + ". " + e.getMessage(), e);
                content = "";
            }
        } else {
            final Pair<String, Map<String, Object>> result = FileUtils.readTextFileFast(_file);
            content = result.first;
            _withBom = (boolean) result.second.get(FileUtils.WITH_BOM);
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

        // Also set hash and time on load - should prevent unnecessary saves
        setContentHash(content);
        _modTime = lastModified();

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

    public boolean testCreateParent() {
        return testCreateParent(_file);
    }

    public static boolean testCreateParent(final File file) {
        try {
            final File parent = file.getParentFile();
            return parent != null && (parent.exists() || parent.mkdirs());
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean saveContent(final Context context, final String content) {
        return saveContent(context, content, null, false);
    }

    // Doing as we don't want to convert to string or copy unless necessary
    private static int trimmedLength(final CharSequence c) {
        return StringUtils.getLastNonWhitespace(c, c.length()) - StringUtils.getNextNonWhitespace(c, 0);
    }

    public synchronized boolean saveContent(final Context context, final CharSequence content, ShareUtil shareUtil, boolean isManualSave) {
        if (!isManualSave && trimmedLength(content) < ShareUtil.MIN_OVERWRITE_LENGTH) {
            return false;
        }

        if (!testCreateParent()) {
            return false;
        }

        // Don't write same content if base file not changed
        if (_modTime >= lastModified() && isContentSame(content)) {
            return true;
        }

        boolean success;
        try {
            final char[] pw;
            final byte[] contentAsBytes;
            if (isEncrypted() && (pw = getPasswordWithWarning(context)) != null) {
                contentAsBytes = new JavaPasswordbasedCryption(Build.VERSION.SDK_INT, new SecureRandom()).encrypt(content.toString(), pw);
            } else {
                contentAsBytes = content.toString().getBytes();
            }

            shareUtil = shareUtil != null ? shareUtil : new ShareUtil(context);

            if (shareUtil.isUnderStorageAccessFolder(_file)) {
                shareUtil.writeFile(_file, false, (fileOpened, fos) -> {
                    try {
                        if (_withBom) {
                            fos.write(0xEF);
                            fos.write(0xBB);
                            fos.write(0xBF);
                        }
                        fos.write(contentAsBytes);
                        fos.flush();
                    } catch (Exception ignored) {
                    }
                });
                success = true;
            } else {
                final Map<String, Object> options = new HashMap<>(1);
                options.put(FileUtils.WITH_BOM, _withBom);
                success = FileUtils.writeFile(_file, contentAsBytes, options);
            }
        } catch (JavaPasswordbasedCryption.EncryptionFailedException e) {
            Log.e(Document.class.getName(), "writeContent:  encrypt failed for File " + getPath() + ". " + e.getMessage(), e);
            Toast.makeText(context, R.string.could_not_encrypt_file_content_the_file_was_not_saved, Toast.LENGTH_LONG).show();
            success = false;
        }

        if (success) {
            setContentHash(content);
            final long curModTime = lastModified();
            if (_modTime >= curModTime) {
                Log.w("MARKOR_DOCUMENT", "File modification time unchanged after write");
            }
            _modTime = curModTime;
        }

        return success;
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
            return getFileNameWithTimestamp(false);
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
            return getFileNameWithTimestamp(false);
        }
    }

    // Convenient wrapper
    private static String getFileNameWithTimestamp(boolean includeExt) {
        final String ext = includeExt ? MarkdownTextConverter.EXT_MARKDOWN__TXT : "";
        return ShareUtil.getFilenameWithTimestamp("", "", ext);
    }
}
