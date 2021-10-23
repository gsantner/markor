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

import net.gsantner.markor.format.TextFormat;

import java.io.File;
import java.io.Serializable;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Document implements Serializable {
    private final static int MIN_HISTORY_DELAY = 1500; // [ms]
    private final static int MAX_HISTORY_SIZE = 5;

    private int _format = TextFormat.FORMAT_UNKNOWN;
    private File _file = null; // Full filepath (path + filename + extension)
    private String _title = "";  // The title of the document. May lead to a rename at save
    private String _fileExtension = ""; // Not versioned. folder(path) /  title + ext
    private String _content = "";
    private long _modTime = 0;
    private int _initialLineNumber = -1;

    public Document() {
    }

    public Document(File file) {
        _file = file;
    }

    public static String getPath(final Document document) {
        if (document != null) {
            final File file = document.getFile();
            if (file != null) {
                return file.getPath();
            }
        }
        return null;
    }

    public Document cloneDocument() {
        return fromDocumentToDocument(this, new Document());
    }

    public Document loadFromDocument(Document source) {
        return fromDocumentToDocument(source, this);
    }

    public static Document fromDocumentToDocument(Document source, Document target) {
        target.setFile(source.getFile());
        target.setTitle(source.getTitle());
        target.setContent(source.getContent());
        target.setFormat(source.getFormat());
        target.setModTime(source.getModTime());
        return target;
    }

    public File getFile() {
        return _file;
    }

    public void setFile(File file) {
        if (!equalsc(getFile(), file)) {
            _file = file;
        }
    }

    public String getTitle() {
        return _title;
    }

    public void setTitle(String title) {
        if (!equalsc(getTitle(), title)) {
            _title = title;
        }
    }

    public String getContent() {
        return _content;
    }

    public void setContent(String content) {
        if (!equalsc(getContent(), content)) {
            _content = content;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Document) {
            Document other = ((Document) obj);
            return equalsc(getFile(), other.getFile())
                    && equalsc(getTitle(), other.getTitle())
                    && equalsc(getContent(), other.getContent());
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
        if (_fileExtension == null && _file != null) {
            final String name = _file.getName();
            _fileExtension = (name.contains(".") ? name.substring(name.lastIndexOf(".")) : "").toLowerCase();
        }
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

    public void setInitialLineNumber(final int lineNumber) {
        _initialLineNumber = lineNumber;
    }

    public int getInitialLineNumber() {
        return _initialLineNumber;
    }

}
