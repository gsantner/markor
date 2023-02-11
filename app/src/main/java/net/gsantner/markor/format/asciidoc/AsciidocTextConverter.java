/*#######################################################
 *
 *   Maintained 2018-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;

import net.gsantner.markor.format.plaintext.PlaintextTextConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class AsciidocTextConverter extends PlaintextTextConverter {
    //########################
    //## Extensions
    //########################

    private static final List<String> EXT_ASCIIDOC = Arrays.asList(".adoc", ".asciidoc", ".asc");
    private static final List<String> EXT = new ArrayList<>();

    static {
        EXT.addAll(EXT_ASCIIDOC);
    }

    @Override
    protected boolean isFileOutOfThisFormat(String filepath, String extWithDot) {
        return EXT.contains(extWithDot);
    }
}
