/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.net> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
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
