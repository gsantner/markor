/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A timer for quick time measurement. Nano - in both, time and functions
 */
public class GsNanoProfiler {
    private final DecimalFormat formatter = new DecimalFormat("000000000.0000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private long _profilingGroupValue = 0;
    private int _groupCount = 0;
    private boolean _profilerEnabled = true;
    private long _profilingValue = -1;
    private String _text;
    private static String _debugText = "";

    public GsNanoProfiler setEnabled(boolean enabled) {
        _profilerEnabled = enabled;
        return this;
    }

    public String resetDebugText() {
        String text = _debugText;
        _debugText = "";
        return text;
    }

    public void start(boolean increaseGroupCounter, String... optionalText) {
        if (_profilerEnabled) {
            if (increaseGroupCounter) {
                _groupCount++;
                _profilingGroupValue = 0;
            }
            _text = optionalText != null && optionalText.length == 1 ? optionalText[0] : "action";
            _profilingValue = System.nanoTime();
        }
    }

    public void restart(String... optionalText) {
        end();
        start(false, optionalText);
    }

    public void printProfilingGroup() {
        if (_profilerEnabled) {
            String text = formatter.format(_profilingGroupValue / 1000f).replaceAll("\\G0", " ") + " [ms] for Group " + _groupCount;
            text = "NanoProfiler::: " + _groupCount + text;
            _debugText += text + "\n";
            System.out.println(text);
        }
    }

    public void end() {
        long now = System.nanoTime();
        if (_profilerEnabled) {
            _profilingValue = now - _profilingValue;
            _profilingGroupValue += _profilingValue / 1000f;
            String text = formatter.format(_profilingValue / 1000f).replaceAll("\\G0", " ") + " [µs] for " + _text;
            text = "NanoProfiler::: " + _groupCount + text;
            _debugText += text + "\n";
            System.out.println(text);
        }
    }
}
