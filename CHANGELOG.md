### Recent changes
- See [Discussions](https://github.com/gsantner/markor/discussions), [Issues](https://github.com/gsantner/markor/issues) and [Project news](https://github.com/gsantner/markor/blob/master/NEWS.md) to see what is going on.
- New Updates also visible here: <https://github.com/gsantner/markor/releases>

### v2.12 series
- [2.12.0](https://github.com/gsantner/markor/compare/v2.11.1...v2.12.0)
- [2.12.1](https://github.com/gsantner/markor/compare/v2.12.0...v2.12.1)
- [2.12.2](https://github.com/gsantner/markor/compare/v2.12.1...v2.12.2)
- [2.12.3](https://github.com/gsantner/markor/compare/v2.12.2...v2.12.3)
- [2.12.4](https://github.com/gsantner/markor/compare/v2.12.3...v2.12.4)
- [2.12.5](https://github.com/gsantner/markor/compare/v2.12.4...v2.12.5)
- [2.12.6](https://github.com/gsantner/markor/compare/v2.12.5...v2.12.6)

### v2.11 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v211---asciidoc-csv-and-org-mode-todotxt-advanced-search-line-numbers))
- Reworked attachments (PR #2106 by @harshad1)
- Editor/viewer: Side margin improvements 2, closes #2111 (PR #2119 by @guanglinn)
- Format: Add Orgmode - SyntaxHighlight basic support (PR #2107 by @bigger124 @gsantner)
- Editor/viewer: Side margin improvements, closes #2111 (PR #2118 by @guanglinn)
- Reformat code, by @gsantner
- Update translations (PR #2108)
- ShareInto: Automatically remove new YouTube tracking parameter si
- New folder in copy/move dialog, closes #2093 (PR #2098)
- AsciiDoc: Support view-mode light theme, by @TimReset (#1880 #2091 #2092)
- Update translations (PR #2071)
- Line numbers improvements, by @harshad1 @guang-lin @gsantner (PR #2090)
- Feature: Add Line numbers support (Issue #2057, PR #2062, by @guang-lin)
- Chunked undo redo, by @harshad1 (#2052)
- Update translations (PR #2056)
- Update CSV documentation and NEWS/CHANGELOG (PR #2058)
- Markdown: Text converter do not make duplicate header id (closes #2045, by @gsantner)

### v2.10 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v210---custom-file-templates-share-into-remove-url-tracking-parameters))
- [Search: Keep in-files search dialog open, closes #1663, by @harshad1 (PR #1689)](https://github.com/gsantner/markor/pull/1689/files)
- [File management: Support UTF-8 with BOM, by @tifish (PR #1693)](https://github.com/gsantner/markor/pull/1693/files)
- [Editor: Increase performance on editables, more chunked operations, by @harshad1 (PR #1694)](https://github.com/gsantner/markor/pull/1694/files)
- [Dialogs: Don't show OK button at dialogs that require specific selection, by @gsantner closes #1699, closes #1700](https://github.com/gsantner/markor/commit/b4ae32bf0e8ab890ded57718a9598da7a7d52870)
- [Sync clients: Remove irritating file paths from unsupported dialog, by @gsantner, closes #1705](https://github.com/gsantner/markor/issues/1699)
- [Check file existance case insensitive (Android filesystem usually is insensitive), by @gsantner, closes #1695](https://github.com/gsantner/markor/issues/1695)
- [Improve Dialog OK button constraints #1699, by @harshad1 (PR #1720)](https://github.com/gsantner/markor/pull/1720/files)
- [dotFiles: Hide "*_files" and "*.assets" from browser->save page to html, by @tifish (PR #1704)](https://github.com/gsantner/markor/pull/1704/files)
- [More deterministic save / resume - Switching on every tab move, by @harshad1 (PR #1736)](https://github.com/gsantner/markor/pull/1736/files)
- [Improve file handling (hash calc) & FileInfo detection, by @harshad1 (PR #1719)](https://github.com/gsantner/markor/pull/1719/files)
- [Use style/AppTheme.Unified.StartupFlash for DocumentActivity, by @gsantner, closes #1717](https://github.com/gsantner/markor/issues/1717)
- [Editor: Disable richtext pasting which can lead to dropped characters, by @gsantner, closes #1614](https://github.com/gsantner/markor/issues/1614)
- [New file dialog: Custom file templates, using snippets folder (<notebook>/.app/snippets, by @gsantner, closes #676](https://github.com/gsantner/markor/issues/676)
- [Various performance improvements, by @harshad1 (PR #1735)](https://github.com/gsantner/markor/pull/1735/files)
- [ShareInto: Filter few additional tracking parameters in shared URLs, by @gsantner](https://github.com/gsantner/markor/issues/1490)
- [Launcher shortcuts: Open ToDo & QuickNote at bottom, by @harshad1 (PR #1748)](https://github.com/gsantner/markor/pull/1748/files)
- [SearchDialog: Improve condition calculation for simple dialogs, by @harshad1 (PR #1751)](https://github.com/gsantner/markor/pull/1751/files)
- [todo.txt filter search: Disable highlighting at completed tasks, by @harshad1 (PR #1754)](https://github.com/gsantner/markor/pull/1754/files)
- [Editor: Restore behaviour - don't overwrite newer files unless content modified, by @harshad1 (PR #1758)](https://github.com/gsantner/markor/pull/1758/files)

### v2.9 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v29---snippets-templates-graphs-charts-diagrams-yaml-front-matter-chemistry))
- Updates are officially only available from GitHub and F-Droid as of now
- New feature: Snippets
- Discussion forum & questions now on Markor GitHub Discussion
- Faster Markor application startup with less flashing
- Improve performance at filebrowser and editor
- Allow to install APK files from filebrowser on click
- Markdown Table of Contents options
- Zim: Follow links to other wiki pages
- Under the hood improvements for I/O, widgets & syntax highlighting
- todo.txt: Better browsing at editor with dialog & saved search queries
- Settings option to enable/disable Chrome Custom Tabs
- Privacy settings option to disallow screenshots of Markor
- Debloat & drop experimental/unused features, i.e. todo.txt huuid
- Support Android Day/Night theme system
- Filebrowser: Show full filename (multiline allowed instead of singleline only)
- View mode: Open links to folders in filebrowser
- Markdown: Better Math support, add mhchem chemistry module
- Markdown: Add support flowcharts with Mermaid
- Markdown: Add admonition extension (create fancy info boxes quickly)
- Per-file settings - In addition to the global settings, many options are now also configurable on a file basis
- Markdown: Display YAML fron-matter contents (like article titles and publish date)

### v2.8 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v28---multi-selection-for-todotxt-dialogs))
- Reference to GitHub discussion on More page, by @gsantner
- Add multi-selection to todo.txt dialogs, by @harshad1 @gsantner
- In-content search support for encrypted files, #1388 by @opensource21
- Remove alternative todo.txt naming (tags/categories), by @gsantner

### v2.7 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v27---search-in-content-backup--restore-settings))
- Recursive file search with in-content search, #1337 by @adelobosko @harshad1 @gsantner
- Backup and restore settings, Format selection with radio buttons, #1244 by @harshad1 @gsantner
- Per-file font size, #1332 by @harshad1
- Markdown: Support superscript^2^ syntax, #1268 by @gsantner
- View mode: Image/PDF export whole page, add seperate screenshot option, by @gsantner
- todo.txt: Fix trailing space resulting in contexts/projects/due-dates to be entered twice, #1282 by @harshad1
- Markdown: Enable GitLab extension, display video links as html5-video, #1280 by @gsantner
- Markdown: Enable typographic transformation, #1277 by @gsantner
- todo.txt: Fix tags dialog not shows up onLongClick, #1292 by @gsantner
- Editor: Allow top-menu back button also when the file is empty, #1290 by @harshad1
- ShareInto: Improve automatic link reformatting, #1275 by @harshad1
- Search dialogs: Hide search input field when if there is no data, #1298 by @harshad1
- Decrease scrollbar width for better usability, #1306 by @harshad1
- todo.txt: Settings option for always-visible @contexts & +projects, #1305 by @harshad1 @gsantner
- TextActions: Improve cursor placement at Regex replace, #1310 by @harshad1
- File manager: Move/Copy file improvements, add Yes/No/All overwrite options, #1281 by @harshad1
- Fix App might crash on toolbar-click for TOC, #1336 by @adelobosko
- Fix Android 4.4 crash on file move/copy, #1333 by @harshad1
- Fix Android 4.4 crash when opening .txt file due to ZimWiki format detection, #1341 by @fredericjacob
- Markdown: Don't match extra spaces at ordered-list regex, #1367 by @harshad1
- File Manager: Fix MB being displayed as GB at description (SI 1000 unit), #1352 by @gsantner
- File Manager: Duplicate file / allow copy into same folder, #1345 by @harshad1- Filemanager formatter: fix MB being displayed as GB (SI 1000 unit), #1352 by @gsantner

### v2.6 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v26---zim-wiki-newline--new-paragraph-save-format))
- Markdown: Add settings option for newlines to start new paragraphs, #1260 by @gsantner
- Editor/Viewer: Remember last used file format, show current selected format, #1226 by @harshad1
- Editor/Viewer: Back arrow (top menu) finish activity, #1165 by @gsantner
- Editor: Per-file option to enable/disable syntax highlighting, #1168 by @harshad1
- Share-Into: Add launcher, #1184 by @gsantner
- Markdown: Apply Markor Table of Content config for custom `[TOC]: #` too, #1189 by @gsantner
- Editor: Improve writing to sdcard, #1192 by @gsantner
- Zim: Support file generation on `Android<7/Java=6`, #1194 by @gsantner
- Zim: Editor: Support Table of contents (top menu), #1186 by @fredericjacob
- Markdown: Math/KaTex: Improve \\ line breaks usage, #1196 by @radanovicnik
- ShareInto: Add space after formatted link - messengers then show correct link preview, by @gsantner
- Markdown: Add break page example to Markdown reference, by @gsantner
- Editor: Prevent Android accessibility & autofill to produce errors, #1204 by @harshad1
- Main page: Reduce friction when app was running in background for a while, #1210 by @harshad1
- Search: Add input field to filter search results, #1222 by @harshad1
- Markdown: Don't start new list item when reaching file end and toggling, #1213 by @harshad1
- Zim: Simplify Zim format detection, #1227 by @gsantner
- Zim: Add more text actions (links, images, checkbox, ..), #1195 by @fredericjacob
- All formats: Date/Time dialog don't add entry twice to history, #1229 by @harshad1
- Editor/Viewer: Increase scrollbar width, #1241 by @harshad1
- File browser: File move start from current folder, #1234 by @harshad1
- Editor/Viwer: Add file info option (document top menu), #1233 by @harshad1
- Viewer: Privacy: Opt-out of Android WebView's internal metrics, #1181 by @gsantner
- Markdown: Support Notable's special home brewed syntax for attachments, #1252 by @gsantner
- Dependencies: Add source code of colorpicker and build subproject, by @gsantner
- Optimize image assets, by @gsantner
- DevOps: Improvements to GitHub Actions CI/CD configuration, by @gsantner
- Improve encryption wording & usage, #1171 #1179 by @opensource21


### v2.5 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v25---zim-wiki---search--replace---zettelkasten))
- Add Zim Wiki format and template #1098
- Add search & replace (simple|regex, replace once|all) #1112
- Add settings for current file to toolbar #1129
- Fix file sometimes not opens from launcher shortcut #1139
- Use GitHub Actions for CI/CD #1151
- Add template for Zettelkasten #1156
- Add Nord editor color scheme #1134
- Allow to select folder when create new file via share into Markor #1138
- Improve license dialog readability #1119
- General improvements, fixes and translations

### v2.4 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v24---all-new-todotxt---programming-language-syntax-highlighting))
- Markdown: Correctly insert or remove list item on press enter at empty list item
- Remove title from todo.txt date dialog, better usable on small devices
- Fix search sometimes not working when chaging from view to edit mode
- Rework Indent & Move lines Actions
- Add settings option to control todo.txt completition date auto insert
- All new todo.txt support in Markor
- Add many languages to view-mode code highlighting
- Add xlf format (plaintext)
- Remove colored highlighting from changelog dialog
- Add search to Markdown edit-mode outline/TOC dialog
- New file dialog remember type selection
- Better preserve current open folder across device rotation & reboot
- todo.txt: Long press sort to sort by most recent used method
- Markdown: Improve bold/italic syntax highlighting with punctuations
- todo.txt: Create done file again when not exists
- Add Actions: Move line up & down, start new line
- Insert Date/Time text action: List of recent time formats
- Control visibility of text actions
- Edit-Mode Search: Open selected position instead of first match
- File browser: Add settings option to customize file description format
- todo.txt: Syntax highlighting in edit-mode search dialog
- Save last used folder to settings, use info for titlebar
- New File Dialog: Remember type selection

### v2.3 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v23---table-of-contents-custom-action-order))
- Add action to Move current selected line(s)/cursor text up/down
- Add settings option for View-Mode link color
- Improve table of contents - add border, disable underline
- Long press toolbar to jump to top/bottom (edit & view mode)
- Add search to View Mode
- Accessibility improvements & Talkback support
- Allow http protocol on Android>=9
- Telegram file edit support
- Markdown: Normal sized headers by default, increases performance
- Disable highlighting on big files to improve edit performance
- Don't sort non-document files in third group
- Add Accordion (Click to expand) example and add action button
- Tooltips for action buttons
- For index.html files, show foldername at favourites/recents
- todo.txt: Set completition date also when there is no creation date
- Markdown: Configurable unordered list character
- Custom order of action buttons
- Markdown: Add alternative more performant heading highlighting
- Fix foldername in Main toolbar not reloaded
- Plaintext: Add extensions for AsciiDoc (.adoc), OrgMode (.org), Ledger (.dg .ledger), Diff (.diff .patch)
- Remember last used file extension for new file creation
- todo.txt: Preselect last used archive file by default for archiving
- Markdown: Long press code to insert code block
- todo.txt: Improved task sort functionalities
- Add action button to expand selection of cursror to whole line
- Markdown: Add Table of contents / Outline for Edit mode (Press toolbar)
- Vertical Scrollbar now draggable at view & edit mode
- todo.txt: Date&Time selection dialogs
- Markdown: Auto update ordered list numbers


### v2.2 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v22---presentations-voice-notes-markdown-table-editor))
- Added Presentations & Slides with Markdown
- Added audio recording dialog which allows to add voice to documents. Manual interaction required to start & stop voice recording. Voice recording permission required for this feature
- Added editor button to create Markdown tables
- Markdown Footnotes support added
- Added attachment button for all formats (insert color, link, image, file, audio, date)
- Date/Time button long press now inserts text with last used format
- Improved SD Card reading & writing
- Added option to File-import-dialog to import to notebook instead of current folder
- Reordered editor buttons so global actions are on same position at all formats
- Source code highlighting for View mode
- Added settings option to enable experimental features
- New experimental feature: Convert epub to plaintext and replace current text with ebook
- New experimental feature: Speed Reading for (text from) edit mode
- New Special Keys option: Indent / Deindent current line
- Copy textfile to clipboard from file browser
- Added highlighting todo.txt due dates
- Long press the todo.txt date button to insert due date
- Sepia editor theme


### v2.1 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v21---key-value-highlighting-jsoniniyamlcsv-improved-performance))
- Improved editor, highlighting and overall performance
- New file dialog: Templates
- New format: KeyValue - highlighting for json, ini, csv, yaml, vcard, ics, toml and other simple key-value like syntax
- Long click on main view plus button -> open favourites/recents
- Use lightweight Markdown heading highlighting on non-highend devices
- Show SD Card dialog when opening file that is under SAF
- Share: Rename PDF -> Print/PDF 
- Text action to sort todo by date
- Keep view mode scroll position
- Remove LinkBox from main screen to improve performance
- Make filesystem selection dialog fill screen
- Rework share into: Use file browser to select favourite/recent/popular files
- Special keys added: Insert page break for PDF/Printing, ohm key, punctation mark arrows
- Append linefeed on end when saving
- Show error when trying to rename to existing file/folder
- Add special handling for percent encoded filenames in nextcloud/owncloud folder
- Link 'More->Help' to Project website FAQ
- Debug Log settings option
- Improve local/linked file opening when clicking link at preview
- Add option to set font size in view mode
- Share (multiple) files from file browser
- PlaintextConverter: Put HTML into preview as is (allow to view html files)
- Fix folder title not visible sometimes
- Enable hex color highlighting for various prefix/postfixes (like colon, quote, ...)

### v2.0 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v20---search-dotfiles-pdf-export))
- Recursive file & folder search
- Search button in editor, viewer and file browser
- Always export PDF and images with light theme and white background (improves printing)
- Show dialog on for textfiles to choose open in Markor or other app
- Setting to set file extensions to always load in Markor
- Always view files starting with "index."
- Setting to configure wrap mode (=line breaking)
- Menu option for reload file (editor/viewer)
- Menu option for hiding files & folders starting with a dot
- Setting to set tab width
- Improve back button when always start view mode is set
- Keep file browser sort order
- Improve inline code highlighting
- Add new line when archiving tasks


### v1.8 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v18---all-new-file-browser-favourites-and-faster-markdown-preview))
- Show app intro at first start  
- All new file navigation  
- Add favourite files  
- Add quick navigiation options (to notebook, sdcard, AppData and more)  
- Add option to set Navigation-Bar color  
- Combine edit & view mode to one fragment, show view as overlay  
- Add horizontal scrolling for code blocks in view mode  
- More efficient undo/redo  
- Option to enable/disable swipe to change mode  
- WikiLinks: Disable default escaped characters, so subfolder path is not converted to hyphen  
- Added fonts: Source Pro, DejaVu Sans Mono, Ubuntu, Lato  
- Scan storage Fonts folder for custom fonts  
- Add word count to document info dialog  


### v1.7 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v17---custom-fonts-linkbox-with-markdown))
- Improved app color theme for better readability  
- Load custom fonts from file  
  - Markor bundles 5 additional open fonts  
  - Copy custom fonts to folder: 'Notebook/.app/fonts/'  
- Links shared from e.g. browsers are automatically converted to Markdown syntax if possible  
- LinkBox is now listed on the main view bottom bar  
- LinkBox defaults on new installations to LinkBox.md as filename  
- Default to last used date/time format at dialog  
- Apply todo.txt format only for .txt files  
- L/R Swipe in edit/representation mode to change mode  
- Open link textaction: Don't include trailing ')' in parsed URL, which is common for markdown  
- Added App Shortcuts, requires Android 7.1+  
- Markdown: Enable WikiLink style to reference [[file]] relative  
- Strip #ref from URL in representation to determine if another file should be opened on click  
- Option to set app start tab (Notebook / ToDo / QuickNote / LinkBox / More)  


### v1.6 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v16---datetime-dialog---jekyll-and-katex-improvements))
**New features:**  
- TextAction: Insert date/time  
- Add website title when sharing into Markor, if browser supports it  
  - Website title + URL formatted in Markdown format if possible

**Improved:**  
- Automatically create ToDo/linkbox/QuickNote and parent folders when using respective launcher  
- KaTex/Math: Improve inline math  
- Close virtual keyboard after creating new file  
- Language selection: Load system's most important language as system hint  
- Markdown + Jekyll: Replace {{ site.baseurl }} with .. in representation  
- More padding at settings on older devices  
- Use the new file dialog for sharing into new documents
- Filesystem dialog now shows images / textfiles only at respective file selection  

**Fixed:**  
- New file dialog: Jekyll option on older devices  
- Title not updated when swiping  

### v1.5 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v15---multiple-windows-markdown-tasks-theming))
**New features:**  
- App-wide  
  - Settings option: Keep screen on
- Editor  
  - Open multiple Windows  
- Document browser  
  - Completly new 'New file' dialog  
- Text Actions  
  - Sort todo.txt files  
  - Tasks support in Markdown  

**Improved:**  
- Document browser  
  - Replace 'Reload button' with pull down to refresh  
  - Added 'Last modified' to File information dialog  
- Editor  
  - Added greenscale basic editor colors  
- Representation  
  - Set inital background color before loading document  
  - Math/KaTex: Show inline when single dollar is used

### v1.4
**New features:**  
- App-wide  
  - Add popular documents to 'share into'  
- Editor  
  - Settings options for editor background and foreground color  
  - todo.txt: Highlight multiple levels of context/projects (@@/++)  
- Text Actions  
  - Add zero-width space character to 'special characters'  
  - Add color picker  

**Improved:**  
- TextActions  
  - Markdown: Multiline textaction for header/quote/list  
- Editor  
  - More space for document title  
  - Harden automatic file naming and moving  
- Representation  
  - Enable block rendering for KaTex (math)  
- App-wide  
  - Natural scrolling in dialogs  

**Fixed:**  
- Filesystem  
  - Discard selection when leaving filesystem view  

### v1.3
**New features:**  
- App-wide  
  - Add 'Auto' theme, switch light/dark theme by current hour  
  - Support for Chrome Custom Tabs
- Editor
  - Start document at the recent cursor position (jump to bottom on new documents and at special files)  
  - Enable link highlighting in plaintext format (especially easier to distinguish title and links in linkbox)  
- Text Actions  
  - Markdown: Long press image adds img-src with max-height  
  - Long press 'Special key' jumps to top/bottom  
  - Long press 'Open external' opens content search  
 
**Improved:**  
- TextActions  
  - Don't list empty lines in simple search  
  - Edit picture supports now relative filepaths too  
  - Show import dialog for selected pictures too (like in file selection)  
- Representation  
  - Renamed from Preview
  - Performance improvement for TOC & Math - only use when text contains headers/math  
  - Markdown: Underline h2 too (like h1, more common for two levels)  
  - ToDo: Add alternative naming for contexts/projects  
- App-wide  
  - 

**Fixed:**  
- Editor
  - Disable 'History disable' performance option for data integrity
- App-wide
  - Special files: When app launcher was used, create file if not exists   

### v1.2 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v12---markdown-with-katexmath---search-in-current-document))
**New features:**
- General
  - Launchers to directly open LinkBox/ToDo/QuickNote (opt-in)
- Text Actions
  - Search/filter lines by input (available in special-keys button menu)
  - Todo: context aware search for projects,contexts (longpress project/context button)
- Preview
  - Table of contents (opt-in))
  - Math using KaTex (opt-in)

**Improved:**
- Converter
  - Markdown: More features enabled, notably GFM like table parsing and underlined h1
- Settings
  - More spacing between categories

**Fixed:**
- Editor
  - File saving

### v1.1 ([Details](https://github.com/gsantner/markor/blob/master/NEWS.md#markor-v11---markdown-picture-import-from-gallery-and-camera))
**New features:**  
- Text Module Actions
  - Markdown Picture Dialog
  - Load picture from gallery
  - Take picture with camera
  - Edit picture with graphics app

**Improved:**  
- Formats
  - Load Markdown Format for .md.txt files

**Fixed:**  
- Editor
  - Change default lineheight back to 100%
  - Not connects multiple lines anymore
- Filesystem view
  - More checks for storage access and the yellow info box

### v1.0.1
**New features:**  
- Add popular files (most used files by access count)
- Add popular & recent files as virtual folder under /storage/
  - Selectable e.g. for widgets

**Improved:**  
- Text-Module-Actions
  - More safety checks at execution
- Highlighting
  - MD: Better code readability
  - MD: Better unordered list readability

### v1.0.0
**New features:**  
- ShareInto
  - Added export: calendar appointment

**Improved:**  
- Widget
  - Added shortcuts to ToDo, QuickNote and LinkBox
- SD Card handling and permission errors
  - Show warning when opening a file on not writeable path
  - Add shortcuts to writeable SD card folders
  - Mark unwriteable files red in selection dialog
- ShareInto
  - Better separator placement

**Fixed:**  
- Widget
  - Open selected file
- Editor
  - Markdown header highlighting padding
- Share to app
  - Fix view intent not starting on some devices
- Filesystem view
  - Allow to view Details for folder too

### v0.3.10
**New features:**  
- ShareInto
  - Show "open in browser" option if text contains link
  - Prepend separator to all existing documents
- Settings / Preview
  - User customizeable CSS/JS injection option (for preview)
  - Configureable in settings
  - Contains some (uncommented) modification lines for important elements
  - like font size, font type, script to load when page loaded etc.

**Improved:**  
- Inherit font size from global font preference

**Fixed:**  
- Recents working without having opened anything yet

### v0.3.9
**Improved:**  
- Translation updated
- Updated project description
- Slightly modified adaptive icon

**Fixed:**  
- Editor-Rotation: Creates new file again when editing before
- Create folder: Screen rotation

### v0.3.8
**New features:**  
- Recently viewed documents
  - Start editing of recent documents, button in the toolbar of main view
  - Allow sharing into recend documents
  - Queue containing the 10 last viewed files
- Keep scroll position when reloading document list (Notebook)
- Document/File Info: Dialog showing information about selected file
  - Openable at main views toolbar when one item is selected

**Improved:**  
- Overall better performance 
  - Faster document loading
  - Decreased memory usage
- Reduce edit history size (undo/redo) to 5 for lower memory usage
- Preview/Rendering (All):
  - Rework of theme, font-size and font injection
- Preview/Rendering (Markdown):
  - Blockquote theme based styling
  - Blockquote RTL compatibility

**Fixed:**  
- Crash when Markor put to background and huge file is loaded
  - Document contents are not stored into resume cache anymore if they are too big
  - Make no major differences for huge files, just undo/redo history is cleared when switchting away

### v0.3.7
- Option to disable spellchecking-underline
- More file managers and sync clients supported (notably: seafile)
- Improve default settings
- Limit history size to improve performance
- Support UTF8 local filename references

### v0.3.6
- Decrease padding of textmoduleactions for more fitting elements
- Add delete action for all formats
- Add open link in browser moduleaction
- Fix actionmode icon color
- Share into:: Add LinkBox option
- Share into:: Fix re-sharing of text

### v0.3.5
- All new More section
- All new "Share into" handling
- Fix: Keep dates when priority is assigned
- Mod: Disable colored,underlined header text
- Add: Enable zoom gestures in Preview
- Mod: md::link/image filesystem selection in working directory
- Remove MoreFragment and AboutActivity
- Share into:: Editable text
- Share into:: Re-Share option
- Share into:: Copy to clipboard option
- Share into:: Landscape support
- Update settings
- Improve permission checking

### v0.3.4
- Replace commonmark markdown parser with flexmark
- Various smaller bug fixes
- Updated translations
- Settings option: Edit in screen center
- Add ".." in folder selection to go up

### v0.3.3
- Add support for editing files from most file managers
- Allow to open from Own/NextCloud
- Hints about using Markor with Dropbox
- Allow to set document folder outside of internal storage
- Fix import dialog orientation crash
- Add option to start editing on bottom
- md: moduleaction: end line with 2 spaces
- Improve project icon
- Trim share-into text
- Improved exporting/sharing


### v0.3.2
- Todotxt: Support delete, archive tasks
- Todotxt: Try to keep cursor position
- Translation updates :)

### v0.3.1
- Option for custom line height
- Remember cursor position when switching away from app
- Special keyboard actions (Page up, Tab,..)

### v0.3.0
- New project icon
- Settings ordered in subscreens
- Additional syntax information

### v0.2.5
- Improve highlighter performance
- Improve default highlighter settings
- Added new highlting settings options
- Preference support lib for settings

### v0.2.4
- Abstract highlighting, converter and text module actions
- Added: Support for todo.txt
- Added: Icons to settings
- Added: Hex-Color-Code underlining
- Added: Changelog dialog

### v0.2.3
- Select  file/image from filesystem
- Fix relative web local file loading
- Added: manually save option
- Added: Launcher shortcuts
- Filesystem: Add refresh menu option

### v0.2.2
- Show document and file amount below folders
- Settings toolbar option
- Highlight line-endings with two spaces (MD line break)
- Translation updates

### v0.2.1
- Translation update
- Widget and document list fixes
- Improve editor actions

### v0.2.0
- Rework of core functionalities of the app
- Added QuickNote
- Redo/Undo editing
- Separate Preview/Edit by fragments, unify functions
- Rewrite storage/handling of documents
- Improved sharing into the app, allow appending
- Added bottom navigation
- New font chooser in settings

### v0.1.6
- Added: Many new supported languages
- More supported markdown elements in highlighter
- More markdown file extensions supported
- Improved language selection
- Share as image fixed


### v0.1.5
- Added: Translation: Brazilian, Polish, Hindi, French, Russian, Ukrainian, Italian
- Added: Sort files
- Added: Replace markdown charbar with actions
- Improved: Syntax highlighting

### v0.1.4
- Fixed: replaced Uri.fromFile with FileProvider
- Added: Share as file, share as PDF
- Use commonmark-java instead of AndDown markdown parser
- Added: Spanish translation (#26
- Added: More font size options

### v0.1.3
- Fix: Renaming case sensitive
- Mod: Syntax highlighting
- Fix: Widget
- Added: Copyright notices
- Changed: Translation license

### v0.1.2
- Overall refactoring
- Remove startup ""authentication""
- Remove slow animations
- FilesystemDialog from scratch
- Save only if title/content changed
- Rework most callbacks and broadcasts
- Rework settings

### v0.1.1
- Fix import (#2)
- Use appcompat in dialogs
- Change cursor in editor
- Fix resizing with fullscreen in editor
- Dialogs in dark

### v0.1.0
- Initial release
- Start of community project Markor
- Fork of writeily-pro
- Different branding
- New initial features

### v
**New features:**  
-

**Improved:**  
-

**Fixed:**  
-
