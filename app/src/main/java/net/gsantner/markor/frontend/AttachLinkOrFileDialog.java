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
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.GsAudioRecordOmDialog;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsHashMap;

import java.io.File;
import java.util.regex.Matcher;

public class AttachLinkOrFileDialog {
    public final static int IMAGE_ACTION = 2, FILE_OR_LINK_ACTION = 3, AUDIO_ACTION = 4;

    public static void showInsertImageOrLinkDialog(
            final int action,
            final int textFormatId,
            final Activity activity,
            final Editable text,
            final File currentFile
    ) {
        final int[] sel = TextViewUtils.getSelection(text);

        final AppSettings _appSettings = ApplicationObject.settings();
        final File attachmentDir = _appSettings.getAttachmentFolder(currentFile);

        final androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.select_path_dialog, (ViewGroup) null);
        final EditText inputPathName = view.findViewById(R.id.ui__select_path_dialog__name);
        final EditText inputPathUrl = view.findViewById(R.id.ui__select_path_dialog__url);
        final Button buttonBrowseFilesystem = view.findViewById(R.id.ui__select_path_dialog__browse_filesystem);
        final Button buttonPictureGallery = view.findViewById(R.id.ui__select_path_dialog__gallery_picture);
        final Button buttonPictureCamera = view.findViewById(R.id.ui__select_path_dialog__camera_picture);
        final Button buttonPictureEdit = view.findViewById(R.id.ui__select_path_dialog__edit_picture);
        final Button buttonAudioRecord = view.findViewById(R.id.ui__select_path_dialog__record_audio);

        final boolean isActionImage = action == IMAGE_ACTION;
        final boolean isActionAudio = action == AUDIO_ACTION;
        final boolean isActionLink = action == FILE_OR_LINK_ACTION;

        buttonAudioRecord.setVisibility(isActionAudio ? View.VISIBLE : View.GONE);
        buttonPictureCamera.setVisibility(isActionImage ? View.VISIBLE : View.GONE);
        buttonPictureGallery.setVisibility(isActionImage ? View.VISIBLE : View.GONE);
        buttonPictureEdit.setVisibility(isActionImage ? View.VISIBLE : View.GONE);

        final int actionTitle;
        final String formatTemplate;
        if (isActionImage) {
            actionTitle = R.string.insert_image;
            formatTemplate = new GsHashMap<Integer, String>().load(
                    FormatRegistry.FORMAT_MARKDOWN, "![{{ template.title }}]({{ template.link }})",
                    FormatRegistry.FORMAT_ASCIIDOC, "image::{{ template.link }}[\"{{ template.title }}\"]",
                    FormatRegistry.FORMAT_WIKITEXT, "{{{{ template.link }}}}"
            ).getOrDefault(textFormatId, "<img style='width:auto;max-height: 256px;' alt='{{ template.title }}' src='{{ template.link }}' />");
        } else if (isActionAudio) {
            formatTemplate = "<audio src='{{ template.link }}' controls><a href='{{ template.link }}'>{{ template.title }}</a></audio>";
            actionTitle = R.string.audio;
        } else {
            actionTitle = R.string.insert_link;
            formatTemplate = new GsHashMap<Integer, String>().load(
                    FormatRegistry.FORMAT_MARKDOWN, "[{{ template.title }}]({{ template.link }})",
                    FormatRegistry.FORMAT_ASCIIDOC, "link:{{ template.link }}[{{ template.title }}]",
                    FormatRegistry.FORMAT_WIKITEXT, "[[{{ template.link }}|{{ template.title }}]]"
            ).getOrDefault(textFormatId, "<a href='{{ template.link }}'>{{ template.title }}</a>");
        }

        // Extract filepath if using Markdown
        if (textFormatId == FormatRegistry.FORMAT_MARKDOWN) {
            if (sel[0] > 0 && sel[1] > 0 && sel[0] != sel[1]) {
                inputPathName.setText(text.subSequence(sel[0], sel[1]));
            } else if (text.length() == 0) {
                inputPathName.setText("");
            } else {
                final int[] lineSel = TextViewUtils.getLineSelection(text, sel);
                final String line = text.subSequence(lineSel[0], lineSel[1]).toString();
                final Matcher m;
                if (isActionImage) {
                    m = MarkdownSyntaxHighlighter.ACTION_IMAGE_PATTERN.matcher(line);
                } else {
                    m = MarkdownSyntaxHighlighter.ACTION_LINK_PATTERN.matcher(line);
                }
                if (m.find()) {
                    inputPathName.setText(m.group(1));
                    inputPathUrl.setText((m.group(2)));
                    sel[0] = m.start() + lineSel[0];
                    sel[1] = m.end() + lineSel[0];
                }
            }
        }

        // Source, dest to be written when the user hits accept
        final GsCallback.a2<String, String> insertLink = (title, path) -> {
            if (TextViewUtils.isNullOrEmpty(path)) {
                return;
            }

            String newText = formatTemplate
                    .replace("{{ template.title }}", title)
                    .replace("{{ template.link }}", path);

            if (textFormatId == FormatRegistry.FORMAT_WIKITEXT && newText.endsWith("|]]")) {
                newText = newText.replaceFirst("\\|]]$", "]]");
            }

            if (!text.subSequence(sel[0], sel[1]).equals(newText)) {
                text.replace(sel[0], sel[1], newText);
            }
        };

        builder.setView(view)
                .setTitle(actionTitle)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    final String title = inputPathName.getText().toString().trim().replace(")", "\\)");

                    final String path = inputPathUrl.getText().toString().trim()
                            .replace(")", "\\)")
                            .replace(" ", "%20")  // Workaround for parser - cannot deal with spaces and have other entities problems
                            .replace("{{%20site.baseurl%20}}", "{{ site.baseurl }}"); // Disable space encoding for Jekyll

                    insertLink.callback(title, path);
                });

        final AlertDialog dialog = builder.show();

        final GsCallback.a1<File> insertFileLink = (file) -> {
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
            dialog.dismiss();
        };

        // Inserts path relative if inside savedir, else absolute. asks to copy file if not in savedir
        final GsFileBrowserOptions.SelectionListener fsListener = new GsFileBrowserOptions.SelectionListenerAdapter() {
            @Override
            public void onFsViewerSelected(final String request, final File file, final Integer lineNumber) {
                inputPathUrl.setText(GsFileUtils.relativePath(currentFile, file));

                if (TextViewUtils.isNullOrEmpty(inputPathName.getText())) {
                    inputPathName.setText(GsFileUtils.getFilenameWithoutExtension(file));
                }
            }

            @Override
            public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                dopt.rootFolder = currentFile.getParentFile();
            }
        };

        final MarkorContextUtils shu = new MarkorContextUtils(activity);
        final BroadcastReceiver br = shu.receiveResultFromLocalBroadcast(
                activity,
                (intent, _br) -> insertFileLink.callback(new File(intent.getStringExtra(MarkorContextUtils.EXTRA_FILEPATH))),
                true,
                "" + MarkorContextUtils.REQUEST_CAMERA_PICTURE, "" + MarkorContextUtils.REQUEST_PICK_PICTURE
        );
        dialog.setOnDismissListener(d -> LocalBroadcastManager.getInstance(activity).unregisterReceiver(br));

        // Get picture from camera
        buttonPictureCamera.setOnClickListener(button -> shu.requestCameraPicture(activity, attachmentDir));

        // Get picture from gallery
        buttonPictureGallery.setOnClickListener(button -> shu.requestGalleryPicture(activity));

        // Browse filesystem
        buttonBrowseFilesystem.setOnClickListener(button -> {
            if (activity instanceof AppCompatActivity) {
                AppCompatActivity a = (AppCompatActivity) activity;
                GsCallback.b2<Context, File> f = isActionAudio ? MarkorFileBrowserFactory.IsMimeAudio : (isActionLink ? null : MarkorFileBrowserFactory.IsMimeImage);
                MarkorFileBrowserFactory.showFileDialog(fsListener, a.getSupportFragmentManager(), activity, f);
            }
        });

        // Audio Record
        buttonAudioRecord.setOnClickListener(v -> GsAudioRecordOmDialog.showAudioRecordDialog(
                activity, R.string.record_audio, insertFileLink
        ));

        // Edit picture
        buttonPictureEdit.setOnClickListener(v -> {
            String filepath = inputPathUrl.getText().toString().replace("%20", " ");
            if (!filepath.startsWith("/")) {
                filepath = new File(currentFile.getParent(), filepath).getAbsolutePath();
            }
            File file = new File(filepath);
            if (file.exists() && file.isFile()) {
                shu.requestPictureEdit(activity, file);
            }
        });
    }
}
