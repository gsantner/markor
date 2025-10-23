/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.markor.frontend;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.markdown.MarkdownActionButtons;
import net.gsantner.markor.format.wikitext.WikitextLinkResolver;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserListAdapter;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.util.GsContextUtils;
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
        } else if (textFormatId == FormatRegistry.FORMAT_ORGMODE) {
            return "#+CAPTION: %TITLE%\n[[file:%LINK%]]";
        } else {
            return "<img style='width:auto;max-height:256px;' alt='%TITLE%' src='%LINK%' />";
        }
    }

    private static String getLinkFormat(final int textFormatId) {
        if (textFormatId == FormatRegistry.FORMAT_MARKDOWN) {
            return "[%TITLE%](%LINK%)";
        } else if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
            return "[[%LINK%|%TITLE%]]";
        } else if (textFormatId == FormatRegistry.FORMAT_ASCIIDOC) {
            return "link:%LINK%[%TITLE%]";
        } else if (textFormatId == FormatRegistry.FORMAT_TODOTXT) {
            return "%TITLE% link:%LINK%";
        } else if (textFormatId == FormatRegistry.FORMAT_ORGMODE) {
            return "[[file:%LINK%][%TITLE%]]";
        } else {
            return "<a href=\"%LINK%\">%TITLE%</a>";
        }
    }

    private static String getAudioFormat(final int textFormatId) {
        if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
            return "[[%LINK%|%TITLE%]]";
        } else {
            return "<audio src='%LINK%' controls><a href='%LINK%'>%TITLE%</a></audio>";
        }
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
        final Button buttonSearch = view.findViewById(R.id.ui__select_path_dialog__search);
        final Button buttonPictureGallery = view.findViewById(R.id.ui__select_path_dialog__gallery_picture);
        final Button buttonPictureCamera = view.findViewById(R.id.ui__select_path_dialog__camera_picture);
        final Button buttonPictureEdit = view.findViewById(R.id.ui__select_path_dialog__edit_picture);

        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, (di, b) -> di.dismiss());

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
        final GsCallback.a1<InsertType> _insertItem = (type) -> fetchAndInsertItem(type, textFormatId, activity, edit, currentFile, sel, dialog);

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
            browseType = InsertType.AUDIO_BROWSE;
            okType = InsertType.AUDIO_DIALOG;
        } else {
            dialog.setTitle(R.string.insert_link);
            buttonSearch.setVisibility(View.VISIBLE);
            browseType = InsertType.LINK_BROWSE;
            okType = InsertType.LINK_DIALOG;
        }

        final String ok = activity.getString(android.R.string.ok);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, ok, (di, b) -> _insertItem.callback(okType));
        buttonBrowseFilesystem.setOnClickListener(v -> _insertItem.callback(browseType));
        buttonSearch.setOnClickListener(v -> _insertItem.callback(InsertType.LINK_SEARCH));
        buttonPictureCamera.setOnClickListener(b -> _insertItem.callback(InsertType.IMAGE_CAMERA));
        buttonPictureGallery.setOnClickListener(v -> _insertItem.callback(InsertType.IMAGE_GALLERY));
        buttonPictureEdit.setOnClickListener(v -> _insertItem.callback(InsertType.IMAGE_EDIT));

        dialog.show();
        final Window win = dialog.getWindow();
        if (win != null) {
            win.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            inputPathName.requestFocus();
        }
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

    private static File copyFileToAttachmentDir(final File attachment, final File attachmentDir) {
        final File local = GsFileUtils.findNonConflictingDest(attachmentDir, attachment.getName());
        attachmentDir.mkdirs();
        GsFileUtils.copyFile(attachment, local);
        return local;
    }

    private static String setupFileAttachment(
            final int textFormatId,
            final File attachment,
            final File document,
            final AppSettings as
    ) {
        final File notebookDir = as.getNotebookDirectory();

        String path = "";
        if (textFormatId == FormatRegistry.FORMAT_WIKITEXT) {
            final boolean shouldDynamicallyDetermineRoot = as.isWikitextDynamicNotebookRootEnabled();
            path = WikitextLinkResolver.resolveSystemFilePath(attachment, notebookDir, document, shouldDynamicallyDetermineRoot);
            if (path.startsWith("/")) {
                final File attachmentDir = WikitextLinkResolver.findAttachmentDir(document);
                final File local = copyFileToAttachmentDir(attachment, attachmentDir);
                path = WikitextLinkResolver.resolveSystemFilePath(local, notebookDir, document, shouldDynamicallyDetermineRoot);
            }
        } else {
            if (!GsFileUtils.isChild(notebookDir, attachment)) {
                final File attachmentDir = as.getAttachmentFolder(document);
                final File local = copyFileToAttachmentDir(attachment, attachmentDir);
                path = GsFileUtils.relativePath(document, local);
            } else {
                path = GsFileUtils.relativePath(document, attachment);
            }
        }

        // Remove trailing slashes if any
        path = path.replaceAll("/+$", "");

        return path;
    }

    public static String makeAttachmentLink(final int textFormatId, final String title, final File attachment, final File document) {
        final String path = setupFileAttachment(textFormatId, attachment, document, AppSettings.get(null));
        final boolean isImage = GsFileUtils.getMimeType(attachment).contains("image");
        return formatLink(title, path, textFormatId, isImage ? InsertType.IMAGE_DIALOG : InsertType.LINK_DIALOG);
    }

    private static void fetchAndInsertItem(
            final InsertType action,
            final int textFormatId,
            final Activity activity,
            final Editable edit,
            final File currentFile,
            @Nullable final int[] region,
            @Nullable AlertDialog dialog
    ) {
        final int[] sel;
        if (region != null && region.length > 1 && region[0] >= 0 && region[1] >= 0) {
            sel = region;
        } else {
            sel = TextViewUtils.getSelection(edit);
        }

        final AppSettings as = AppSettings.get(activity);

        // Title, path to be written when the user hits accept
        final GsCallback.a2<String, String> insertLink = (title, path) -> {
            if (GsTextUtils.isNullOrEmpty(path)) {
                return;
            }

            final String newText = formatLink(title, path, textFormatId, action);

            if (!newText.equals(edit.subSequence(sel[0], sel[1]).toString())) {
                edit.replace(sel[0], sel[1], newText);
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
        final long hash = GsFileUtils.crc32(edit);

        final GsCallback.a1<String> insertFileLink = (path) -> {
            if (GsFileUtils.crc32(edit) != hash) {
                return;
            }

            // If path is not under notebook, copy it to the res folder
            final File attachment = new File(path);

            // Pull the appropriate title
            String title = "";
            if (nameEdit != null) {
                title = nameEdit.getText().toString();
            }

            if (GsTextUtils.isNullOrEmpty(title)) {
                title = GsFileUtils.getFilenameWithoutExtension(attachment);
            }

            final String localPath = setupFileAttachment(textFormatId, attachment, currentFile, as);

            insertLink.callback(title, localPath);
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

                final File notebookDir = as.getNotebookDirectory();
                final boolean shouldDynamicallyDetermineRoot = as.isWikitextDynamicNotebookRootEnabled();
                pathEdit.setText(WikitextLinkResolver.resolveSystemFilePath(file, notebookDir, currentFile, shouldDynamicallyDetermineRoot));

                if (GsTextUtils.isNullOrEmpty(nameEdit.getText())) {
                    nameEdit.setText(pathEdit.getText());
                }
            } else {
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
                    Toast.makeText(activity, "❌", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case LINK_BROWSE:
            case IMAGE_BROWSE:
            case AUDIO_BROWSE: {
                if (activity instanceof AppCompatActivity && nameEdit != null && pathEdit != null) {
                    final GsFileBrowserOptions.SelectionListener fsListener = new GsFileBrowserOptions.SelectionListenerAdapter() {
                        GsFileBrowserOptions.Options _dopt = null;

                        @Override
                        public void onFsViewerSelected(final String request, final File file, final Integer lineNumber) {
                            setFields.callback(file);
                        }

                        @Override
                        public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                            dopt.startFolder = currentFile.getParentFile();
                            dopt.rootFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT;

                            if (action == InsertType.LINK_BROWSE) {
                                dopt.neutralButtonText = R.string.folder;
                            }

                            _dopt = dopt;
                        }

                        @Override
                        public void onFsViewerNeutralButtonPressed(File currentFolder) {
                            setFields.callback(currentFolder);
                            if (_dopt != null) {
                                _dopt.dialogInterface.dismiss();
                                ;
                            }
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
                MarkorDialogFactory.showNotebookFilterDialog(activity, null, null, (file, l) -> {
                    setFields.callback(file);
                });
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
            final Editable edit,
            final File currentFile
    ) {
        fetchAndInsertItem(InsertType.IMAGE_CAMERA, textFormatId, activity, edit, currentFile, null, null);
    }

    public static void insertGalleryPhoto(
            final Activity activity,
            final int textFormatId,
            final Editable edit,
            final File currentFile
    ) {
        fetchAndInsertItem(InsertType.IMAGE_GALLERY, textFormatId, activity, edit, currentFile, null, null);
    }

    public static void insertAudioRecording(
            final Activity activity,
            final int textFormatId,
            final Editable edit,
            final File currentFile
    ) {
        fetchAndInsertItem(InsertType.AUDIO_RECORDING, textFormatId, activity, edit, currentFile, null, null);
    }
}
