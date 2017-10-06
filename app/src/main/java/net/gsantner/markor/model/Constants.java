/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.model;

import net.gsantner.markor.BuildConfig;

import java.util.regex.Pattern;

public class Constants {
    public static final String[] KEYBOARD_SHORTCUTS = {"*", "-", "_", "#", "´"};
    public static final String[] KEYBOARD_SHORTCUTS_BRACKETS = {"(", ")", "[", "]"};
    public static final String[] KEYBOARD_SMART_SHORTCUTS = {"()", "[]"};
    public static final String[] KEYBOARD_SHORTCUTS_MORE = {"!", "+", "~", ":", "<", ">"};
    public static final String UTF_CHARSET = "utf-8";

    public static final String MD_EXT1 = ".md";
    public static final String MD_EXT2 = ".markdown";

    public static final int MAX_TITLE_LENGTH = 20;
    public static final String EXTRA_FOLDERPATH = "filesystem_folderpath";
    public static final String EXTRA_FILEPATH = "filesystem_filepath";

    // ----- DIALOG TAGS -----
    public static final String SHARE_BROADCAST_TAG = "share_broadcast_tag";
    public static final String FILESYSTEM_IMPORT_DIALOG_TAG = "filesystem_import_dialog_tag";
    public static final String FILESYSTEM_MOVE_DIALOG_TAG = "filesystem_move_dialog_tag";
    public static final String FILESYSTEM_SELECT_FOLDER_TAG = "filesystem_select_folder_dialog_tag";

    // ----- KEYS -----
    public static final String CURRENT_DIRECTORY_DIALOG_KEY = "current_dir_folder_key";
    public static final String NOTE_KEY = "note_key";
    public static final String MD_PREVIEW_BASE = "md_preview_base";
    public static final String MD_PREVIEW_KEY = "md_preview_key";

    // ----- HTML PREFIX AND SUFFIXES -----
    public static final String UNSTYLED_HTML_PREFIX = "<html><body>";
    public static final String MD_HTML_PREFIX = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#303030;}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#388E3C;text-decoration:underline;}img{height:auto;width:325px;margin:auto;}</style>";
    public static final String DARK_MD_HTML_PREFIX = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#ffffff;background-color:#303030;}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#388E3C;text-decoration:underline;}a:visited{color:#dddddd;}img{height:auto;width:325px;margin:auto;}</style>";
    public static final String MD_HTML_PREFIX_END = "</head><body>";
    public static final String MD_HTML_RTL_CSS = "<style>body{text-align:right; direction:rtl;}</style>";
    public static final String MD_HTML_SUFFIX = "</body></html>";
    public static final String TARGET_DIR = "note_source_dir";

    // ----- INTENT EXTRAS -----
    public static Pattern MD_EXTENSION = Pattern.compile("((?i)\\.md$)");

    // --- WIDGET
    public static final String WIDGET_PATH = "WIDGET_PATH";

    //FILE PROVIDER AUTHORITIES
    public static final String FILE_PROVIDER_AUTHORITIES = BuildConfig.APPLICATION_ID + ".provider";
}
