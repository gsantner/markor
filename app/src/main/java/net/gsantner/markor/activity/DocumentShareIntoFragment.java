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
import android.util.Pair;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.frontend.AttachLinkOrFileDialog;
import net.gsantner.markor.frontend.NewFileDialog;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.base.GsPreferenceFragmentBase;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserListAdapter;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentShareIntoFragment extends MarkorBaseFragment {
    public static final String FRAGMENT_TAG = "DocumentShareIntoFragment";
    public static final String EXTRA_SHARED_TEXT = "EXTRA_SHARED_TEXT";
    public static final String TEXT_TOKEN = "{{text}}";

    public static DocumentShareIntoFragment newInstance(final Intent intent) {
        final DocumentShareIntoFragment f = new DocumentShareIntoFragment();
        final Bundle args = new Bundle();

        final String sharedText = extractShareText(intent);

        final Object intentFile = intent.getSerializableExtra(Document.EXTRA_FILE);
        if (intentFile instanceof File && ((File) intentFile).isDirectory()) {
            f.workingDir = (File) intentFile;
        }

        args.putString(EXTRA_SHARED_TEXT, sharedText);
        f.setArguments(args);
        return f;
    }

    private File workingDir;

    public DocumentShareIntoFragment() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.document__fragment__share_into;
    }

    @Override
    public void onViewCreated(final @NonNull View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        HighlightingEditor _hlEditor = view.findViewById(R.id.document__fragment__share_into__highlighting_editor);

        final String sharedText = (getArguments() != null ? getArguments().getString(EXTRA_SHARED_TEXT, "") : "").trim();
        final ShareIntoImportOptionsFragment _shareIntoImportOptionsFragment;
        if (_savedInstanceState == null) {
            FragmentTransaction t = getChildFragmentManager().beginTransaction();
            _shareIntoImportOptionsFragment = new ShareIntoImportOptionsFragment();
            _shareIntoImportOptionsFragment.setWorkingDir(workingDir);
            t.replace(R.id.document__share_into__fragment__placeholder_fragment, _shareIntoImportOptionsFragment, ShareIntoImportOptionsFragment.TAG).commit();
        } else {
            _shareIntoImportOptionsFragment = (ShareIntoImportOptionsFragment) getChildFragmentManager().findFragmentByTag(ShareIntoImportOptionsFragment.TAG);
        }

        if (_shareIntoImportOptionsFragment != null) {
            _shareIntoImportOptionsFragment.setEditor(_hlEditor);
        }

        _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, _appSettings.getFontSize());
        _hlEditor.setTypeface(Typeface.create(_appSettings.getFontFamily(), Typeface.NORMAL));
        _hlEditor.setHighlighter(new PlaintextSyntaxHighlighter(_appSettings));
        _hlEditor.setHighlightingEnabled(true);
        _hlEditor.setText(sharedText);

        if (sharedText.isEmpty()) {
            _hlEditor.requestFocus();
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
        private File workingDir;

        private EditText _editor = null;

        @Override
        public boolean isDividerVisible() {
            return true;
        }

        public void setWorkingDir(File dir) {
            workingDir = dir;
        }

        public void setEditor(final EditText editor) {
            _editor = editor;
        }

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
            doUpdatePreferences();

            if (_editor != null) {
                final CheckBoxPreference asLinkPref = (CheckBoxPreference) findPreference(R.string.pref_key__attach_as_link);
                if (asLinkPref != null) {
                    asLinkPref.setVisible(hasLinks(_editor.getText()));
                    asLinkPref.setChecked(true);
                    _editor.addTextChangedListener(GsTextWatcherAdapter.on((ctext, arg2, arg3, arg4) ->
                            asLinkPref.setVisible(hasLinks(ctext))));
                }
            }
        }

        private boolean shareAsLink() {
            final CheckBoxPreference asLinkPref = (CheckBoxPreference) findPreference(R.string.pref_key__attach_as_link);
            return asLinkPref != null && asLinkPref.isVisible() && asLinkPref.isEnabled() && asLinkPref.isChecked();
        }

        private void appendToExistingDocumentAndClose(final File file, final boolean showEditor) {
            final Activity context = getActivity();
            final Document document = new Document(file);
            final int format = _appSettings.getDocumentFormat(document.getPath(), document.getFormat());
            final String formatted = getFormatted(shareAsLink(), file, format);

            final String oldContent = document.loadContent(context);
            if (oldContent != null) {
                final String nline = oldContent.endsWith("\n") ? "" : "\n";
                final String newContent = oldContent + nline + formatted;
                document.saveContent(context, newContent);
            } else {
                Toast.makeText(context, R.string.error_could_not_open_file, Toast.LENGTH_LONG).show();
            }

            _appSettings.addRecentFile(file);
            if (showEditor) {
                showInDocumentActivity(document);
            } else {
                context.finish();
            }
        }

        private static Pair<String, File> getLinePath(final CharSequence line) {
            final String trimmed = line.toString().trim();
            final int si = trimmed.lastIndexOf(" ");
            final String path = si == -1 ? trimmed : trimmed.substring(si + 1);
            final File file = new File(path);
            if (file.exists()) {
                final String title = si == -1 ? file.getName() : trimmed.substring(0, si);
                return Pair.create(title, file);
            }
            return null;
        }

        // Title and link or null
        private static Pair<String, String> getLineLink(final CharSequence line) {
            final String trimmed = line.toString().trim();
            final int si = trimmed.lastIndexOf(" ");
            final String path = si == -1 ? trimmed : trimmed.substring(si + 1);
            if (Patterns.WEB_URL.matcher(path).matches()) {
                final String title = si == -1 ? getLinkTitle(path) : trimmed.substring(0, si);
                return Pair.create(title, path);
            }
            return null;
        }

        private static boolean hasLinks(final CharSequence text) {
            final boolean[] hasLinks = {false};

            GsTextUtils.forEachline(text, (li, start, end) -> {
                final CharSequence line = text.subSequence(start, end);
                if (getLinePath(line) != null || getLineLink(line) != null) {
                    hasLinks[0] = true;
                    return false;
                }
                return true;
            });

            return hasLinks[0];
        }

        /**
         * Iterates over each line of the text and checks if it is a path or link
         * The whole line has to be a path or the line must end with a link to match.
         *
         * @param text Text to parse
         * @return List of pairs.
         * The first element of the pair is the start and end index of the link/path.
         * Second element is true if it is a path, false if it is a link.
         */
        public static List<Pair<int[], Boolean>> findLinksAndPaths(final CharSequence text) {
            final List<Pair<int[], Boolean>> links = new ArrayList<>();

            GsTextUtils.forEachline(text, (line, start, end) -> {

                start = TextViewUtils.getNextNonWhitespace(text, start);
                end = TextViewUtils.getLastNonWhitespace(text, end - 1) + 1;

                if (start != -1 && end != -1 && start <= end) {

                    final String trimmed = text.subSequence(start, end).toString();
                    final boolean hasSpaces = trimmed.contains(" ") || trimmed.contains("\\t");

                    // Test for web links
                    if (!hasSpaces && Patterns.WEB_URL.matcher(trimmed).matches()) {
                        links.add(Pair.create(new int[]{start, end}, false));
                        return true;
                    }

                    // Test for file links
                    try {
                        if (new File(trimmed.replace("%20", " ")).exists()) {
                            links.add(Pair.create(new int[]{start, end}, true));
                            return true;
                        }
                    } catch (NullPointerException ignored) {
                    }
                }

                return true;
            });

            return links;
        }

        public static String getLinkTitle(final String link) {
            final Matcher m = Patterns.WEB_URL.matcher(link);
            if (m.matches()) {
                final String title = m.group(4);
                return (title != null && title.endsWith(".")) ? title.substring(0, title.length() - 1) : title;
            }
            return "";
        }

        private String getFormatted(final boolean asLink, final File src, final int format) {
            final String text = _editor.getText().toString();
            String formatted = text;
            if (asLink) {

                // Go over every line in the text replacing them with formatted links if appropriate
                final StringBuilder sb = new StringBuilder();
                GsTextUtils.forEachline(text, (line, start, end) -> {

                    final String lineText = text.subSequence(start, end).toString().trim();

                    final String title, path;
                    final Pair<String, File> linePath = getLinePath(lineText);
                    if (linePath != null) {
                        title = linePath.first;
                        path = GsFileUtils.relativePath(src, linePath.second);
                    } else {
                        final Pair<String, String> lineLink = getLineLink(lineText);
                        if (lineLink != null) {
                            title = lineLink.first;
                            path = lineLink.second;
                        } else {
                            title = lineText;
                            path = null;
                        }
                    }

                    if (path != null) {
                        sb.append(AttachLinkOrFileDialog.formatLink(title, path, format));
                    } else {
                        sb.append(lineText);
                    }
                    sb.append("\n");

                    return true;
                });

                formatted = sb.toString();
            }

            formatted = formatShare(formatted);

            if (format == FormatRegistry.FORMAT_TODOTXT) {
                formatted = TodoTxtTask.getToday() + " " + formatted.replaceAll("\\n+", " ");
            } else {
                formatted = "\n" + formatted;
            }

            return formatted;
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
            final File startFolder;
            switch (keyId) {
                case R.string.pref_key__favourite_files: {
                    startFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_FAVOURITE;
                    break;
                }
                case R.string.pref_key__popular_documents: {
                    startFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_POPULAR;
                    break;
                }
                case R.string.pref_key__recent_documents: {
                    startFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_RECENTS;
                    break;
                }
                default: {
                    startFolder = _appSettings.getNotebookDirectory();
                    break;
                }
            }

            MarkorFileBrowserFactory.showFileDialog(new GsFileBrowserOptions.SelectionListenerAdapter() {
                @Override
                public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                    dopt.rootFolder = startFolder;
                    dopt.newDirButtonEnable = false;
                }

                @Override
                public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
                    appendToExistingDocumentAndClose(file, true);
                }

            }, getParentFragmentManager(), getActivity(), MarkorFileBrowserFactory.IsMimeText);
        }


        private void createSelectNewDocument() {
            MarkorFileBrowserFactory.showFileDialog(new GsFileBrowserOptions.SelectionListenerAdapter() {
                GsFileBrowserOptions.Options _dopt = null;

                @Override
                public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                    dopt.rootFolder = _appSettings.getNotebookDirectory();
                    dopt.startFolder = workingDir;
                    dopt.okButtonText = R.string.create_new_document;
                    dopt.okButtonEnable = true;
                    dopt.dismissAfterCallback = false;
                    _dopt = dopt;
                }

                @Override
                public void onFsViewerSelected(final String request, final File sel, final Integer lineNumber) {
                    if (sel.isDirectory()) {
                        NewFileDialog.newInstance(sel, false, f -> {
                            if (f.isFile()) {
                                appendToExistingDocumentAndClose(f, true);
                            }
                        }).show(getChildFragmentManager(), NewFileDialog.FRAGMENT_TAG);
                    } else {
                        appendToExistingDocumentAndClose(sel, true);
                    }
                }

                @Override
                public void onFsViewerCancel(final String request) {
                    // Will cause the dialog to dismiss after this callback
                    _dopt.dismissAfterCallback = true;
                }
            }, getParentFragmentManager(), getActivity(), MarkorFileBrowserFactory.IsMimeText);
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
            final String text = _editor.getText().toString();
            final MarkorContextUtils shu = new MarkorContextUtils(activity);
            String tmps;

            boolean close = false;
            switch (keyId) {
                case R.string.pref_key__share_into__clipboard: {
                    shu.setClipboard(getContext(), text);
                    close = true;
                    break;
                }
                case R.string.pref_key__select_create_document: {
                    createSelectNewDocument();
                    return true;
                }
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
                    if ((tmps = GsTextUtils.tryExtractUrlAroundPos(text, text.length())) != null) {
                        _cu.openWebpageInExternalBrowser(getActivity(), tmps);
                        close = true;
                    }
                    break;
                }
                case R.string.pref_key__share_into__reshare: {
                    shu.shareText(getActivity(), text, null);
                    close = true;
                    break;
                }
                case R.string.pref_key__share_into__calendar_event: {
                    if (shu.createCalendarAppointment(getActivity(), null, text, null)) {
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
            final String text = _editor.getText().toString();
            final boolean maybeHasWebUrl = text.contains("http://") || text.contains("https://");
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

    private static String sanitize(final String link) {
        return link.replaceAll("(?m)(?<=&|\\?)(utm_|source|si|__mk_|ref|sprefix|crid|partner|promo|ad_sub|gclid|fbclid|msclkid).*?(&|$|\\s|\\))", "");
    }

    private static String extractShareText(final Intent intent) {
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (title != null) {
            title = title.trim() + " ";
        }

        String link = intent.getStringExtra(Intent.EXTRA_TEXT);
        link = link != null ? link.trim() : "";

        if (Patterns.WEB_URL.matcher(link).matches()) {
            link = (title != null? title : "") + sanitize(link);
        }

        return link;
    }
}
