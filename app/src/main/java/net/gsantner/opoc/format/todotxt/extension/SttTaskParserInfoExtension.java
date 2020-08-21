/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.format.todotxt.extension;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public interface SttTaskParserInfoExtension<TTHIS> {
    int PROP_TASK_LINE = 1001;
    int PROP_LINE_OFFSET_IN_TEXT = 1002;
    int PROP_CURSOR_OFFSET_IN_LINE = 1003;

    String getTaskLine();

    TTHIS setTaskLine(String value);

    int getLineOffsetInText();

    TTHIS setLineOffsetInText(int value);

    int getCursorOffsetInLine();

    TTHIS setCursorOffsetInLine(int value);


}
