/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ShareUtil;
import net.gsantner.opoc.util.StringUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import other.so.AndroidBug5497Workaround;

public class DocumentActivity extends MarkorBaseActivity {
    public static final String EXTRA_DO_PREVIEW = "EXTRA_DO_PREVIEW";

    @BindView(R.id.document__placeholder_fragment)
    FrameLayout _fragPlaceholder;
    @BindView(R.id.toolbar)
    Toolbar _toolbar;
    @BindView(R.id.note__activity__text_note_title)
    TextView _toolbarTitleText;

    private FragmentManager _fragManager;

    private static boolean nextLaunchTransparentBg = false;


    public static void launch(Activity activity, File path, Boolean doPreview, Intent intent, final Integer lineNumber) {
        if (intent == null) {
            intent = new Intent(activity, DocumentActivity.class);
        }
        if (path != null) {
            intent.putExtra(Document.EXTRA_PATH, path);
        }
        if (lineNumber != null && lineNumber >= 0) {
            intent.putExtra(Document.EXTRA_FILE_LINE_NUMBER, lineNumber);
        }
        if (doPreview != null) {
            intent.putExtra(DocumentActivity.EXTRA_DO_PREVIEW, doPreview);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && new AppSettings(activity).isMultiWindowEnabled()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        nextLaunchTransparentBg = (activity instanceof MainActivity);
        new ActivityUtils(activity).animateToActivity(intent, false, null).freeContextRef();
    }

    public static Object[] checkIfLikelyTextfileAndGetExt(File file) {
        String fn = file.getName().toLowerCase();
        if (!fn.contains(".")) {
            return new Object[]{true, ""};
        }
        String ext = fn.substring(fn.lastIndexOf("."));
        for (String ce : new String[]{".py", ".cpp", ".h", ".js", ".html", ".css", ".java", ".qml", ".go", ".sh", ".rb", ".tex", ".json", ".xml", ".ini", ".yaml", ".yml", ".csv", ".xlf"}) {
            if (ext.equals(ce)) {
                return new Object[]{true, ext};
            }
        }
        return new Object[]{false, ext};
    }

    public static void askUserIfWantsToOpenFileInThisApp(final Activity activity, final File file) {
        Object[] fret = checkIfLikelyTextfileAndGetExt(file);
        boolean isLikelyTextfile = (boolean) fret[0];
        String ext = (String) fret[1];
        boolean isYes = new AppSettings(activity).isExtOpenWithThisApp(ext);

        Callback.a1<Boolean> openFile = (openInThisApp) -> {
            if (openInThisApp) {
                DocumentActivity.launch(activity, file, null, null, null);
            } else {
                new net.gsantner.markor.util.ShareUtil(activity).viewFileInOtherApp(file, null);
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
        AppSettings.clearDebugLog();
        if (nextLaunchTransparentBg) {
            //getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
            nextLaunchTransparentBg = false;
        }
        setContentView(R.layout.document__activity);
        ButterKnife.bind(this);
        if (_appSettings.isHideSystemStatusbar()) {
            AndroidBug5497Workaround.assistActivity(this);
        }

        setSupportActionBar(_toolbar);

        _toolbar.setOnClickListener(this::onToolbarTitleClicked);

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

        String intentAction = intent.getAction();
        Uri intentData = intent.getData();

        File file = (File) intent.getSerializableExtra(Document.EXTRA_PATH);

        boolean intentIsView = Intent.ACTION_VIEW.equals(intentAction);
        boolean intentIsSend = Intent.ACTION_SEND.equals(intentAction);
        boolean intentIsEdit = Intent.ACTION_EDIT.equals(intentAction);

        if (intentIsSend && intent.hasExtra(Intent.EXTRA_TEXT)) {
            showShareInto(intent);
        } else if (file == null && (intentIsView || intentIsEdit)) {
            // No EXTRA_PATH and view of open intent
            file = new ShareUtil(this).extractFileFromIntent(intent);
            if (file == null && intentData != null && intentData.toString().startsWith("content://")) {
                showNotSupportedMessage();
            }
        }

        if (!intentIsSend && file != null) {
            final Document doc = new Document(file);
            final int paramLineNumber = intent.getIntExtra(Document.EXTRA_FILE_LINE_NUMBER, (intentData != null ? StringUtils.tryParseInt(intentData.getQueryParameter("line"), -1) : -1));
            final boolean paramPreview = (paramLineNumber < 0) && (intent.getBooleanExtra(EXTRA_DO_PREVIEW, false)
                    || (file.exists() && file.isFile() && _appSettings.getDocumentPreviewState(doc.getPath()))
                    || file.getName().startsWith("index."));
            showTextEditor(doc, paramPreview, paramLineNumber);
        }
    }

    private void showNotSupportedMessage() {

        final String notSupportedMessage = (
                getString(R.string.filemanager_doesnot_supply_required_data__appspecific) + "\n\n"
                        + getString(R.string.sync_to_local_folder_notice) + "\n\n"
                        + getString(R.string.sync_to_local_folder_notice_paths,
                        getString(R.string.configure_in_the_apps_settings))
        ).replace("\n", "<br/>");

        DialogInterface.OnClickListener listener = (dialogInterface, i) -> {
            _activityUtils.openWebpageInExternalBrowser(getString(R.string.sync_client_support_issue_url));
        };

        new AlertDialog.Builder(this)
                .setMessage(Html.fromHtml(notSupportedMessage))
                .setNegativeButton(R.string.more_info, listener)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener((dialogInterface) -> finish())
                .create().show();
    }

    private final RectF point = new RectF(0, 0, 0, 0);
    private static final int SWIPE_MIN_DX = 150;
    private static final int SWIPE_MAX_DY = 90;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (_appSettings.isSwipeToChangeMode() && _appSettings.isEditorLineBreakingEnabled() && getCurrentVisibleFragment() instanceof DocumentEditFragment) {
            try {
                Rect activityVisibleSize = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(activityVisibleSize);

                if (event.getAction() == MotionEvent.ACTION_DOWN && event.getY() > (_toolbar.getBottom() + _activityUtils.convertDpToPx(8)) & event.getY() < (activityVisibleSize.bottom - _activityUtils.convertDpToPx(52))) {
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
        ShareUtil shu = new ShareUtil(this);
        shu.extractResultFromActivityResult(requestCode, resultCode, data);
    }

    public void setDocumentTitle(final String title) {
        _toolbarTitleText.setText(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && _appSettings.isMultiWindowEnabled()) {
            setTaskDescription(new ActivityManager.TaskDescription(title));
        }
    }

    public void showTextEditor(final Document document, final boolean preview, final Integer lineNumber) {
        GsFragmentBase currentFragment = getCurrentVisibleFragment();
        final int fileLineNumber = (lineNumber != null && lineNumber >= 0) ? lineNumber : -1;

        final boolean sameDocumentRequested = (
                currentFragment instanceof DocumentEditFragment &&
                document.getPath().equals(((DocumentEditFragment) currentFragment).getDocument().getPath()));

        if (!sameDocumentRequested) {
            showFragment(DocumentEditFragment.newInstance(document, fileLineNumber).setPreviewFlag(preview));
        }
    }

    public void showShareInto(Intent intent) {
        // Disable edittext when going to shareinto
        _toolbarTitleText.setText(R.string.share_into);
        showFragment(DocumentShareIntoFragment.newInstance(intent));
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        new PermissionChecker(this).checkPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (_appSettings.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
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
        FragmentManager fmgr = getSupportFragmentManager();
        GsFragmentBase fragment = (GsFragmentBase) fmgr.findFragmentByTag(fragmentTag);
        return fragment;
    }

    private GsFragmentBase getCurrentVisibleFragment() {
        return (GsFragmentBase) getSupportFragmentManager().findFragmentById(R.id.document__placeholder_fragment);
    }

    private void onToolbarTitleClicked(View v) {
        if (getExistingFragment(DocumentEditFragment.FRAGMENT_TAG) != null) {
            DocumentEditFragment def = ((DocumentEditFragment) getExistingFragment(DocumentEditFragment.FRAGMENT_TAG));
            def.onToolbarTitleClicked(_toolbar);
        }
    }
}
