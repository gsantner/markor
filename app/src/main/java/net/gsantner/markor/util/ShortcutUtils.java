/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentRelayActivity;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for app shortcuts.
 * https://developer.android.com/guide/topics/ui/shortcuts
 */
public class ShortcutUtils {

    private static final String ID_PREFIX = "shortcut_";
    private static final String ID_TO_DO = ID_PREFIX + "to_do";
    private static final String ID_QUICK_NOTE = ID_PREFIX + "quick_note";

    private static final int MAX_RECENT_DOCUMENTS = 1;

    private ShortcutUtils() {

    }

    /**
     * Update the app shortcuts.
     * The list will contain a link to to-do, QuickNote and 1 recent documents.
     * <p>
     * Due to a limit in the Android API, only 4 shortcuts can be displayed.
     *
     * @param context Context
     */
    public static void setShortcuts(@NonNull Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                return;
            }

            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            List<ShortcutInfo> newShortcuts = new ArrayList<>();

            final AppSettings appSettings = new AppSettings(context);

            // Create the to-do shortcut
            Intent openTodo = new Intent(context, DocumentRelayActivity.class)
                    .setAction(Intent.ACTION_EDIT)
                    .setData(Uri.fromFile(appSettings.getTodoFile()));

            ShortcutInfo shortcutToDo = new ShortcutInfo.Builder(context, ID_TO_DO)
                    .setShortLabel(createShortLabel(context.getString(R.string.todo)))
                    .setLongLabel(createLongLabel(context.getString(R.string.todo)))
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_shortcut_todo))
                    .setIntent(openTodo)
                    .build();
            newShortcuts.add(shortcutToDo);

            // Create the QuickNote shortcut
            Intent openQuickNote = new Intent(context, DocumentRelayActivity.class)
                    .setAction(Intent.ACTION_EDIT)
                    .setData(Uri.fromFile(appSettings.getQuickNoteFile()));

            ShortcutInfo shortcutQuickNote = new ShortcutInfo.Builder(context, ID_QUICK_NOTE)
                    .setShortLabel(createShortLabel(context.getString(R.string.quicknote)))
                    .setLongLabel(createLongLabel(context.getString(R.string.quicknote)))
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_shortcut_quicknote))
                    .setIntent(openQuickNote)
                    .build();
            newShortcuts.add(shortcutQuickNote);

            // Generate shortcuts for the most recent documents. Maximum of MAX_RECENT_DOCUMENTS.
            AppSettings settings = new AppSettings(context);
            List<String> recentDocuments = settings.getRecentDocuments();

            int count = 0;
            for (String filePath : recentDocuments) {
                if (count > MAX_RECENT_DOCUMENTS) break;
                count++;

                File file = new File(filePath);

                Intent openFile = new Intent(context, DocumentRelayActivity.class)
                        .setAction(Intent.ACTION_EDIT)
                        .setData(Uri.fromFile(file));

                final String name = file.getName();
                newShortcuts.add(new ShortcutInfo.Builder(context, ID_PREFIX + name)
                        .setShortLabel(createShortLabel(name))
                        .setLongLabel(createLongLabel(name))
                        .setIcon(Icon.createWithResource(context, R.mipmap.ic_shortcut_file))
                        .setIntent(openFile)
                        .build());
            }

            shortcutManager.setDynamicShortcuts(newShortcuts);
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    /**
     * Create a short label. Text will be truncated to 10 characters.
     *
     * @param input Input text
     * @return Truncated input text
     */
    private static String createShortLabel(String input) {
        return (input.length() > 10) ? input.substring(0, 7) + "..." : input;
    }

    /**
     * Create a long label. Text will be truncated to 25 characters.
     *
     * @param input Input text
     * @return Truncated input text
     */
    private static String createLongLabel(String input) {
        return (input.length() > 25) ? input.substring(0, 22) + "..." : input;
    }
}
