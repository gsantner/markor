/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import net.gsantner.opoc.format.GsTextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection"})
public class GsFileUtils {
    @SuppressLint("ConstantLocale")
    public final static Locale INITIAL_LOCALE = Locale.getDefault();
    public final static SimpleDateFormat DATEFORMAT_IMG = new SimpleDateFormat("yyyyMMdd-HHmmss", INITIAL_LOCALE); //20190511-230845

    // Used on methods like copyFile(src, dst)
    private final static int BUFFER_SIZE = 4096;
    private final static Map<String, String> MIME_TYPE_CACHE = new ConcurrentHashMap<>();

    /**
     * Info of various types about a file
     */
    public static class FileInfo implements Serializable {
        public boolean hasBom = false;
        public boolean ioError = false;
    }

    public static Pair<String, FileInfo> readInputStreamFast(final InputStream inputStream, @Nullable FileInfo info) {
        info = info == null ? new FileInfo() : info;

        try {
            final ByteArrayOutputStream result = new ByteArrayOutputStream();

            final byte[] bomBuffer = new byte[3];
            final int bomReadLength = inputStream.read(bomBuffer);

            info.hasBom = bomReadLength == 3 &&
                    bomBuffer[0] == (byte) 0xEF &&
                    bomBuffer[1] == (byte) 0xBB &&
                    bomBuffer[2] == (byte) 0xBF;

            if (!info.hasBom && bomReadLength > 0) {
                result.write(bomBuffer, 0, bomReadLength);
            }
            if (bomReadLength < 3) {
                return new Pair<>(result.toString("UTF-8"), info);
            }

            final byte[] buffer = new byte[1024];
            for (int length; (length = inputStream.read(buffer)) != -1; ) {
                result.write(buffer, 0, length);
            }
            return new Pair<>(result.toString("UTF-8"), info);
        } catch (IOException e) {
            e.printStackTrace();
            info.ioError = true;
        }

        return new Pair<>("", info);
    }

    public static Pair<String, FileInfo> readTextFileFast(final File file) {
        final FileInfo info = new FileInfo();

        try (final FileInputStream inputStream = new FileInputStream(file)) {
            return readInputStreamFast(inputStream, info);
        } catch (FileNotFoundException e) {
            System.err.println("readTextFileFast: File " + file + " not found.");
        } catch (IOException e) {
            System.err.println("readTextFileFast: File " + file + " could not be read.");
            info.ioError = true;
        }

        return new Pair<>("", info);
    }

    public static byte[] readCloseStreamWithSize(final InputStream stream, int size) {
        byte[] data = new byte[size];
        try (DataInputStream dis = new DataInputStream(stream)) {
            dis.readFully(data);
        } catch (IOException ignored) {
        }
        return data;
    }

    public static String readTextFile(final File file) {
        try {
            return readCloseTextStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            System.err.println("readTextFile: File " + file + " not found.");
        }

        return "";
    }

    public static String readCloseTextStream(final InputStream stream) {
        return readCloseTextStream(stream, true).get(0);
    }

    public static List<String> readCloseTextStream(final InputStream stream, boolean concatToOneString) {
        final ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        String line = "";
        try {
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(stream));

            while ((line = reader.readLine()) != null) {
                if (concatToOneString) {
                    sb.append(line).append('\n');
                } else {
                    lines.add(line);
                }
            }
            line = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (concatToOneString) {
            lines.clear();
            lines.add(line);
        }
        return lines;
    }

    public static byte[] readBinaryFile(final File file) {
        try {
            return readCloseBinaryStream(new FileInputStream(file), (int) file.length());
        } catch (FileNotFoundException e) {
            System.err.println("readBinaryFile: File " + file + " not found.");
        }

        return new byte[0];
    }

    public static byte[] readCloseBinaryStream(final InputStream stream, int byteCount) {
        final ArrayList<String> lines = new ArrayList<>();
        BufferedInputStream reader = null;
        byte[] buf = new byte[byteCount];
        int totalBytesRead = 0;
        try {
            reader = new BufferedInputStream(stream);
            while (totalBytesRead < byteCount) {
                int bytesRead = reader.read(buf, totalBytesRead, byteCount - totalBytesRead);
                if (bytesRead > 0) {
                    totalBytesRead = totalBytesRead + bytesRead;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return buf;
    }

    // Read binary stream (of unknown conf size)
    public static byte[] readCloseBinaryStream(final InputStream stream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return baos.toByteArray();
    }

    public static boolean writeFile(final File file, final byte[] data, final FileInfo options) {
        try (final FileOutputStream output = new FileOutputStream(file, false)) {
            if (options != null && options.hasBom) {
                output.write(0xEF);
                output.write(0xBB);
                output.write(0xBF);
            }
            output.write(data);
            output.flush();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean writeFile(final File file, final String data, final FileInfo options) {
        return writeFile(file, data.getBytes(), options);
    }

    public static boolean copyFile(final File src, final File dst) {
        // Just touch file if src is empty
        if (src.length() == 0) {
            return touch(dst);
        }

        InputStream is = null;
        FileOutputStream os = null;
        try {
            try {
                is = new FileInputStream(src);
                os = new FileOutputStream(dst);
                byte[] buf = new byte[BUFFER_SIZE];
                int len;
                while ((len = is.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
                return true;
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.flush();
                    os.close();
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean copyFile(final File src, final OutputStream os) {
        InputStream is = null;
        try {
            try {
                is = new FileInputStream(src);
                byte[] buf = new byte[BUFFER_SIZE];
                int len;
                while ((len = is.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
                return true;
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    // Returns -1 if the file did not contain any of the needles, otherwise,
    // the index of which needle was found in the contents of the file.
    //
    // Needless MUST be in lower-case.
    public static int fileContains(File file, String... needles) {
        try {
            FileInputStream in = new FileInputStream(file);

            int i;
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                for (i = 0; i != needles.length; ++i)
                    if (line.toLowerCase(Locale.ROOT).contains(needles[i])) {
                        return i;
                    }
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static boolean deleteRecursive(final File file) {
        boolean ok = true;
        if (file.exists()) {
            if (file.isDirectory()) {
                for (final File child : file.listFiles())
                    ok &= deleteRecursive(child);
            }
            ok &= file.delete();
        }
        return ok;
    }

    // Example: Check if this is maybe a conf: (str, "jpg", "png", "jpeg")
    public static boolean hasExtension(String str, String... extensions) {
        String lc = str.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lc.endsWith("." + extension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean renameFile(File srcFile, File destFile) {
        if (srcFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
            return false;
        }

        // renameTo will fail in case of case-changed filename in same dir.Even on case-sensitive FS!!!
        if (srcFile.getParent().equals(destFile.getParent()) && srcFile.getName().toLowerCase(Locale.getDefault()).equals(destFile.getName().toLowerCase(Locale.getDefault()))) {
            File tmpFile = new File(destFile.getParent(), UUID.randomUUID().getLeastSignificantBits() + ".tmp");
            if (!tmpFile.exists()) {
                renameFile(srcFile, tmpFile);
                srcFile = tmpFile;
            }
        }

        if (!srcFile.renameTo(destFile)) {
            if (copyFile(srcFile, destFile) && !srcFile.delete()) {
                if (!destFile.delete()) {
                    return false;
                }
                return false;
            }
        }
        return true;
    }

    public static boolean renameFileInSameFolder(File srcFile, String destFilename) {
        return renameFile(srcFile, new File(srcFile.getParent(), destFilename));
    }

    public static boolean touch(File file) {
        try {
            if (!file.exists()) {
                new FileOutputStream(file).close();
            }
            return file.setLastModified(System.currentTimeMillis());
        } catch (IOException e) {
            return false;
        }
    }

    // Get relative path to specified destination
    public static String relativePath(File src, File dest) {
        try {
            String[] srcSplit = (src.isDirectory() ? src : src.getParentFile()).getCanonicalPath().split(Pattern.quote(File.separator));
            String[] destSplit = dest.getCanonicalPath().split(Pattern.quote(File.separator));
            StringBuilder sb = new StringBuilder();
            int i = 0;

            for (; i < destSplit.length && i < srcSplit.length; ++i) {
                if (!destSplit[i].equals(srcSplit[i]))
                    break;
            }
            if (i != srcSplit.length) {
                for (int iUpperDir = i; iUpperDir < srcSplit.length; ++iUpperDir) {
                    sb.append("..");
                    sb.append(File.separator);
                }
            }
            for (; i < destSplit.length; ++i) {
                sb.append(destSplit[i]);
                sb.append(File.separator);
            }
            if (!dest.getPath().endsWith("/") && !dest.getPath().endsWith("\\")) {
                sb.delete(sb.length() - File.separator.length(), sb.length());
            }
            return sb.toString();
        } catch (IOException | NullPointerException exception) {
            return null;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static String gatherMimeType(final File file) {
        if (file == null) {
            return "*/*";
        }

        final String ext = getFilenameExtension(file).replace(".", "");
        if (file.isDirectory()) {
            return "inode/directory";
        } else if (ext.matches("ya?ml")) {
            return "text/yaml";
        } else if (ext.matches("json.*")) {
            return "text/json";
        } else if (ext.matches("((md)|(markdown)|(mkd)|(mdown)|(mkdn)|(mdwn)|(mdx)|(rmd))")) {
            return "text/markdown";
        } else if (ext.matches("(te?xt)|(taskpaper)")) {
            return "text/plain";
        } else if (ext.matches("webp")) {
            return "image/webp";
        } else if (ext.matches("jpe?g")) {
            return "image/jpeg";
        } else if (ext.matches("png")) {
            return "image/png";
        } else if (ext.matches("a(sciidoc)?doc")) {
            return "text/asciidoc";
        }

        String t;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !GsTextUtils.isNullOrEmpty(t = Files.probeContentType(file.toPath()))) {
                return t;
            }
        } catch (Exception ignored) {
        }

        try {
            if (!GsTextUtils.isNullOrEmpty(t = URLConnection.guessContentTypeFromName(file.getName().replace(".jenc", "")))) {
                return t;
            }
        } catch (Exception ignored) {
        }

        // Try extracting by running shell file -b on the file - for textfiles this very often results in "ASCII text"
        try {
            final String cmd = "file -b " + file.getAbsolutePath().replace(" ", "\\ ").replace(";", "").replace("&", "").replace("$", "");
            final Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            t = reader.readLine().trim().toLowerCase(Locale.ROOT);
            try {
                p.destroy();
                reader.close();
            } catch (Exception ignored) {
            }
            if (GsTextUtils.isNullOrEmpty(t) || t.contains("not found")) {
            } else if (t.contains("ascii text") || t.contains("empty")) {
                return "text/plain";
            } else if (t.contains("text") || t.contains("script")) {
                return "text/x-" + t.replaceAll("(\\s|-)*script(\\s|-)*", "").replace(" ", "-");
            }
        } catch (Exception ignored) {
        }
        return "*/*";
    }

    /**
     * Try to detect MimeType by backwards compatible methods
     * Android/Java's own MimeType mapping support is small and detection barely works at all
     * Hence use custom map for some file extensions
     */
    public static String getMimeType(final File file) {
        final String fp = file.getAbsolutePath();
        String mime = MIME_TYPE_CACHE.get(fp);
        if (mime == null) {
            mime = gatherMimeType(file);
            MIME_TYPE_CACHE.put(fp, mime);
        }
        return mime;
    }

    public static boolean isTextFile(final File file) {
        final String mime = getMimeType(file);
        return mime != null && (mime.startsWith("text/") || mime.contains("xml")) && !mime.contains("openxml");
    }

    /**
     * Reads the first kb of a file and checks if it is likely a text file
     **/
    public static boolean isContentsPlainText(final File file) {
        // Empty files are considered text files
        if (file.length() == 0) {
            return true;
        }

        try (final FileInputStream fis = new FileInputStream(file)) {
            final byte[] bytes = readCloseStreamWithSize(fis, 1024);
            Charset.forName("UTF-8").newDecoder().decode(java.nio.ByteBuffer.wrap(bytes));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Analyze given textfile and retrieve multiple information from it
     * Information is written back to the {@link AtomicInteger} parameters
     */
    public static void retrieveTextFileSummary(File file, AtomicInteger numCharacters, AtomicInteger numLines, AtomicInteger numWords) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                numLines.getAndIncrement();
                numCharacters.getAndSet(numCharacters.get() + line.length());
                if (!line.equals("")) {
                    numWords.getAndSet(numWords.get() + line.split("\\s+").length);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            numCharacters.set(-1);
            numLines.set(-1);
            numWords.set(-1);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Format filesize to human readable format
     * Get size in bytes e.g. from {@link File} using {@code File#length()}
     */
    public static String getReadableFileSize(long size, boolean abbreviation) {
        if (size <= 0) {
            return "0B";
        }
        String[] units = abbreviation ? new String[]{"B", "kB", "MB", "GB", "TB"} : new String[]{"Bytes", "Kilobytes", "Megabytes", "Gigabytes", "Terabytes"};
        int unit = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(size / Math.pow(1024, unit)) + " " + units[unit];
    }

    public static int[] getTimeDiffHMS(long now, long past) {
        int[] ret = new int[3];
        long diff = Math.abs(now - past);
        ret[0] = (int) (diff / (1000 * 60 * 60)); // hours
        ret[1] = (int) (diff / (1000 * 60)) % 60; // min
        ret[2] = (int) (diff / 1000) % 60; // sec
        return ret;
    }

    public static String getHumanReadableByteCountSI(final long bytes) {
        String out;
        if (bytes < 1000) {
            out = String.format(Locale.getDefault(), "%d %s", bytes, "B");
        } else if (bytes < 1000000) {
            out = String.format(Locale.getDefault(), "%.2f %s", (bytes / 1000f), "KB");
        } else if (bytes < 1000000000) {
            out = String.format(Locale.getDefault(), "%.2f %s", (bytes / 1000000f), "MB");
        } else if (bytes < 1000000000000L) {
            out = String.format(Locale.getDefault(), "%.2f %s", (bytes / 1000000000f), "GB");
        } else {
            out = String.format(Locale.getDefault(), "%.2f %s", (bytes / 1000000000000f), "TB");
        }
        return out.replaceFirst(",0+( [A-Z])", "$1");
    }

    public static File join(File file, String... childSegments) {
        for (final String s : childSegments != null ? childSegments : new String[0]) {
            file = new File(file, s);
        }
        return file;
    }

    public static File join(final File... files) {
        if (files == null || files.length == 0) {
            return null;
        }

        File result = files[0];
        for (int i = 1; i < files.length; i++) {
            result = new File(result, files[i].getAbsolutePath());
        }
        return result;
    }

    private static String hash(final byte[] data, final String alg) {
        try {
            return Arrays.toString(MessageDigest.getInstance(alg).digest(data));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String md5(final byte[] data) {
        return hash(data, "MD5");
    }

    public static String sha512(final byte[] data) {
        return hash(data, "SHA-512");
    }

    public static long crc32(final CharSequence data) {
        final CRC32 alg = new CRC32();
        final int length = data.length();
        for (int i = 0; i < length; i++) {
            final char c = data.charAt(i);
            // Upper and lower bytes
            alg.update((byte) (c & 0xff));
            alg.update((byte) (c >> 8));
        }
        return alg.getValue();
    }

    public static long crc32(final byte[] data) {
        final CRC32 alg = new CRC32();
        alg.update(data);
        return alg.getValue();
    }

    // Return true if the target file exists, false if there is an issue with the file or it's parent directories
    public static boolean fileExists(final File checkFile, boolean... caseInsensitive) {
        boolean isAndroid = System.getProperty("java.specification.vendor").contains("Android");
        boolean sensitive = !isAndroid && (caseInsensitive == null || caseInsensitive.length == 0 || !caseInsensitive[0]);

        File[] files;
        if (checkFile != null && checkFile.getParentFile() != null && (files = checkFile.getParentFile().listFiles()) != null) {
            final String checkFilename = checkFile.getName();
            for (final File existingFile : files) {
                final String existingName = existingFile.getName();
                if (sensitive ? existingName.equals(checkFilename) : existingName.equalsIgnoreCase(checkFilename)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Get the title of the file
    public static String getFilenameWithoutExtension(final File file) {
        return getNameWithoutExtension(file.getName());
    }

    public static String getNameWithoutExtension(final String fileName) {
        final int doti = fileName.lastIndexOf(".");
        return (doti < 0) ? fileName : fileName.substring(0, doti);
    }

    /// Get the file extension of the file, including dot
    public static String getFilenameExtension(final File file) {
        final String name = file.getName().replace(".jenc", "");
        final int doti = name.lastIndexOf(".");
        return (doti < 0) ? "" : name.substring(doti).toLowerCase();
    }

    public static String getFilteredFilenameWithoutDisallowedChars(String str, final boolean... a1NoRCEguard) {
        final boolean noRCEguard = a1NoRCEguard != null && a1NoRCEguard.length > 0 && a1NoRCEguard[0];
        final String disallowedChars = "<|[\\?*:\"\n\r\t\0]/>" + (noRCEguard ? "" : "`$%;=&#@");
        for (char disallowed : disallowedChars.toCharArray()) {
            str = str.replace(String.valueOf(disallowed), "");
        }
        return str.trim();
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
    public static String getFilenameWithTimestamp(final String... A0prefixA1postfixA2ext) {
        final String prefix = (((A0prefixA1postfixA2ext != null && A0prefixA1postfixA2ext.length > 0 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[0])) ? A0prefixA1postfixA2ext[0] : "Screenshot") + "_").trim().replaceFirst("^_$", "");
        final String postfix = ("_" + ((A0prefixA1postfixA2ext != null && A0prefixA1postfixA2ext.length > 1 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[1])) ? A0prefixA1postfixA2ext[1] : "")).trim().replaceFirst("^_$", "");
        final String ext = (A0prefixA1postfixA2ext != null && A0prefixA1postfixA2ext.length > 2 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[2])) ? A0prefixA1postfixA2ext[2] : "jpg";
        String filename = String.format("%s%s%s.%s", prefix.trim(), DATEFORMAT_IMG.format(new Date()), postfix.trim(), ext.toLowerCase().replace(".", "").replace("jpeg", "jpg"));
        filename = getFilteredFilenameWithoutDisallowedChars(filename);
        return filename;
    }

    public static String getPrefix(final String name) {
        return name.split("_")[0];
    }

    public static final String SORT_BY_NAME = "NAME", SORT_BY_FILESIZE = "FILESIZE", SORT_BY_MTIME = "MTIME", SORT_BY_MIMETYPE = "MIMETYPE";

    /**
     * Get a key which can be use to sort File objects
     * This is highly performant as each file is processed exactly once.
     * Inspired by python's sort
     *
     * @param sortBy String key of what to sort
     * @param file   The file object to get the
     * @return A key which can be used for comparisons / sorting
     */
    private static String makeSortKey(final String sortBy, final File file) {
        final String name = file.getName().toLowerCase();
        switch (sortBy) {
            case SORT_BY_MTIME: {
                return file.lastModified() + name;
            }
            case SORT_BY_FILESIZE: {
                return String.format("%015d", file.length()) + name;
            }
            case SORT_BY_MIMETYPE: {
                return getMimeType(file).toLowerCase() + name;
            }
            case SORT_BY_NAME:
            default: {
                return name;
            }
        }
    }

    public static void sortFiles(
            final List<File> filesToSort,
            final String sortBy,
            final boolean folderFirst,
            final boolean reverse
    ) {
        if (filesToSort != null && !filesToSort.isEmpty()) {
            try {
                GsCollectionUtils.keySort(filesToSort, (f) -> makeSortKey(sortBy, f));
                if (reverse) {
                    Collections.reverse(filesToSort);
                }
                if (folderFirst) {
                    GsCollectionUtils.keySort(filesToSort, (f) -> !f.isDirectory());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if a file can be created (parent exists and can be written)
     */
    public static boolean isWritable(final File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return file != null && Files.isWritable(file.toPath());
        } else {
            return file != null && file.canWrite();
        }
    }

    /**
     * Check if a file can be created.
     * Checks if closest ancestor is writeable.
     */
    public static boolean canCreate(File file) {
        // A file is creatable if the first existing ancestor is writeable
        while (file != null && !file.exists()) {
            file = file.getParentFile();
        }
        return isWritable(file);
    }

    /**
     * Check if file is child of folder. A folder is not its own child.
     *
     * @param parent Parent folder
     * @param test   File to test
     * @return if parent is a child of test
     */
    public static boolean isChild(final File parent, File test) {
        if (test == null || parent == null || parent.equals(test)) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return test.toPath().toAbsolutePath().startsWith(parent.toPath().toAbsolutePath());
        }

        do {
            test = test.getParentFile();
            if (parent.equals(test)) {
                return true;
            }
        } while (test != null);

        return false;
    }

    public static File findNonConflictingDest(final File destDir, final String name) {
        File dest = new File(destDir, name);
        final String[] splits = name.split("\\.");
        final String baseName = splits[0];
        splits[0] = "";
        final String extension = String.join(".", splits);
        int i = 1;
        while (dest.exists()) {
            dest = new File(destDir, String.format("%s_%d%s", baseName, i, extension));
            i++;
        }
        return dest;
    }

    public static boolean isUriOrFilePath(final String path) {
        try {
            // Attempt to create a URI from the given path
            // If the URI scheme is "file", it's a file path
            return "file".equals(Uri.parse(path).getScheme());
        } catch (Exception e) {
            // If parsing the path as a URI fails, it's not a valid URI
            // So, assume it's a file path
            return true;
        }
    }

    public static void copyUriToFile(final Context context, final Uri source, final File dest) {

        try (final OutputStream outputStream = new FileOutputStream(dest, false);
             final InputStream inputStream = context.getContentResolver().openInputStream(source)
        ) {
            final byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException ignored) {
        }
    }

    public static File makeAbsolute(final String path, final File base) {
        return path != null ? makeAbsolute(new File(path.trim()), base) : null;
    }

    public static File makeAbsolute(final File file, final File base) {
        if (file == null) {
            return null;
        } else if (file.isAbsolute()) {
            return file;
        } else if (base != null) {
            return new File(base, file.getPath()).getAbsoluteFile();
        } else {
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static List<File> searchFiles(final File root, String glob) {
        try {
            glob = glob.trim();
            glob = glob.startsWith("glob:") ? glob : "glob:" + glob;

            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);
            final List<File> found = new ArrayList<>();
            Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {
                    if (matcher.matches(path)) {
                        found.add(path.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return found;
        } catch (IOException e) {
            Log.d(GsFileUtils.class.getName(), e.toString());
        }
        return null;
    }
}
