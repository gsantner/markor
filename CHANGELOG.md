### v1.4.0 [Blog post](https://gsantner.net/blog/2018/11/12/markor-release-v1.4.html)
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

### v1.3.0
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

### v1.2.0 [Blog post](https://gsantner.net/blog/2018/09/18/markor-release-v1.2.html)
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

### v1.1.0 [Blog post](https://gsantner.net/blog/2018/09/09/markor-release-v1.1.html)
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
  - Better seperator placment

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
  - Prepend seperator to all existing documents
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
- Seperate Preview/Edit by fragments, unify functions
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
