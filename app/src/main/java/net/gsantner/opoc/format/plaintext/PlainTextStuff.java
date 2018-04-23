/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.format.plaintext;

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
        pos = Math.min(Math.max(0, pos), text.length()-1);
        if (pos >= 0 && pos < text.length()) {
            int begin = Math.max(text.lastIndexOf("https://", pos), text.lastIndexOf("http://", pos));
            if (begin >= 0) {
                int end = text.length() - 1;
                for (String check : new String[]{"\n", " ", "\t", "\r"}) {
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
}
