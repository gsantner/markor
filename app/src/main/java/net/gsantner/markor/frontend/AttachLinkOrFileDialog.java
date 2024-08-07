/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.markor.frontend;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.markdown.MarkdownActionButtons;
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.format.wikitext.WikitextLinkResolver;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.filesearch.FileSearchDialog;
import net.gsantner.markor.frontend.filesearch.FileSearchEngine;
import net.gsantner.markor.frontend.filesearch.FileSearchResultSelectorDialog;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.GsAudioRecordOmDialog;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;

public class AttachLinkOrFileDialog {
    public final static int IMAGE_ACTION = 2, FILE_OR_LINK_ACTION = 3, AUDIO_ACTION = 4;

    private static String getImageFormat(final int textFormatId) {
        if (textFormatId == FormatRegistry.FORMAT_MARKDOWN) {
            return "![%TITLE%](%LINK%)";
        } else if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
            return "{{%LINK%}}";
        } else if (textFormatId == FormatRegistry.FORMAT_ASCIIDOC) {
            return "image::%LINK%[\"%TITLE%\"]";
        } else {
            return "<img style='width:auto;max-height:256px;' alt='%TITLE%' src='%LINK%' />";
        }
    }

    private static String getLinkFormat(final int textFormatId) {
        if (textFormatId == FormatRegistry.FORMAT_MARKDOWN) {
            return "[%TITLE%](%LINK%)";
        } else if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
            return "[[LINK|TITLE]]";
        } else if (textFormatId == FormatRegistry.FORMAT_ASCIIDOC) {
            return "link:%LINK%[%TITLE%]";
        } else if (textFormatId == FormatRegistry.FORMAT_TODOTXT) {
            return "%TITLE% link:%LINK%";
        } else {
            return "<a href=\"%LINK%\">%TITLE%</a>";
        }
    }

    private static String getAudioFormat(final int textFormatId) {
        if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
            return "[[LINK|TITLE]]";
        }
        return "<audio src='%LINK%' controls><a href='%LINK%'>%TITLE%</a></audio>";
    }

    public static void showInsertImageOrLinkDialog(
            final int action,
            final int textFormatId,
            final Activity activity,
            final Editable edit,
            final File currentFile
    ) {
        final int[] sel = TextViewUtils.getSelection(edit);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_DayNight_Dialog_Rounded);
        final View view = activity.getLayoutInflater().inflate(R.layout.select_path_dialog, null);
        final EditText inputPathName = view.findViewById(R.id.ui__select_path_dialog__name);
        final EditText inputPathUrl = view.findViewById(R.id.ui__select_path_dialog__url);
        final Button buttonBrowseFilesystem = view.findViewById(R.id.ui__select_path_dialog__browse_filesystem);
        final Button buttonSelectSpecial = view.findViewById(R.id.ui__select_path_dialog__select_special);
        final Button buttonSearch = view.findViewById(R.id.ui__select_path_dialog__search);
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
                final MarkdownActionButtons.Link link = MarkdownActionButtons.Link.extract(edit, sel[0]);
                final boolean isImage = action == IMAGE_ACTION && link.isImage;
                final boolean isLink = action == FILE_OR_LINK_ACTION && !link.isImage;
                if (link.isValid() && (isImage || isLink)) {
                    inputPathName.setText(link.title);
                    inputPathUrl.setText(link.link);
                    sel[0] = link.start;
                    sel[1] = link.end;
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
            buttonSelectSpecial.setVisibility(View.VISIBLE);
            buttonSearch.setVisibility(View.VISIBLE);
            browseType = InsertType.LINK_BROWSE;
            okType = InsertType.LINK_DIALOG;
        }

        final String ok = activity.getString(android.R.string.ok);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, ok, (di, b) -> _insertItem.callback(okType));
        buttonBrowseFilesystem.setOnClickListener(v -> _insertItem.callback(browseType));
        buttonSelectSpecial.setOnClickListener(v -> _insertItem.callback(InsertType.LINK_SPECIAL));
        buttonSearch.setOnClickListener(v -> _insertItem.callback(InsertType.LINK_SEARCH));
        buttonPictureCamera.setOnClickListener(b -> _insertItem.callback(InsertType.IMAGE_CAMERA));
        buttonPictureGallery.setOnClickListener(v -> _insertItem.callback(InsertType.IMAGE_GALLERY));
        buttonAudioRecord.setOnClickListener(v -> _insertItem.callback(InsertType.AUDIO_RECORDING));
        buttonPictureEdit.setOnClickListener(v -> _insertItem.callback(InsertType.IMAGE_EDIT));

        dialog.show();
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
        LINK_SPECIAL,
        LINK_SEARCH
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
            case LINK_SPECIAL:
            case LINK_SEARCH:
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
            case LINK_SPECIAL:
            case LINK_SEARCH:
            default: {
                return null;
            }
        }
    }

    public static String formatLink(String title, String path, final int textFormatId) {
        return formatLink(title, path, textFormatId, InsertType.LINK_DIALOG);
    }

    private static String formatLink(String title, String path, final int textFormatId, final InsertType action) {
        title = title.trim().replace("|", "/");

        path = path.trim()
                // Workaround for parser - cannot deal with spaces and have other entities problems
                .replace(" ", "%20")
                // Disable space encoding for Jekyll
                .replace("{{%20site.baseurl%20}}", "{{ site.baseurl }}");

        String newText = getTemplateForAction(action, textFormatId)
                .replace("%TITLE%", title)
                .replace("%LINK%", path);

        if (textFormatId == FormatRegistry.FORMAT_WIKITEXT && newText.endsWith("|]]")) {
            newText = newText.replaceFirst("\\|]]$", "]]");
        }

        if (textFormatId == FormatRegistry.FORMAT_TODOTXT) {
            newText = newText.replaceAll("\\n", " ");
        }

        return newText;
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
            if (GsTextUtils.isNullOrEmpty(path)) {
                return;
            }

            final String newText = formatLink(title, path, textFormatId, action);

            if (!newText.equals(text.subSequence(sel[0], sel[1]).toString())) {
                text.replace(sel[0], sel[1], newText);
            }

            if (dialog != null) {
                dialog.dismiss();
            }
        };

        // Pull dialog elements
        final EditText nameEdit, pathEdit;
        if (dialog != null) {
            nameEdit = dialog.findViewById(R.id.ui__select_path_dialog__name);
            pathEdit = dialog.findViewById(R.id.ui__select_path_dialog__url);
        } else {
            nameEdit = pathEdit = null;
        }

        // Defensive checks to make sure file has not changed
        // Can happen if the callback is triggered after a long delay
        final long hash = GsFileUtils.crc32(text);

        final GsCallback.a1<String> insertFileLink = (path) -> {
            if (GsFileUtils.crc32(text) != hash) {
                return;
            }

            // If path is not under notebook, copy it to the res folder
            File file = new File(path);

            if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
                final File notebookDir = _appSettings.getNotebookDirectory();
                final boolean shouldDynamicallyDetermineRoot = _appSettings.isWikitextDynamicNotebookRootEnabled();
                path = WikitextLinkResolver.resolveSystemFilePath(file, notebookDir, currentFile, shouldDynamicallyDetermineRoot);
                if (path.startsWith("/")) {
                    final File zimAttachmentDir = WikitextLinkResolver.findAttachmentDir(currentFile);
                    final File local = GsFileUtils.findNonConflictingDest(zimAttachmentDir, file.getName());
                    zimAttachmentDir.mkdirs();
                    GsFileUtils.copyFile(file, local);
                    file = local;
                    path = WikitextLinkResolver.resolveSystemFilePath(file, notebookDir, currentFile, shouldDynamicallyDetermineRoot);
                }
                insertLink.callback(path, path);
            } else {
                if (!GsFileUtils.isChild(_appSettings.getNotebookDirectory(), file)) {
                    final File local = GsFileUtils.findNonConflictingDest(attachmentDir, file.getName());
                    attachmentDir.mkdirs();
                    GsFileUtils.copyFile(file, local);
                    file = local;
                }

                // Pull the appropriate title
                String title = "";
                if (nameEdit != null) {
                    title = nameEdit.getText().toString();
                }

                if (GsTextUtils.isNullOrEmpty(title)) {
                    title = GsFileUtils.getFilenameWithoutExtension(file);
                }
                insertLink.callback(title, GsFileUtils.relativePath(currentFile, file));
            }
        };

        final MarkorContextUtils cu = new MarkorContextUtils(activity);

        final GsCallback.a1<File> setFields = file -> {
            if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
                // About the Zim's window 'Insert Link', where it is possible to browse for a file to select,
                // Zim defaults, for the first time, to the file link's path to set the description when it's
                // considered empty.  Then, Zim will automatically replace a description with the path of the
                // next selection only if the description had been already automatically set, or manually set
                // before switching the file, to the path of the current selection.  Zim will not replace the
                // description that had been manually set to the path of a future selection, after exchanging
                // that file.  Nor Zim will replace an empty description if this happens after the first time
                // a link is inserted.  Here, for clarity, always replace an empty description, or one set to
                // the path of the current selection, with the path of the next selection.
                if (nameEdit.getText().toString().equals(pathEdit.getText().toString())) {
                    nameEdit.setText("");
                }

                final File notebookDir = _appSettings.getNotebookDirectory();
                final boolean shouldDynamicallyDetermineRoot = _appSettings.isWikitextDynamicNotebookRootEnabled();
                pathEdit.setText(WikitextLinkResolver.resolveSystemFilePath(file, notebookDir, currentFile, shouldDynamicallyDetermineRoot));

                if (GsTextUtils.isNullOrEmpty(nameEdit.getText())) {
                    nameEdit.setText(pathEdit.getText());
                }
            }  else {
                if (pathEdit != null) {
                    pathEdit.setText(GsFileUtils.relativePath(currentFile, file));
                }
                if (nameEdit != null && GsTextUtils.isNullOrEmpty(nameEdit.getText())) {
                    nameEdit.setText(GsFileUtils.getNameWithoutExtension(file.getName()));
                }
            }
        };

        // Do each thing as necessary
        switch (action) {
            case IMAGE_CAMERA: {
                cu.requestCameraPicture(activity, insertFileLink);
                break;
            }
            case IMAGE_GALLERY: {
                cu.requestGalleryPicture(activity, insertFileLink);
                break;
            }
            case IMAGE_EDIT: {
                if (pathEdit != null) {
                    final String path = pathEdit.getText().toString().replace("%20", " ");

                    final File abs = new File(path).getAbsoluteFile();
                    if (abs.isFile()) {
                        cu.requestFileEdit(activity, abs);
                        break;
                    }

                    final File currentDir = (textFormatId == FormatRegistry.FORMAT_WIKITEXT) ? WikitextLinkResolver.findAttachmentDir(currentFile) : currentFile.getParentFile();
                    final File rel = new File(currentDir, path).getAbsoluteFile();
                    if (rel.isFile()) {
                        cu.requestFileEdit(activity, rel);
                    }
                }
                break;
            }
            case AUDIO_RECORDING: {
                if (!cu.requestAudioRecording(activity, insertFileLink)) {
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
                            setFields.callback(file);
                        }

                        @Override
                        public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                            dopt.rootFolder = currentFile.getParentFile();
                        }
                    };

                    final FragmentManager f = ((AppCompatActivity) activity).getSupportFragmentManager();
                    MarkorFileBrowserFactory.showFileDialog(fsListener, f, activity, getFilterForAction(action));
                }
                break;
            }
            case LINK_SPECIAL: {
                MarkorDialogFactory.showSelectSpecialFileDialog(activity, setFields);
                break;
            }
            case LINK_SEARCH: {
                final File nb = _appSettings.getNotebookDirectory();
                final FileSearchDialog.Options options = new FileSearchDialog.Options();
                options.enableSearchInContent = false;
                options.searchLocation = R.string.notebook;
                if (!FileSearchEngine.isSearchExecuting.get()) {
                    FileSearchDialog.showDialog(activity, options, searchOptions -> {
                        searchOptions.rootSearchDir = nb;
                        FileSearchEngine.queueFileSearch(activity, searchOptions, searchResults ->
                                FileSearchResultSelectorDialog.showDialog(activity, searchResults, (file, line, isLong) ->
                                        setFields.callback(new File(nb, file))));
                    });
                }
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
