/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

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

        static Intent getIntentWithPathExtra(String action, String path) {
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
                VIEW_FOLDER_CHANGED.ACTION,
                CREATE_FOLDER.ACTION
        };
        for (String action : BROADCAST_ACTIONS) {
            intentFilter.addAction(action);
        }
        return intentFilter;
    }

    //########################
    //## Actions
    //########################
    public static class VIEW_FOLDER_CHANGED extends PathExtra {
        public static final String ACTION = "VIEW_FOLDER_CHANGED";
        public static final String EXTRA_FORCE_RELOAD = "EXTRA_FORCE_RELOAD";

        public static void send(Context c, String path, boolean forceReload) {
            Intent intent = getIntentWithPathExtra(ACTION, path);
            intent.putExtra(EXTRA_FORCE_RELOAD, forceReload);
            sendBroadcast(c, intent);
        }
    }

    public static class CREATE_FOLDER extends PathExtra {
        public static final String ACTION = "CREATE_FOLDER";

        public static void send(Context c, String path) {
            sendBroadcast(c, getIntentWithPathExtra(ACTION, path));
        }
    }
}
