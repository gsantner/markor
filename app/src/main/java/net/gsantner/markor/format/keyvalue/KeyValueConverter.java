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

import net.gsantner.markor.format.plaintext.PlaintextConverter;

import java.util.Arrays;

@SuppressWarnings("WeakerAccess")
public class KeyValueConverter extends PlaintextConverter {

    @Override
    public boolean isFileOutOfThisFormat(String filepath) {
        if (!filepath.contains(".")) {
            return false;
        }
        String ext = filepath.substring(filepath.lastIndexOf(".")).toLowerCase();
        return Arrays.asList(new String[]{".yml", ".yaml", ".toml", ".vcf", ".ics", ".ini", ".json", ".csv"}).contains(ext);
    }
}
