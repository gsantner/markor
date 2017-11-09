package net.gsantner.markor.editor.shortcut;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import net.gsantner.markor.R;
import net.gsantner.markor.editor.highlighter.HighlightingEditor;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EscMarkdown extends EditorShortcuts {

    public EscMarkdown(HighlightingEditor contentEditor, Document document, Activity activity) {
        super(contentEditor, document, activity);
    }

    @Override
    public void appendShortcutsToBar(ViewGroup barLayout) {
        if (AppSettings.get().isShowMarkdownShortcuts() && barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            // Smart Actions
            for (int[] actions : KEYBOARD_SMART_ACTIONS_ICON) {
                appendShortcutToBar(barLayout, actions[0], new KeyboardSmartActionsListener(KEYBOARD_SMART_ACTIONS[actions[1]]));
            }

            // Regular actions
            for (int[] actions : KEYBOARD_REGULAR_ACTIONS_ICONS) {
                appendShortcutToBar(barLayout, actions[0], new KeyboardRegularActionListener(KEYBOARD_REGULAR_ACTIONS[actions[1]]));
            }

            // Extra actions
            for (int[] actions : KEYBOARD_EXTRA_ACTIONS_ICONS) {
                appendShortcutToBar(barLayout, actions[0], new KeyboardExtraActionsListener(actions[1]));
            }
        } else if (!AppSettings.get().isShowMarkdownShortcuts()) {
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
                if ((text.substring(selectionStart, selectionEnd)
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

    private void getAlertDialog(int action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = _activity.getLayoutInflater().inflate(R.layout.format_dialog, (ViewGroup) null);

        final EditText linkName = view.findViewById(R.id.format_dialog_name);
        final EditText linkUrl = view.findViewById(R.id.format_dialog_url);
        linkName.setHint(_context.getString(R.string.format_dialog_name_hint));
        linkUrl.setHint(_context.getString(R.string.format_dialog_url_or_path_hint));
        int startCursorPos = _contentEditor.getSelectionStart();
        if (_contentEditor.hasSelection()) {
            String selected_text = _contentEditor.getText().subSequence(
                    _contentEditor.getSelectionStart(),
                    _contentEditor.getSelectionEnd()
            ).toString();
            linkName.setText(selected_text);
        } else {
            Editable contentText = _contentEditor.getText();
            int lineStartidx = Math.max(startCursorPos - 1, 0);
            int lineEndidx = Math.min(startCursorPos, contentText.length() - 1);
            for (; lineStartidx > 0; lineStartidx--) {
                if (contentText.charAt(lineStartidx) == '\n') {
                    break;
                }
            }
            for (; lineEndidx <= contentText.length(); lineEndidx++) {
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
                linkName.setText(m.group(1));
                linkUrl.setText((m.group(2)));
            }
        }

        String actionTitle = "";
        if (action == 1) {
            actionTitle = "Insert Link";
        } else if (action == 2) {
            actionTitle = "Insert Image";
        }
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
                                    String.format("[%s](%s)", linkName.getText().toString(),
                                            linkUrl.getText().toString()));
                            _contentEditor.setSelection(_contentEditor.getSelectionStart());
                        } else {
                            _contentEditor.getText().insert(_contentEditor.getSelectionStart(),
                                    String.format("[%s](%s)", linkName.getText().toString(),
                                            linkUrl.getText().toString()));
                        }
                    }
                });

        builder.show();
    }
}
