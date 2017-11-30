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

import net.gsantner.opoc.format.todotxt.SttTask;

public class SttTaskWithParserInfo extends SttTask implements SttTaskParserInfoExtension<SttTaskWithParserInfo> {

    @Override
    public String getTaskLine() {
        return _data.getString(PROP_TASK_LINE, "");
    }

    @Override
    public SttTaskWithParserInfo setTaskLine(String value) {
        _data.setString(PROP_TASK_LINE, value);
        return this;
    }

    @Override
    public int getLineOffsetInText() {
        return _data.getInt(PROP_LINE_OFFSET_IN_TEXT, 0);
    }

    @Override
    public SttTaskWithParserInfo setLineOffsetInText(int value) {
        _data.setInt(PROP_LINE_OFFSET_IN_TEXT, value);
        return this;
    }

    @Override
    public int getCursorOffsetInLine() {
        return _data.getInt(PROP_CURSOR_OFFSET_IN_LINE, 0);
    }

    @Override
    public SttTaskWithParserInfo setCursorOffsetInLine(int iOffsetInLine) {
        _data.setInt(PROP_CURSOR_OFFSET_IN_LINE, iOffsetInLine);
        return this;
    }
}
