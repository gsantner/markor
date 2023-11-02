/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2023 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.markor.frontend;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.GsAudioRecordOmDialog;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.util.regex.Matcher;

public class AttachLinkOrFileDialog {
    public final static int IMAGE_ACTION = 2, FILE_OR_LINK_ACTION = 3, AUDIO_ACTION = 4;

    private static String getImageFormat(final int textFormatId) {
        if (textFormatId == FormatRegistry.FORMAT_MARKDOWN) {
            return "![TITLE](LINK)";
        } else if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
            return "{{LINK}}";
        } else if (textFormatId == FormatRegistry.FORMAT_ASCIIDOC) {
            return "image::LINK[\"TITLE\"]";
        } else {
            return "<img style='width:auto;max-height:256px;' alt='TITLE' src='LINK' />";
        }
    }

    private static String getLinkFormat(final int textFormatId) {
        if (textFormatId == FormatRegistry.FORMAT_MARKDOWN) {
            return "[TITLE](LINK)";
        } else if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
            return "{{LINK|TITLE}}";
        } else if (textFormatId == FormatRegistry.FORMAT_ASCIIDOC) {
            return "link:LINK[TITLE]";
        } else {
            return "<a href=\"LINK\">TITLE</a>";
        }
    }

    private static String getAudioFormat(final int textFormatId) {
        return "<audio src='LINK' controls><a href='LINK'>TITLE</a></audio>";
    }

    public static void showInsertImageOrLinkDialog(
            final int action,
            final int textFormatId,
            final Activity activity,
            final Editable edit,
            final File currentFile
    ) {
        final int[] sel = TextViewUtils.getSelection(edit);

        final androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.select_path_dialog, null);
        final EditText inputPathName = view.findViewById(R.id.ui__select_path_dialog__name);
        final EditText inputPathUrl = view.findViewById(R.id.ui__select_path_dialog__url);
        final Button buttonBrowseFilesystem = view.findViewById(R.id.ui__select_path_dialog__browse_filesystem);
        final Button buttonPictureGallery = view.findViewById(R.id.ui__select_path_dialog__gallery_picture);
        final Button buttonPictureCamera = view.findViewById(R.id.ui__select_path_dialog__camera_picture);
        final Button buttonPictureEdit = view.findViewById(R.id.ui__select_path_dialog__edit_picture);
        final Button buttonAudioRecord = view.findViewById(R.id.ui__select_path_dialog__record_audio);

        // Extract filepath if using Markdown
        if (textFormatId == FormatRegistry.FORMAT_MARKDOWN) {
            if (sel[0] > 0 && sel[1] > 0 && sel[0] != sel[1]) {
                inputPathName.setText(edit.subSequence(sel[0], sel[1]));
            } else if (edit.length() == 0) {
                inputPathName.setText("");
            } else {
                final int[] lineSel = TextViewUtils.getLineSelection(edit, sel);
                final String line = edit.subSequence(lineSel[0], lineSel[1]).toString();
                final Matcher m;
                if (action == IMAGE_ACTION) {
                    m = MarkdownSyntaxHighlighter.ACTION_IMAGE_PATTERN.matcher(line);
                } else if (action == FILE_OR_LINK_ACTION) {
                    m = MarkdownSyntaxHighlighter.ACTION_LINK_PATTERN.matcher(line);
                } else {
                    m = null;
                }
                if (m != null && m.find()) {
                    inputPathName.setText(m.group(1));
                    inputPathUrl.setText((m.group(2)));
                    sel[0] = m.start() + lineSel[0];
                    sel[1] = m.end() + lineSel[0];
                }
            }
        }

        // Create dialog first as we need a ref to it
        final AlertDialog dialog = builder.setView(view).create();

        // Helper func
        final GsCallback.a1<InsertType> _insertItem = (type) -> insertItem(type, textFormatId, activity, edit, currentFile, sel, dialog);

        // Setup all the various choices
        final InsertType browseType, okType;
        if (action == IMAGE_ACTION) {
            buttonPictureCamera.setVisibility(View.VISIBLE);
            buttonPictureGallery.setVisibility(View.VISIBLE);
            buttonPictureEdit.setVisibility(View.VISIBLE);
            dialog.setTitle(R.string.insert_image);
            browseType = InsertType.IMAGE_BROWSE;
            okType = InsertType.IMAGE_DIALOG;
        } else if (action == AUDIO_ACTION) {
            dialog.setTitle(R.string.audio);
            buttonAudioRecord.setVisibility(View.VISIBLE);
            browseType = InsertType.AUDIO_BROWSE;
            okType = InsertType.AUDIO_DIALOG;
        } else {
            dialog.setTitle(R.string.insert_link);
            browseType = InsertType.LINK_BROWSE;
            okType = InsertType.LINK_DIALOG;
        }

        final String ok = activity.getString(android.R.string.ok);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, ok, (di, b) -> _insertItem.callback(okType));
        buttonBrowseFilesystem.setOnClickListener(v -> _insertItem.callback(browseType));
        buttonPictureCamera.setOnClickListener(b -> _insertItem.callback(InsertType.IMAGE_CAMERA));
        buttonPictureGallery.setOnClickListener(v -> _insertItem.callback(InsertType.IMAGE_GALLERY));
        buttonAudioRecord.setOnClickListener(v -> _insertItem.callback(InsertType.AUDIO_RECORDING));
        buttonPictureEdit.setOnClickListener(v -> _insertItem.callback(InsertType.IMAGE_EDIT));

        dialog.show();
    }

    private enum InsertType {
        IMAGE_CAMERA,
        IMAGE_GALLERY,
        IMAGE_BROWSE,
        IMAGE_DIALOG,
        IMAGE_EDIT,
        AUDIO_RECORDING,
        AUDIO_BROWSE,
        AUDIO_DIALOG,
        LINK_BROWSE,
        LINK_DIALOG,
    }

    private static String getTemplateForAction(final InsertType action, final int textFormatId) {
        switch (action) {
            case IMAGE_CAMERA:
            case IMAGE_GALLERY:
            case IMAGE_EDIT:
            case IMAGE_DIALOG:
            case IMAGE_BROWSE: {
                return getImageFormat(textFormatId);
            }
            case AUDIO_RECORDING:
            case AUDIO_DIALOG:
            case AUDIO_BROWSE: {
                return getAudioFormat(textFormatId);
            }
            case LINK_DIALOG:
            case LINK_BROWSE:
            default: {
                return getLinkFormat(textFormatId);
            }
        }
    }

    private static GsCallback.b2<Context, File> getFilterForAction(final InsertType action) {
        switch (action) {
            case IMAGE_CAMERA:
            case IMAGE_GALLERY:
            case IMAGE_EDIT:
            case IMAGE_DIALOG:
            case IMAGE_BROWSE: {
                return MarkorFileBrowserFactory.IsMimeImage;
            }
            case AUDIO_RECORDING:
            case AUDIO_DIALOG:
            case AUDIO_BROWSE: {
                return MarkorFileBrowserFactory.IsMimeAudio;
            }
            case LINK_DIALOG:
            case LINK_BROWSE:
            default: {
                return null;
            }
        }
    }

    private static void insertItem(
            final InsertType action,
            final int textFormatId,
            final Activity activity,
            final Editable text,
            final File currentFile,
            @Nullable final int[] region,
            @Nullable AlertDialog dialog
    ) {
        final int[] sel;
        if (region != null && region.length > 1 && region[0] >= 0 && region[1] >= 0) {
            sel = region;
        } else {
            sel = TextViewUtils.getSelection(text);
        }

        final AppSettings _appSettings = ApplicationObject.settings();
        final File attachmentDir = _appSettings.getAttachmentFolder(currentFile);

        // Title, path to be written when the user hits accept
        final GsCallback.a2<String, String> insertLink = (title, path) -> {
            if (TextViewUtils.isNullOrEmpty(path)) {
                return;
            }

            title = title.trim().replace(")", "\\)");
            path = path.trim().replace(")", "\\)")
                    // Workaround for parser - cannot deal with spaces and have other entities problems
                    .replace(" ", "%20")
                    // Disable space encoding for Jekyll
                    .replace("{{%20site.baseurl%20}}", "{{ site.baseurl }}");

            String newText = getTemplateForAction(action, textFormatId)
                    .replace("TITLE", title)
                    .replace("LINK", path);

            if (textFormatId == FormatRegistry.FORMAT_WIKITEXT && newText.endsWith("|]]")) {
                newText = newText.replaceFirst("\\|]]$", "]]");
            }

            if (!newText.equals(text.subSequence(sel[0], sel[1]).toString())) {
                text.replace(sel[0], sel[1], newText);
            }

            if (dialog != null) {
                dialog.dismiss();
            }
        };

        final GsCallback.a1<File> insertFileLink = (file) -> {
            // If path is empty, unregister the broadcast (autoUnregister == true)
            if (TextUtils.isEmpty(file.toString())) {
                return;
            }

            // If path is not under notebook, copy it to the res folder
            if (!GsFileUtils.isChild(_appSettings.getNotebookDirectory(), file)) {
                final File local = GsFileUtils.findNonConflictingDest(attachmentDir, file.getName());
                attachmentDir.mkdirs();
                GsFileUtils.copyFile(file, local);
                file = local;
            }
            final String title = GsFileUtils.getFilenameWithoutExtension(file);
            final String path = GsFileUtils.relativePath(currentFile, file);
            insertLink.callback(title, path);
        };

        final MarkorContextUtils shu = new MarkorContextUtils(activity);
        final BroadcastReceiver br = shu.receiveResultFromLocalBroadcast(
                activity,
                (intent, _br) -> insertFileLink.callback(new File(intent.getStringExtra(MarkorContextUtils.EXTRA_FILEPATH))),
                true,
                "" + MarkorContextUtils.REQUEST_CAMERA_PICTURE,
                "" + MarkorContextUtils.REQUEST_PICK_PICTURE,
                "" + MarkorContextUtils.REQUEST_FILE_EDIT,
                "" + MarkorContextUtils.REQUEST_RECORD_AUDIO
        );

        final EditText nameEdit, pathEdit;
        if (dialog != null) {
            nameEdit = dialog.findViewById(R.id.ui__select_path_dialog__name);
            pathEdit = dialog.findViewById(R.id.ui__select_path_dialog__url);
            dialog.setOnDismissListener(d -> LocalBroadcastManager.getInstance(activity).unregisterReceiver(br));
        } else {
            nameEdit = pathEdit = null;
        }

        // Do each thing as necessary
        switch (action) {
            case IMAGE_CAMERA: {
                shu.requestCameraPicture(activity);
                break;
            }
            case IMAGE_GALLERY: {
                shu.requestGalleryPicture(activity);
                break;
            }
            case IMAGE_EDIT: {
                if (pathEdit != null) {
                    final String path = pathEdit.getText().toString().replace("%20", " ");

                    final File abs = new File(path).getAbsoluteFile();
                    if (abs.isFile()) {
                        shu.requestFileEdit(activity, abs);
                        break;
                    }

                    final File rel = new File(currentFile.getParentFile(), path).getAbsoluteFile();
                    if (rel.isFile()) {
                        shu.requestFileEdit(activity, rel);
                        break;
                    }
                }

                shu.requestFileEdit(activity, null);
                break;
            }
            case AUDIO_RECORDING: {
                if (!shu.requestAudioRecording(activity)) {
                    GsAudioRecordOmDialog.showAudioRecordDialog(activity, R.string.record_audio, insertFileLink);
                }
                break;
            }
            case LINK_BROWSE:
            case IMAGE_BROWSE:
            case AUDIO_BROWSE: {
                if (activity instanceof AppCompatActivity && nameEdit != null && pathEdit != null) {
                    final GsFileBrowserOptions.SelectionListener fsListener = new GsFileBrowserOptions.SelectionListenerAdapter() {
                        @Override
                        public void onFsViewerSelected(final String request, final File file, final Integer lineNumber) {
                            pathEdit.setText(GsFileUtils.relativePath(currentFile, file));

                            if (TextViewUtils.isNullOrEmpty(nameEdit.getText())) {
                                nameEdit.setText(GsFileUtils.getFilenameWithoutExtension(file));
                            }
                        }

                        @Override
                        public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                            dopt.rootFolder = currentFile.getParentFile();
                        }

                        @Override
                        public void onFsViewerDismissed() {
                            LocalBroadcastManager.getInstance(activity).unregisterReceiver(br);
                        }
                    };

                    final FragmentManager f = ((AppCompatActivity) activity).getSupportFragmentManager();
                    MarkorFileBrowserFactory.showFileDialog(fsListener, f, activity, getFilterForAction(action));
                }
                break;
            }
            case LINK_DIALOG:
            case AUDIO_DIALOG:
            case IMAGE_DIALOG: {
                if (nameEdit != null && pathEdit != null) {
                    insertLink.callback(nameEdit.getText().toString(), pathEdit.getText().toString());
                }
            }
        }
    }

    public static void insertCameraPhoto(
            final Activity activity,
            final int textFormatId,
            final Editable text,
            final File currentFile
    ) {
        insertItem(InsertType.IMAGE_CAMERA, textFormatId, activity, text, currentFile, null, null);
    }

    public static void insertGalleryPhoto(
            final Activity activity,
            final int textFormatId,
            final Editable text,
            final File currentFile
    ) {
        insertItem(InsertType.IMAGE_GALLERY, textFormatId, activity, text, currentFile, null, null);
    }

    public static void insertAudioRecording(
            final Activity activity,
            final int textFormatId,
            final Editable text,
            final File currentFile
    ) {
        insertItem(InsertType.AUDIO_RECORDING, textFormatId, activity, text, currentFile, null, null);
    }
}
