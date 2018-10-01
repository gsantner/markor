/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

import java.text.DecimalFormat;

/**
 * A timer for quick time measurement. Nano - in both, time and functions
 */
public class NanoProfiler {
    private final DecimalFormat formatter = new DecimalFormat("000000000.0000000");
    private long _profilingGroupValue = 0;
    private int _groupCount = 0;
    private boolean _profilerEnabled = true;
    private long _profilingValue = -1;
    private String _text;

    public NanoProfiler setEnabled(boolean enabled) {
        _profilerEnabled = enabled;
        return this;
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
            System.out.println("NanoProfiler::: " + _groupCount + text);
        }
    }

    public void end() {
        long now = System.nanoTime();
        if (_profilerEnabled) {
            _profilingValue = now - _profilingValue;
            _profilingGroupValue += _profilingValue / 1000f;
            String text = formatter.format(_profilingValue / 1000f).replaceAll("\\G0", " ") + " [µs] for " + _text;
            System.out.println("NanoProfiler::: " + _groupCount + text);
        }
    }
}
