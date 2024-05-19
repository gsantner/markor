/*#######################################################
 *
 * SPDX-FileCopyrightText: 2019-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2019-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/

/*
 * Simple Parser for M3U playlists with some extensions
 * Mainly for playlists with video streams

 */
package net.gsantner.opoc.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple Parser for M3U playlists
 */
@SuppressWarnings({"WeakerAccess", "CaughtExceptionImmediatelyRethrown", "unused", "SpellCheckingInspection"})
public class GsSimplePlaylistParser {
    private final static String EXTINF_TAG = "#EXTINF:";
    private final static String EXTINF_TVG_NAME = "tvg-name=\"";
    private final static String EXTINF_TVG_ID = "tvg-id=\"";
    private final static String EXTINF_TVG_LOGO = "tvg-logo=\"";
    private final static String EXTINF_TVG_EPGURL = "tvg-epgurl=\"";
    private final static String EXTINF_GROUP_TITLE = "group-title=\"";
    private final static String EXTINF_RADIO = "radio=\"";
    private final static String EXTINF_TAGS = "tags=\"";

    // ########################
    // ##
    // ## Members
    // ##
    // ########################

    // Parse m3u file by reading content from file
    public ArrayList<Item> parse(File filepath) {
        try {
            return parse(new FileInputStream(filepath));
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    // Parse m3u file by reading from inputstream
    public ArrayList<Item> parse(InputStream inputStream) {

        StringBuilder text = new StringBuilder();
        String line = "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append("\n");
            }
        } catch (Exception ignored) {
        }

        return parse(text.toString());
    }

    public ArrayList<Item> parse(String text) {
        final AtomicReference<Item> lastEntry = new AtomicReference<>(null);
        final ArrayList<Item> entries = new ArrayList<>();

        text = text.trim().replace("\r", "");

        for (String line : text.split("\n")) {
            try {
                parseLine(line, entries, lastEntry);
            } catch (Exception e) {
                lastEntry.set(null);
            }
        }
        return entries;
    }

    // Parse one line of m3u
    private void parseLine(String line, final List<Item> entries, final AtomicReference<Item> lastEntry) {
        line = line.trim();

        if (line.startsWith(EXTINF_TAG)) {
            // #EXTINF line
            try {
                lastEntry.set(parseExtInf(line));
            } catch (Exception ignored) {
            }
        } else if (!line.isEmpty() && !line.startsWith("#")) {
            // URL line (no comment, no empty line(trimmed))
            lastEntry.compareAndSet(null, new Item());
            lastEntry.get().url = line.trim();
            entries.add(lastEntry.getAndSet(null));
        } else {
            // No useable data -> reset last EXTINF for next entry
            lastEntry.set(null);
        }
    }

    private Item parseExtInf(String line) {
        Item curEntry = new Item();
        StringBuilder buf = new StringBuilder(20);
        if (line.length() < EXTINF_TAG.length() + 1) {
            return curEntry;
        }

        // Strip tag
        line = line.substring(EXTINF_TAG.length());

        // Read seconds (may end with comma or whitespace)
        while (line.length() > 0) {
            char c = line.charAt(0);
            if (Character.isDigit(c) || c == '-' || c == '+') {
                buf.append(c);
                line = line.substring(1);
            } else {
                break;
            }
        }
        if (buf.length() == 0 || line.isEmpty()) {
            return curEntry;
        }
        curEntry.seconds = Integer.parseInt(buf.toString());

        // tvg tags
        String old = null;
        while (!line.isEmpty() && !line.startsWith(",") && !line.equals(old)) {
            old = line = line.trim();
            if (line.startsWith(EXTINF_TVG_NAME) && line.length() > EXTINF_TVG_NAME.length()) {
                line = line.substring(EXTINF_TVG_NAME.length());
                int i = line.indexOf("\"");
                curEntry.tvgName = line.substring(0, i).replace("'", "");
                line = line.substring(i + 1);
            } else if (line.startsWith(EXTINF_TVG_LOGO) && line.length() > EXTINF_TVG_LOGO.length()) {
                line = line.substring(EXTINF_TVG_LOGO.length());
                int i = line.indexOf("\"");
                curEntry.tvgLogo = line.substring(0, i);
                line = line.substring(i + 1);
            } else if (line.startsWith(EXTINF_TVG_EPGURL) && line.length() > EXTINF_TVG_EPGURL.length()) {
                line = line.substring(EXTINF_TVG_EPGURL.length());
                int i = line.indexOf("\"");
                curEntry.tvgEpgUrl = line.substring(0, i);
                line = line.substring(i + 1);
            } else if (line.startsWith(EXTINF_RADIO) && line.length() > EXTINF_RADIO.length()) {
                line = line.substring(EXTINF_RADIO.length());
                int i = line.indexOf("\"");
                curEntry.isRadio = Boolean.parseBoolean(line.substring(0, i));
                line = line.substring(i + 1);
            } else if (line.startsWith(EXTINF_GROUP_TITLE) && line.length() > EXTINF_GROUP_TITLE.length()) {
                line = line.substring(EXTINF_GROUP_TITLE.length());
                int i = line.indexOf("\"");
                curEntry.groupTitle = line.substring(0, i);
                line = line.substring(i + 1);
            } else if (line.startsWith(EXTINF_TVG_ID) && line.length() > EXTINF_TVG_ID.length()) {
                line = line.substring(EXTINF_TVG_ID.length());
                int i = line.indexOf("\"");
                curEntry.tvgId = line.substring(0, i);
                line = line.substring(i + 1);
            } else if (line.startsWith(EXTINF_TAGS) && line.length() > EXTINF_TAGS.length()) {
                line = line.substring(EXTINF_TAGS.length());
                int i = line.indexOf("\"");
                curEntry.tags = line.substring(0, i).split(",");
                line = line.substring(i + 1);
            } else {
                line = line.substring(line.indexOf("\"") + 1);
                line = line.substring(line.indexOf("\"") + 1);
            }
        }

        // Name
        line = line.trim();
        if (line.length() > 1 && line.startsWith(",")) {
            line = line.substring(1);
            line = line.trim();
            if (!line.isEmpty()) {
                curEntry.name = line.replace("'", "");
            }
        }
        return curEntry;
    }

    /**
     * Data class for playlist Entries with getters & setters
     */
    public static class Item {
        public String tvgName, name;
        public String tvgLogo;
        public String tvgEpgUrl;
        public String tvgId;
        public String groupTitle;
        public String url;
        public String[] tags = new String[0];
        public int seconds = -1;
        public boolean isRadio = false;

        public String getName(int... maxLen) {
            final int limitTotal = maxLen != null && maxLen.length > 0 ? maxLen[0] : 999;
            final int limitBegin = Math.round(limitTotal * 0.565f);
            final int limitEnd = Math.round(limitTotal * 0.435f);

            String t = "";
            if (tvgName != null) {
                t = tvgName;
            } else if (name != null) {
                t = name;
            } else if (url != null) {
                t = url.replaceFirst("(?i)https?:..", "").replaceFirst("\\.\\w+$", "");
            }
            t = t.length() < limitTotal ? t : t.replaceFirst("(.{" + limitBegin + "}).+(.{" + limitEnd + "})", "$1…$2");
            return t;
        }

        public String getUrl() {
            return url == null ? "" : url;
        }

        @Override
        public String toString() {
            return getName() + " " + getUrl();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////
    ////
    ///  Examples
    //
/*
    public static class Examples {
        public static List<M3U_Entry> example() {
            OpocSimplePlaylistParser simpleM3UParser = new OpocSimplePlaylistParser();
            File moviesFolder = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MOVIES);
            return simpleM3UParser.parse(new File(moviesFolder, "streams.m3u"));
        }

        public static List<M3U_Entry> exampleWithLogoRewrite() {
            List<M3U_Entry> playlist = new ArrayList<>();
            OpocSimplePlaylistParser simpleM3UParser = new OpocSimplePlaylistParser();
            File moviesFolder = new File(new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_MOVIES), "liveStreams");
            File logosFolder = new File(moviesFolder, "Senderlogos");
            File streams = new File(moviesFolder, "streams.m3u");
            for (M3U_Entry entry : simpleM3UParser.parse(streams)) {
                if (entry.tvgLogo != null) {
                    entry.tvgLogo = new File(logosFolder, entry.tvgLogo).getAbsolutePath();
                }
                playlist.add(entry);
            }
            return playlist;
        }

        public static void startStreamPlaybackInVLC(Activity activity, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.setDataAndTypeAndNormalize(Uri.parse(url), "video/*");
            intent.setPackage("org.videolan.vlc");
            activity.startActivity(intent);
        }
    }
*/
}
