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
package net.gsantner.markor.format.markdown;

// Neutral means good readability in both, light and dark theme
@SuppressWarnings({"FieldCanBeLocal", "SpellCheckingInspection", "WeakerAccess"})
public class MarkdownHighlighterColors {
    private final int COLOR_HEADER = 0xffef6D00;
    private final int COLOR_LINK = 0xff1ea3fe;
    private final int COLOR_LIST = 0xffdaa521;
    private final int COLOR_QUOTE = 0xff88b04c;
    private final int COLOR_DOUBLESPACE = 0xffe0e1e0;

    public int getHeaderColor() {
        return COLOR_HEADER;
    }

    public int getLinkColor() {
        return COLOR_LINK;
    }

    public int getListColor() {
        return COLOR_LIST;
    }

    public int getDoublespaceColor() {
        return COLOR_DOUBLESPACE;
    }

    public int getQuotationColor() {
        return COLOR_QUOTE;
    }

}
