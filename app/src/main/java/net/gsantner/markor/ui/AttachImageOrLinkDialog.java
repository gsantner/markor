package net.gsantner.markor.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.core.util.Function;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import net.gsantner.markor.R;
import net.gsantner.markor.format.markdown.MarkdownHighlighterPattern;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.ui.FilesystemViewerData;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.regex.Matcher;

public class AttachImageOrLinkDialog {


    @SuppressWarnings("RedundantCast")
    public static Dialog showInsertImageOrLinkDialog(int action, Activity _activity, HighlightingEditor _hlEditor, final File currentWorkingFile) {
        final AppSettings _appSettings = new AppSettings(_activity.getApplicationContext());
        final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(_activity);
        final View view = _activity.getLayoutInflater().inflate(R.layout.select_path_dialog, (ViewGroup) null);
        final EditText editPathName = view.findViewById(R.id.ui__select_path_dialog__name);
        final EditText editPathUrl = view.findViewById(R.id.ui__select_path_dialog__url);
        final Button buttonBrowseFs = view.findViewById(R.id.ui__select_path_dialog__browse_filesystem);
        final Button buttonPictureGallery = view.findViewById(R.id.ui__select_path_dialog__gallery_picture);
        final Button buttonPictureCamera = view.findViewById(R.id.ui__select_path_dialog__camera_picture);
        final Button buttonPictureEdit = view.findViewById(R.id.ui__select_path_dialog__edit_picture);

        final int startCursorPos = _hlEditor.getSelectionStart();
        if (_hlEditor.hasSelection()) {
            String selected_text = _hlEditor.getText().subSequence(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd()).toString();
            editPathName.setText(selected_text);
        } else if (_hlEditor.getText().toString().isEmpty()) {
            editPathName.setText("");
        } else {
            final Editable contentText = _hlEditor.getText();
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

            final String line = contentText.subSequence(lineStartidx, lineEndidx).toString();
            Matcher m = (action == 3 ? MarkdownHighlighterPattern.ACTION_LINK_PATTERN : MarkdownHighlighterPattern.ACTION_IMAGE_PATTERN).pattern.matcher(line);
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
        final FilesystemViewerData.SelectionListener fsListener = new FilesystemViewerData.SelectionListenerAdapter() {
            @Override
            public void onFsViewerSelected(final String request, final File file) {
                final String saveDir = _appSettings.getNotebookDirectoryAsStr();
                String text = null;
                if (file.getAbsolutePath().startsWith(saveDir) && currentWorkingFile.getAbsolutePath().startsWith(saveDir)) {
                    text = FileUtils.relativePath(currentWorkingFile, file);
                } else {
                    new AlertDialog.Builder(_activity)
                            .setTitle(R.string.import_)
                            .setMessage(R.string.file_not_in_save_path_do_import_notice__appspecific)
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                                        File targetCopy = new File(currentWorkingFile.getParentFile(), file.getName());
                                        if (FileUtils.copyFile(file, targetCopy)) {
                                            onFsViewerSelected(request, targetCopy);
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
            public void onFsViewerConfig(FilesystemViewerData.Options opt) {
                if (currentWorkingFile != null) {
                    opt.rootFolder = currentWorkingFile.getParentFile();
                }
            }
        };

        // Request camera / gallery picture button handling
        final ShareUtil shu = new ShareUtil(_activity);
        final BroadcastReceiver lbr = shu.receiveResultFromLocalBroadcast((intent, lbr_ref) -> {
                    fsListener.onFsViewerSelected("pic", new File(intent.getStringExtra(ShareUtil.EXTRA_FILEPATH)));
                },
                false, ShareUtil.REQUEST_CAMERA_PICTURE + "", ShareUtil.REQUEST_PICK_PICTURE + "");
        final File targetFolder = currentWorkingFile != null ? currentWorkingFile.getParentFile() : _appSettings.getNotebookDirectory();
        buttonPictureCamera.setOnClickListener(button -> shu.requestCameraPicture(targetFolder));
        buttonPictureGallery.setOnClickListener(button -> shu.requestGalleryPicture());

        buttonBrowseFs.setOnClickListener(button -> {
            if (_activity instanceof AppCompatActivity) {
                AppCompatActivity a = (AppCompatActivity) _activity;
                Function<File, Boolean> f = action == 3 ? null : FilesystemViewerFactory.IsMimeImage;
                FilesystemViewerFactory.showFileDialog(fsListener, a.getSupportFragmentManager(), _activity, f);
            }
        });

        buttonPictureEdit.setOnClickListener(v -> {
            String filepath = editPathUrl.getText().toString().replace("%20", " ");
            if (!filepath.startsWith("/")) {
                filepath = new File(currentWorkingFile.getParent(), filepath).getAbsolutePath();
            }
            File file = new File(filepath);
            if (file.exists() && file.isFile()) {
                shu.requestPictureEdit(file);
            }
        });

        builder.setView(view)
                .setTitle(actionTitle)
                .setOnDismissListener(dialog -> {
                    LocalBroadcastManager.getInstance(_activity).unregisterReceiver(lbr);
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    if (_hlEditor.hasSelection()) {
                        _hlEditor.setSelection(startCursorPos);
                    }
                })
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    String title = editPathName.getText().toString().replace(")", "\\)");
                    String url = editPathUrl.getText().toString().replace(")", "\\)")
                            .replace(" ", "%20");  // Workaround for parser - cannot deal with spaces and have other entities problems
                    if (_hlEditor.hasSelection()) {
                        _hlEditor.getText().replace(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd(), String.format(formatTemplate, title, url));
                        _hlEditor.setSelection(_hlEditor.getSelectionStart());
                    } else {
                        _hlEditor.getText().insert(_hlEditor.getSelectionStart(), String.format(formatTemplate, title, url));
                    }
                });
        return builder.show();
    }
}
