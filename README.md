[![GitHub releases](https://img.shields.io/github/tag/gsantner/markor.svg)](https://github.com/gsantner/markor/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/gsantner/markor/total.svg?logo=github&logoColor=lime)](https://github.com/gsantner/markor/releases)
[![Translate on Crowdin](https://img.shields.io/badge/translate-crowdin-green.svg)](https://crowdin.com/project/markor/invite)
[![Donate - say thanks](https://img.shields.io/badge/donate-say%20thanks-red.svg)](https://gsantner.net/page/supportme.html?project=markor&source=readme)
[![Community Discussion](https://img.shields.io/badge/chat-community-blue.svg)](https://github.com/gsantner/markor/discussions)
[![GitHub CI](https://github.com/gsantner/markor/workflows/CI/badge.svg)](https://github.com/gsantner/markor/actions)


# Markor
<img src="/app/src/main/ic_launcher-web.png" align="left" width="128" hspace="10" vspace="10">
<b>Text editor - Notes &amp; ToDo (for Android)</b>.
<br/>Simple and lightweight, supporting Markdown, todo.txt, Zim & more!<br/><br/>

<div style="display:flex;" >
<a href="https://f-droid.org/repository/browse/?fdid=net.gsantner.markor">
    <img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="64">
</a>
<a href="https://github.com/gsantner/markor/releases/latest">
    <img alt="Get it on GitHub" height="64" src="https://gsantner.net/assets/blog/img/badge-download-github-apk.png" />
</a>
</div><br/>

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
<br/>🖍 Formats: Markdown, todo.txt, Zim/WikiText, Plaintext, csv, ics, ini, json, toml, vcf, yaml
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


## Contribute
* **Programming**  
  The project is always open for contributions and welcomes merge requests. Take a look at our [issue tracker](https://github.com/gsantner/markor/issues) for open issues, especially "[good first issues](https://github.com/gsantner/markor/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22)".
* **Feature requests & discussions**  
  Start a discussion [here](https://github.com/gsantner/markor/discussions).
* **Bug reports**  
  Report issues [here](https://github.com/gsantner/markor/issues). Please [search](https://github.com/gsantner/markor/issues?q=) for similar issues & [requests](https://github.com/gsantner/markor/discussions?discussions_q=) first. If it's not a bug, please head to discussions.
* **Localization**  
  Translate on [Crowdin](https://crowdin.com/project/markor/invite) (free).

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
The app is working completly offline, no internet connection required!
No personal data is shared with the author or any third parties (i.e. calendar or contacts).
Files can be shared to other apps from inside the app by pressing the share button.
Files are stored locally in a user selectable folder, defaulting to the device public documents folder.

#### Android Permissions
* WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE  
  Read and write files from/to device storage.
* INTERNET  
  In user-generated content data can be loaded from the internet.
* INSTALL_SHORTCUT  
  Install a shortcut to launchers to open a file/folder in Markor.
* RECORD_AUDIO  
  Attach voice notes to the text. The permission is only used after click on the "attach audio" button, at the audio record dialog. Audio recording is always started and stopped manually by you (button press).

## License
The code of the app is licensed Apache 2.0 or Commerical (See [LICENSE](/LICENSE.txt) for details).  
Localization & translation files (string\*.xml) are licensed CC0 1.0.  
Project is based on the unmaintained projects writeily and writeily-pro.
