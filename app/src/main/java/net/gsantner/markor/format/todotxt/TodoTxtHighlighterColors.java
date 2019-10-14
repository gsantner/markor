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

// Neutral means good readability in both, light and dark theme
@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
public class TodoTxtHighlighterColors {

    private final int COLOR_CATEGORY = 0xffef6C00;
    private final int COLOR_CONTEXT = 0xff88b04b;
    private final int COLOR_LINK = 0xff1ea3fd;

    public int getContextColor() {
        return COLOR_CONTEXT;
    }

    public int getLinkColor() {
        return COLOR_LINK;
    }

    public int getPriorityColor(int priority) {
        switch (priority) {
            case 1:
                return 0xffEF2929;
            case 2:
                return 0xffF57900;
            case 3:
                return 0xff73D216;
            case 4:
                return 0xff0099CC;
            case 5:
                return 0xffEDD400;
            default:
            case 6:
                return 0xff888A85;
        }
    }

    public int getDoneColor() {
        return 0x993d3d3d;
    }

    public int getDateColor() {
        return 0xcc6d6d6d;
    }

    public int getCategoryColor() {
        return COLOR_CATEGORY;
    }
}
