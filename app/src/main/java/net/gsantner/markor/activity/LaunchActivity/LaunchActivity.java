package net.gsantner.markor.activity.LaunchActivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.DocumentShareIntoFragment;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.activity.MarkorBaseActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.PermissionChecker;

import java.io.File;

/**
 * This Activity exists solely to launch the correct activity with the correct intent
 * it is necessary as widget and shortcut intents do not respect MultipleTask etc
 */
public class LaunchActivity extends MarkorBaseActivity {

    public static final String EXTRA_DOCUMENT = "EXTRA_DOCUMENT"; // Document
    public static final String EXTRA_PATH = "EXTRA_PATH"; // java.io.File
    public static final String EXTRA_FILE_LINE_NUMBER = "EXTRA_FILE_LINE_NUMBER"; // int
    public static final String EXTRA_DO_PREVIEW = "EXTRA_DO_PREVIEW";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launchIfNotNull(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        launchIfNotNull(getIntent());
    }

    private void launchIfNotNull(final Intent intent) {
        if (intent != null) {
            launch(this, intent);
        }
    }

    public static boolean checkPermissions(final Activity activity, final File file) {
        try {
            final String message = activity.getString(R.string.error_need_storage_permission_to_save_documents);
            if (new PermissionChecker(activity).doIfExtStoragePermissionGranted(message)) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                if (!file.exists() && !file.isDirectory()) {
                    FileUtils.writeFile(file, "");
                }
                return true;
            }
        }
        catch (Exception ignored) {
            // ignored
        }
        return false;
    }

    public static void launch(final Activity activity, final File path, final Boolean doPreview, final Integer lineNumber) {
        final Intent intent = new Intent();
        if (path != null) {
            intent.putExtra(EXTRA_PATH, path);
        }
        if (doPreview != null) {
            intent.putExtra(EXTRA_DO_PREVIEW, doPreview);
        }
        if (lineNumber != null) {
            intent.putExtra(EXTRA_FILE_LINE_NUMBER, lineNumber);
        }
        launch(activity, intent);
    }

    // Take receiving intent, reformat and add some parameters and launch
    public static void launch(final Activity activity, final Intent intent) {
        final Intent newIntent = new Intent(intent);

        Object extraObj = intent.getSerializableExtra(EXTRA_PATH);
        File extraPath = extraObj instanceof File ? (File) extraObj : null;

        // Put any uri in extra path
        if (extraPath == null) {
            Uri data = intent.getData();
            if (data != null) {
                String dataPath = data.getPath();
                if (dataPath != null) {
                    final File dataFile = new File(dataPath);
                    if (checkPermissions(activity, dataFile)) {
                        intent.putExtra(EXTRA_PATH, dataFile);
                        extraPath = dataFile;
                    }
                }
            }
        }

        // Default if no path or bad file at this state
        if (extraPath == null || !checkPermissions(activity, extraPath)) {
            intent.putExtra(EXTRA_PATH, Document.getDefault(activity).getFile());
        }

        // pick class if not set
        final ComponentName component = newIntent.getComponent();
        final String className = component != null ? component.getClassName() : null;
        if (className == null && extraPath != null) {
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                newIntent.setClass(activity, DocumentShareIntoFragment.class);
            } else if (extraPath.isDirectory()) {
                newIntent.setClass(activity, MainActivity.class);
            } else {
                newIntent.setClass(activity, DocumentActivity.class);
            }
        }

        // Set multiple task as appropriate
        if ( DocumentActivity.class.getName().equals(className) && // Only if launching a document
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                new AppSettings(activity).isMultiWindowEnabled()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        new ActivityUtils(activity).animateToActivity(newIntent, true, null).freeContextRef();
    }
}