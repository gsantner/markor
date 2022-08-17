package net.gsantner.markor.format.plaintext;
/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.util.AppSettings;

public class PlaintextHighlighter extends Highlighter {

    public PlaintextHighlighter(AppSettings as) {
        super(as);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        createSmallBlueLinkSpans();
    }

}

