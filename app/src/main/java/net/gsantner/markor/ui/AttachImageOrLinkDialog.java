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
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.markdown.MarkdownHighlighterPattern;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.ui.AudioRecordOmDialog;
import net.gsantner.opoc.ui.FilesystemViewerData;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.regex.Matcher;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public class AttachImageOrLinkDialog {
    @SuppressWarnings("RedundantCast")
    public static Dialog showInsertImageOrLinkDialog(final int action, final int textFormatId, final Activity activity, final HighlightingEditor _hlEditor, final File currentWorkingFile) {
        final AppSettings _appSettings = new AppSettings(activity.getApplicationContext());
        final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.select_path_dialog, (ViewGroup) null);
        final EditText inputPathName = view.findViewById(R.id.ui__select_path_dialog__name);
        final EditText inputPathUrl = view.findViewById(R.id.ui__select_path_dialog__url);
        final Button buttonBrowseFilesystem = view.findViewById(R.id.ui__select_path_dialog__browse_filesystem);
        final Button buttonPictureGallery = view.findViewById(R.id.ui__select_path_dialog__gallery_picture);
        final Button buttonPictureCamera = view.findViewById(R.id.ui__select_path_dialog__camera_picture);
        final Button buttonPictureEdit = view.findViewById(R.id.ui__select_path_dialog__edit_picture);
        final Button buttonAudioRecord = view.findViewById(R.id.ui__select_path_dialog__record_audio);

        final int startCursorPos = _hlEditor.getSelectionStart();
        buttonAudioRecord.setVisibility(action == 4 ? View.VISIBLE : View.GONE);
        buttonPictureCamera.setVisibility(action == 2 ? View.VISIBLE : View.GONE);
        buttonPictureGallery.setVisibility(action == 2 ? View.VISIBLE : View.GONE);
        buttonPictureEdit.setVisibility(action == 2 ? View.VISIBLE : View.GONE);
        final int actionTitle;
        final String formatTemplate;
        switch (action) {
            default:
            case 3: { // file / link
                formatTemplate = (textFormatId == TextFormat.FORMAT_MARKDOWN ? "[{{ template.title }}]({{ template.link }})" : "<a href='{{ template.link }}'>{{ template.title }}</a>");
                actionTitle = R.string.insert_link;
                break;
            }
            case 2: { // image
                formatTemplate = (textFormatId == TextFormat.FORMAT_MARKDOWN ? "![{{ template.title }}]({{ template.link }})" : "<img style='width:auto;max-height: 256px;' alt='{{ template.title }}' src='{{ template.link }}' />");
                actionTitle = R.string.insert_image;
                break;
            }
            case 4: { // audio
                formatTemplate = "<audio src='{{ template.link }}' controls><a href='{{ template.link }}'>{{ template.title }}</a></audio>";
                actionTitle = R.string.audio;
                break;
            }

        }

        // Extract filepath if using Markdown
        if (textFormatId == TextFormat.FORMAT_MARKDOWN) {
            if (_hlEditor.hasSelection()) {
                String selected_text = _hlEditor.getText().subSequence(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd()).toString();
                inputPathName.setText(selected_text);
            } else if (_hlEditor.getText().toString().isEmpty()) {
                inputPathName.setText("");
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
                    inputPathName.setText(m.group(1));
                    inputPathUrl.setText((m.group(2)));
                }
            }
        }


        // Inserts path relative if inside savedir, else absolute. asks to copy file if not in savedir
        final FilesystemViewerData.SelectionListener fsListener = new FilesystemViewerData.SelectionListenerAdapter() {
            @Override
            public void onFsViewerSelected(final String request, final File file) {
                final String saveDir = _appSettings.getNotebookDirectoryAsStr();
                String text = null;
                boolean isInSaveDir = file.getAbsolutePath().startsWith(saveDir) && currentWorkingFile.getAbsolutePath().startsWith(saveDir);
                boolean isInCurrentDir = currentWorkingFile.getAbsolutePath().startsWith(file.getParentFile().getAbsolutePath());
                if (isInCurrentDir || isInSaveDir) {
                    text = FileUtils.relativePath(currentWorkingFile, file);
                } else if ("abs_if_not_relative".equals(request)) {
                    text = file.getAbsolutePath();
                } else {
                    String filename = file.getName();
                    if ("audio_record_om_dialog".equals(request)) {
                        filename = AudioRecordOmDialog.generateFilename(file).getName();
                    }
                    File targetCopy = new File(currentWorkingFile.getParentFile(), filename);
                    showCopyFileToDirDialog(activity, file, targetCopy, false, (cbRetValSuccess, cbRestValTargetFile) -> onFsViewerSelected("abs_if_not_relative", cbRestValTargetFile));
                }
                if (text == null) {
                    text = file.getAbsolutePath();
                }

                inputPathUrl.setText(text);

                if (inputPathName.getText().toString().isEmpty()) {
                    text = file.getName();
                    text = text.contains(".") ? text.substring(0, text.lastIndexOf('.')) : text;
                    inputPathName.setText(text);
                }
                text = inputPathUrl.getText().toString();
                try {
                    if (text.startsWith("../assets/") && currentWorkingFile.getParentFile().getName().equals("_posts")) {
                        text = "{{ site.baseurl }}" + text.substring(2);
                        inputPathUrl.setText(text);
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                if (currentWorkingFile != null) {
                    dopt.rootFolder = currentWorkingFile.getParentFile();
                }
            }
        };

        // Request camera / gallery picture button handling
        final ShareUtil shu = new ShareUtil(activity);
        final BroadcastReceiver lbr = shu.receiveResultFromLocalBroadcast((intent, lbr_ref) -> {
                    fsListener.onFsViewerSelected("pic", new File(intent.getStringExtra(ShareUtil.EXTRA_FILEPATH)));
                },
                false, ShareUtil.REQUEST_CAMERA_PICTURE + "", ShareUtil.REQUEST_PICK_PICTURE + "");
        final File targetFolder = currentWorkingFile != null ? currentWorkingFile.getParentFile() : _appSettings.getNotebookDirectory();
        buttonPictureCamera.setOnClickListener(button -> shu.requestCameraPicture(targetFolder));
        buttonPictureGallery.setOnClickListener(button -> shu.requestGalleryPicture());

        buttonBrowseFilesystem.setOnClickListener(button -> {
            if (activity instanceof AppCompatActivity) {
                AppCompatActivity a = (AppCompatActivity) activity;
                Function<File, Boolean> f = action == 4 ? FilesystemViewerCreator.IsMimeAudio : (action == 3 ? null : FilesystemViewerCreator.IsMimeImage);
                FilesystemViewerCreator.showFileDialog(fsListener, a.getSupportFragmentManager(), activity, f);
            }
        });

        // Audio Record -> fs listener with arg file,"audio_record"
        buttonAudioRecord.setOnClickListener(v -> AudioRecordOmDialog.showAudioRecordDialog(activity, R.string.record_audio, cbValAudioRecordFilepath -> fsListener.onFsViewerSelected("audio_record_om_dialog", cbValAudioRecordFilepath)));

        buttonPictureEdit.setOnClickListener(v -> {
            String filepath = inputPathUrl.getText().toString().replace("%20", " ");
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
                .setOnDismissListener(dialog -> LocalBroadcastManager.getInstance(activity).unregisterReceiver(lbr))
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    if (_hlEditor.hasSelection()) {
                        _hlEditor.setSelection(startCursorPos);
                    }
                })
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    String title = inputPathName.getText().toString().replace(")", "\\)");
                    String url = inputPathUrl.getText().toString().replace(")", "\\)").replace(" ", "%20");  // Workaround for parser - cannot deal with spaces and have other entities problems
                    url = url.replace("{{%20site.baseurl%20}}", "{{ site.baseurl }}"); // Disable space encoding for Jekyll
                    String newText = formatTemplate.replace("{{ template.title }}", title).replace("{{ template.link }}", url);
                    if (_hlEditor.hasSelection()) {
                        _hlEditor.getText().replace(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd(), newText);
                        _hlEditor.setSelection(_hlEditor.getSelectionStart());
                    } else {
                        _hlEditor.getText().insert(_hlEditor.getSelectionStart(), newText);
                    }
                });
        return builder.show();
    }

    public static Dialog showCopyFileToDirDialog(final Activity activity, final File srcFile, final File tarFile, boolean disableCancel, final Callback.a2<Boolean, File> copyFileFinishedCallback) {
        final Callback.a1<File> copyToDirInvocation = cbValTargetFile -> new ShareUtil(activity).writeFile(cbValTargetFile, false, (wfCbValOpened, wfCbValStream) -> {
            if (wfCbValOpened && FileUtils.copyFile(srcFile, wfCbValStream)) {
                copyFileFinishedCallback.callback(true, cbValTargetFile);
            }
        });

        final File tarFileInAssetsDir = new File(new AppSettings(activity).getNotebookDirectory(), tarFile.getName());


        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                .setTitle(R.string.copy_file)
                .setMessage(R.string.file_not_in_current_folder_do_copy__appspecific)
                .setPositiveButton(R.string.current, (dialogInterface, which) -> copyToDirInvocation.callback(tarFile))
                .setNeutralButton(R.string.notebook, (dialogInterface, which) -> copyToDirInvocation.callback(tarFileInAssetsDir));
        if (disableCancel) {
            dialogBuilder.setCancelable(false);
        } else {
            dialogBuilder.setNegativeButton(android.R.string.no, null);
        }
        return dialogBuilder.show();
    }
}
