package net.gsantner.markor.util;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import java.io.Serializable;

/**
 * Broadcasts Helper for broadcasts sent & received in app
 */
public class AppCast {
    //########################
    //## Send broadcast
    //########################
    private static void sendBroadcast(Context c, Intent i) {
        if (c != null) {
            LocalBroadcastManager.getInstance(c).sendBroadcast(i);
        }
    }

    //########################
    //## Basics
    //########################
    private static class PathExtra {
        public static final String EXTRA_PATH = "EXTRA_PATH";

        static Intent getFolderNameIntent(String action, String path) {
            Intent intent = new Intent(action);
            intent.putExtra(EXTRA_PATH, path);
            return intent;
        }
    }

    //########################
    //## Filter
    //########################
    public static IntentFilter getLocalBroadcastFilter() {
        IntentFilter intentFilter = new IntentFilter();
        String[] BROADCAST_ACTIONS = {
                CURRENT_FOLDER_CHANGED.ACTION,
                CREATE_FOLDER.ACTION,
                CONFIRM.ACTION
        };
        for (String action : BROADCAST_ACTIONS) {
            intentFilter.addAction(action);
        }
        return intentFilter;
    }

    //########################
    //## Actions
    //########################
    public static class CURRENT_FOLDER_CHANGED extends PathExtra {
        public static final String ACTION = "CURRENT_FOLDER_CHANGED";
        public static final String EXTRA_ROOT_FOLDERPATH = "ROOT_FOLDERPATH";

        public static void send(Context c, String path, String root) {
            Intent intent = getFolderNameIntent(ACTION, path);
            intent.putExtra(EXTRA_ROOT_FOLDERPATH, root);
            sendBroadcast(c, intent);
        }
    }

    public static class CREATE_FOLDER extends PathExtra {
        public static final String ACTION = "CREATE_FOLDER";

        public static void send(Context c, String path) {
            sendBroadcast(c, getFolderNameIntent(ACTION, path));
        }
    }

    public static class RENAME extends PathExtra {
        public static final String ACTION = "RENAME";
        public static final String EXTRA_RENAME_TO_NAME = "RENAME_TO"; // Just name, no path

        public static void send(Context c, String path, String renameToName) {
            Intent intent = getFolderNameIntent(ACTION, path);
            intent.putExtra(EXTRA_RENAME_TO_NAME, renameToName);
            sendBroadcast(c, intent);
        }
    }

    public static class CONFIRM {
        public static final String ACTION = "CONFIRM";
        public static final String EXTRA_WHAT = "WHAT";
        public static final String EXTRA_DATA = "DATA";

        public static void send(Context c, String what, Serializable data) {
            Intent intent = new Intent(ACTION);
            intent.putExtra(EXTRA_WHAT, what);
            intent.putExtra(EXTRA_DATA, data);
            sendBroadcast(c, intent);
        }
    }
}
