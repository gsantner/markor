/*#######################################################
 *
 * SPDX-FileCopyrightText: 2018-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.model;

import java.util.List;

@SuppressWarnings({"UnusedReturnValue", "SpellCheckingInspection", "unused", "SameParameterValue"})
public interface GsPropertyBackend<TKEY, TTHIS> {
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
