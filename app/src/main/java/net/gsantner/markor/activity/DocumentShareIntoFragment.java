/*#######################################################
 *
 *   Maintained 2018-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.frontend.NewFileDialog;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.base.GsPreferenceFragmentBase;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserListAdapter;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentShareIntoFragment extends MarkorBaseFragment {
    public static final String FRAGMENT_TAG = "DocumentShareIntoFragment";
    public static final String EXTRA_SHARED_TEXT = "EXTRA_SHARED_TEXT";
    public static final String TEXT_TOKEN = "{{text}}";

    public static DocumentShareIntoFragment newInstance(Intent intent) {
        DocumentShareIntoFragment f = new DocumentShareIntoFragment();
        Bundle args = new Bundle();

        final String sharedText = formatLink(intent.getStringExtra(Intent.EXTRA_SUBJECT), intent.getStringExtra(Intent.EXTRA_TEXT));

        Object intentFile = intent.getSerializableExtra(Document.EXTRA_PATH);
        if (intentFile instanceof File && ((File) intentFile).isDirectory()) {
            f.workingDir = (File) intentFile;
        }

        args.putString(EXTRA_SHARED_TEXT, sharedText);
        f.setArguments(args);
        return f;
    }

    private File workingDir;
    private HighlightingEditor _hlEditor;
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
        _hlEditor = view.findViewById(R.id.document__fragment__share_into__highlighting_editor);
        _hlEditor.addTextChangedListener(GsTextWatcherAdapter.on((ctext, arg2, arg3, arg4) -> onTextChanged(ctext)));

        final String sharedText = (getArguments() != null ? getArguments().getString(EXTRA_SHARED_TEXT, "") : "").trim();
        if (_savedInstanceState == null) {
            FragmentTransaction t = getChildFragmentManager().beginTransaction();
            _shareIntoImportOptionsFragment = ShareIntoImportOptionsFragment.newInstance(sharedText);
            _shareIntoImportOptionsFragment.setWorkingDir(workingDir);
            t.replace(R.id.document__share_into__fragment__placeholder_fragment, _shareIntoImportOptionsFragment, ShareIntoImportOptionsFragment.TAG).commit();
        } else {
            _shareIntoImportOptionsFragment = (ShareIntoImportOptionsFragment) getChildFragmentManager().findFragmentByTag(ShareIntoImportOptionsFragment.TAG);
        }

        _hlEditor.setText(sharedText);
        _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, _appSettings.getFontSize());
        _hlEditor.setTypeface(Typeface.create(_appSettings.getFontFamily(), Typeface.NORMAL));
        _hlEditor.setHighlighter(new PlaintextSyntaxHighlighter(_appSettings));
        _hlEditor.setHighlightingEnabled(true);

        if (sharedText.isEmpty()) {
            _hlEditor.requestFocus();
        }
    }

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


    public static class ShareIntoImportOptionsFragment extends GsPreferenceFragmentBase<AppSettings> {
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
            return ApplicationObject.settings();
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

        public void setText(String text) {
            _sharedText = text;
            if (isAdded()) {
                doUpdatePreferences();
            }
        }

        // Check set type and fallback to ext check
        private boolean isTodoTxt(final Document doc) {
            final int format = _appSettings.getDocumentFormat(doc.getPath(), FormatRegistry.FORMAT_UNKNOWN);
            if (format != FormatRegistry.FORMAT_UNKNOWN) {
                return format == FormatRegistry.FORMAT_TODOTXT;
            }
            return doc.getFormat() == FormatRegistry.FORMAT_TODOTXT;
        }

        private void appendToExistingDocumentAndClose(final File file, final boolean showEditor) {
            final Activity context = getActivity();
            final Document document = new Document(file);
            final String formatted;
            if (isTodoTxt(document)) {
                final String date = _appSettings.getDocumentAutoFormatEnabled(document.getPath()) ? TodoTxtTask.getToday() : "";
                formatted = date + " " + _sharedText.replaceAll("\\n+", " ");
            } else {
                formatted = formatShare(_sharedText);
            }

            final String oldContent = document.loadContent(context);
            if (oldContent != null) {
                final String newContent = oldContent + "\n" + formatted;
                document.saveContent(context, newContent);
            } else {
                Toast.makeText(context, R.string.error_could_not_open_file, Toast.LENGTH_LONG).show();
            }

            _appSettings.addRecentDocument(file);
            if (showEditor) {
                showInDocumentActivity(document);
            } else {
                context.finish();
            }
        }

        private String formatShare(final String shared) {
            final Context context = getContext();
            final String prefix = _appSettings.getShareIntoPrefix();
            final List<String> parts = new ArrayList<>(Arrays.asList(prefix.split(Pattern.quote(TEXT_TOKEN))));

            // Interpolate parts
            final long time = System.currentTimeMillis();
            for (int i = 0; i < parts.size(); i++) {
                parts.set(i, _cu.formatDateTime(context, parts.get(i), time));
            }

            // Put the shared text in the right place
            parts.add(1, shared);

            return TextUtils.join("", parts);
        }

        private void showAppendDialog(int keyId) {

            MarkorFileBrowserFactory.showFileDialog(new GsFileBrowserOptions.SelectionListenerAdapter() {
                @Override
                public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                    dopt.newDirButtonEnable = false;
                    switch (keyId) {
                        case R.string.pref_key__favourite_files: {
                            dopt.rootFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_FAVOURITE;
                            break;
                        }
                        case R.string.pref_key__popular_documents: {
                            dopt.rootFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_POPULAR;
                            break;
                        }
                        case R.string.pref_key__recent_documents: {
                            dopt.rootFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_RECENTS;
                            break;
                        }
                        default: {
                            dopt.rootFolder = _appSettings.getNotebookDirectory();
                            dopt.newDirButtonEnable = true;
                            dopt.createFileCallback = (dir, callback) -> NewFileDialog.newInstance(dir, false, (ok, f) -> {
                                    if (ok && f.isFile()) {
                                        appendToExistingDocumentAndClose(f, true);
                                    }
                                }
                            ).show(getActivity().getSupportFragmentManager(), NewFileDialog.FRAGMENT_TAG);
                            break;
                        }
                    }
                }

                @Override
                public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
                    appendToExistingDocumentAndClose(file, true);
                }

            }, getParentFragmentManager(), getActivity(), MarkorFileBrowserFactory.IsMimeText);
        }


        private void createNewDocument() {
            MarkorFileBrowserFactory.showFolderDialog(new GsFileBrowserOptions.SelectionListenerAdapter() {
                @Override
                public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                    dopt.rootFolder = (workingDir == null) ? _appSettings.getNotebookDirectory() : workingDir;
                }

                @Override
                public void onFsViewerSelected(String request, File dir, final Integer lineNumber) {
                    NewFileDialog dialog = NewFileDialog.newInstance(dir, false, (ok, f) -> {
                        if (ok && f.isFile()) {
                            appendToExistingDocumentAndClose(f, true);
                        }
                    });
                    dialog.show(getActivity().getSupportFragmentManager(), NewFileDialog.FRAGMENT_TAG);
                }
            }, getParentFragmentManager(), getActivity());
        }

        private void showInDocumentActivity(final Document document) {
            if (getActivity() instanceof DocumentActivity) {
                DocumentActivity a = (DocumentActivity) getActivity();
                a.showTextEditor(document, null, null);
            }
        }

        @Override
        @SuppressLint("NonConstantResourceId")
        @SuppressWarnings({"ConstantConditions", "ConstantIfStatement"})
        public Boolean onPreferenceClicked(Preference preference, String key, int keyId) {
            final Activity activity = getActivity();
            final MarkorContextUtils shu = new MarkorContextUtils(activity);
            String tmps;

            boolean close = false;
            switch (keyId) {
                case R.string.pref_key__share_into__clipboard: {
                    shu.setClipboard(getContext(), _sharedText);
                    close = true;
                    break;
                }
                case R.string.pref_key__select_create_document:
                case R.string.pref_key__favourite_files:
                case R.string.pref_key__popular_documents:
                case R.string.pref_key__recent_documents: {
                    showAppendDialog(keyId);
                    return true;
                }
                case R.string.pref_key__share_into__quicknote: {
                    appendToExistingDocumentAndClose(_appSettings.getQuickNoteFile(), false);
                    break;
                }
                case R.string.pref_key__share_into__todo: {
                    appendToExistingDocumentAndClose(_appSettings.getTodoFile(), false);
                    break;
                }
                case R.string.pref_key__share_into__open_in_browser: {
                    if ((tmps = GsTextUtils.tryExtractUrlAroundPos(_sharedText, _sharedText.length())) != null) {
                        _cu.openWebpageInExternalBrowser(getActivity(), tmps);
                        close = true;
                    }
                    break;
                }
                case R.string.pref_key__share_into__reshare: {
                    shu.shareText(getActivity(), _sharedText, null);
                    close = true;
                    break;
                }
                case R.string.pref_key__share_into__calendar_event: {
                    if (shu.createCalendarAppointment(getActivity(), null, _sharedText, null)) {
                        close = true;
                    } else {
                        Toast.makeText(getContext(), R.string.no_calendar_app_is_installed, Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }

            if (preference.getKey().startsWith("/")) {
                appendToExistingDocumentAndClose(new File(preference.getKey()), true);
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
            final boolean maybeHasWebUrl = _sharedText.contains("http://") || _sharedText.contains("https://");
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

            link = link.replaceAll("(?m)(?<=&|\\?)(utm_|source|si|__mk_|ref|sprefix|crid|partner|promo|ad_sub|gclid|fbclid|msclkid).*?(&|$|\\s|\\))", "");

            formattedLink = String.format("[%s](%s )",
                    text.trim().replace("[", "\\[").replace("]", "\\]").replace("|", "/"),
                    link.trim().replace("(", "\\(").replace(")", "\\)")
            );
        } else {
            formattedLink = text + " " + link;
        }
        return formattedLink;
    }
}
