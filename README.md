[![Translate](https://img.shields.io/badge/translate-crowdin-green.svg)](https://crowdin.com/project/markor/invite)
[![Donate say thanks](https://img.shields.io/badge/donate-say%20thanks-red.svg)](https://gsantner.net/page/supportme.html?project=markor&source=readme)
[![Chat - Matrix](https://img.shields.io/badge/chat-matrix-blue.svg)](https://matrix.to/#/#markor:matrix.org) [![Chat - FreeNode IRC](https://img.shields.io/badge/chat-irc-blue.svg)](https://kiwiirc.com/client/irc.freenode.net/?nick=markor-anon|?#markor) ![](https://test.gsantner.net/matomo/piwik.php?action_name=readme&idsite=2&rec=1&urlref=https%3A%2F%2Fgithub.com%2Fgsantner%2Fmarkor%2FREADME.md&_cvar=%7B%221%22%3A%5B%22source%22%2C%22readme%22%5D%2C%222%22%3A%5B%22project%22%2C%22markor%22%5D%2C%223%22%3A%5B%22packageid%22%2C%22net.gsantner.markor%22%5D%2C%224%22%3A%5B%22referrer%22%2C%22https%3A%2F%2Fgithub.com%2Fgsantner%2Fmarkor%2FREADME.md%22%5D%7D)
[![Build Status](https://travis-ci.org/gsantner/markor.svg?branch=master)](https://travis-ci.org/gsantner/markor)
[![GitHub release](https://img.shields.io/github/tag/gsantner/markor.svg)](https://github.com/gsantner/markor/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/gsantner/markor/total.svg?logo=github&logoColor=lime)](https://github.com/gsantner/markor/releases)
[![Codacy Code quality](https://img.shields.io/codacy/grade/aff869c440bc48b7bd64680e97cbc453)](https://www.codacy.com/app/gsantner/markor)


# Markor
<img src="/app/src/main/ic_launcher-web.png" align="left" width="100" hspace="10" vspace="10">
<b>Text editor - Notes &amp; ToDo (for Android)</b>.
<br/>Simple and lightweight, supporting Markdown and todo.txt<br/><br/>

<div style="display:flex;" >
<a href="https://f-droid.org/repository/browse/?fdid=net.gsantner.markor">
    <img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">
</a>
<a href="https://play.google.com/store/apps/details?id=net.gsantner.markor">
    <img alt="Get it on Google Play" height="80" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>
</div></br>

Markor is a TextEditor for Android. This project aims to make an editor that is versatile, flexible, and lightweight. Markor utilizes simple markup formats like Markdown and todo.txt for note-taking and list management. Markor is versatile at working with text, it can also be used for keeping bookmarks, copying to clipboard, fast opening a link from text and lots of more. Created files are interoptable with any other plaintext software on any platform.Markor is using open formats and is free software, openly developed and accepts community contributions.

## Features
📝 Create notes and manage your to-do list using simple markup formats
<br/>🌲 Work completely offline - whenever, wherever
<br/>👌 Compatible with any other plaintext software on any platform -- edit with notepad or vim, filter with grep, convert to PDF or create a zip archive
<br/>
<br/>🖍 Syntax Highlighting and format related actions -- quick insert pictures and to-do
<br/>👀 Convert, preview, and share documents as HTML and PDF
<br/>
<br/>📚 Notebook: Store all documents on a common filesystem folder
<br/>📓 QuickNote: Fast accessible for keeping notes
<br/>☑️ To-Do: Write down your to-do
<br/>🖍 Formats: Markdown, todo.txt, csv, ics, ini, json, toml, txt, vcf, yaml  
<br/>📋 Copy to clipboard: Copy any text, including text shared into Markor
<br/>💡 Notebook is the root folder of documents and can be changed to any location on filesystem. QuickNote and ToDo are textfiles
<br/>
<br/>🎨 Highly customizeable, dark theme available
<br/>💾 Auto-Save with options for undo/redo
<br/>👌 No ads or unnecessary permissions
<br/>🌎 Language selection -- use other language than on the system
<br/>
<br/>🔃 Markor is an offline app. It works with sync apps, but they have to do syncing respectively. Sync clients known to work in combination include BitTorrent Sync, Dropbox, FolderSync, OwnCloud, NextCloud, Seafile, Syncthing, Syncopoli

### Privacy<a name="privacy"></a>
The app doesn't use the internet connection unless external resources (e.g. display image  by URL) are referenced in user generated content. 
The app is working completly offline, no internet connection required! No personal data will be requested or shared with the author or third parties (i.e. calendar or contacts).
Files can be shared to other apps from inside the app by pressing the share button.
Files are stored locally in a user selectable folder, defaulting to the device public documents folder.

#### Android Permissions
* WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE  
  Read and write files from/to device storage.
* INTERNET  
  In user generated content data can be loaded from the internet.
* INSTALL_SHORTCUT  
  Install shortcut to launchers to open a file/folder in Markor.
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
* F-Droid Store: [Listing](https://f-droid.org/packages/net.gsantner.markor/) [2](https://f-droid.org/repository/browse/?fdid=net.gsantner.markor) | [Wiki](https://f-droid.org/wiki/page/net.gsantner.markor) | [Metadata](https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/net.gsantner.markor.txt) | [Build log](https://f-droid.org/wiki/page/net.gsantner.markor/lastbuild)
* Google Play Store: [Listing](https://play.google.com/store/apps/details?id=net.gsantner.markor&utm_source=reporeadme) | [Dev Console](https://play.google.com/apps/publish/?p=net.gsantner.markor&#AppDashboardPlace:p=net.gsantner.markor) | [Crash60](https://play.google.com/apps/publish/?p=net.gsantner.markor&#AndroidMetricsErrorsPlace:p=net.gsantner.markor&appVersion=PRODUCTION&lastReportedRange=LAST_60_DAYS)
* More download options: [Aptoide](https://markor.en.aptoide.com/) | [GitHub Releases(https://github.com/gsantner/markor/releases)

## Licensing
The code of the app is licensed Apache 2.0 or Commerical (See [LICENSE](/LICENSE.txt) for details).  
Localization & translation files (string\*.xml) are licensed CC0 1.0.  
Project is based on unmaintained projects writeily and writeily-pro.



<!--
### Notice
-->
