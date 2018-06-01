/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.preference;

import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "SpellCheckingInspection", "unused", "SameParameterValue"})
public interface PropertyBackend<TKEY, TTHIS> {
    String getString(TKEY key, String defaultValue);

    int getInt(TKEY key, int defaultValue);

    long getLong(TKEY key, long defaultValue);

    boolean getBool(TKEY key, boolean defaultValue);

    float getFloat(TKEY key, float defaultValue);

    double getDouble(TKEY key, double defaultValue);

    List<Integer> getIntList(TKEY key);

    List<String> getStringList(TKEY key);

    TTHIS setString(TKEY key, String value);

    TTHIS setInt(TKEY key, int value);

    TTHIS setLong(TKEY key, long value);

    TTHIS setBool(TKEY key, boolean value);

    TTHIS setFloat(TKEY key, float value);

    TTHIS setDouble(TKEY key, double value);

    TTHIS setIntList(TKEY key, List<Integer> value);

    TTHIS setStringList(TKEY key, List<String> value);
}
