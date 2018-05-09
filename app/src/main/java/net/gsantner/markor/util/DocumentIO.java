/*#######################################################
 *
 *   Maintained by Gregor Santner, 2016-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.converter.MarkdownTextConverter;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

public class DocumentIO {
    public static final String EXTRA_DOCUMENT = "EXTRA_DOCUMENT"; // Document
    public static final String EXTRA_PATH = "EXTRA_PATH"; // java.io.File
    public static final String EXTRA_PATH_IS_FOLDER = "EXTRA_PATH_IS_FOLDER"; // boolean
    public static final String EXTRA_ALLOW_RENAME = "EXTRA_ALLOW_RENAME";

    public static final int MAX_TITLE_EXTRACTION_LENGTH = 25;


    public static Document loadDocument(Context context, Intent arguments, @Nullable Document existingDocument) {
        if (existingDocument != null) {
            return existingDocument;
        }

        Bundle bundle = new Bundle();
        if (arguments.hasExtra(EXTRA_DOCUMENT)) {
            bundle.putSerializable(EXTRA_DOCUMENT, arguments.getSerializableExtra(EXTRA_DOCUMENT));
        } else {
            bundle.putBoolean(EXTRA_ALLOW_RENAME, arguments.getBooleanExtra(EXTRA_ALLOW_RENAME, true));
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
        document.setFileExtension(MarkdownTextConverter.EXT_MARKDOWN__MD);
        File extraPath = (File) arguments.getSerializable(EXTRA_PATH);
        File filePath = extraPath;

        // Generate random not existing filepath if filename not specified
        boolean extraPathIsFolder = arguments.getBoolean(EXTRA_PATH_IS_FOLDER);
        if (extraPathIsFolder) {
            extraPath.mkdirs();
            while (filePath.exists()) {
                filePath = new File(extraPath, String.format("%s-%s.%s", context.getString(R.string.document_one), UUID.randomUUID().toString(), MarkdownTextConverter.EXT_MARKDOWN__MD));
            }
        } else if (filePath.isFile() && filePath.canRead()) {
            // Extract existing extension
            for (String ext : MarkdownTextConverter.MD_EXTENSIONS) {
                if (filePath.getName().toLowerCase(Locale.getDefault()).endsWith(ext)) {
                    document.setFileExtension(ext);
                    break;
                }
            }

            // Extract content and title
            document.setTitle(MarkdownTextConverter.MD_EXTENSION_PATTERN.matcher(filePath.getName()).replaceAll(""));
            document.setContent(FileUtils.readTextFile(filePath));
        }

        document.setFile(filePath);

        if (document.getFormat() == TextFormat.FORMAT_UNKNOWN) {
            String fnlower = document.getFile().getName().toLowerCase();

            if (SttCommander.TODOTXT_FILE_PATTERN.matcher(fnlower).matches()) {
                document.setFormat(TextFormat.FORMAT_TODOTXT);
            } else if (fnlower.endsWith(".txt")) {
                document.setFormat(TextFormat.FORMAT_PLAIN);
            } else if (ContextUtils.get().isMaybeMarkdownFile(filePath)) {
                document.setFormat(TextFormat.FORMAT_MARKDOWN);
            } else {
                document.setFormat(TextFormat.FORMAT_PLAIN);
            }
        }

        document.setDoHistory(true);
        return document;
    }

    public static synchronized boolean saveDocument(Document document, boolean argAllowRename, String currentText) {
        boolean ret;
        String filename = DocumentIO.normalizeTitleForFilename(document) + document.getFileExtension();
        document.setDoHistory(true);
        document.setFile(new File(document.getFile().getParentFile(), filename));

        Document documentInitial = document.getInitialVersion();
        if (argAllowRename) {
            if (!document.getFile().equals(documentInitial.getFile())) {
                if (documentInitial.getFile().exists()) {
                    if (FileUtils.renameFile(documentInitial.getFile(), document.getFile())) {
                        // Rename succeeded -> Rename everything in history too
                        for (Document hist : document.getHistory()) {
                            hist.setFile(document.getFile());
                            for (Document hist2 : hist.getHistory()) {
                                hist2.setFile(document.getFile());
                            }
                        }
                    }
                }
            }
        } else {
            document.setFile(documentInitial.getFile());
        }

        if (!currentText.equals(documentInitial.getContent())) {
            document.forceAddNextChangeToHistory();
            document.setContent(currentText);
            ret = FileUtils.writeFile(document.getFile(), document.getContent());
        } else {
            ret = true;
        }
        return ret;
    }

    public static String normalizeTitleForFilename(Document _document) {
        String name = _document.getTitle();
        try {
            if (name.length() == 0) {
                if (_document.getContent().length() == 0) {
                    return null;
                } else {
                    String contentL1 = _document.getContent().split("\n")[0];
                    if (contentL1.length() < MAX_TITLE_EXTRACTION_LENGTH) {
                        name = contentL1.substring(0, contentL1.length());
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
}
