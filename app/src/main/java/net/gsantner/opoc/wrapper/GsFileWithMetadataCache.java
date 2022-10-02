/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2022-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.wrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.net.URI;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class GsFileWithMetadataCache extends File {
    private Integer cHashCode;
    private Boolean cCanRead, cCanWrite, cExists, cIsAbsolute, cIsDirectory, cIsFile;
    private Long cLastModified, cLength;
    private String cAbsolutePath, cName;
    private File cAbsoluteFile;
    private String[] cList;

    public GsFileWithMetadataCache(@NonNull String pathname) {
        super(pathname);
    }

    public GsFileWithMetadataCache(@Nullable String parent, @NonNull String child) {
        super(parent, child);
    }

    public GsFileWithMetadataCache(@Nullable File parent, @NonNull String child) {
        super(parent, child);
    }

    public GsFileWithMetadataCache(@NonNull URI uri) {
        super(uri);
    }

    public GsFileWithMetadataCache(@NonNull File f) {
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
    public String getName() {
        if (cName == null) {
            cName = super.getName();
        }
        return cName;
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
