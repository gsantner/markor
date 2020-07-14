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

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;
import other.de.stanetz.jpencconverter.PasswordStore;

public class DocumentIO {
    public static final String EXTRA_DOCUMENT = "EXTRA_DOCUMENT"; // Document
    public static final String EXTRA_PATH = "EXTRA_PATH"; // java.io.File
    public static final String EXTRA_PATH_IS_FOLDER = "EXTRA_PATH_IS_FOLDER"; // boolean

    public static final int MAX_TITLE_EXTRACTION_LENGTH = 25;
    public static boolean SAVE_IGNORE_EMTPY_NEXT_TIME = false;

    public static Document loadDocument(Context context, Intent arguments, @Nullable Document existingDocument) {
        if (existingDocument != null) {
            return existingDocument;
        }

        Bundle bundle = new Bundle();
        if (arguments.hasExtra(EXTRA_DOCUMENT)) {
            bundle.putSerializable(EXTRA_DOCUMENT, arguments.getSerializableExtra(EXTRA_DOCUMENT));
        } else {
            bundle.putSerializable(EXTRA_PATH, arguments.getSerializableExtra(EXTRA_PATH));
            bundle.putBoolean(EXTRA_PATH_IS_FOLDER, arguments.getBooleanExtra(EXTRA_PATH_IS_FOLDER, false));
        }
        return loadDocument(context, bundle, existingDocument);
    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    public static synchronized Document loadDocument(Context context, Bundle arguments, @Nullable Document existingDocument) {
        if (existingDocument != null) {
            return existingDocument;
        }

        // When called directly from a filepath
        if (arguments.containsKey(EXTRA_DOCUMENT)) {
            return (Document) arguments.getSerializable(EXTRA_DOCUMENT);
        }

        Document document = new Document();
        document.setDoHistory(false);
        File extraPath = (File) arguments.getSerializable(EXTRA_PATH);
        File filePath = extraPath;

        // Generate random not existing filepath if filename not specified
        boolean extraPathIsFolder = arguments.getBoolean(EXTRA_PATH_IS_FOLDER);
        if (extraPathIsFolder) {
            extraPath.mkdirs();
            while (filePath.exists()) {
                filePath = new File(extraPath, String.format("%s-%s%s", context.getString(R.string.document), UUID.randomUUID().toString(), MarkdownTextConverter.EXT_MARKDOWN__MD));
            }
        } else if (filePath.isFile() && filePath.canRead()) {
            // Extract content and title
            document.setTitle(filePath.getName());
            String content;
            if (isEncryptedFile(filePath) && getPassword(context) != null) {
                try {
                    final byte[] encyptedContext = FileUtils.readCloseStreamWithSize(new FileInputStream(filePath), (int) filePath.length());
                    if (encyptedContext.length > JavaPasswordbasedCryption.Version.NAME_LENGTH) {
                        content = JavaPasswordbasedCryption.getDecyptedText(encyptedContext, getPassword(context));
                    } else {
                        content = new String(encyptedContext, StandardCharsets.UTF_8);
                    }
                } catch (FileNotFoundException e) {
                    Log.e(DocumentIO.class.getName(), "loadDocument:  File " + filePath + " not found.");
                    content = "";
                } catch (JavaPasswordbasedCryption.EncryptionFailedException | IllegalArgumentException e) {
                    Toast.makeText(context, R.string.could_not_decrypt_file_content_wrong_password_or_is_the_file_maybe_not_encrypted, Toast.LENGTH_LONG).show();
                    Log.e(DocumentIO.class.getName(), "loadDocument:  decrypt failed for File " + filePath + ". " + e.getMessage(), e);
                    content = "";
                }
            } else {
                content = FileUtils.readTextFileFast(filePath);
            }
            document.setContent(content);
            document.setModTime(filePath.lastModified());
        }

        document.setFile(filePath);

        if (document.getFormat() == TextFormat.FORMAT_UNKNOWN) {
            String fnlower = document.getFile().getName().toLowerCase();
            document.setFormat(TextFormat.FORMAT_PLAIN);

            if (TextFormat.CONVERTER_TODOTXT.isFileOutOfThisFormat(fnlower)) {
                document.setFormat(TextFormat.FORMAT_TODOTXT);
                if (!TextUtils.isEmpty(document.getContent())) {
                    document.setContent(document.getContent().trim());
                }
            } else if (TextFormat.CONVERTER_KEYVALUE.isFileOutOfThisFormat(fnlower)) {
                document.setFormat(TextFormat.FORMAT_KEYVALUE);
            } else if (TextFormat.CONVERTER_MARKDOWN.isFileOutOfThisFormat(fnlower)) {
                document.setFormat(TextFormat.FORMAT_MARKDOWN);
            } else if (fnlower.endsWith(".txt") || fnlower.endsWith(".zim")) {
                document.setFormat(TextFormat.FORMAT_PLAIN);
            } else {
                document.setFormat(TextFormat.FORMAT_PLAIN);
            }
        }

        String title;
        if ((title = document.getTitle()).contains(".")) {
            int lastIndexOfDot = title.lastIndexOf(".");
            document.setTitle(title.substring(0, lastIndexOfDot));
        }

        document.setDoHistory(true);
        if (MainActivity.IS_DEBUG_ENABLED) {
            String c = document.getContent();
            AppSettings.appendDebugLog("\n\n\n--------------\nLoaded document, filepattern " + document.getFile().getName().replaceAll(".*\\.", "-") + ", chars: " + c.length() + " bytes:" + c.getBytes().length + "(" + FileUtils.getReadableFileSize(c.getBytes().length, true) + "). Language >" + Locale.getDefault().toString() + "<, Language override >" + AppSettings.get().getLanguage() + "<");
        }
        return document;
    }

    public static synchronized boolean saveDocument(final Document document, final String text, final ShareUtil shareUtil, Context context) {
        if (text == null || (!SAVE_IGNORE_EMTPY_NEXT_TIME && text.trim().isEmpty() && text.length() < ShareUtil.MIN_OVERWRITE_LENGTH)) {
            return false;
        }
        boolean ret;
        String filename = DocumentIO.normalizeTitleForFilename(document, text) + document.getFileExtension();
        document.setDoHistory(true);
        document.setFile(new File(document.getFile().getParentFile(), filename));

        Document documentInitial = document.getInitialVersion();

        document.setFile(documentInitial.getFile());

        if (!text.equals(documentInitial.getContent())) {
            ret = writeContent(document, text, shareUtil, context);
        } else {
            ret = true;
        }
        return ret;
    }

    private static boolean writeContent(Document document, String text, ShareUtil shareUtil, Context context) {
        boolean ret;
        document.forceAddNextChangeToHistory();
        document.setContent(text + (!TextUtils.isEmpty(text) && !text.endsWith("\n") ? "\n" : ""));

        // Create parent (=folder of file) if not exists
        if (!document.getFile().getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            document.getFile().getParentFile().mkdirs();
        }
        try {
            final byte[] contentAsBytes;
            if (isEncryptedFile(document.getFile()) && getPassword(context) != null) {
                contentAsBytes = new JavaPasswordbasedCryption(JavaPasswordbasedCryption.Version.V001, new SecureRandom()).encrypt(document.getContent(), getPassword(context));
            } else {
                contentAsBytes = document.getContent().getBytes();
            }

            if (shareUtil.isUnderStorageAccessFolder(document.getFile())) {
                shareUtil.writeFile(document.getFile(), false, (fileOpened, fos) -> {
                    try {
                        fos.write(contentAsBytes);
                    } catch (Exception ignored) {
                    }
                });
                ret = true;
            } else {
                ret = FileUtils.writeFile(document.getFile(), contentAsBytes);
            }
        } catch (JavaPasswordbasedCryption.EncryptionFailedException e) {
            Log.e(DocumentIO.class.getName(), "loadDocument:  enrypt failed for File " +
                    document.getFile().getAbsolutePath() + ". " + e.getMessage(), e);
            Toast.makeText(context, R.string.could_not_encrypt_file_content_the_file_was_not_saved, Toast.LENGTH_LONG).show();
            ret = false;
        }
        return ret;
    }

    public static String getMaskedContent(Document document) {
        String text = document.getContent().toLowerCase();
        String httpToken = "§$§$§$§$";
        text = text.replace("http://", httpToken).replace("https://", httpToken);
        text = text.replaceAll("\\w", "a");
        text = text.replace(httpToken, "https://");
        return text;
    }

    public static String normalizeTitleForFilename(Document _document, String currentContent) {
        String name = _document.getTitle();
        try {
            if (name.length() == 0) {
                if (currentContent.length() == 0) {
                    return null;
                } else {
                    String contentL1 = currentContent.split("\n")[0];
                    if (contentL1.length() < MAX_TITLE_EXTRACTION_LENGTH) {
                        name = contentL1;
                    } else {
                        name = contentL1.substring(0, MAX_TITLE_EXTRACTION_LENGTH);
                    }
                }
            }
            name = name.replaceAll("[\\\\/:\"´`'*$?<>\n\r@|#]+", "").trim();
        } catch (Exception ignored) {
        }
        if (name == null || name.isEmpty()) {
            name = "Note " + UUID.randomUUID().toString();
        }
        return name;
    }

    public static final InputFilter INPUT_FILTER_FILESYSTEM_FILENAME = new InputFilter() {
        private final String blockCharacterSet = "\\/:\"´`'*?<>\n\r@|";

        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            for (int i = 0; !TextUtils.isEmpty(source) && i < source.length(); i++) {
                if (blockCharacterSet.contains(("" + source.charAt(i)))) {
                    return "";
                }
            }
            return null;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static char[] getPassword(Context context) {
        final PasswordStore securityStore = new PasswordStore(context);
        final char[] pw = securityStore.loadKey(R.string.pref_key__default_encryption_password);
        if (pw == null || pw.length == 0) {
            final String warningText = context.getString(R.string.no_password_set_cannot_encrypt_decrypt);
            Toast.makeText(context, warningText, Toast.LENGTH_LONG).show();
            Log.w(DocumentIO.class.getName(), warningText);
            return null;
        }
        return pw;
    }

    private static boolean isEncryptedFile(File file) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && file.getName().endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
    }

}
