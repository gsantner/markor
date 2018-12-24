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
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.ui.NewFileDialog;
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

    public static DocumentShareIntoFragment newInstance(Intent intent) {
        DocumentShareIntoFragment f = new DocumentShareIntoFragment();
        Bundle args = new Bundle();

        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        String tmp;
        if (intent.hasExtra(Intent.EXTRA_SUBJECT) && !sharedText.contains((tmp = intent.getStringExtra(Intent.EXTRA_SUBJECT)))) {
            if (!tmp.trim().contains("\n") && !sharedText.trim().contains("\n") && !sharedText.trim().contains(" ") && (sharedText.startsWith("http://") || sharedText.startsWith("https://"))) {
                tmp = "[" + tmp.replace("[", "\\[").replace("]", "\\]") + "]";
                sharedText = "(" + sharedText.replace("(", "\\(").replace(")", "\\)") + ")";
            } else {
                tmp += " ";
            }
            sharedText = tmp + sharedText;
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
            String currentContent = TextUtils.isEmpty(document.getContent().trim()) ? "" : (document.getContent().trim() + "\n");
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

            }, getFragmentManager(), getActivity(), FilesystemDialogCreator.IsMimeText);
        }


        private void createNewDocument() {
            NewFileDialog dialog = NewFileDialog.newInstance(AppSettings.get().getNotebookDirectory(), (ok, f) -> {
                if (ok && f.isFile()) {
                    appendToExistingDocument(f, "", true);
                }
            });
            dialog.show(getActivity().getSupportFragmentManager(), NewFileDialog.FRAGMENT_TAG);
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

            setPreferenceVisible(R.string.pref_key__share_into__todo, !_sharedText.trim().contains("\n") && _sharedText.length() < 300);
            setPreferenceVisible(R.string.pref_key__share_into__linkbox, maybeHasWebUrl && _sharedText.length() < 1200);
            setPreferenceVisible(R.string.pref_key__share_into__quicknote, maybeHasWebUrl && _sharedText.length() < 10000);
            setPreferenceVisible(R.string.pref_key__share_into__open_in_browser, maybeHasWebUrl);

            if ((pref = findPreference(R.string.pref_key__recent_documents)) != null && ((PreferenceGroup) pref).getPreferenceCount() == 0) {
                for (String filepath : new AppSettings(pref.getContext()).getRecentDocuments()) {
                    addDocumentToPrefgroup(filepath, (PreferenceGroup) pref);
                }
            }
            if ((pref = findPreference(R.string.pref_key__popular_documents)) != null && ((PreferenceGroup) pref).getPreferenceCount() == 0) {
                for (String filepath : new AppSettings(pref.getContext()).getPopularDocuments()) {
                    addDocumentToPrefgroup(filepath, (PreferenceGroup) pref);
                }
            }
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
}
