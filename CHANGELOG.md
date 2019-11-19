### v2.3 (in progress)
- Add action to Move current selected line(s)/cursor text up/down by 1
- Add settings option for View-Mode link color
- Improve table of contents - add border around, disable underline
- Update Markdown parser
- Long press toolbar to jump to top/bottom (edit & view mode)
- Add search to View Mode
- Accessibility improvements & Talkback support
- Allow http on Android>=9
- Telegram file edit support
- Add accordion dropdown markdown button
- Yellow icon for favourite files
- Markdown: Normal sized headers by default, increases performance
- Disable highlighting on big files to improve edit performance
- Don't sort non-document files in third group

### v2.2 [Blog Post](https://gsantner.net/blog/2019/10/27/markor-v2.2-markdown-presentations-voicenotes-audiorecord-tables.html?source=changelog)
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


### v2.1 [Blog post](https://gsantner.net/blog/2019/09/04/markor-v2.1-search-dotfiles-pdf-export.html?source=inapp_changelog&project=markor) | [Code changes](https://github.com/gsantner/markor/compare/v2.0.0...v2.1.0)  
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

### v2.0 [Blog post](https://gsantner.net/blog/2019/07/03/markor-v2.0-search-dotfiles-pdf-export.html?source=inapp_changelog&project=markor) | [Code changes](https://github.com/gsantner/markor/compare/v1.8.0...v2.0.0)  
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


### v1.8 [Blog post](https://gsantner.net/blog/2019/05/06/markor-v1.8-file-navigation-favourites-markdown-editor.html?source=inapp_changelog&project=markor) | [Code changes](https://github.com/gsantner/markor/compare/v1.7.0...v1.8.0)  
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


### v1.7 [Blog post](https://gsantner.net/blog/2019/03/11/markor-release-v1.7-custom-fonts-linkbox-markdown.html?source=inapp_changelog&project=markor)  
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


### v1.6 [Blog post](https://gsantner.net/blog/2018/12/30/markor-release-v1.6.html?source=inapp_changelog&project=markor)
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

### v1.5 [Blog post](https://gsantner.net/blog/2018/12/09/markor-release-v1.5.html?source=inapp_changelog&project=markor)
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

### v1.2 [Blog post](https://gsantner.net/blog/2018/09/18/markor-release-v1.2.html?source=inapp_changelog&project=markor)
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

### v1.1 [Blog post](https://gsantner.net/blog/2018/09/09/markor-release-v1.1.html?source=inapp_changelog&project=markor)
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
