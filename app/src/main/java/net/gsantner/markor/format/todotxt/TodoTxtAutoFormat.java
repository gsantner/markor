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
import net.gsantner.opoc.format.plaintext.PlainTextStuff;
import net.gsantner.opoc.util.StringUtils;

import java.util.Date;

public class TodoTxtAutoFormat implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            if (start < source.length() && dstart <= dest.length() && StringUtils.isNewLine(source, start, end)) {
                return autoIndent(source);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
        }
        return source;
    }

    private CharSequence autoIndent(CharSequence source) {
        String t = "";
        final AppSettings settings = AppSettings.get();
        if (settings.isTodoStartTasksWithTodaysDateEnabled()) {
            t += TodoTxtTask.DATEF_YYYY_MM_DD.format(new Date()) + " ";
        }
        if (settings.isTodoNewTaskWithHuuidEnabled()) {
            t += "huuid:" + PlainTextStuff.newHuuid(AppSettings.get().getHuuidDeviceId()) + " ";
        }
        return source + t;
    }
}
