/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.text.InputFilter;
import android.text.Spanned;

import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.todotxt.SttCommander;

import java.util.Date;

public class TodoTxtAutoFormat implements InputFilter {

    @Override
    public CharSequence filter(
            CharSequence source,
            int start,
            int end,
            Spanned dest,
            int dstart,
            int dend) {
        if (end - start == 1 &&
                start < source.length() &&
                dstart <= dest.length()) {
            char newChar = source.charAt(start);

            if (newChar == '\n') {
                return autoIndent(
                        source,
                        dest,
                        dstart,
                        dend);
            }
        }

        return source;
    }

    private CharSequence autoIndent(CharSequence source, Spanned dest, int dstart, int dend) {
        int istart = findLineBreakPosition(dest, dstart);

        // append white space of previous line and new indent
        return source + createIndentForNextLine(dest, dend, istart);
    }

    private int findLineBreakPosition(Spanned dest, int dstart) {
        int istart = dstart - 1;

        for (; istart > -1; --istart) {
            char c = dest.charAt(istart);

            if (c == '\n')
                break;
        }
        return istart;
    }

    private String createIndentForNextLine(Spanned dest, int dend, int istart) {
        if (AppSettings.get().isTodoStartTasksWithTodaysDateEnabled()) {
            if (dend == 0 || (dend == dest.length() || dend == dest.length() - 1)
                    || (dest.charAt(dend) == '\n')) {
                return SttCommander.DATEF_YYYY_MM_DD.format(new Date()) + " ";
            }
        }
        return "";
    }
}
