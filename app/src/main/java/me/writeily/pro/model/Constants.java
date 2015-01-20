package me.writeily.pro.model;

/**
 * Created by jeff on 2014-04-11.
 */
public class Constants {
    public static final String[] KEYBOARD_SHORTCUTS = {"*", "-", "_", "#", "(", ")", "[", "]"};
    public static final String UTF_CHARSET = "utf-8";

    public static final int MAX_TITLE_LENGTH = 20;
    public static final String SET_PIN_ACTION = "set_pin";
    public static final String FOLDER_DIALOG_TAG = "folder_dialog_tag";
    public static final String FOLDER_NAME = "folder_name";
    public static final String FILESYSTEM_FILE_NAME = "filesystem_file_name";

    // ----- DIALOG TAGS -----
    public static final String SHARE_BROADCAST_TAG = "share_broadcast_tag";
    public static final String FILESYSTEM_IMPORT_DIALOG_TAG = "filesystem_import_dialog_tag";
    public static final String FILESYSTEM_MOVE_DIALOG_TAG = "filesystem_move_dialog_tag";
    public static final String CONFIRM_DIALOG_TAG = "confirm_dialog_tag";

    // ----- KEYS -----
    public static final String CURRENT_DIRECTORY_DIALOG_KEY = "current_dir_folder_key";
    public static final String FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY = "FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY";
    public static final String FILESYSTEM_FOLDER_ACCESS_TYPE = "FILESYSTEM_FOLDER_ACCESS_TYPE";
    public static final String FILESYSTEM_FILE_ACCESS_TYPE = "FILESYSTEM_FILE_ACCESS_TYPE";
    public static final String NOTE_KEY = "note_key";
    public static final String MD_PREVIEW_BASE = "md_preview_base";
    public static final String MD_PREVIEW_KEY = "md_preview_key";
    public static final String USER_PIN_KEY = "user_pin";

    // ----- DEFAULT FOLDERS -----
    public static final String WRITEILY_FOLDER = "/writeily/";

    // ----- REQUEST CODES -----
    public static final int SET_PIN_REQUEST_CODE = 3;

    // ----- HTML PREFIX AND SUFFIXES -----
    public static final String UNSTYLED_HTML_PREFIX = "<html><body>";
    public static final String MD_HTML_PREFIX = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#303030}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#0099ff;text-decoration:underline;}}</style></head><body>";
    public static final String DARK_MD_HTML_PREFIX = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#ffffff;background-color:#303030}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#ffffff;text-decoration:underline;}a:visited{color:#dddddd}}</style></head><body>";
    public static final String MD_HTML_SUFFIX = "</body></html>";
    public static final String NOTE_SOURCE_DIR = "note_source_dir";
}
