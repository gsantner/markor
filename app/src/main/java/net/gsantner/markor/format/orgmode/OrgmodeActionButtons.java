package net.gsantner.markor.format.orgmode;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.format.markdown.MarkdownReplacePatternGenerator;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.model.Document;

import java.util.Arrays;
import java.util.List;

public class OrgmodeActionButtons extends ActionButtonBase {

    public OrgmodeActionButtons(@NonNull Context context, Document document) {
        super(context, document);
    }

    @Override
    public List<ActionItem> getFormatActionList() {
        return Arrays.asList(
                new ActionItem(R.string.abid_common_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(R.string.abid_common_unordered_list_char, R.drawable.ic_list_black_24dp, R.string.unordered_list),
                new ActionItem(R.string.abid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),
                new ActionItem(R.string.abid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),
                new ActionItem(R.string.abid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.abid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent)
        );
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__orgmode__action_keys;
    }

    @Override
    protected void renumberOrderedList() {
        // Use markdown format for orgmode too
        AutoTextFormatter.renumberOrderedList(_hlEditor.getText(), MarkdownReplacePatternGenerator.formatPatterns);
    }
}
