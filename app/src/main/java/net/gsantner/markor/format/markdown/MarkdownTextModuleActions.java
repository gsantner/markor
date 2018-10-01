/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import net.gsantner.markor.R;
import net.gsantner.markor.format.plaintext.CommonTextModuleActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.ui.hleditor.TextModuleActions;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.ui.FilesystemDialogData;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownTextModuleActions extends TextModuleActions {

    public MarkdownTextModuleActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public void appendTextModuleActionsToBar(ViewGroup barLayout) {
        if (AppSettings.get().isEditor_ShowTextmoduleBar() && barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);
            for (int[] actions : TMA_ACTIONS) {
                MarkdownTextModuleActionsImpl actionCallback = new MarkdownTextModuleActionsImpl(actions[0]);
                appendTextModuleActionToBar(barLayout, actions[1], actionCallback, actionCallback);
            }
        } else if (!AppSettings.get().isEditor_ShowTextmoduleBar()) {
            setBarVisible(barLayout, false);
        }
    }

    //
    //
    //


    // Mapping from action (string res) to icon (drawable res)
    private static final int[][] TMA_ACTIONS = {
            {R.string.tmaid_markdown_bold, R.drawable.ic_format_bold_black_24dp},
            {R.string.tmaid_markdown_italic, R.drawable.ic_format_italic_black_24dp},
            {R.string.tmaid_markdown_code_inline, R.drawable.ic_code_black_24dp},
            {R.string.tmaid_markdown_insert_image, R.drawable.ic_image_black_24dp},
            {R.string.tmaid_general_delete_lines, CommonTextModuleActions.ACTION_DELETE_LINES_ICON},
            {R.string.tmaid_general_open_link_browser, CommonTextModuleActions.ACTION_OPEN_LINK_BROWSER__ICON},
            {R.string.tmaid_general_special_key, CommonTextModuleActions.ACTION_SPECIAL_KEY__ICON},
            {R.string.tmaid_markdown_insert_link, R.drawable.ic_link_black_24dp},

            {R.string.tmaid_markdown_horizontal_line, R.drawable.ic_more_horiz_black_24dp},
            {R.string.tmaid_markdown_strikeout, R.drawable.ic_format_strikethrough_black_24dp},
            {R.string.tmaid_markdown_quote, R.drawable.ic_format_quote_black_24dp},
            {R.string.tmaid_markdown_h1, R.drawable.format_header_1},
            {R.string.tmaid_markdown_h2, R.drawable.format_header_2},
            {R.string.tmaid_markdown_h3, R.drawable.format_header_3},
            {R.string.tmaid_markdown_ul, R.drawable.ic_list_black_24dp},
            {R.string.tmaid_markdown_ol, R.drawable.ic_format_list_numbered_black_24dp},
    };

    private class MarkdownTextModuleActionsImpl implements View.OnClickListener, View.OnLongClickListener {
        private int _action;

        MarkdownTextModuleActionsImpl(int action) {
            _action = action;
        }

        @Override
        public void onClick(View v) {
            switch (_action) {
                case R.string.tmaid_markdown_quote: {
                    runMarkdownRegularPrefixAction("> ");
                    break;
                }
                case R.string.tmaid_markdown_h1: {
                    runMarkdownRegularPrefixAction("# ");
                    break;
                }
                case R.string.tmaid_markdown_h2: {
                    runMarkdownRegularPrefixAction("## ");
                    break;
                }
                case R.string.tmaid_markdown_h3: {
                    runMarkdownRegularPrefixAction("### ");
                    break;
                }
                case R.string.tmaid_markdown_ul: {
                    runMarkdownRegularPrefixAction("- ");
                    break;
                }
                case R.string.tmaid_markdown_ol: {
                    runMarkdownRegularPrefixAction("1. ");
                    break;
                }
                case R.string.tmaid_markdown_bold: {
                    runMarkdownInlineAction("**");
                    break;
                }
                case R.string.tmaid_markdown_italic: {
                    runMarkdownInlineAction("_");
                    break;
                }
                case R.string.tmaid_markdown_strikeout: {
                    runMarkdownInlineAction("~~");
                    break;
                }
                case R.string.tmaid_markdown_code_inline: {
                    runMarkdownInlineAction("`");
                    break;
                }
                case R.string.tmaid_markdown_horizontal_line: {
                    runMarkdownInlineAction("----\n");
                    break;
                }
                case R.string.tmaid_general_delete_lines: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_DELETE_LINES);
                    break;
                }
                case R.string.tmaid_general_open_link_browser: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_OPEN_LINK_BROWSER);
                    break;
                }
                case R.string.tmaid_general_special_key: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_SPECIAL_KEY);
                    break;
                }
                case R.string.tmaid_markdown_insert_link:
                case R.string.tmaid_markdown_insert_image: {
                    showInsertImageOrLinkDialog(_action == R.string.tmaid_markdown_insert_image ? 2 : 3);
                    break;
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            switch (_action) {
                case R.string.tmaid_general_open_link_browser: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_SEARCH);
                    return true;
                }
                case R.string.tmaid_general_special_key: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_JUMP_BOTTOM_TOP);
                    return true;
                }
                case R.string.tmaid_markdown_insert_image: {
                    int pos = _hlEditor.getSelectionStart();
                    _hlEditor.getText().insert(pos, "<img style=\"width:auto;max-height: 256px;\" src=\"\" />");
                    _hlEditor.setSelection(pos + 48);
                    return true;
                }
            }
            return false;
        }

        private void runMarkdownRegularPrefixAction(String action) {
            if (_hlEditor.hasSelection()) {
                String text = _hlEditor.getText().toString();
                int selectionStart = _hlEditor.getSelectionStart();
                int selectionEnd = _hlEditor.getSelectionEnd();

                //Check if Selection includes the shortcut characters
                if (text.substring(selectionStart, selectionEnd)
                        .matches("(>|#{1,3}|-|[1-9]\\.)(\\s)?[a-zA-Z0-9\\s]*")) {

                    text = text.substring(selectionStart + action.length(), selectionEnd);
                    _hlEditor.getText()
                            .replace(selectionStart, selectionEnd, text);

                }
                //Check if Selection is Preceded by shortcut characters
                else if ((selectionStart >= action.length()) && (text.substring(selectionStart - action.length(), selectionEnd)
                        .matches("(>|#{1,3}|-|[1-9]\\.)(\\s)?[a-zA-Z0-9\\s]*"))) {

                    text = text.substring(selectionStart, selectionEnd);
                    _hlEditor.getText()
                            .replace(selectionStart - action.length(), selectionEnd, text);

                }
                //Condition to insert shortcut preceding the selection
                else {
                    _hlEditor.getText().insert(selectionStart, action);
                }
            } else {
                //Condition for Empty Selection. Should insert the action at the start of the line
                int cursor = _hlEditor.getSelectionStart();
                int i = cursor - 1;
                Editable s = _hlEditor.getText();
                for (; i >= 0; i--) {
                    if (s.charAt(i) == '\n') {
                        break;
                    }
                }

                s.insert(i + 1, action);
            }
        }


        private void runMarkdownInlineAction(String _action) {
            if (_hlEditor.hasSelection()) {
                String text = _hlEditor.getText().toString();
                int selectionStart = _hlEditor.getSelectionStart();
                int selectionEnd = _hlEditor.getSelectionEnd();

                //Check if Selection includes the shortcut characters
                if (selectionEnd < text.length() && selectionStart >= 0 && (text.substring(selectionStart, selectionEnd)
                        .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                    text = text.substring(selectionStart + _action.length(),
                            selectionEnd - _action.length());
                    _hlEditor.getText()
                            .replace(selectionStart, selectionEnd, text);

                }
                //Check if Selection is Preceded and succeeded by shortcut characters
                else if (((selectionEnd <= (_hlEditor.length() - _action.length())) &&
                        (selectionStart >= _action.length())) &&
                        (text.substring(selectionStart - _action.length(),
                                selectionEnd + _action.length())
                                .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                    text = text.substring(selectionStart, selectionEnd);
                    _hlEditor.getText()
                            .replace(selectionStart - _action.length(),
                                    selectionEnd + _action.length(), text);

                }
                //Condition to insert shortcut preceding and succeeding the selection
                else {
                    _hlEditor.getText().insert(selectionStart, _action);
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), _action);
                }
            } else {
                //Condition for Empty Selection
                if (false) {
                    // Condition for things that should only be placed at the start of the line even if no text is selected
                } else if (_action.equals("----\n")) {
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), _action);
                } else {
                    // Condition for formatting which is inserted on either side of the cursor
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), _action)
                            .insert(_hlEditor.getSelectionEnd(), _action);
                    _hlEditor.setSelection(_hlEditor.getSelectionStart() - _action.length());
                }
            }
        }

    }

    private static final Pattern LINK_PATTERN = Pattern.compile("(?m)\\[(.*?)\\]\\((.*?)\\)");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("(?m)!\\[(.*?)\\]\\((.*?)\\)");

    @SuppressWarnings("RedundantCast")
    private void showInsertImageOrLinkDialog(int action) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
        final View view = _activity.getLayoutInflater().inflate(R.layout.select_path_dialog, (ViewGroup) null);
        final EditText editPathName = view.findViewById(R.id.ui__select_path_dialog__name);
        final EditText editPathUrl = view.findViewById(R.id.ui__select_path_dialog__url);
        final Button buttonBrowseFs = view.findViewById(R.id.ui__select_path_dialog__browse_filesystem);
        final Button buttonPictureGallery = view.findViewById(R.id.ui__select_path_dialog__gallery_picture);
        final Button buttonPictureCamera = view.findViewById(R.id.ui__select_path_dialog__camera_picture);
        final Button buttonPictureEdit = view.findViewById(R.id.ui__select_path_dialog__edit_picture);

        int startCursorPos = _hlEditor.getSelectionStart();
        if (_hlEditor.hasSelection()) {
            String selected_text = _hlEditor.getText().subSequence(
                    _hlEditor.getSelectionStart(),
                    _hlEditor.getSelectionEnd()
            ).toString();
            editPathName.setText(selected_text);
        } else if (_hlEditor.getText().toString().isEmpty()) {
            editPathName.setText("");
        } else {
            Editable contentText = _hlEditor.getText();
            int lineStartidx = Math.max(startCursorPos, 0);
            int lineEndidx = Math.min(startCursorPos, contentText.length() - 1);
            lineStartidx = Math.min(lineEndidx, lineStartidx);
            for (; lineStartidx > 0; lineStartidx--) {
                if (contentText.charAt(lineStartidx) == '\n') {
                    break;
                }
            }
            for (; lineEndidx < contentText.length(); lineEndidx++) {
                if (contentText.charAt(lineEndidx) == '\n') {
                    break;
                }
            }

            String line = contentText.subSequence(lineStartidx, lineEndidx).toString();
            Matcher m = (action == 3 ? LINK_PATTERN : IMAGE_PATTERN).matcher(line);
            if (m.find() && startCursorPos > lineStartidx + m.start() && startCursorPos < m.end() + lineStartidx) {
                int stat = lineStartidx + m.start();
                int en = lineStartidx + m.end();
                _hlEditor.setSelection(stat, en);
                editPathName.setText(m.group(1));
                editPathUrl.setText((m.group(2)));
            }
        }

        final String formatTemplate = action == 3 ? "[%s](%s)" : "![%s](%s)";
        int actionTitle = R.string.select;
        if (action == 3) {
            actionTitle = R.string.insert_link;
            buttonPictureCamera.setVisibility(View.GONE);
            buttonPictureGallery.setVisibility(View.GONE);
            buttonPictureEdit.setVisibility(View.GONE);
        } else if (action == 2) {
            actionTitle = R.string.insert_image;
        }

        // Inserts path relative if inside savedir, else absolute. asks to copy file if not in savedir
        final FilesystemDialogData.SelectionListener fsListener = new FilesystemDialogData.SelectionListenerAdapter() {
            @Override
            public void onFsSelected(final String request, final File file) {
                final String saveDir = _appSettings.getNotebookDirectoryAsStr();
                String text = null;
                if (file.getAbsolutePath().startsWith(saveDir) && _document.getFile().getAbsolutePath().startsWith(saveDir)) {
                    text = FileUtils.relativePath(_document.getFile(), file);
                } else {
                    new AlertDialog.Builder(_activity)
                            .setTitle(R.string.import_)
                            .setMessage(R.string.file_not_in_save_path_do_import_notice__appspecific)
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                                        File targetCopy = new File(_document.getFile().getParentFile(), file.getName());
                                        if (FileUtils.copyFile(file, targetCopy)) {
                                            onFsSelected(request, targetCopy);
                                        }
                                    }
                            ).create().show();
                }
                if (text == null) {
                    text = file.getAbsolutePath();
                }

                editPathUrl.setText(text);

                if (editPathName.getText().toString().isEmpty()) {
                    text = file.getName();
                    text = text.contains(".") ? text.substring(0, text.lastIndexOf('.')) : text;
                    editPathName.setText(text);
                }
            }

            @Override
            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                if (_document != null && _document.getFile() != null) {
                    opt.rootFolder = _document.getFile().getParentFile();
                }
            }
        };

        // Request camera / gallery picture button handling
        ShareUtil shu = new ShareUtil(_activity);
        final BroadcastReceiver lbr = shu.receiveResultFromLocalBroadcast((intent, lbr_ref) -> {
                    fsListener.onFsSelected("pic", new File(intent.getStringExtra(ShareUtil.EXTRA_FILEPATH)));
                },
                false, ShareUtil.REQUEST_CAMERA_PICTURE + "", ShareUtil.REQUEST_PICK_PICTURE + "");
        File targetFolder = _document.getFile() != null ? _document.getFile().getParentFile() : _appSettings.getNotebookDirectory();
        buttonPictureCamera.setOnClickListener(button -> shu.requestCameraPicture(targetFolder));
        buttonPictureGallery.setOnClickListener(button -> shu.requestGalleryPicture());

        buttonBrowseFs.setOnClickListener(button -> {
            if (getActivity() instanceof AppCompatActivity) {
                AppCompatActivity a = (AppCompatActivity) getActivity();
                FilesystemDialogCreator.showFileDialog(fsListener, a.getSupportFragmentManager(), a);
            }
        });

        buttonPictureEdit.setOnClickListener(v -> {
            String filepath = editPathUrl.getText().toString().replace("%20", " ");
            if (!filepath.startsWith("/")) {
                filepath = new File(_document.getFile().getParent(), filepath).getAbsolutePath();
            }
            File file = new File(filepath);
            if (file.exists() && file.isFile()) {
                shu.requestPictureEdit(file);
            }
        });

        builder.setView(view)
                .setTitle(actionTitle)
                .setOnDismissListener(dialog -> {
                    LocalBroadcastManager.getInstance(_context).unregisterReceiver(lbr);
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (_hlEditor.hasSelection()) {
                            _hlEditor.setSelection(startCursorPos);
                        }
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String title = editPathName.getText().toString().replace(")", "\\)");
                        String url = editPathUrl.getText().toString().replace(")", "\\)")
                                .replace(" ", "%20");  // Workaround for parser - cannot deal with spaces and have other entities problems
                        if (_hlEditor.hasSelection()) {
                            _hlEditor.getText().replace(_hlEditor.getSelectionStart(),
                                    _hlEditor.getSelectionEnd(),
                                    String.format(formatTemplate, title, url));
                            _hlEditor.setSelection(_hlEditor.getSelectionStart());
                        } else {
                            _hlEditor.getText().insert(_hlEditor.getSelectionStart(),
                                    String.format(formatTemplate, title, url));
                        }
                    }
                });
        builder.show();
    }
}
