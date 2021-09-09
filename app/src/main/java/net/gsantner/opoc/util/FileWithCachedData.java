//
// License of this file FileWithCachedData.java: Public Domain
// Created by Gregor Santner, 2021 - https://gsantner.net
//

package net.gsantner.opoc.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.net.URI;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class FileWithCachedData extends File {
    private Integer cHashCode;
    private Boolean cCanRead, cCanWrite, cExists, cIsAbsolute, cIsDirectory, cIsFile;
    private Long cLastModified, cLength;
    private String cAbsolutePath;
    private File cAbsoluteFile;
    private String[] cList;

    public FileWithCachedData(@NonNull String pathname) {
        super(pathname);
    }

    public FileWithCachedData(@Nullable String parent, @NonNull String child) {
        super(parent, child);
    }

    public FileWithCachedData(@Nullable File parent, @NonNull String child) {
        super(parent, child);
    }

    public FileWithCachedData(@NonNull URI uri) {
        super(uri);
    }

    public FileWithCachedData(@NonNull File f) {
        super(f.getPath());
    }


    @NonNull
    @Override
    public synchronized String getAbsolutePath() {
        if (cAbsolutePath == null) {
            cAbsolutePath = super.getAbsolutePath();
        }
        return cAbsolutePath;
    }

    @NonNull
    @Override
    public File getAbsoluteFile() {
        if (cAbsoluteFile == null) {
            cAbsoluteFile = super.getAbsoluteFile();
        }
        return cAbsoluteFile;
    }

    @Override
    public boolean isAbsolute() {
        if (cIsAbsolute == null) {
            cIsAbsolute = super.isAbsolute();
        }
        return cIsAbsolute;
    }

    @Override
    public boolean canRead() {
        if (cCanRead == null) {
            cCanRead = super.canRead();
        }
        return cCanRead;
    }

    @Override
    public boolean canWrite() {
        if (cCanWrite == null) {
            cCanWrite = super.canWrite();
        }
        return cCanWrite;
    }

    @Override
    public boolean exists() {
        if (cExists == null) {
            cExists = super.exists();
        }
        return cExists;
    }

    @Override
    public boolean isDirectory() {
        if (cIsDirectory == null) {
            cIsDirectory = super.isDirectory();
        }
        return cIsDirectory;
    }

    @Override
    public boolean isFile() {
        if (cIsFile == null) {
            cIsFile = super.isFile();
        }
        return cIsFile;
    }

    @Override
    public long lastModified() {
        if (cLastModified == null) {
            cLastModified = super.lastModified();
        }
        return cLastModified;
    }

    @Override
    public long length() {
        if (cLength == null) {
            cLength = super.length();
        }
        return cLength;
    }

    @Nullable
    @Override
    public String[] list() {
        if (cList == null) {
            cList = super.list();
        }
        return cList;
    }

    @Override
    public int hashCode() {
        if (cHashCode == null) {
            cHashCode = super.hashCode();
        }
        return cHashCode;
    }
}
