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
import android.util.TypedValue;
import android.view.View;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.format.plaintext.PlainTextStuff;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.ui.FilesystemDialogData;

import java.io.File;

import butterknife.BindView;
import butterknife.OnTextChanged;

public class DocumentShareIntoFragment extends GsFragmentBase {
    public static final String FRAGMENT_TAG = "DocumentShareIntoFragment";
    public static final String EXTRA_SHARED_TEXT = "EXTRA_SHARED_TEXT";

    public static DocumentShareIntoFragment newInstance(String sharedText) {
        DocumentShareIntoFragment f = new DocumentShareIntoFragment();
        Bundle args = new Bundle();
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
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppSettings as = new AppSettings(view.getContext());
        ContextUtils cu = new ContextUtils(view.getContext());
        cu.setAppLanguage(as.getLanguage());
        String sharedText = getArguments() != null ? getArguments().getString(EXTRA_SHARED_TEXT, "") : "";
        sharedText = sharedText.trim();

        view.setBackgroundColor(as.getBackgroundColor());
        if (_savedInstanceState == null) {
            FragmentTransaction t = getChildFragmentManager().beginTransaction();
            _shareIntoImportOptionsFragment = ShareIntoImportOptionsFragment.newInstance(sharedText);
            t.replace(R.id.document__share_into__fragment__placeholder_fragment, _shareIntoImportOptionsFragment, ShareIntoImportOptionsFragment.TAG).commit();
        } else {
            _shareIntoImportOptionsFragment = (ShareIntoImportOptionsFragment) getChildFragmentManager().findFragmentByTag(ShareIntoImportOptionsFragment.TAG);
        }
        _hlEditor.setText(sharedText);
        _hlEditor.setBackgroundColor(ContextCompat.getColor(view.getContext(), as.isDarkThemeEnabled() ? R.color.dark__background_2 : R.color.light__background_2));
        _hlEditor.setTextColor(ContextCompat.getColor(view.getContext(), as.isDarkThemeEnabled() ? R.color.white : R.color.dark_grey));
        _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, as.getFontSize());
        _hlEditor.setTypeface(Typeface.create(as.getFontFamily(), Typeface.NORMAL));
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


    public static class ShareIntoImportOptionsFragment extends GsPreferenceFragmentCompat {
        public static final String TAG = "ShareIntoImportOptionsFragment";
        private static final String EXTRA_TEXT = Intent.EXTRA_TEXT;
        private static final String SEP_RULER = "\n---\n";

        public static ShareIntoImportOptionsFragment newInstance(String sharedText) {
            ShareIntoImportOptionsFragment f = new ShareIntoImportOptionsFragment();
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_TEXT, sharedText);
            f.setArguments(bundle);
            return f;
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
        protected SharedPreferencesPropertyBackend getAppSettings(Context context) {
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
            outState.putString(EXTRA_TEXT, _sharedText);
        }

        @Override
        public Integer getIconTintColor() {
            boolean dark = ((AppSettings) getAppSettings(getContext())).isDarkThemeEnabled();
            return _cu.rcolor(dark ? R.color.dark__primary_text : R.color.light__primary_text);
        }

        public void setText(String text) {
            _sharedText = text;
            if (isAdded()) {
                doUpdatePreferences();
            }
        }

        private void appendToExistingDocument(File file, String seperator, boolean showEditor) {
            Bundle args = new Bundle();
            args.putSerializable(DocumentIO.EXTRA_PATH, file);
            args.putBoolean(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
            Document document = DocumentIO.loadDocument(getContext(), args, null);
            String currentContent = TextUtils.isEmpty(document.getContent()) ? "" : (document.getContent().trim() + "\n");
            DocumentIO.saveDocument(document, false, currentContent + seperator + _sharedText);
            if (showEditor) {
                showInDocumentActivity(document);
            }

            if (file != null) {
                ((AppSettings) _appSettings).addRecentDocument(file);
            }
        }

        private void showAppendDialog() {
            FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
                @Override
                public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                    opt.rootFolder = AppSettings.get().getNotebookDirectory();
                }

                @Override
                public void onFsSelected(String request, File file) {
                    appendToExistingDocument(file, SEP_RULER, true);
                }

            }, getFragmentManager(), getActivity());
        }


        private void createNewDocument() {
            // Create a new document
            Bundle args = new Bundle();
            args.putSerializable(DocumentIO.EXTRA_PATH, AppSettings.get().getNotebookDirectory());
            args.putBoolean(DocumentIO.EXTRA_PATH_IS_FOLDER, true);
            Document document = DocumentIO.loadDocument(getContext(), args, null);
            DocumentIO.saveDocument(document, false, _sharedText);

            // Load document as file
            args.putSerializable(DocumentIO.EXTRA_PATH, document.getFile());
            args.putBoolean(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
            document = DocumentIO.loadDocument(getContext(), args, null);
            document.setTitle("");
            showInDocumentActivity(document);
        }

        private void showInDocumentActivity(Document document) {
            if (getActivity() instanceof DocumentActivity) {
                DocumentActivity a = (DocumentActivity) getActivity();
                a.setDocument(document);
                if (AppSettings.get().isPreviewFirst()) {
                    a.showPreview(document, null);
                } else {
                    a.showTextEditor(document, null, false);
                }
            }
        }

        @Override
        @SuppressWarnings({"ConstantConditions", "ConstantIfStatement", "StatementWithEmptyBody"})
        public Boolean onPreferenceClicked(Preference preference) {
            AppSettings appSettings = new AppSettings(getActivity().getApplicationContext());
            PermissionChecker permc = new PermissionChecker(getActivity());
            ShareUtil shu = new ShareUtil(getContext());
            String tmps;

            int keyId = _cu.getResId(net.gsantner.opoc.util.ContextUtils.ResType.STRING, preference.getKey());
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
                case R.string.pref_key__share_into__existing_document: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        showAppendDialog();
                    }
                    return true;
                }
                case R.string.pref_key__share_into__quicknote: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        appendToExistingDocument(AppSettings.get().getQuickNoteFile(), _sharedText.length() > 200 ? SEP_RULER : "\n", false);
                        close = true;
                    }
                    break;
                }
                case R.string.pref_key__share_into__linkbox: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        _sharedText = _sharedText
                                .replace("http://", "\nhttp://").replace("https://", "\nhttps://")
                                .replaceAll("(\\s*)?-(\\s*)?\\n", "\n")
                                .trim();
                        appendToExistingDocument(AppSettings.get().getLinkBoxFile(), "\n", false);
                        close = true;
                    }
                    break;
                }
                case R.string.pref_key__share_into__todo: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        String sep = "\n";
                        if (appSettings.isTodoStartTasksWithTodaysDateEnabled()) {
                            tmps = SttCommander.getToday() + " ";
                            if (!_sharedText.startsWith(tmps)) {
                                sep = tmps;
                            }
                        }
                        appendToExistingDocument(AppSettings.get().getTodoFile(), sep, false);
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
                    shu.createCalendarAppointment(null, _sharedText, null);
                    close = true;
                    break;
                }
            }

            if (preference.getKey().startsWith("/")) {
                if (permc.doIfExtStoragePermissionGranted()) {
                    appendToExistingDocument(new File(preference.getKey()), SEP_RULER, true);
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
            Preference pref;
            if ((pref = findPreference(R.string.pref_key__share_into__todo)) != null) {
                pref.setVisible(!_sharedText.contains("\n"));
            }
            if ((pref = findPreference(R.string.pref_key__share_into__linkbox)) != null) {
                pref.setVisible(maybeHasWebUrl);
            }

            if ((pref = findPreference(R.string.pref_key__share_into__open_in_browser)) != null) {
                pref.setVisible(maybeHasWebUrl);
            }
            if ((pref = findPreference(R.string.pref_key__share_into__reshare)) != null) {
                if (pref.getTitle().toString().equals(getString(R.string.share))) {
                    pref.setTitle(String.format("%s (%s)", pref.getTitle(), getString(R.string.plaintext)));
                }
            }

            if ((pref = findPreference(R.string.pref_key__recent_documents)) != null && ((PreferenceGroup) pref).getPreferenceCount() == 0) {
                for (String doc_s : new AppSettings(pref.getContext()).getRecentDocuments()) {
                    File file = new File(doc_s);
                    if (!file.exists()) {
                        continue;
                    }
                    Preference prefd = new Preference(pref.getContext());
                    prefd.setTitle(file.getName());
                    prefd.setSummary(file.getParent());
                    prefd.setKey(file.getAbsolutePath());
                    appendPreference(prefd, (PreferenceGroup) pref);
                }
            }
        }
    }
}
