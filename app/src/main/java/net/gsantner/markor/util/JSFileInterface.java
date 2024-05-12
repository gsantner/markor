package net.gsantner.markor.util;

import android.webkit.JavascriptInterface;

import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JSFileInterface {

    private final File root;
    private boolean _enabled = true;

    public JSFileInterface(final File root) {
        this.root = root;
    }

    public JSFileInterface setEnabled(boolean enabled) {
        _enabled = enabled;
        return this;
    }

    private File toFile(final String... path) {
        File file = root;
        for (final String p : path) {
            if (p != null && !p.trim().isEmpty()) {
                file = new File(file, p);
            }
        }
        return file;
    }

    // Method to get the list of files in a directory
    @JavascriptInterface
    public List<String> listFiles(final String path) {
        if (!_enabled) {
            return Collections.emptyList();
        }

        final File directory = toFile(path);
        final List<String> fileList = new ArrayList<>();
        final File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // Add file name to the list
                fileList.add(file.getName());
            }
        }
        return fileList;
    }

    // Method to get the parent directory
    @JavascriptInterface
    public String getParent(final String path) {
        if (!_enabled) {
            return null;
        }

        return toFile(path).getParent();
    }

    // Method to get the path of a child file or directory
    @JavascriptInterface
    public String getChild(final String path, final String child) {
        if (!_enabled) {
            return null;
        }

        return toFile(path, child).getPath();
    }

    @JavascriptInterface
    public boolean isDirectory(final String path) {
        if (!_enabled) {
            return false;
        }

        return path != null && toFile(path).isDirectory();
    }

    @JavascriptInterface
    public boolean isFile(final String path) {
        if (!_enabled) {
            return false;
        }

        return path != null && toFile(path).isFile();
    }

    @JavascriptInterface
    public boolean exists(final String path) {
        if (!_enabled) {
            return false;
        }

        return path != null && toFile(path).exists();
    }


    private boolean canRead(final File f) {
        return f.canRead() && FormatRegistry.isFileSupported(f, true);
    }

    @JavascriptInterface
    public boolean canRead(final String path) {
        if (!_enabled) {
            return false;
        }

        return path != null && canRead(toFile(path));
    }

    @JavascriptInterface
    public String read(final String path) {
        if (!_enabled) {
            return "";
        }

        final File f = toFile(path);
        if (canRead(f)) {
            return GsFileUtils.readTextFileFast(toFile(path)).first;
        }
        else return "";
    }

    private boolean canWrite(final File f) {
        return f.canWrite() && FormatRegistry.isFileSupported(f, false);
    }

    @JavascriptInterface
    public boolean canWrite(final String path) {
        if (!_enabled) {
            return false;
        }

        return path != null && canWrite(toFile(path));
    }

    @JavascriptInterface
    public boolean write(final String path, final String content) {
        if (!_enabled) {
            return false;
        }

        final File f = toFile(path);
        if (canWrite(f)) {
            return GsFileUtils.writeFile(f, content, null);
        }
        return false;
    }
}
