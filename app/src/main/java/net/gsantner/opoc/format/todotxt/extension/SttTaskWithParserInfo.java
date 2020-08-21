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
