/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.markor.frontend;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.GsAudioRecordOmDialog;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsHashMap;

import java.io.File;
import java.util.regex.Matcher;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public class AttachLinkOrFileDialog {
    public final static int IMAGE_ACTION = 2, FILE_OR_LINK_ACTION = 3, AUDIO_ACTION = 4;

    @SuppressWarnings("RedundantCast")
    public static Dialog showInsertImageOrLinkDialog(final int action, final int textFormatId, final Activity activity, final HighlightingEditor _hlEditor, final File currentWorkingFile) {
        final AppSettings _appSettings = ApplicationObject.settings();
        final androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.select_path_dialog, (ViewGroup) null);
        final EditText inputPathName = view.findViewById(R.id.ui__select_path_dialog__name);
        final EditText inputPathUrl = view.findViewById(R.id.ui__select_path_dialog__url);
        final Button buttonBrowseFilesystem = view.findViewById(R.id.ui__select_path_dialog__browse_filesystem);
        final Button buttonPictureGallery = view.findViewById(R.id.ui__select_path_dialog__gallery_picture);
        final Button buttonPictureCamera = view.findViewById(R.id.ui__select_path_dialog__camera_picture);
        final Button buttonPictureEdit = view.findViewById(R.id.ui__select_path_dialog__edit_picture);
        final Button buttonAudioRecord = view.findViewById(R.id.ui__select_path_dialog__record_audio);

        final int startCursorPos = _hlEditor.getSelectionStart();
        buttonAudioRecord.setVisibility(action == AUDIO_ACTION ? View.VISIBLE : View.GONE);
        buttonPictureCamera.setVisibility(action == IMAGE_ACTION ? View.VISIBLE : View.GONE);
        buttonPictureGallery.setVisibility(action == IMAGE_ACTION ? View.VISIBLE : View.GONE);
        buttonPictureEdit.setVisibility(action == IMAGE_ACTION ? View.VISIBLE : View.GONE);
        final int actionTitle;
        final String formatTemplate;
        switch (action) {
            default:
            case FILE_OR_LINK_ACTION: {
                actionTitle = R.string.insert_link;
                formatTemplate = new GsHashMap<Integer, String>().load(
                        FormatRegistry.FORMAT_MARKDOWN, "[{{ template.title }}]({{ template.link }})",
                        FormatRegistry.FORMAT_ASCIIDOC, "link:{{ template.link }}[{{ template.title }}]",
                        FormatRegistry.FORMAT_WIKITEXT, "[[{{ template.link }}|{{ template.title }}]]"
                ).getOrDefault(textFormatId, "<a href='{{ template.link }}'>{{ template.title }}</a>");
                break;
            }
            case IMAGE_ACTION: {
                actionTitle = R.string.insert_image;
                formatTemplate = new GsHashMap<Integer, String>().load(
                        FormatRegistry.FORMAT_MARKDOWN, "![{{ template.title }}]({{ template.link }})",
                        FormatRegistry.FORMAT_ASCIIDOC, "image::{{ template.link }}[\"{{ template.title }}\"]",
                        FormatRegistry.FORMAT_WIKITEXT, "{{{{ template.link }}}}"
                ).getOrDefault(textFormatId, "<img style='width:auto;max-height: 256px;' alt='{{ template.title }}' src='{{ template.link }}' />");
                break;
            }
            case AUDIO_ACTION: {
                formatTemplate = "<audio src='{{ template.link }}' controls><a href='{{ template.link }}'>{{ template.title }}</a></audio>";
                actionTitle = R.string.audio;
                break;
            }

        }

        // Extract filepath if using Markdown
        if (textFormatId == FormatRegistry.FORMAT_MARKDOWN) {
            if (_hlEditor.hasSelection()) {
                String selected_text = "";
                try {
                    selected_text = _hlEditor.getText().subSequence(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd()).toString();
                } catch (Exception ignored) {
                }
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
                Matcher m = (action == FILE_OR_LINK_ACTION ? MarkdownSyntaxHighlighter.ACTION_LINK_PATTERN : MarkdownSyntaxHighlighter.ACTION_IMAGE_PATTERN).matcher(line);
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
        final GsFileBrowserOptions.SelectionListener fsListener = new GsFileBrowserOptions.SelectionListenerAdapter() {
            @Override
            public void onFsViewerSelected(final String request, final File file, final Integer lineNumber) {
                final String saveDir = _appSettings.getNotebookDirectoryAsStr();
                String text = null;
                boolean isInSaveDir = file.getAbsolutePath().startsWith(saveDir) && currentWorkingFile.getAbsolutePath().startsWith(saveDir);
                boolean isInCurrentDir = currentWorkingFile.getAbsolutePath().startsWith(file.getParentFile().getAbsolutePath());
                if (isInCurrentDir || isInSaveDir) {
                    text = GsFileUtils.relativePath(currentWorkingFile, file);
                } else if ("abs_if_not_relative".equals(request)) {
                    text = file.getAbsolutePath();
                } else {
                    String filename = file.getName();
                    if ("audio_record_om_dialog".equals(request)) {
                        filename = GsAudioRecordOmDialog.generateFilename(file).getName();
                    }
                    File targetCopy = new File(currentWorkingFile.getParentFile(), filename);
                    showCopyFileToDirDialog(activity, file, targetCopy, false, (cbRetValSuccess, cbRestValTargetFile) -> onFsViewerSelected("abs_if_not_relative", cbRestValTargetFile, null));
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
            public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                if (currentWorkingFile != null) {
                    dopt.rootFolder = currentWorkingFile.getParentFile();
                }
            }
        };

        // Request camera / gallery picture button handling
        final MarkorContextUtils shu = new MarkorContextUtils(activity);
        final BroadcastReceiver lbr = shu.receiveResultFromLocalBroadcast(activity, (intent, lbr_ref) -> {
                    fsListener.onFsViewerSelected("pic", new File(intent.getStringExtra(MarkorContextUtils.EXTRA_FILEPATH)), null);
                },
                false, MarkorContextUtils.REQUEST_CAMERA_PICTURE + "", MarkorContextUtils.REQUEST_PICK_PICTURE + "");
        final File targetFolder = currentWorkingFile != null ? currentWorkingFile.getParentFile() : _appSettings.getNotebookDirectory();
        buttonPictureCamera.setOnClickListener(button -> shu.requestCameraPicture(activity, targetFolder));
        buttonPictureGallery.setOnClickListener(button -> shu.requestGalleryPicture(activity));

        buttonBrowseFilesystem.setOnClickListener(button -> {
            if (activity instanceof AppCompatActivity) {
                AppCompatActivity a = (AppCompatActivity) activity;
                GsCallback.b2<Context, File> f = action == AUDIO_ACTION ? MarkorFileBrowserFactory.IsMimeAudio : (action == FILE_OR_LINK_ACTION ? null : MarkorFileBrowserFactory.IsMimeImage);
                MarkorFileBrowserFactory.showFileDialog(fsListener, a.getSupportFragmentManager(), activity, f);
            }
        });

        // Audio Record -> fs listener with arg file,"audio_record"
        buttonAudioRecord.setOnClickListener(v -> GsAudioRecordOmDialog.showAudioRecordDialog(activity, R.string.record_audio, cbValAudioRecordFilepath -> fsListener.onFsViewerSelected("audio_record_om_dialog", cbValAudioRecordFilepath, null)));

        buttonPictureEdit.setOnClickListener(v -> {
            String filepath = inputPathUrl.getText().toString().replace("%20", " ");
            if (!filepath.startsWith("/")) {
                filepath = new File(currentWorkingFile.getParent(), filepath).getAbsolutePath();
            }
            File file = new File(filepath);
            if (file.exists() && file.isFile()) {
                shu.requestPictureEdit(activity, file);
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
                    try {
                        String title = inputPathName.getText().toString().replace(")", "\\)");
                        String url = inputPathUrl.getText().toString().trim().replace(")", "\\)").replace(" ", "%20");  // Workaround for parser - cannot deal with spaces and have other entities problems
                        url = url.replace("{{%20site.baseurl%20}}", "{{ site.baseurl }}"); // Disable space encoding for Jekyll
                        String newText = formatTemplate.replace("{{ template.title }}", title).replace("{{ template.link }}", url);
                        if (textFormatId == FormatRegistry.FORMAT_WIKITEXT && newText.endsWith("|]]")) {
                            newText = newText.replaceFirst("\\|]]$", "]]");
                        }
                        if (_hlEditor.hasSelection()) {
                            _hlEditor.getText().replace(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd(), newText);
                            _hlEditor.setSelection(_hlEditor.getSelectionStart());
                        } else {
                            _hlEditor.getText().insert(_hlEditor.getSelectionStart(), newText);
                        }
                    } catch (Exception ignored) {
                    }
                });
        return builder.show();
    }

    public static Dialog showCopyFileToDirDialog(final Activity activity, final File srcFile, final File tarFile, boolean disableCancel, final GsCallback.a2<Boolean, File> copyFileFinishedCallback) {
        final GsCallback.a1<File> copyToDirInvocation = cbValTargetFile -> new MarkorContextUtils(activity).writeFile(activity, cbValTargetFile, false, (wfCbValOpened, wfCbValStream) -> {
            if (wfCbValOpened && GsFileUtils.copyFile(srcFile, wfCbValStream)) {
                copyFileFinishedCallback.callback(true, cbValTargetFile);
            }
        });

        final File tarFileInAssetsDir = new File(ApplicationObject.settings().getNotebookDirectory(), tarFile.getName());


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
