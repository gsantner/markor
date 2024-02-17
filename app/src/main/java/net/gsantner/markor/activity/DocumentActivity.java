/*#######################################################
 *
 *   Maintained 2017-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.base.GsFragmentBase;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;

import other.so.AndroidBug5497Workaround;

public class DocumentActivity extends MarkorBaseActivity {
    public static final String EXTRA_DO_PREVIEW = "EXTRA_DO_PREVIEW";

    private Toolbar _toolbar;
    private TextView _toolbarTitleText;

    private FragmentManager _fragManager;

    private static boolean nextLaunchTransparentBg = false;

    public static void launch(Activity activity, File path, Boolean doPreview, Intent intent, final Integer lineNumber) {
        final AppSettings as = ApplicationObject.settings();
        if (intent == null) {
            intent = new Intent(activity, DocumentActivity.class);
        }
        if (path != null) {
            intent.putExtra(Document.EXTRA_FILE, path);
        } else {
            path = intent.hasExtra(Document.EXTRA_FILE) ? ((File) intent.getSerializableExtra(Document.EXTRA_FILE)) : null;
        }
        if (lineNumber != null && lineNumber >= 0) {
            intent.putExtra(Document.EXTRA_FILE_LINE_NUMBER, lineNumber);
        }
        if (doPreview != null) {
            intent.putExtra(DocumentActivity.EXTRA_DO_PREVIEW, doPreview);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && ApplicationObject.settings().isMultiWindowEnabled()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        if (path != null && path.isDirectory()) {
            intent = new Intent(activity, MainActivity.class).putExtra(Document.EXTRA_FILE, path);
        }
        if (path != null && path.isFile() && as.isPreferViewMode()) {
            as.setDocumentPreviewState(path.getAbsolutePath(), true);
        }
        nextLaunchTransparentBg = (activity instanceof MainActivity);
        GsContextUtils.instance.animateToActivity(activity, intent, false, null);
    }

    public static void handleFileClick(Activity activity, File file, Integer lineNumber) {
        if (activity == null || file == null) {
            return;
        }

        if (file.isDirectory()) {
            if (file.canRead()) {
                launch(activity, file, null, null, null);
            }
        } else if (FormatRegistry.isFileSupported(file) && GsFileUtils.canCreate(file)) {
            launch(activity, file, null, null, lineNumber);
        } else if (GsFileUtils.getFilenameExtension(file).equals(".apk")) {
            GsContextUtils.instance.requestApkInstallation(activity, file);
        } else {
            askUserIfWantsToOpenFileInThisApp(activity, file);
        }
    }

    public static Object[] checkIfLikelyTextfileAndGetExt(File file) {
        String fn = file.getName().toLowerCase();
        if (!fn.contains(".")) {
            return new Object[]{true, ""};
        }
        String ext = fn.substring(fn.lastIndexOf("."));
        for (String ce : new String[]{".dummy"}) {
            if (ext.equals(ce)) {
                return new Object[]{true, ext};
            }
        }
        return new Object[]{false, ext};
    }

    public static void askUserIfWantsToOpenFileInThisApp(final Activity activity, final File file) {
        final Object[] fret = checkIfLikelyTextfileAndGetExt(file);
        final boolean isLikelyTextfile = (boolean) fret[0];
        final String ext = (String) fret[1];
        final boolean isYes = ApplicationObject.settings().isExtOpenWithThisApp(ext);

        final GsCallback.a1<Boolean> openFile = (openInThisApp) -> {
            if (openInThisApp) {
                DocumentActivity.launch(activity, file, null, null, null);
            } else {
                new MarkorContextUtils(activity).viewFileInOtherApp(activity, file, null);
            }
        };

        if (isYes) {
            openFile.callback(true);
        } else if (isLikelyTextfile) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_DayNight_Dialog);
            dialog.setTitle(R.string.open_with)
                    .setMessage(R.string.selected_file_may_be_a_textfile_want_to_open_in_editor)
                    .setIcon(R.drawable.ic_open_in_browser_black_24dp)
                    .setPositiveButton(R.string.app_name, (dialog1, which) -> openFile.callback(true))
                    .setNegativeButton(R.string.other, (dialog1, which) -> openFile.callback(false));
            dialog.create().show();
        } else {
            openFile.callback(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StoragePermissionActivity.requestPermissions(this);
        AppSettings.clearDebugLog();
        if (nextLaunchTransparentBg) {
            //getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
            nextLaunchTransparentBg = false;
        }
        setContentView(R.layout.document__activity);
        _toolbar = findViewById(R.id.toolbar);
        _toolbarTitleText = findViewById(R.id.note__activity__text_note_title);

        if (_appSettings.isHideSystemStatusbar()) {
            AndroidBug5497Workaround.assistActivity(this);
        }

        setSupportActionBar(findViewById(R.id.toolbar));
        _fragManager = getSupportFragmentManager();

        handleLaunchingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleLaunchingIntent(intent);
    }

    private void handleLaunchingIntent(final Intent intent) {
        if (intent == null) return;

        final String intentAction = intent.getAction();
        final Uri intentData = intent.getData();

        // Pull the file from the intent
        // -----------------------------------------------------------------------
        File file = (File) intent.getSerializableExtra(Document.EXTRA_FILE);

        final boolean intentIsView = Intent.ACTION_VIEW.equals(intentAction);
        final boolean intentIsSend = Intent.ACTION_SEND.equals(intentAction);
        final boolean intentIsEdit = Intent.ACTION_EDIT.equals(intentAction);

        if (intentIsSend && intent.hasExtra(Intent.EXTRA_TEXT)) {
            showShareInto(intent);
            return;
        } else if (Intent.ACTION_PROCESS_TEXT.equals(intentAction) && intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
            intent.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra("android.intent.extra.PROCESS_TEXT"));
            showShareInto(intent);
            return;
        } else if (file == null && (intentIsView || intentIsEdit || intentIsSend)) {
            file = _cu.extractFileFromIntent(this, intent);
            if (file == null) {
                // More permissive - file may not exist
                // Will be filtered out in next stage
                file = MarkorContextUtils.getIntentFile(intent, null);
            }
        }

        // Decide what to do with the file
        // -----------------------------------------------------------------------
        if (file == null) {
            showNotSupportedMessage();
        } else if (file.isDirectory() || !FormatRegistry.isFileSupported(file)) {
            // File readable but is not a text-file (and not a supported binary-embed type)
            handleFileClick(this, file, null);
            finish();
        } else {
            // Open in editor/viewer
            final Document doc = new Document(file);
            Integer startLine = null;
            if (intent.hasExtra(Document.EXTRA_FILE_LINE_NUMBER)) {
                startLine = intent.getIntExtra(Document.EXTRA_FILE_LINE_NUMBER, -1);
            } else if (intentData != null) {
                final String line = intentData.getQueryParameter("line");
                if (line != null) {
                    startLine = GsTextUtils.tryParseInt(line, -1);
                }
            }

            // Start in a specific mode if required. Otherwise let the fragment decide
            Boolean startInPreview = null;
            if (startLine != null) {
                // If a line is requested, open in edit mode so the line is shown
                startInPreview = false;
            } else if (intent.getBooleanExtra(EXTRA_DO_PREVIEW, false) || file.getName().startsWith("index.")) {
                startInPreview = true;
            }

            showTextEditor(doc, startLine, startInPreview);
        }
    }

    private void showNotSupportedMessage() {
        final String notSupportedMessage = (getString(R.string.filemanager_doesnot_supply_required_data__appspecific) + "\n\n" + getString(R.string.sync_to_local_folder_notice)).replace("\n", "<br/>");
        new AlertDialog.Builder(this)
                .setMessage(Html.fromHtml(notSupportedMessage))
                .setNegativeButton(R.string.more_info, (di, i) -> _cu.openWebpageInExternalBrowser(this, getString(R.string.sync_client_support_issue_url)))
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener((dialogInterface) -> finish())
                .create().show();
    }

    private final RectF point = new RectF(0, 0, 0, 0);
    private static final int SWIPE_MIN_DX = 150;
    private static final int SWIPE_MAX_DY = 90;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (_appSettings.isSwipeToChangeMode() && _appSettings.isEditorLineBreakingEnabled() && getCurrentVisibleFragment() instanceof DocumentEditAndViewFragment) {
            try {
                Rect activityVisibleSize = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(activityVisibleSize);

                if (event.getAction() == MotionEvent.ACTION_DOWN && event.getY() > (_toolbar.getBottom() + _cu.convertDpToPx(this, 8)) & event.getY() < (activityVisibleSize.bottom - _cu.convertDpToPx(this, 52))) {
                    point.set(event.getX(), event.getY(), 0, 0);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    point.set(point.left, point.top, event.getX(), event.getY());
                    if (Math.abs(point.width()) > SWIPE_MIN_DX && Math.abs(point.height()) < SWIPE_MAX_DY) {
                        getCurrentVisibleFragment().getFragmentMenu().performIdentifierAction(R.id.action_preview_edit_toggle, 0);
                    }
                }
            } catch (Exception ignored) {
                // No fancy exception handling :P. Nothing to see here.
            }
        }
        try {
            return super.dispatchTouchEvent(event);
        } catch (IndexOutOfBoundsException ignored) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        _cu.extractResultFromActivityResult(this, requestCode, resultCode, data);
    }

    public void setDocumentTitle(final String title) {
        _toolbarTitleText.setText(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && _appSettings.isMultiWindowEnabled()) {
            setTaskDescription(new ActivityManager.TaskDescription(title));
        }
    }

    public void showTextEditor(final Document document, final Integer lineNumber, final Boolean startPreview) {
        final GsFragmentBase currentFragment = getCurrentVisibleFragment();

        final boolean sameDocumentRequested = (
                currentFragment instanceof DocumentEditAndViewFragment &&
                        document.getPath().equals(((DocumentEditAndViewFragment) currentFragment).getDocument().getPath()));

        if (!sameDocumentRequested) {
            showFragment(DocumentEditAndViewFragment.newInstance(document, lineNumber, startPreview));
        }
    }

    public void showShareInto(Intent intent) {
        // Disable edittext when going to shareinto
        _toolbarTitleText.setText(R.string.share_into);
        showFragment(DocumentShareIntoFragment.newInstance(intent));
    }

    @Override
    protected void onResume() {
        super.onResume();
        _cu.setKeepScreenOn(this, _appSettings.isKeepScreenOn());
    }

    @Override
    @SuppressWarnings("StatementWithEmptyBody")
    public void onBackPressed() {
        FragmentManager fragMgr = getSupportFragmentManager();
        GsFragmentBase top = getCurrentVisibleFragment();
        if (top != null) {
            if (!top.onBackPressed()) {
                if (fragMgr.getBackStackEntryCount() == 1) {
                    // Back action was not handled by fragment, handle in activity
                } else if (fragMgr.getBackStackEntryCount() > 0) {
                    // Back action was to go one fragment back
                    fragMgr.popBackStack();
                    return;
                }
            } else {
                // Was handled by child fragment
                return;
            }
        }

        // Handle in this activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onReceiveKeyPress(getCurrentVisibleFragment(), keyCode, event) ? true : super.onKeyDown(keyCode, event);
    }

    public GsFragmentBase showFragment(GsFragmentBase fragment) {
        if (fragment != getCurrentVisibleFragment()) {
            _fragManager.beginTransaction()
                    .replace(R.id.document__placeholder_fragment, fragment, fragment.getFragmentTag())
                    .commit();

            supportInvalidateOptionsMenu();
        }
        return fragment;
    }

    public synchronized GsFragmentBase getExistingFragment(final String fragmentTag) {
        return (GsFragmentBase) getSupportFragmentManager().findFragmentByTag(fragmentTag);
    }

    private GsFragmentBase getCurrentVisibleFragment() {
        return (GsFragmentBase) getSupportFragmentManager().findFragmentById(R.id.document__placeholder_fragment);
    }
}