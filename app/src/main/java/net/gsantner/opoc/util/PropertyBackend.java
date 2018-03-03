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
package net.gsantner.opoc.util;

import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "SpellCheckingInspection"})
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
