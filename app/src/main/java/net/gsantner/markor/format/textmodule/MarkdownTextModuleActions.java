package net.gsantner.markor.format.textmodule;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import net.gsantner.markor.R;
import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.ui.FilesystemDialogData;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownTextModuleActions extends TextModuleActions {

    public MarkdownTextModuleActions(HighlightingEditor contentEditor, Document document, Activity activity) {
        super(contentEditor, document, activity);
    }

    @Override
    public void appendTextModuleActionsToBar(ViewGroup barLayout) {
        if (AppSettings.get().isEditor_ShowTextmoduleBar() && barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            // Smart Actions
            for (int[] actions : KEYBOARD_SMART_ACTIONS_ICON) {
                appendTextModuleActionToBar(barLayout, actions[0], new KeyboardSmartActionsListener(KEYBOARD_SMART_ACTIONS[actions[1]]));
            }

            // Extra actions
            for (int[] actions : KEYBOARD_EXTRA_ACTIONS_ICONS) {
                appendTextModuleActionToBar(barLayout, actions[0], new KeyboardExtraActionsListener(actions[1]));
            }

            // Regular actions
            for (int[] actions : KEYBOARD_REGULAR_ACTIONS_ICONS) {
                appendTextModuleActionToBar(barLayout, actions[0], new KeyboardRegularActionListener(KEYBOARD_REGULAR_ACTIONS[actions[1]]));
            }
        } else if (!AppSettings.get().isEditor_ShowTextmoduleBar()) {
            setBarVisible(barLayout, false);
        }
    }

    //
    //
    //

    private static final int[][] KEYBOARD_REGULAR_ACTIONS_ICONS = {
            {R.drawable.format_blockquote, 0}, {R.drawable.format_header_1, 1},
            {R.drawable.format_header_2, 2}, {R.drawable.format_header_3, 3},
            {R.drawable.format_list_bulleted, 4}, {R.drawable.format_list_numbers, 5}
    };
    private static final String[] KEYBOARD_REGULAR_ACTIONS = {"> ", "# ", "## ", "### ", "- ", "1. "};

    private class KeyboardRegularActionListener implements View.OnClickListener {
        String _action;

        KeyboardRegularActionListener(String action) {
            _action = action;
        }

        @Override
        public void onClick(View v) {

            if (_contentEditor.hasSelection()) {
                String text = _contentEditor.getText().toString();
                int selectionStart = _contentEditor.getSelectionStart();
                int selectionEnd = _contentEditor.getSelectionEnd();

                //Check if Selection includes the shortcut characters
                if (text.substring(selectionStart, selectionEnd)
                        .matches("(>|#{1,3}|-|[1-9]\\.)(\\s)?[a-zA-Z0-9\\s]*")) {

                    text = text.substring(selectionStart + _action.length(), selectionEnd);
                    _contentEditor.getText()
                            .replace(selectionStart, selectionEnd, text);

                }
                //Check if Selection is Preceded by shortcut characters
                else if ((selectionStart >= _action.length()) && (text.substring(selectionStart - _action.length(), selectionEnd)
                        .matches("(>|#{1,3}|-|[1-9]\\.)(\\s)?[a-zA-Z0-9\\s]*"))) {

                    text = text.substring(selectionStart, selectionEnd);
                    _contentEditor.getText()
                            .replace(selectionStart - _action.length(), selectionEnd, text);

                }
                //Condition to insert shortcut preceding the selection
                else {
                    _contentEditor.getText().insert(selectionStart, _action);
                }
            } else {
                //Condition for Empty Selection. Should insert the action at the start of the line
                int cursor = _contentEditor.getSelectionStart();
                int i = cursor - 1;
                Editable s = _contentEditor.getText();
                for (; i >= 0; i--) {
                    if (s.charAt(i) == '\n') {
                        break;
                    }
                }

                s.insert(i + 1, _action);
            }
        }
    }

    //
    //
    //

    private static final int[][] KEYBOARD_SMART_ACTIONS_ICON = {
            {R.drawable.format_bold, 0}, {R.drawable.format_italic, 1},
            {R.drawable.format_strikethrough, 2}, {R.drawable.format_code, 3},
            {R.drawable.format_horizontal_line, 4}
    };
    private static final String[] KEYBOARD_SMART_ACTIONS = {"**", "_", "~~", "`", "----\n"};

    private class KeyboardSmartActionsListener implements View.OnClickListener {
        String _action;

        KeyboardSmartActionsListener(String action) {
            _action = action;
        }

        @Override
        public void onClick(View v) {
            if (_contentEditor.hasSelection()) {
                String text = _contentEditor.getText().toString();
                int selectionStart = _contentEditor.getSelectionStart();
                int selectionEnd = _contentEditor.getSelectionEnd();

                //Check if Selection includes the shortcut characters
                if (selectionEnd < text.length() && (text.substring(selectionStart, selectionEnd)
                        .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                    text = text.substring(selectionStart + _action.length(),
                            selectionEnd - _action.length());
                    _contentEditor.getText()
                            .replace(selectionStart, selectionEnd, text);

                }
                //Check if Selection is Preceded and succeeded by shortcut characters
                else if (((selectionEnd <= (_contentEditor.length() - _action.length())) &&
                        (selectionStart >= _action.length())) &&
                        (text.substring(selectionStart - _action.length(),
                                selectionEnd + _action.length())
                                .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                    text = text.substring(selectionStart, selectionEnd);
                    _contentEditor.getText()
                            .replace(selectionStart - _action.length(),
                                    selectionEnd + _action.length(), text);

                }
                //Condition to insert shortcut preceding and succeeding the selection
                else {
                    _contentEditor.getText().insert(selectionStart, _action);
                    _contentEditor.getText().insert(_contentEditor.getSelectionEnd(), _action);
                }
            } else {
                //Condition for Empty Selection
                if (false) {
                    // Condition for things that should only be placed at the start of the line even if no text is selected
                } else if (_action.equals("----\n")) {
                    _contentEditor.getText().insert(_contentEditor.getSelectionStart(), _action);
                } else {
                    // Condition for formatting which is inserted on either side of the cursor
                    _contentEditor.getText().insert(_contentEditor.getSelectionStart(), _action)
                            .insert(_contentEditor.getSelectionEnd(), _action);
                    _contentEditor.setSelection(_contentEditor.getSelectionStart() - _action.length());
                }
            }
        }
    }

    //
    //
    //

    private static final int[][] KEYBOARD_EXTRA_ACTIONS_ICONS = {
            {R.drawable.format_link, 1}, {R.drawable.format_image, 2}
    };
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*)\\]\\((.*)\\)");

    private class KeyboardExtraActionsListener implements View.OnClickListener {
        int _action;

        KeyboardExtraActionsListener(int action) {
            _action = action;
        }

        @Override
        public void onClick(View view) {
            getAlertDialog(_action);
        }
    }

    @SuppressWarnings("RedundantCast")
    private void getAlertDialog(int action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = _activity.getLayoutInflater().inflate(R.layout.ui__select_path_dialog, (ViewGroup) null);
        final EditText editPathName = view.findViewById(R.id.ui__select_path_dialog__name);
        final EditText editPathUrl = view.findViewById(R.id.ui__select_path_dialog__url);
        final Button buttonBrowseFs = view.findViewById(R.id.ui__select_path_dialog__browse_filesystem);

        int startCursorPos = _contentEditor.getSelectionStart();
        if (_contentEditor.hasSelection()) {
            String selected_text = _contentEditor.getText().subSequence(
                    _contentEditor.getSelectionStart(),
                    _contentEditor.getSelectionEnd()
            ).toString();
            editPathName.setText(selected_text);
        } else if (_contentEditor.getText().toString().isEmpty()) {
            editPathName.setText("");
        } else {
            Editable contentText = _contentEditor.getText();
            int lineStartidx = Math.max(startCursorPos - 1, 0);
            int lineEndidx = Math.min(startCursorPos, contentText.length() - 1);
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
            Matcher m = LINK_PATTERN.matcher(line);
            if (m.find()) {
                int stat = lineStartidx + m.regionStart();
                int en = lineStartidx + m.regionEnd();
                _contentEditor.setSelection(stat, en);
                editPathName.setText(m.group(1));
                editPathUrl.setText((m.group(2)));
            }
        }

        final String formatTemplate = action == 1 ? "[%s](%s)" : "![%s](%s)";
        int actionTitle = R.string.select;
        if (action == 1) {
            actionTitle = R.string.insert_link;
        } else if (action == 2) {
            actionTitle = R.string.insert_image;
        }

        // Inserts path relative if inside savedir, else absolute. asks to copy file if not in savedir
        final FilesystemDialogData.SelectionListener fsListener = new FilesystemDialogData.SelectionListenerAdapter() {
            @Override
            public void onFsSelected(final String request, final File file) {
                final String saveDir = AppSettings.get().getSaveDirectory();
                String text = null;
                if (file.getAbsolutePath().startsWith(saveDir) && _document.getFile().getAbsolutePath().startsWith(saveDir)) {
                    text = FileUtils.relativePath(_document.getFile(), file);
                } else {
                    new AlertDialog.Builder(_activity)
                            .setTitle(R.string.import_)
                            .setMessage(R.string.file_not_in_save_path_do_import)
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
                // TODO: Set start/home folder
            }
        };

        buttonBrowseFs.setOnClickListener(button -> {
            if (getActivity() instanceof AppCompatActivity) {
                AppCompatActivity a = (AppCompatActivity) getActivity();
                FilesystemDialogCreator.showFileDialog(fsListener, a.getSupportFragmentManager(), a);
            }
        });

        builder.setView(view)
                .setTitle(actionTitle)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (_contentEditor.hasSelection()) {
                            _contentEditor.setSelection(startCursorPos);
                        }
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (_contentEditor.hasSelection()) {
                            _contentEditor.getText().replace(_contentEditor.getSelectionStart(),
                                    _contentEditor.getSelectionEnd(),
                                    String.format(formatTemplate, editPathName.getText().toString(),
                                            editPathUrl.getText().toString()));
                            _contentEditor.setSelection(_contentEditor.getSelectionStart());
                        } else {
                            _contentEditor.getText().insert(_contentEditor.getSelectionStart(),
                                    String.format(formatTemplate, editPathName.getText().toString(),
                                            editPathUrl.getText().toString()));
                        }
                    }
                });

        builder.show();
    }
}
