package net.gsantner.markor.format.asciidoc;
/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

public class AsciidocSyntaxHighlighter extends SyntaxHighlighterBase {
    public AsciidocSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        // TODO: font is very small, where to set font size?
        // also it uses private static String formatLink(String text, String link), which is adapted for Markdown
        // needs to be adapted for AsciiDoc
        createSmallBlueLinkSpans();
    }

}

