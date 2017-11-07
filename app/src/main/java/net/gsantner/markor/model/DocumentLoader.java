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
package net.gsantner.markor.model;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

public class DocumentLoader {
    public static final String EXTRA_DOCUMENT = "EXTRA_DOCUMENT"; // Document
    public static final String EXTRA_PATH = "EXTRA_PATH"; // java.io.File
    public static final String EXTRA_PATH_IS_FOLDER = "EXTRA_PATH_IS_FOLDER"; // boolean
    public static final String EXTRA_ALLOW_RENAME = "EXTRA_ALLOW_RENAME";


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
    public static Document loadDocument(Context context, Bundle arguments, @Nullable Document existingDocument) {
        if (existingDocument != null) {
            return existingDocument;
        }

        // When called directly from a filepath
        if (arguments.containsKey(EXTRA_DOCUMENT)) {
            return (Document) arguments.getSerializable(EXTRA_DOCUMENT);
        }

        Document document = new Document();
        document.setDoHistory(false);
        document.setFileExtension(Constants.MD_EXT1_MD);
        File extraPath = (File) arguments.getSerializable(EXTRA_PATH);
        File filePath = extraPath;

        // Generate random not existing filepath if filename not specified
        boolean extraPathIsFolder = arguments.getBoolean(EXTRA_PATH_IS_FOLDER);
        if (extraPathIsFolder) {
            extraPath.mkdirs();
            while (filePath.exists()) {
                filePath = new File(extraPath, String.format("%s-%s.%s", context.getString(R.string.document_one), UUID.randomUUID().toString(), Constants.MD_EXT1_MD));
            }
        } else if (filePath.isFile() && filePath.canRead()) {
            // Extract existing extension
            for (String ext : Constants.EXTENSIONS) {
                if (filePath.getName().toLowerCase(Locale.getDefault()).endsWith(ext)) {
                    document.setFileExtension(ext);
                    break;
                }
            }

            // Extract content and title
            document.setTitle(Constants.MD_EXTENSION.matcher(filePath.getName()).replaceAll(""));
            document.setContent(FileUtils.readTextFile(filePath));
        }

        document.setFile(filePath);
        document.setDoHistory(true);
        return document;
    }

    public static String normalizeTitleForFilename(Document _document) {
        String name = _document.getTitle();
        if (name.length() == 0) {
            if (_document.getContent().length() == 0) {
                return null;
            } else {
                String contentL1 = _document.getContent().split("\n")[0];
                if (contentL1.length() < Constants.MAX_TITLE_EXTRACTION_LENGTH) {
                    name = contentL1.substring(0, contentL1.length());
                } else {
                    name = contentL1.substring(0, Constants.MAX_TITLE_EXTRACTION_LENGTH);
                }
            }
        }
        name = name.replaceAll("[\\\\/:\"*?<>|]+", "").trim();

        if (name.isEmpty()) {
            name = "Note " + UUID.randomUUID().toString();
        }
        return name;
    }
}
