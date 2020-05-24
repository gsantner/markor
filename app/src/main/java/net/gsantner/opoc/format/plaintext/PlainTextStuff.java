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
package net.gsantner.opoc.format.plaintext;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Random;

public class PlainTextStuff {

    /**
     * This is a simple method that tries to extract an URL around a given index.
     * It doesn't do any validation. Separation by whitespace or end. Detects http and https.
     *
     * @param text Text to extract from
     * @param pos  Position to start searching from (backwards)
     * @return Extracted URL or {@code null} if none found
     */
    public static String tryExtractUrlAroundPos(String text, int pos) {
        pos = Math.min(Math.max(0, pos), text.length() - 1);
        if (pos >= 0 && pos < text.length()) {
            int begin = Math.max(text.lastIndexOf("https://", pos), text.lastIndexOf("http://", pos));
            if (begin >= 0) {
                int end = text.length();
                for (String check : new String[]{"\n", " ", "\t", "\r", ")"}) {
                    if ((pos = text.indexOf(check, begin)) > begin && pos < end) {
                        end = pos;
                    }
                }
                if ((end - begin) > 5 && end > 5) {
                    return text.substring(begin, end);
                }
            }
        }
        return null;
    }

    public static int[] getNeighbourLineEndings(String text, int pos, int posEnd) {
        final int len = text.length();

        if (pos < len && pos >= 0 && text.charAt(pos) == '\n') {
            pos--;
        }
        pos = Math.min(Math.max(0, pos), len - 1);
        posEnd = Math.min(Math.max(0, posEnd), len - 1);
        if (pos == len) {
            pos--;
        }
        if (pos < 0 || pos > len) {
            return null;
        }
        pos = Math.max(0, text.lastIndexOf("\n", pos));
        posEnd = text.indexOf("\n", posEnd);
        if (posEnd < 0 || posEnd >= len - 1) {
            posEnd = len;
        }
        if (pos == 0 && pos == posEnd && posEnd + 1 <= len) {
            posEnd++;
        }
        if (pos <= len && posEnd <= len && pos <= posEnd) {
            return new int[]{pos, posEnd};
        }
        return null;
    }


    public static String removeLinesOfTextAround(String text, int pos, int posEnd) {
        int[] endings = getNeighbourLineEndings(text, pos, posEnd);
        if (endings != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(text.substring(0, pos));
            sb.append(text.substring(posEnd));
            return sb.toString();
        }
        return text;
    }


    // Code snippet 'function huuid' is licensed CC0/Public Domain license. Revision 1, Gregor Santner, 2020
    //
    // Generate a UUID that starts with human readable datetime, use 8-4-4-4-12 UUID grouping
    // While there are zero guarantees, you will get most relevant datetime information out of a huuid
    // Plays nice with 'sort by name' in file managers and other tools
    //
    // Example based on "Mon Jan 2 15:04:05 MST 2006", deviceID dddd, 430 milliseconds, random string ffffffff, timezone MST=UTC+07:00
    // 20060102-1504-0543-070a-ddddffffffff
    //
    // Format detail: Milliseconds -> first two digits(=Centiseconds); UTC+/- -> a/f instead of last minute digit
    @SuppressLint("ALL")
    public static String newHuuid(String hostid4c) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hhmm-ssSSS'%STRIP1BEFORE%'-'%UTCOFFSET%'-'%HOSTID%%RAND%'");
        String rnd8c = String.format("%08x", new Random().nextInt());
        hostid4c = ((hostid4c == null ? "" : hostid4c) + "0000").substring(0, 4).replaceAll("[^A-Fa-f0-9]", "0");
        String utcoffset4c = String.format("%tz", OffsetDateTime.now().getOffset()) + "0000";
        utcoffset4c = utcoffset4c.substring(1, 4) + (utcoffset4c.startsWith("-") ? "a" : "f");

        return sdf.format(new Date())
                .replace("%UTCOFFSET%", utcoffset4c)
                .replace("%HOSTID%", hostid4c)
                .replace("%RAND%", rnd8c)
                .replaceAll(".%STRIP1BEFORE%", "").toLowerCase();
    }
}
