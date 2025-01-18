package net.gsantner.markor.format.orgmode;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.format.orgmode.OrgmodeReplacePatternGenerator;
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
                new ActionItem(R.string.abid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.abid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                new ActionItem(R.string.abid_common_insert_link, R.drawable.ic_link_black_24dp, R.string.insert_link),
                new ActionItem(R.string.abid_common_insert_image, R.drawable.ic_image_black_24dp, R.string.insert_image),
                new ActionItem(R.string.abid_common_insert_audio, R.drawable.ic_keyboard_voice_black_24dp, R.string.audio),
                new ActionItem(R.string.abid_orgmode_bold, R.drawable.ic_format_bold_black_24dp, R.string.bold),
                new ActionItem(R.string.abid_orgmode_italic, R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(R.string.abid_orgmode_strikeout, R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(R.string.abid_orgmode_underline, R.drawable.ic_format_underlined_black_24dp, R.string.underline),
                new ActionItem(R.string.abid_orgmode_code_inline, R.drawable.ic_code_black_24dp, R.string.inline_code),
                new ActionItem(R.string.abid_orgmode_h1, R.drawable.format_header_1, R.string.heading_1),
                new ActionItem(R.string.abid_orgmode_h2, R.drawable.format_header_2, R.string.heading_2),
                new ActionItem(R.string.abid_orgmode_h3, R.drawable.format_header_3, R.string.heading_3)
        );
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__orgmode__action_keys;
    }

    @Override
    protected void renumberOrderedList() {
        AutoTextFormatter.renumberOrderedList(_hlEditor.getText(), OrgmodeReplacePatternGenerator.formatPatterns);
    }

    @Override
    public boolean onActionClick(final @StringRes int action) {
        switch (action) {
            case R.string.abid_orgmode_h1: {
                runRegexReplaceAction(OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(1));
                return true;
            }
            case R.string.abid_orgmode_h2: {
                runRegexReplaceAction(OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(2));
                return true;
            }
            case R.string.abid_orgmode_h3: {
                runRegexReplaceAction(OrgmodeReplacePatternGenerator.setOrUnsetHeadingWithLevel(3));
                return true;
            }
            case R.string.abid_common_unordered_list_char: {
                final String listChar = _appSettings.getUnorderedListCharacter();
                runRegexReplaceAction(OrgmodeReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix(listChar));
                return true;
            }
            case R.string.abid_common_checkbox_list: {
                final String listChar = _appSettings.getUnorderedListCharacter();
                runRegexReplaceAction(OrgmodeReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix(listChar));
                return true;
            }
            case R.string.abid_common_ordered_list_number: {
                runRegexReplaceAction(OrgmodeReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix());
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.abid_orgmode_bold: {
                runSurroundAction("*");
                return true;
            }
            case R.string.abid_orgmode_italic: {
                runSurroundAction("/");
                return true;
            }
            case R.string.abid_orgmode_strikeout: {
                runSurroundAction("+");
                return true;
            }
            case R.string.abid_orgmode_underline: {
                runSurroundAction("_");
                return true;
            }
            case R.string.abid_orgmode_code_inline: {
                runSurroundAction("=");
                return true;
            }
            default: {
                return runCommonAction(action);
            }
        }
    }
}
