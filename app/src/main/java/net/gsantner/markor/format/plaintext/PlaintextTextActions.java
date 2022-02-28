/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.plaintext;

import android.app.Activity;
import android.support.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.format.AutoFormatter;
import net.gsantner.markor.format.markdown.MarkdownAutoFormat;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.TextActions;

import java.util.Arrays;
import java.util.List;

public class PlaintextTextActions extends TextActions {

    public PlaintextTextActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public List<ActionItem> getActiveActionList() {

        final ActionItem[] TMA_ACTIONS = {
                new ActionItem(R.string.tmaid_common_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(R.string.tmaid_common_unordered_list_char, R.drawable.ic_list_black_24dp, R.string.unordered_list),
                new ActionItem(R.string.tmaid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),
                new ActionItem(R.string.tmaid_common_jump_to_bottom, R.drawable.ic_vertical_align_center_black_24dp, R.string.jump_to_bottom),
                new ActionItem(R.string.tmaid_common_delete_lines, R.drawable.ic_delete_black_24dp, R.string.delete_lines),
                new ActionItem(R.string.tmaid_common_open_link_browser, R.drawable.ic_open_in_browser_black_24dp, R.string.open_link),
                new ActionItem(R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),
                new ActionItem(R.string.tmaid_common_special_key, R.drawable.ic_keyboard_black_24dp, R.string.special_key),
                new ActionItem(R.string.tmaid_common_time, R.drawable.ic_access_time_black_24dp, R.string.date_and_time),
                new ActionItem(R.string.tmaid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.tmaid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                new ActionItem(R.string.tmaid_common_new_line_below, R.drawable.ic_baseline_keyboard_return_24, R.string.start_new_line_below),
                new ActionItem(R.string.tmaid_common_move_text_one_line_up, R.drawable.ic_baseline_arrow_upward_24, R.string.move_text_one_line_up),
                new ActionItem(R.string.tmaid_common_move_text_one_line_down, R.drawable.ic_baseline_arrow_downward_24, R.string.move_text_one_line_down),
                new ActionItem(R.string.tmaid_common_insert_snippet, R.drawable.ic_baseline_file_copy_24, R.string.insert_snippet),
        };

        return Arrays.asList(TMA_ACTIONS);
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__plaintext__action_keys;
    }

    @Override
    protected void renumberOrderedList(final int position) {
        // Use markdown format for plain text too
        AutoFormatter.renumberOrderedList(_hlEditor.getText(), position, MarkdownAutoFormat.getPrefixPatterns());
    }
}
