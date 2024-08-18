[![GitHub releases](https://img.shields.io/github/tag/gsantner/markor.svg)](https://github.com/gsantner/markor/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/gsantner/markor/total.svg?logo=github&logoColor=lime)](https://github.com/gsantner/markor/releases)
[![Translate on Crowdin](https://img.shields.io/badge/translate-crowdin-green.svg)](https://crowdin.com/project/markor)
[![Community Discussion](https://img.shields.io/badge/chat-community-blue.svg)](https://github.com/gsantner/markor/discussions)
[![GitHub CI](https://github.com/gsantner/markor/workflows/CI/badge.svg)](https://github.com/gsantner/markor/actions)


# Markor
<img src="/app/src/main/ic_launcher-web.png" align="left" width="128" hspace="10" vspace="10">
<b>Text editor - Notes &amp; ToDo (for Android)</b>.
<br/>Simple and lightweight, supporting Markdown, todo.txt, Zim & more!<br/><br/>

**Download:**  [F-Droid](https://f-droid.org/repository/browse/?fdid=net.gsantner.markor), [GitHub](https://github.com/gsantner/markor/releases/latest)

Markor is a TextEditor for Android.
This project aims to make an editor that is versatile, flexible, and lightweight.
Markor utilizes simple markup formats like Markdown and todo.txt for note-taking and list management.
It is versatile at working with text; it can also be used for keeping bookmarks, copying to clipboard, fast opening a link from text and lots more.
Created files are interoperable with any other plaintext software on any platform.
Markor is openly developed free software that accepts community contributions.

![Screenshots](https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/99-123.jpg)  
![Screenshots](https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/99-456.jpg)  

## Features
📝 Create notes and manage your to-do list using simple markup formats
<br/>🌲 Work completely offline -- whenever, wherever
<br/>👌 Compatible with any other plaintext software on any platform -- edit with notepad or vim, filter with grep, convert to PDF or create a zip archive
<br/>
<br/>🖍 Syntax Highlighting and format related actions -- quickly insert pictures and to-dos
<br/>👀 Convert, preview, and share documents as HTML and PDF
<br/>
<br/>📚 Notebook: Store all documents on a common filesystem folder
<br/>📓 QuickNote: Fast accessible for keeping notes
<br/>☑️ To-Do: Write down your to-do
<br/>🖍 Formats: Markdown, todo.txt, Zim/WikiText, Plaintext, [csv](doc/2023-06-02-csv-readme.md), ics, ini, json, toml, vcf, yaml
<br/>📋 Copy to clipboard: Copy any text, including text shared into Markor
<br/>💡 Notebook is the root folder of documents and can be changed to any location on the filesystem. QuickNote and To-Do are textfiles
<br/>
<br/>🎨 Highly customizable, dark theme available
<br/>💾 Auto-Save with options for undo/redo
<br/>👌 No ads or unnecessary permissions
<br/>🌎 Language selection -- use other language than on the system
<br/>
<br/>🔃 Markor is an offline app. It works with sync apps, but they have to do syncing respectively.
<br/>🔒 Can encrypt your textfiles with AES256. You need to set a password at the settings and use Android device with version Marshmallow or newer. You can use [jpencconverter](https://gitlab.com/opensource21/jpencconverter) to encrypt/decrypt on desktop. Be aware that only the text is encrypted not pictures or attachments.

## New features in the latest update - Markor v2.11 - AsciiDoc, CSV and Org-Mode, Todo.txt advanced search, Line numbers

### Line number support

Markor supports showing line numbers now. In the top file menu you can find a new option to enable numbers.
It is supported in editor as well in view mode of documents (in code blocks).

![Line numbers](doc/assets/2023-10-11-line-numbers.webp)

### New format: AsciiDoc
AsciiDoc is one of the new formats that are now supported.
While it might be not as much fleshed out like Markdown, it should fit for general use.

![AsciiDoc](doc/assets/2023-10-11-asciidoc.webp)

### New format: CSV
[CSV file](https://en.wikipedia.org/wiki/Comma-separated_values) are supported now (in sense of syntax highlighting and preview). 
For details see [CSV README](doc/2023-06-02-csv-readme.md), it was implemented in #1988, #1987, #1980, #1667.

* Editor with SyntaxHighlighter
* Each csv column is shown in a different unique color to see which csv-data belongs to which colum/header
* Preview as html-Table with export as pdf
* A csv column may contain markdown (See Example column in screenshot)

![](doc/assets/csv/2023-06-25-csv-landscape.webp)

### New format: Org-Mode
The third and last new format newly added is Org-Mode. Note that currently only editor syntax highlighting and action buttons to make editing easier are available.
There is no dedicated view mode implemented.

![Org-Mode](doc/assets/2023-10-07-orgmode.webp)

### Navigation
* [**README**](README.md)
  * [Features](README.md#features)
  * [Contribute](README.md#contribute)
  * [Develop](README.md#develop)
  * [Privacy](README.md#privacy)
  * [License](README.md#license)
* [**FAQ**](README.md#FAQ)
  * [File browser, file management](README.md#file-browser--file-management)
  * [Format: Markdown](README.md#format-markdown)
  * [Format: todo.txt](README.md#format-todotxt)
* [**More**](doc)
  * [Synced plaintext TODO and notes - Vim / Vimwiki, Markor Android, Syncthing, GTD (Pitt)](doc/2020-09-26-vimwiki-sync-plaintext-to-do-and-notes-todotxt-markdown.md)
  * [Markor: How to synchronize files with Syncthing (wmww,tengucrow)](doc/2020-04-04-syncthing-file-sync-setup-how-to-use-with-markor.md)
  * [Using Markor to Write (and More) on an Android Device (The Plain Text Project)](doc/2019-07-16-using-markor-to-write-on-an-android-device-plaintextproject.md)
  * [How I Take Notes With Vim, Markdown, and Pandoc (Vaughan)](doc/2018-05-15-pandoc-vim-markdown-how-i-take-notes-vaughan.md)
* [**NEWS**](NEWS.md)
  * [Markor v2.11 - AsciiDoc, CSV and Org-Mode, Todo.txt advanced search](NEWS.md#markor-v211---asciidoc-csv-and-org-mode-todotxt-advanced-search-line-numbers) 
  * [Markor v2.10 - Custom file templates, Share Into automatically remove URL tracking parameters](NEWS.md#markor-v210---custom-file-templates-share-into-automatically-remove-url-tracking-parameters)
  * [Markor v2.9 - Snippets, Templates, Graphs, Charts, Diagrams, YAML front-matter, Chemistry](NEWS.md#markor-v29---snippets-templates-graphs-charts-diagrams-yaml-front-matter-chemistry)
  * [Markor v2.8 - Multi-selection for todo.txt dialogs](NEWS.md#markor-v28---multi-selection-for-todotxt-dialogs)
  * [Markor v2.7 - Search in content, Backup & Restore settings](NEWS.md#markor-v27---search-in-content-backup--restore-settings)
  * [Markor v2.6 - Zim Wiki, Newline = New Paragraph, Save Format](NEWS.md#markor-v26---zim-wiki-newline--new-paragraph-save-format)
  * [Markor v2.5 - Zim Wiki - Search & Replace - Zettelkasten](NEWS.md#markor-v25---zim-wiki---search--replace---zettelkasten)
  * [Markor v2.4 - All new todo.txt - Programming language syntax highlighting](NEWS.md#markor-v24---all-new-todotxt---programming-language-syntax-highlighting)
  * [Markor v2.3 - Table of Contents, Custom Action Order](NEWS.md#markor-v23---table-of-contents-custom-action-order)
  * [Markor v2.2 - Presentations, Voice notes, Markdown table editor](NEWS.md#markor-v22---presentations-voice-notes-markdown-table-editor)
  * [Markor v2.1 - Key-Value highlighting (json/ini/yaml/csv), improved performance](NEWS.md#markor-v21---key-value-highlighting-jsoniniyamlcsv-improved-performance)
  * [Markor v2.0 - Search, dotFiles, PDF export](NEWS.md#markor-v20---search-dotfiles-pdf-export)
  * [Markor v1.8 - All new file browser, favourites and faster Markdown preview](NEWS.md#markor-v18---all-new-file-browser-favourites-and-faster-markdown-preview)
  * [Markor v1.7 - Custom Fonts, LinkBox with Markdown](NEWS.md#markor-v17---custom-fonts-linkbox-with-markdown)
  * [Markor v1.6 - DateTime dialog - Jekyll and KaTex improvements](NEWS.md#markor-v16---datetime-dialog---jekyll-and-katex-improvements)
  * [Markor v1.5 - Multiple windows, Markdown tasks, theming](NEWS.md#markor-v15---multiple-windows-markdown-tasks-theming)
  * [Markor v1.2 - Markdown with KaTex/Math - Search in current document](NEWS.md#markor-v12---markdown-with-katexmath---search-in-current-document)
  * [Markor v1.1 - Markdown picture import from gallery and camera](NEWS.md#markor-v11---markdown-picture-import-from-gallery-and-camera)
  * [Markor v1.0 - Widget shortcuts to LinkBox, ToDo, QuickNote](NEWS.md#markor-v10---widget-shortcuts-to-linkbox-todo-quicknote)
  * [Markor v0.3 - Faster loading, LinkBox added, Open link in browser TextAction](NEWS.md#markor-v03---faster-loading-linkbox-added-open-link-in-browser-textaction)



## Contribute
* **Programming**  
  The project is always open for contributions and welcomes merge requests. Take a look at our [issue tracker](https://github.com/gsantner/markor/issues) for open issues, especially "[good first issues](https://github.com/gsantner/markor/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22)".
* **Feature requests & discussions**  
  Start a discussion [here](https://github.com/gsantner/markor/discussions).
* **Bug reports**  
  Report issues [here](https://github.com/gsantner/markor/issues). Please [search](https://github.com/gsantner/markor/issues?q=) for similar issues & [requests](https://github.com/gsantner/markor/discussions?discussions_q=) first. If it's not a bug, please head to discussions.
* **Localization**  
  Translate on [Crowdin](https://crowdin.com/project/markor) (free).

## Develop
Clone the project using git. Then open the project in [Android Studio](https://developer.android.com/studio) (recommended), install required Android SDK dependencies where required.
You may also use any other plaintext editor of your preference.

There is a Makefile in the project which makes it easy to test, lint, build, install & run the application on your device. See the Makefile for reference.
You can find binaries (.apk), logs, test results & other outputs in the dist/ directory.  
Example: `make all install run`.

The project code style is the [AOSP Java Code Style](https://source.android.com/source/code-style#follow-field-naming-conventions). Use the _auto reformat_ menu option of Android Studio before commiting or before you create the pull request.

### Technologies / Dependencies
* Java, Android SDK, AndroidX
* No dependency on NDK, 1 APK = all Android supported Architectures
* Editor: Advanced component based on Android EditText
* Preview: Android WebView
* Editor syntax highlighting: Custom implementation for all supported formats
* Markdown parser: [flexmark-java](https://github.com/vsch/flexmark-java/wiki/Extensions)
* Zim/WikiText parser: Custom implementation, transpiling to Markdown
* todo.txt parser: Custom implementation
* Binary support: WebView html img/audio/video with support for most common formats
* CI/CD: GitHub Actions
* Build system: Gradle, Makefile

### Resources
* Project repository: [Changelog](CHANGELOG.md) | [Issues](https://github.com/gsantner/markor/issues?q=is%3Aissue+is%3Aopen) | [Discussions](https://github.com/gsantner/markor/discussions) | [License](/LICENSE.txt) | [GitHub Releases](https://github.com/gsantner/markor/releases) | [Makefile](Makefile)
* F-Droid: [Listing](https://f-droid.org/packages/net.gsantner.markor) | [Wiki](https://f-droid.org/wiki/page/net.gsantner.markor) | [Metadata](https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/net.gsantner.markor.yml) | [Build log](https://f-droid.org/wiki/page/net.gsantner.markor/lastbuild)


## Privacy<a name="privacy"></a>
Markor does not use your internet connection unless your own user-generated content references external resources (for example, when you reference an external image by URL).
The app works completely offline, no internet connection required!
No personal data is shared with the author or any third parties.
Files can be shared to other apps from inside the app by pressing the share button.
Files are stored locally in a user selectable folder, defaulting to the internal storage "Documents" directory.

#### Android Permissions
* WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE  
  Read from and write files to storage.
* INTERNET  
  In user-generated content data can be loaded from the internet.
* INSTALL_SHORTCUT  
  Install a shortcut to launchers to open a file/folder in Markor.
* RECORD_AUDIO  
  Attach voice notes to the text. The permission is only used after click on the "attach audio" button, at the audio record dialog. Audio recording is always started and stopped manually by you (button press).

## License
The code of the app is licensed Apache 2.0.  
Localization & translation files (string\*.xml) as well samples are licensed CC0 1.0 (public domain).  
Project is based on the unmaintained projects writeily and writeily-pro.











# FAQ

## File browser & file management

#### How do I save a file?
Markor automatically saves text when you leave Markor or close a file. Additionally there is save button in the top menu.

#### How do I save files to SD Cards?
Browse to the start folder of your SD Card and press the + button (using file browser or the menu option). Now press the plus button and follow the steps in the dialog. Afterwards Markor's file browser won't strike out filenames anymore and files are writable.

![sdcard-mount](doc/assets/2019-05-06-sdcard-mount.webp)

#### How to synchronize files?
Markor is and will stay an offline focused application. It works with sync synchronization apps, they have to do syncing respectively.
Sync clients known to work in combination include BitTorrent Sync, Dropbox, FolderSync, OwnCloud, NextCloud, Seafile, Syncthing, Syncopoli and others.  
The project recommendation is Syncthing. [-> Guide for Syncthing](doc/2020-04-04-syncthing-file-sync-setup-how-to-use-with-markor.md)

#### What is Notebook?
The root folder of your files! Markor starts with this folder at the main screen and allows you to browse files. You can work at any (accessible) file & location with Markor.

#### What is ToDo?
Your main to-do list file in todo.txt format. You can access it by swiping once at the main screen, by selecting todo.txt at Notebook, or by using the dedicated launcher. You can also open it from Notebook or other apps! You will also have the option to create a to-do task when sharing text into Markor when the text is just one line. The location of this file is freely choosable and independent from the Notebook directory.

#### What is QuickNote?
The fastest and easiest way to take notes! QuickNote is a file in Markdown format with a freely choosable file location. You can access it by swiping twice at the main screen, by selecting QuickNote at Notebook, or by using the dedicated launcher. The location of this file is freely choosable and independent from the Notebook directory.

#### Launchers
A launcher is a "start menu option" in your devices launcher (=appdrawer / start menu). When Markor is installed you have the start menu option for Markor. When the Markor settings option "Launcher (Special Documents)" is enabled, you get the additional start menu options for ToDo and QuickNote. Note that a device restart is required when you change this option.

## Format: Markdown
#### What is Markdown?
A general purpose markup format for documents of all kinds. As Markdown gets converted to HTML prior displaying a rendered view, you can also include HTML in the text, thus you can do everything web browsers can do.

CommonMark is the specification that the markdown parser used in Markor implements.

| **Resources** | |
|-----------------------------------------------------------------------|------------------------------|
| [CommonMark tutorial](http://commonmark.org/help/tutorial/)           | Learn Markdown in 10 minutes |
| [CommonMark help](http://commonmark.org/help/)                        | Quick reference and interactive tutorial for learning Markdown. |
| [CommonMark Spec](http://spec.commonmark.org/)                        | CommonMark Markdown Specification |
| [daringfireball](https://daringfireball.net/projects/markdown/syntax) | Syntax documentation the Markdown creator |


#### Links to files that contains spaces
Most Markdown applications use URL encoding for links, so does Markor. This means replace every space` ` with `%20`. This ensures that your Markdown content is compatible with most other Markdown applications.<br/><br/>

Markor has a dedicated button for adding links and file references, which automatically applies this appropiate format.  Take a look at this [video](https://user-images.githubusercontent.com/6735650/63089879-e6aa9400-bf48-11e9-87c1-78a1ba1c444f.gif) to find out where the file reference button is located and how to use it.<br/><br/>

Example: `[alt](my cool file.md)` ⮕ `[alt](my%20cool%20file.md)`.

#### Can I use Markor in class to write down equations? (Math)
Yes, Markor has advanced functionalities for math! Enable the feature by checking Settings»Format»Markdown»Math.<br/><br/>

Markor's [markdown-reference.md](samples/markor-markdown-reference.md) template (available from new file dialog) showcases some examples.  
Learn more about available functions and symbols here: [1](https://katex.org/docs/supported.html), [2](https://katex.org/docs/support_table.html)


## Format: todo.txt
#### What is todo.txt?
Todo.txt is a simple text format for todo. Each line of text is a task. The idea comes from [Gina Trapani](https://github.com/ginatrapani).

| **Resources** | |
|---------------------------------------------------------------------------------------|----------------------|
| [Homepage](http://todotxt.org/)                                                       | Todo.txt's home      |
| [Format](https://github.com/todotxt/todo.txt/blob/master/README.md)                   | Syntax documentation |
| [User Documentation](https://github.com/todotxt/todo.txt-cli/wiki/User-Documentation) | User documentation   |


![todotxt](doc/assets/todotxt-format.png)

#### How to mark a task done?
Done tasks are marked by a `x ` in begining of the line and can optionally be moved to a done/archive file.

#### What is a context (@)?
  With contexts you can mark a situation or place. You may use it to categorize your todos. Context is part of todo.txt format, add `@` in front of a word to create one.  
  Examples: @home @work

#### What is a project (+)?
  With projects you can group tasks by a specific project. You may use it to tag your todos with recognizable meta information. Context is part of todo.txt format, add `+` in front of a word to create one.  
  Examples: +video +download +holidayPlanning

