/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.keyvalue;

import net.gsantner.markor.format.plaintext.PlaintextTextConverter;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class KeyValueTextConverter extends PlaintextTextConverter {
    private static final List<String> EXT = Arrays.asList(".yml", ".yaml", ".toml", ".vcf", ".ics", ".ini", ".json", ".csv", ".zim");

    @Override
    public boolean isFileOutOfThisFormat(String filepath, String extWithDot) {
        return EXT.contains(extWithDot);
    }
}
