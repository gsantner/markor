/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.format.todotxt;

import net.gsantner.opoc.preference.MapPropertyBackend;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class SttTask implements Serializable {
    //
    // Statics
    //
    protected static final int PROP_DESCRIPTION = 1;
    protected static final int PROP_KEY_VALUE_PAIRS = 10;
    protected static final int PROP_CONTEXTS = 11;
    protected static final int PROP_PROJECTS = 12;
    protected static final int PROP_DONE = 21;
    protected static final int PROP_PRIORITY = 22;
    protected static final int PROP_CREATION_DATE = 23;
    protected static final int PROP_COMPLETION_DATE = 24;
    protected static final int PROP_DUE_DATE = 25;

    public static final char PRIORITY_NONE = (char) -1;

    //
    // Members & Constructor
    //
    protected MapPropertyBackend<Integer> _data = new MapPropertyBackend<>();
    protected Map<String, String> _dataKeyValuePair = new HashMap<>();


    public SttTask() {
    }

    //
    // Methods
    //

    public boolean isDone() {
        return _data.getBool(PROP_DONE, false);
    }

    public SttTask setDone(boolean value) {
        _data.setBool(PROP_DONE, value);
        return this;
    }

    public Map<String, String> getKeyValuePairs() {
        return _dataKeyValuePair;
    }

    public SttTask setKeyValuePairs(Map<String, String> values) {
        _dataKeyValuePair = values;
        return this;
    }

    public String getKeyValuePair(String key, String defaultValue) {
        return _dataKeyValuePair.containsKey(key) ? _dataKeyValuePair.get(key) : defaultValue;
    }

    public SttTask setKeyValuePair(String key, String value) {
        _dataKeyValuePair.put(key, value);
        return this;
    }

    public char getPriority() {
        return (char) _data.getInt(PROP_PRIORITY, PRIORITY_NONE);
    }

    public SttTask setPriority(char value) {
        _data.setInt(PROP_PRIORITY, value);
        return this;
    }

    public List<String> getContexts() {
        return _data.getStringList(PROP_CONTEXTS);
    }

    public SttTask setContexts(List<String> value) {
        _data.setStringList(PROP_CONTEXTS, value);
        return this;
    }

    public List<String> getProjects() {
        return _data.getStringList(PROP_PROJECTS);
    }

    public SttTask setProjects(List<String> value) {
        _data.setStringList(PROP_PROJECTS, value);
        return this;
    }

    public String getCreationDate() {
        return _data.getString(PROP_CREATION_DATE, "");
    }

    public SttTask setCreationDate(String value) {
        _data.setString(PROP_CREATION_DATE, value);
        return this;
    }

    public String getCompletionDate() {
        return _data.getString(PROP_COMPLETION_DATE, "");
    }

    public SttTask setCompletionDate(String value) {
        _data.setString(PROP_COMPLETION_DATE, value);
        return this;
    }

    public String getDueDate() {
        return _data.getString(PROP_DUE_DATE, "");
    }

    public SttTask setDueDate(String value) {
        _data.setString(PROP_DUE_DATE, value);
        return this;
    }

    public String getDescription() {
        return _data.getString(PROP_DESCRIPTION, "");
    }

    public SttTask setDescription(String value) {
        _data.setString(PROP_DESCRIPTION, value);
        return this;
    }

    public MapPropertyBackend<Integer> getData() {
        return _data;
    }


}
