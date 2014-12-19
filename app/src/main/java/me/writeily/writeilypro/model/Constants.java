package me.writeily.writeilypro.model;

/**
 * Created by jeff on 2014-04-11.
 */
public class Constants {
    // ----- FILE EXTENSIONS -----
    public static final String WRITEILY_EXT = ".writeily";
    public static final String TXT_EXT = ".txt";
    public static final String MD_EXT = ".md";
    public static final String MARKDOWN_EXT = ".markdown";
    public static final String MDOWN_EXT = ".mdown";

    public static final String UTF_CHARSET = "utf-8";

    public static final int MAX_TITLE_LENGTH = 25;
    public static final String USER_PIN_KEY = "user_pin";
    public static final String SET_PIN_ACTION = "set_pin";
    public static final String FOLDER_DIALOG_TAG = "folder_dialog_tag";
    public static final String FOLDER_NAME = "folder_name";
    public static final String FILESYSTEM_FOLDER_PATH = "folder_name";
    public static final String FILESYSTEM_FILE_NAME = "filesystem_file_name";

    // ----- DIALOG TAGS -----
    public static final String SHARE_DIALOG_TAG = "share_dialog_tag";
    public static final String SHARE_BROADCAST_TAG = "share_broadcast_tag";
    public static final String BROADCAST_EXTRA = "broadcast_extra";
    public static final String SHARE_TYPE_TAG = "share_type_tag";
    public static final String FILESYSTEM_IMPORT_DIALOG_TAG = "filesystem_import_dialog_tag";
    public static final String FILESYSTEM_MOVE_DIALOG_TAG = "filesystem_move_dialog_tag";

    // ----- KEYS -----
    public static final String CURRENT_DIRECTORY_DIALOG_KEY = "current_dir_folder_key";
    public static final String FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY = "FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY";
    public static final String FILESYSTEM_FOLDER_ACCESS_TYPE = "FILESYSTEM_FOLDER_ACCESS_TYPE";
    public static final String FILESYSTEM_FILE_ACCESS_TYPE = "FILESYSTEM_FILE_ACCESS_TYPE";
    public static final String NOTE_KEY = "note_key";
    public static final String MD_PREVIEW_KEY = "md_preview_key";

    // ----- DEFAULT FOLDERS -----
    public static final String WRITEILY_FOLDER = "/writeily/";
    public static final String NOTES_FOLDER = "/writeily/notes/";
    public static final String ARCHIVED_FOLDER = "/writeily/archived/";

    // ----- REQUEST CODES -----
    public static final int SET_PIN_REQUEST_CODE = 3;
    public static final int FILESYSTEM_ACTIVITY_FOLDER_REQUEST_CODE = 2;

    // ----- SHARE INT ENUMERATIONS -----
    public static final int SHARE_TXT_TYPE = 0;
    public static final int SHARE_HTML_TYPE = 1;

    // ----- HTML PREFIX AND SUFFIXES -----
    public static final String UNSTYLED_HTML_PREFIX = "<html><body>";
    public static final String MD_HTML_PREFIX = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#303030}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#0099ff;text-decoration:underline;}}</style></head><body>";
    public static final String DARK_MD_HTML_PREFIX = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#ffffff;background-color:#303030}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#ffffff;text-decoration:underline;}a:visited{color:#dddddd}}</style></head><body>";
    public static final String MD_HTML_SUFFIX = "</body></html>";
}
