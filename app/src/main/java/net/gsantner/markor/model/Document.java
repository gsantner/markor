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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "unused"})
public class Document implements Serializable {
    private final static int MIN_HISTORY_DELAY = 2000; // [ms]

    private ArrayList<Document> _history = new ArrayList<>();
    private File _file = null; // Full filepath (path + filename + extension)
    private String _title = "";  // The title of the document. May lead to a rename at save
    private String _fileExtension = ""; // Not versioned. folder(path) /  title + ext
    private String _content = "";
    private boolean _doHistory = true;
    private int _historyPosition = 0;
    private long _lastChanged = 0;

    public Document() {
    }

    public Document(File file) {
        _file = file;
    }

    public synchronized Document cloneDocument() {
        return fromDocumentToDocument(this, new Document());
    }

    public synchronized Document loadFromDocument(Document source) {
        return fromDocumentToDocument(source, this);
    }

    public synchronized static Document fromDocumentToDocument(Document source, Document target) {
        target.setDoHistory(false);
        target.setFile(source.getFile());
        target.setTitle(source.getTitle());
        target.setContent(source.getContent());
        target.setDoHistory(true);
        return target;
    }

    public synchronized boolean canGoToEarlierVersion() {
        // Position 5, History is 5 big, yes
        // Position 3, History is 5 big, yes
        // Position 0, History is 5 big, no
        // Position 0, History is 0 big, no
        return _historyPosition > 0 && _history.size() > 0;
    }

    public synchronized boolean canGoToNewerVersion() {
        // Position 5, History is 5 big, no
        // Position 3, History is 5 big, yes
        // Position 0, History is 5 big, yes
        // Position 0, History is 0 big, no
        return _historyPosition < _history.size() - 1;
    }

    public synchronized void goToEarlierVersion() {
        if (canGoToEarlierVersion()) {
            // If we are at the current state, but this was not saved yet -> save current state
            if (hasChangesNotInHistory()) {
                forceAddNextChangeToHistory();
                addToHistory();
                _historyPosition--;
            }

            _historyPosition--;
            if (_historyPosition >= 0 && _historyPosition < _history.size()) {
                loadFromDocument(_history.get(_historyPosition));
            }
        }
    }

    public boolean hasChangesNotInHistory() {
        return _historyPosition == _history.size() && (_history.size() == 0 || !_history.get(_history.size() - 1).equals(this));
    }

    public synchronized void goToNewerVersion() {
        if (canGoToNewerVersion()) {
            _historyPosition++;
            loadFromDocument(_history.get(_historyPosition));
        }
    }

    public synchronized void addToHistory() {
        if (_doHistory && (((_lastChanged + MIN_HISTORY_DELAY) < System.currentTimeMillis()))) {
            while (_historyPosition != _history.size() && _history.size() != 0) {
                _history.remove(_history.size() - 1);
            }
            _history.add(cloneDocument());
            _historyPosition++;
        }
    }

    public synchronized File getFile() {
        return _file;
    }

    public synchronized void setFile(File file) {
        if (!equalsc(getFile(), file)) {
            addToHistory();
            _file = file;
        }
    }

    public synchronized String getTitle() {
        return _title;
    }

    public synchronized void setTitle(String title) {
        if (!equalsc(getTitle(), title)) {
            addToHistory();
            _title = title;
            _lastChanged = System.currentTimeMillis();
        }
    }

    public synchronized String getContent() {
        return _content;
    }

    public synchronized void setContent(String content) {
        if (!equalsc(getContent(), content)) {
            addToHistory();
            _content = content;
            _lastChanged = System.currentTimeMillis();
        }
    }

    public synchronized Document getInitialVersion() {
        if (hasChangesNotInHistory()) {
            boolean history = _doHistory;
            setDoHistory(true);
            addToHistory();
            setDoHistory(history);
        }
        return _history.size() == 0 ? this : _history.get(0);
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

    public boolean isDoHistory() {
        return _doHistory;
    }

    public void setDoHistory(boolean doHistory) {
        _doHistory = doHistory;
    }

    public ArrayList<Document> getHistory() {
        return _history;
    }

    public void setHistory(ArrayList<Document> history) {
        _history = history;
    }

    public int getHistoryPosition() {
        return _historyPosition;
    }

    public void setHistoryPosition(int historyPosition) {
        _historyPosition = historyPosition;
    }

    public void forceAddNextChangeToHistory() {
        _lastChanged = 0;
    }

    public String getFileExtension() {
        return _fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        _fileExtension = fileExtension;
    }
}
