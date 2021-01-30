[![GitHub releases](https://img.shields.io/github/tag/gsantner/markor.svg)](https://github.com/gsantner/markor/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/gsantner/markor/total.svg?logo=github&logoColor=lime)](https://github.com/gsantner/markor/releases)
[![Translate on Crowdin](https://img.shields.io/badge/translate-crowdin-green.svg)](https://crowdin.com/project/markor/invite)
[![Donate - say thanks](https://img.shields.io/badge/donate-say%20thanks-red.svg)](https://gsantner.net/page/supportme.html?project=markor&source=readme)
[![Chat on Matrix](https://img.shields.io/badge/chat-matrix-blue.svg)](https://matrix.to/#/#markor:matrix.org)
[![GitHub CI](https://github.com/gsantner/markor/workflows/CI/badge.svg)](https://github.com/gsantner/markor/actions)
[![Codacy code quality](https://img.shields.io/codacy/grade/aff869c440bc48b7bd64680e97cbc453)](https://www.codacy.com/app/gsantner/markor)


# Markor
<img src="/app/src/main/ic_launcher-web.png" align="left" width="128" hspace="10" vspace="10">
<b>Text editor - Notes &amp; ToDo (for Android)</b>.
<br/>Simple and lightweight, supporting Markdown and todo.txt<br/><br/>

<div style="display:flex;" >
<a href="https://f-droid.org/repository/browse/?fdid=net.gsantner.markor">
    <img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="64">
</a>
<a href="https://play.google.com/store/apps/details?id=net.gsantner.markor">
    <img alt="Get it on Google Play" height="64" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>
</div><br/>

Markor is a TextEditor for Android.
This project aims to make an editor that is versatile, flexible, and lightweight.
Markor utilizes simple markup formats like Markdown and todo.txt for note-taking and list management.
It is versatile at working with text; it can also be used for keeping bookmarks, copying to clipboard, fast opening a link from text and lots more.
Created files are interoperable with any other plaintext software on any platform.
Markor is openly developed free software that accepts community contributions.

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
<br/>🖍 Formats: Markdown, todo.txt, csv, ics, ini, json, toml, txt, vcf, yaml  
<br/>📋 Copy to clipboard: Copy any text, including text shared into Markor
<br/>💡 Notebook is the root folder of documents and can be changed to any location on the filesystem. QuickNote and To-Do are textfiles
<br/>
<br/>🎨 Highly customizable, dark theme available
<br/>💾 Auto-Save with options for undo/redo
<br/>👌 No ads or unnecessary permissions
<br/>🌎 Language selection -- use other language than on the system
<br/>
<br/>🔃 Markor is an offline app. It works with sync apps, but they have to do syncing respectively. Sync clients known to work in combination include BitTorrent Sync, Dropbox, FolderSync, OwnCloud, NextCloud, Seafile, Syncthing, Syncopoli
<br/>🔒 Can encrypt your textfiles with AES256. You need to set a password at the settings and use Android device with version Marshmallow or newer. You can use [jpencconverter](https://gitlab.com/opensource21/jpencconverter) to encrypt/decrypt easily on desktop. Be aware that only the text is encrypted not pictures or attachments.

### Privacy<a name="privacy"></a>
Markor does not use your internet connection unless external resources (e.g. display image by URL) are referenced in user generated content.
The app is working completly offline, no internet connection required!
No personal data will be requested or shared with the author or third parties (i.e. calendar or contacts).
Files can be shared to other apps from inside the app by pressing the share button.
Files are stored locally in a user selectable folder, defaulting to the device public documents folder.

#### Android Permissions
* WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE  
  Read and write files from/to device storage.
* INTERNET  
  In user generated content data can be loaded from the internet.
* INSTALL_SHORTCUT  
  Install a shortcut to launchers to open a file/folder in Markor.
* RECORD_AUDIO  
  Markor allows to attach voice notes to the text. The permission is only used when clicking the "attach audio" button/menu to start the audio record dialog. Audio recording is always started and stopped by the user (button press).

## Demo
<div style="display:flex;" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/01.jpg" width="19%" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/02.jpg" width="19%" style="margin-left:10px;" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/03.jpg" width="19%" style="margin-left:10px;" >
</div>

<div style="display:flex;" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/04.jpg" width="19%" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/05.jpg" width="19%" style="margin-left:10px;" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/06.jpg" width="19%" style="margin-left:10px;" >
</div>

<div style="display:flex;" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/07.jpg" width="19%" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/08.jpg" width="19%" style="margin-left:10px;" >
	<img src="https://raw.githubusercontent.com/gsantner/markor/master/metadata/en-US/phoneScreenshots/09.jpg" width="19%" style="margin-left:10px;" >
</div>

## Contributions
* **Development (Source code)**  
  The project is always open for contributions and welcomes merge requests. Take a look at our [issue tracker](https://github.com/gsantner/markor/issues) for open issues, especially those tagged with [good first issue](https://github.com/gsantner/markor/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) and [help wanted](https://github.com/gsantner/markor/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22).
The project uses [AOSP Java Code Style](https://source.android.com/source/code-style#follow-field-naming-conventions). Additionally: Use `_camelCase` instead of `mCamelCase` for class members. Use Android Studios _auto reformat feature_ before sending a MR.
* **Localization**  
  Translate on [Crowdin](https://crowdin.com/project/markor/invite) (free).

#### Resources
* Project: [Changelog](/CHANGELOG.md) | [Issues](https://github.com/gsantner/markor/issues?q=is%3Aissue+is%3Aopen) [Help Wanted Issues](https://github.com/gsantner/markor/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22) | [License](/LICENSE.txt)
* F-Droid: [Listing](https://f-droid.org/packages/net.gsantner.markor) | [Wiki](https://f-droid.org/wiki/page/net.gsantner.markor) | [Metadata](https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/net.gsantner.markor.txt) | [Build log](https://f-droid.org/wiki/page/net.gsantner.markor/lastbuild)
* Google Play: [Listing](https://play.google.com/store/apps/details?id=net.gsantner.markor&utm_source=reporeadme)  
* Further download options: [Aptoide](https://markor.en.aptoide.com/) | [GitHub Releases](https://github.com/gsantner/markor/releases)

## Licensing
The code of the app is licensed Apache 2.0 or Commerical (See [LICENSE](/LICENSE.txt) for details).  
Localization & translation files (string\*.xml) are licensed CC0 1.0.  
Project is based on the unmaintained projects writeily and writeily-pro.
