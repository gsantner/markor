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

import net.gsantner.opoc.util.TextUtils;

import java.util.Date;

public class TodoTxtAutoFormat implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            if (start < source.length() && dstart <= dest.length() && TextUtils.isNewLine(source, start, end)) {
                return autoIndent(source);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
        }
        return source;
    }

    private CharSequence autoIndent(CharSequence source) {
        return source + TodoTxtTask.DATEF_YYYY_MM_DD.format(new Date()) + " ";
    }
}
