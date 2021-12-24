/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.ui.NewFileDialog;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.format.plaintext.PlainTextStuff;
import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.ui.FilesystemViewerAdapter;
import net.gsantner.opoc.ui.FilesystemViewerData;

import java.io.File;
import java.util.regex.Matcher;

import butterknife.BindView;
import butterknife.OnTextChanged;

public class DocumentShareIntoFragment extends GsFragmentBase {
    public static final String FRAGMENT_TAG = "DocumentShareIntoFragment";
    public static final String EXTRA_SHARED_TEXT = "EXTRA_SHARED_TEXT";
    private File workingDir;

    public static DocumentShareIntoFragment newInstance(Intent intent) {
        DocumentShareIntoFragment f = new DocumentShareIntoFragment();
        Bundle args = new Bundle();

        final String sharedText = formatLink(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT));

        Object intentFile = intent.getSerializableExtra(Document.EXTRA_PATH);
        if (intentFile != null && intent.getBooleanExtra(Document.EXTRA_PATH_IS_FOLDER, false)) {
            f.workingDir = (File) intentFile;
        }

        args.putString(EXTRA_SHARED_TEXT, sharedText);
        f.setArguments(args);
        return f;
    }

    @BindView(R.id.document__fragment__share_into__highlighting_editor)
    HighlightingEditor _hlEditor;

    private ShareIntoImportOptionsFragment _shareIntoImportOptionsFragment;

    public DocumentShareIntoFragment() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.document__fragment__share_into;
    }

    @Override
    public void onViewCreated(final @NonNull View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Context context = view.getContext();
        final AppSettings as = new AppSettings(context);
        final ContextUtils cu = new ContextUtils(context);
        cu.setAppLanguage(as.getLanguage());

        final String sharedText = (getArguments() != null ? getArguments().getString(EXTRA_SHARED_TEXT, "") : "").trim();
        view.setBackgroundColor(as.getBackgroundColor());
        if (_savedInstanceState == null) {
            FragmentTransaction t = getChildFragmentManager().beginTransaction();
            _shareIntoImportOptionsFragment = ShareIntoImportOptionsFragment.newInstance(sharedText);
            _shareIntoImportOptionsFragment.setWorkingDir(workingDir);
            t.replace(R.id.document__share_into__fragment__placeholder_fragment, _shareIntoImportOptionsFragment, ShareIntoImportOptionsFragment.TAG).commit();
        } else {
            _shareIntoImportOptionsFragment = (ShareIntoImportOptionsFragment) getChildFragmentManager().findFragmentByTag(ShareIntoImportOptionsFragment.TAG);
        }

        _hlEditor.setText(sharedText);
        _hlEditor.setBackgroundColor(ContextCompat.getColor(context, as.isDarkThemeEnabled() ? R.color.dark__background_2 : R.color.light__background_2));
        _hlEditor.setTextColor(ContextCompat.getColor(context, as.isDarkThemeEnabled() ? R.color.white : R.color.dark_grey));
        _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, as.getFontSize());
        _hlEditor.setTypeface(Typeface.create(as.getFontFamily(), Typeface.NORMAL));

        if (sharedText.isEmpty()) {
            _hlEditor.requestFocus();
        }
    }

    @OnTextChanged(value = R.id.document__fragment__share_into__highlighting_editor, callback = OnTextChanged.Callback.TEXT_CHANGED)
    public void onTextChanged(CharSequence text) {
        if (_shareIntoImportOptionsFragment != null) {
            _shareIntoImportOptionsFragment.setText(text.toString());
        }
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }


    public static class ShareIntoImportOptionsFragment extends GsPreferenceFragmentCompat<AppSettings> {
        public static final String TAG = "ShareIntoImportOptionsFragment";
        private static final String EXTRA_TEXT = Intent.EXTRA_TEXT;
        private File workingDir;

        @Override
        public boolean isDividerVisible() {
            return true;
        }

        public static ShareIntoImportOptionsFragment newInstance(String sharedText) {
            ShareIntoImportOptionsFragment f = new ShareIntoImportOptionsFragment();
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_TEXT, sharedText);
            f.setArguments(bundle);
            return f;
        }

        public void setWorkingDir(File dir) {
            workingDir = dir;
        }

        private String _sharedText = "";

        @Override
        public int getPreferenceResourceForInflation() {
            return R.xml.prefactions_share_into;
        }

        @Override
        public String getFragmentTag() {
            return TAG;
        }

        @Override
        protected AppSettings getAppSettings(Context context) {
            return new AppSettings(context);
        }

        @Override
        protected void afterOnCreate(Bundle savedInstances, Context context) {
            super.afterOnCreate(savedInstances, context);
            if (getArguments() != null) {
                _sharedText = getArguments().getString(EXTRA_TEXT, "");
            }
            if (savedInstances != null) {
                _sharedText = savedInstances.getString(EXTRA_TEXT, _sharedText);
            }
            doUpdatePreferences();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if ((_sharedText.length() * 1.05) < 8200) {
                outState.putString(EXTRA_TEXT, _sharedText);
            }
        }

        @Override
        public Integer getIconTintColor() {
            boolean dark = getAppSettings(getContext()).isDarkThemeEnabled();
            return _cu.rcolor(dark ? R.color.dark__primary_text : R.color.light__primary_text);
        }

        public void setText(String text) {
            _sharedText = text;
            if (isAdded()) {
                doUpdatePreferences();
            }
        }

        @SuppressWarnings("ConstantConditions")
        private void appendToExistingDocument(final File file, final String separator, final boolean showEditor) {
            final Bundle args = new Bundle();
            args.putSerializable(Document.EXTRA_PATH, file);
            args.putBoolean(Document.EXTRA_PATH_IS_FOLDER, false);
            final Context context = getContext();
            final Document document = Document.fromArguments(context, args);
            final String shareIntoFormat = ShareUtil.formatDateTime(context, _appSettings.getShareIntoPrefix(), System.currentTimeMillis());
            final boolean isTodoTxt = TextFormat.CONVERTER_TODOTXT.isFileOutOfThisFormat(file.getAbsolutePath());

            final String newContent = document.loadContent(context).replaceAll("(?m)^[\\r\\n]+|[\\r\\n]+$", "")
                    + separator
                    + (isTodoTxt ? _sharedText : formatOrPrefixSharedText(shareIntoFormat, _sharedText));
            document.saveContent(context, newContent);

            if (showEditor) {
                showInDocumentActivity(document);
            }
            _appSettings.addRecentDocument(file);
        }

        private void showAppendDialog(int keyId) {
            final File startFolder;
            switch (keyId) {
                case R.string.pref_key__favourite_files: {
                    startFolder = FilesystemViewerAdapter.VIRTUAL_STORAGE_FAVOURITE;
                    break;
                }
                case R.string.pref_key__popular_documents: {
                    startFolder = FilesystemViewerAdapter.VIRTUAL_STORAGE_POPULAR;
                    break;
                }
                case R.string.pref_key__recent_documents: {
                    startFolder = FilesystemViewerAdapter.VIRTUAL_STORAGE_RECENTS;
                    break;
                }
                default: {
                    startFolder = _appSettings.getNotebookDirectory();
                    break;
                }
            }
            FilesystemViewerCreator.showFileDialog(new FilesystemViewerData.SelectionListenerAdapter() {
                @Override
                public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                    dopt.rootFolder = startFolder;
                }

                @Override
                public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
                    appendToExistingDocument(file, "\n", true);
                }

            }, getFragmentManager(), getActivity(), FilesystemViewerCreator.IsMimeText);
        }


        private void createNewDocument() {
            FilesystemViewerCreator.showFolderDialog(new FilesystemViewerData.SelectionListenerAdapter() {
                @Override
                public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                    dopt.rootFolder = (workingDir == null) ? _appSettings.getNotebookDirectory() : workingDir;
                }

                @Override
                public void onFsViewerSelected(String request, File dir, final Integer lineNumber) {
                    NewFileDialog dialog = NewFileDialog.newInstance(dir, false, (ok, f) -> {
                        if (ok && f.isFile()) {
                            appendToExistingDocument(f, "\n", true);
                        }
                    });
                    dialog.show(getActivity().getSupportFragmentManager(), NewFileDialog.FRAGMENT_TAG);
                }
            }, getFragmentManager(), getActivity());
        }

        private void showInDocumentActivity(Document document) {
            if (getActivity() instanceof DocumentActivity) {
                DocumentActivity a = (DocumentActivity) getActivity();
                a.setDocument(document);
                a.showTextEditor(document, null, false, _appSettings.getDocumentPreviewState(document.getPath()), null);
            }
        }

        @Override
        @SuppressLint("NonConstantResourceId")
        @SuppressWarnings({"ConstantConditions", "ConstantIfStatement"})
        public Boolean onPreferenceClicked(Preference preference, String key, int keyId) {
            AppSettings appSettings = new AppSettings(getActivity().getApplicationContext());
            PermissionChecker permc = new PermissionChecker(getActivity());
            ShareUtil shu = new ShareUtil(getContext());
            String tmps;

            boolean close = false;
            switch (keyId) {
                case R.string.pref_key__share_into__clipboard: {
                    shu.setClipboard(_sharedText);
                    close = true;
                    break;
                }
                case R.string.pref_key__share_into__create_document: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        createNewDocument();
                    }
                    return true;
                }
                case R.string.pref_key__favourite_files:
                case R.string.pref_key__popular_documents:
                case R.string.pref_key__recent_documents:
                case R.string.pref_key__share_into__existing_document: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        showAppendDialog(keyId);
                    }
                    return true;
                }
                case R.string.pref_key__share_into__quicknote: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        appendToExistingDocument(_appSettings.getQuickNoteFile(), "\n", false);
                        close = true;
                    }
                    break;
                }
                case R.string.pref_key__share_into__todo: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        String sep = "\n";
                        if (appSettings.isTodoStartTasksWithTodaysDateEnabled()) {
                            sep += TodoTxtTask.getToday() + " ";
                        }
                        if (appSettings.isTodoNewTaskWithHuuidEnabled()) {
                            sep += "huuid:" + PlainTextStuff.newHuuid(appSettings.getHuuidDeviceId()) + " ";
                        }
                        appendToExistingDocument(_appSettings.getTodoFile(), sep, false);
                        close = true;
                    }
                    break;
                }
                case R.string.pref_key__share_into__open_in_browser: {
                    if ((tmps = PlainTextStuff.tryExtractUrlAroundPos(_sharedText, _sharedText.length())) != null) {
                        new ContextUtils(getActivity()).openWebpageInExternalBrowser(tmps);
                        close = true;
                    }
                    break;
                }
                case R.string.pref_key__share_into__reshare: {
                    shu.shareText(_sharedText, null);
                    close = true;
                    break;
                }
                case R.string.pref_key__share_into__calendar_event: {
                    if (shu.createCalendarAppointment(null, _sharedText, null)) {
                        close = true;
                    } else {
                        Toast.makeText(getContext(), R.string.no_calendar_app_is_installed, Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }

            if (preference.getKey().startsWith("/")) {
                if (permc.doIfExtStoragePermissionGranted()) {
                    appendToExistingDocument(new File(preference.getKey()), "\n", true);
                    close = false;
                }
            }

            if (close) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
                return true;
            }
            return null;
        }

        @Override
        public void doUpdatePreferences() {
            super.doUpdatePreferences();
            boolean maybeHasWebUrl = _sharedText.contains("http://") || _sharedText.contains("https://");

            setPreferenceVisible(R.string.pref_key__share_into__todo, _sharedText.length() < 300 && !_sharedText.trim().contains("\n"));
            setPreferenceVisible(R.string.pref_key__share_into__open_in_browser, maybeHasWebUrl);
        }

        private void addDocumentToPrefgroup(String filepath, final PreferenceGroup prefGroup) {
            File file = new File(filepath);
            if (file.exists()) {
                Preference prefd = new Preference(prefGroup.getContext());
                prefd.setTitle(file.getName());
                prefd.setSummary(file.getParent());
                prefd.setKey(file.getAbsolutePath());
                appendPreference(prefd, prefGroup);
            }
        }
    }

    /**
     * Convert text and link into a formatted link, if the text and string appear to be a link
     *
     * @param text Link description
     * @param link Link url
     * @return formatted URL of format [text](url)
     */
    private static String formatLink(String text, String link) {
        link = link == null ? "" : link;
        text = text == null ? "" : text;

        final String formattedLink;
        final Matcher linkMatch = Patterns.WEB_URL.matcher(link.trim());
        if (linkMatch.matches() && !link.trim().matches("\\s") && !text.trim().matches("\\s")) {
            // Get a resonable default text if one is not present. group 4 is the domain name
            try {
                text = TextUtils.isEmpty(text) ? linkMatch.group(4).replaceAll("\\.$", "") : text;
            } catch (IllegalStateException | IndexOutOfBoundsException e) {
                text = "";
            }

            formattedLink = String.format("[%s](%s )",
                    text.trim().replace("[", "\\[").replace("]", "\\]"),
                    link.trim().replace("(", "\\(").replace(")", "\\)")
            );
        } else {
            formattedLink = text + " " + link;
        }
        return formattedLink;
    }

    private static String formatOrPrefixSharedText(final String format, final String value) {
        return (format + (format.contains("%s") ? "" : " %s")).replace("%s", value);
    }
}
