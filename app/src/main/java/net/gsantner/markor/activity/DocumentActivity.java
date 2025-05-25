/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.base.GsFragmentBase;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;

import other.so.AndroidBug5497Workaround;

public class DocumentActivity extends MarkorBaseActivity {

    private Toolbar _toolbar;
    private FragmentManager _fragManager;

    public static void launch(final Activity activity, final Intent intent) {
        final File file = MarkorContextUtils.getIntentFile(intent);
        final Integer lineNumber = intent.hasExtra(Document.EXTRA_FILE_LINE_NUMBER) ? intent.getIntExtra(Document.EXTRA_FILE_LINE_NUMBER, -1) : null;
        final Boolean doPreview = intent.hasExtra(Document.EXTRA_DO_PREVIEW) ? intent.getBooleanExtra(Document.EXTRA_DO_PREVIEW, false) : null;
        launch(activity, file, doPreview, lineNumber);
    }

    public static void launch(
            final Activity activity,
            final File file,
            final Boolean doPreview,
            final Integer lineNumber
    ) {
        launch(activity, file, doPreview, lineNumber, false);
    }

    private static void launch(
            final Activity activity,
            final File file,
            final Boolean doPreview,
            final Integer lineNumber,
            final boolean forceOpenInThisApp
    ) {
        if (activity == null || file == null) {
            return;
        }

        if (GsFileUtils.getFilenameExtension(file).equals(".apk")) {
            GsContextUtils.instance.requestApkInstallation(activity, file);
            return;
        }

        if (!forceOpenInThisApp && file.isFile() && !FormatRegistry.isFileSupported(file)) {
            askUserIfWantsToOpenFileInThisApp(activity, file);
            return;
        }

        final AppSettings as = AppSettings.get(activity);

        final Intent intent;
        if (GsFileUtils.isDirectory(file)) {
            intent = new Intent(activity, MainActivity.class);
        } else {
            intent = new Intent(activity, DocumentActivity.class);

            final boolean lollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
            final boolean fromDocumentActivity = activity instanceof DocumentActivity;
            final boolean isMultiWindow = as.isMultiWindowEnabled();
            if (lollipop && isMultiWindow) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            } else if (isMultiWindow) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            } else if (lollipop && !fromDocumentActivity) {
                // So we can potentially not open duplicate documents
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            }

            if (lineNumber != null) {
                intent.putExtra(Document.EXTRA_FILE_LINE_NUMBER, lineNumber);
            }

            if (doPreview != null) {
                intent.putExtra(Document.EXTRA_DO_PREVIEW, doPreview);
            }
        }

        intent.putExtra(Document.EXTRA_FILE, file);

        GsContextUtils.instance.animateToActivity(activity, intent, false, null);
    }

    public static void askUserIfWantsToOpenFileInThisApp(final Activity activity, final File file) {
        if (!FormatRegistry.isExternalFile(file) && GsFileUtils.isContentsPlainText(file)) {
            new AlertDialog.Builder(activity, R.style.Theme_AppCompat_DayNight_Dialog_Rounded)
                    .setTitle(R.string.open_with)
                    .setMessage(R.string.selected_file_may_be_a_textfile_want_to_open_in_editor)
                    .setIcon(R.drawable.ic_open_in_browser_black_24dp)
                    .setPositiveButton(R.string.app_name, (dialog1, which) -> DocumentActivity.launch(activity, file, null, null, true))
                    .setNegativeButton(R.string.other, (dialog1, which) -> new MarkorContextUtils(activity).viewFileInOtherApp(activity, file, null))
                    .create()
                    .show();
        } else {
            new MarkorContextUtils(activity).viewFileInOtherApp(activity, file, null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StoragePermissionActivity.requestPermissions(this);
        AppSettings.clearDebugLog();
        setContentView(R.layout.document__activity);
        _toolbar = findViewById(R.id.toolbar);

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
        final File file = MarkorContextUtils.getIntentFile(intent, this);

        final boolean intentIsView = Intent.ACTION_VIEW.equals(intentAction);
        final boolean intentIsSend = Intent.ACTION_SEND.equals(intentAction) || Intent.ACTION_SEND_MULTIPLE.equals(intentAction);
        final boolean intentIsEdit = Intent.ACTION_EDIT.equals(intentAction);

        if (intentIsSend) {
            showShareInto(intent);
            return;
        } else if (Intent.ACTION_PROCESS_TEXT.equals(intentAction) && intent.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
            intent.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra("android.intent.extra.PROCESS_TEXT"));
            showShareInto(intent);
            return;
        }

        // Decide what to do with the file
        // -----------------------------------------------------------------------
        if (file == null || !_cu.canWriteFile(this, file, false, true)) {
            showNotSupportedMessage();
        } else {
            // Open in editor/viewer
            final Document doc = new Document(file);
            Integer startLine = null;
            if (intent.hasExtra(Document.EXTRA_FILE_LINE_NUMBER)) {
                startLine = intent.getIntExtra(Document.EXTRA_FILE_LINE_NUMBER, -1);
            } else if (intentData != null) {
                final String line = intentData.getQueryParameter("line");
                startLine = GsTextUtils.tryParseInt(line, -1);
            }

            // Start in a specific mode if required. Otherwise let the fragment decide
            Boolean startInPreview = null;
            if (startLine != null) {
                // If a line is requested, open in edit mode so the line is shown
                startInPreview = false;
            } else if (intent.getBooleanExtra(Document.EXTRA_DO_PREVIEW, false) || file.getName().startsWith("index.")) {
                startInPreview = true;
            }

            // Three cases
            // 1. We have an editor open and it is the same document - show the requested line
            // 2. We have an editor open and it is a different document - open the new document
            // 3. We do not have a current fragment - open the document here
            final GsFragmentBase<?, ?> frag = getCurrentVisibleFragment();
            if (frag != null) {
                if (frag instanceof DocumentEditAndViewFragment) {
                    final DocumentEditAndViewFragment editFrag = (DocumentEditAndViewFragment) frag;
                    if (editFrag.getDocument().path.equals(doc.path)) {
                        if (startLine != null) {
                            // Same document requested, show the requested line
                            TextViewUtils.selectLines(editFrag.getEditor(), startLine);
                        }
                    } else {
                        // Current document is different - launch the new document
                        launch(this, file, startInPreview, startLine);
                    }
                } else {
                    // Current fragment is not an editor - launch the new document
                    launch(this, file, startInPreview, startLine);
                }
            } else {
                // No fragment open - open the document
                showFragment(DocumentEditAndViewFragment.newInstance(doc, startLine, startInPreview));
            }
        }
    }

    private boolean isDocumentAlreadyOpen(final Document doc) {
        final GsFragmentBase<?, ?> frag = getCurrentVisibleFragment();
        if (frag instanceof DocumentEditAndViewFragment) {
            final DocumentEditAndViewFragment editFrag = (DocumentEditAndViewFragment) frag;
            return editFrag.getDocument().path.equals(doc.path);
        }
        return false;
    }

    private void showNotSupportedMessage() {
        final String notSupportedMessage = (getString(R.string.filemanager_doesnot_supply_required_data__appspecific) + "\n\n" + getString(R.string.sync_to_local_folder_notice)).replace("\n", "<br/>");
        new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Rounded)
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
        } catch (Exception e) {
            Log.e(getClass().getName(), "Error in super.dispatchTouchEvent: " + e);
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        _cu.extractResultFromActivityResult(this, requestCode, resultCode, data);
    }

    public void setTitle(final CharSequence title) {
        final ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setTitle(title);
        }
    }

    public void setDocumentTitle(final String title) {
        setTitle(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && _appSettings.isMultiWindowEnabled()) {
            setTaskDescription(new ActivityManager.TaskDescription(title));
        }
    }

    public void showShareInto(Intent intent) {
        setTitle(getString(R.string.share_into));
        showFragment(DocumentShareIntoFragment.newInstance(intent, this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        _cu.setKeepScreenOn(this, _appSettings.isKeepScreenOn());
    }

    @Override
    @SuppressWarnings("StatementWithEmptyBody")
    public void onBackPressed() {
        final int entryCount = _fragManager.getBackStackEntryCount();
        final GsFragmentBase<?, ?> top = getCurrentVisibleFragment();

        // We pop the stack to go back to the previous fragment
        // if the top fragment does not handle the back press
        // Doesn't actually get called as we have 1 fragment in the stack
        if (top != null && !top.onBackPressed() && entryCount > 1) {
            _fragManager.popBackStack();
            return;
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
        return super.onReceiveKeyPress(getCurrentVisibleFragment(), keyCode, event) || super.onKeyDown(keyCode, event);
    }

    public GsFragmentBase<?, ?> showFragment(GsFragmentBase<?, ?> fragment) {
        if (fragment != getCurrentVisibleFragment()) {
            _fragManager.beginTransaction()
                    .replace(R.id.document__placeholder_fragment, fragment, fragment.getFragmentTag())
                    .commit();

            supportInvalidateOptionsMenu();
        }
        return fragment;
    }

    public synchronized GsFragmentBase<?, ?> getExistingFragment(final String fragmentTag) {
        return (GsFragmentBase<?, ?>) getSupportFragmentManager().findFragmentByTag(fragmentTag);
    }

    private GsFragmentBase<?, ?> getCurrentVisibleFragment() {
        return (GsFragmentBase<?, ?>) getSupportFragmentManager().findFragmentById(R.id.document__placeholder_fragment);
    }
}