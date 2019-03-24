<!-- markdownlint-disable MD033 MD041 MD002 -->

<div align="center">
  <h1>Markor</h1>
  <img src="./app/src/main/ic_launcher-web.png" width="100">
  <br />
  <b>Text editor - Notes &amp; ToDo (for Android)</b>.
  <p>Simple and lightweight. Supports Markdown and the <a href="http://todotxt.org/">todo.txt</a> format.</p>

[![GitHub release](https://img.shields.io/github/tag/gsantner/markor.svg)](https://github.com/gsantner/markor/releases)
[![Build Status](https://travis-ci.org/gsantner/markor.svg?branch=master)](https://travis-ci.org/gsantner/markor)
[![Translate](https://img.shields.io/badge/crowdin-translate-green.svg)](https://crowdin.com/project/markor/invite)
[![Donate Appreciation](https://img.shields.io/badge/donate-appreciation-orange.svg)](https://gsantner.net/supportme/?project=markor&source=readme)
[![Donate LiberaPay](https://img.shields.io/badge/donate-liberapay-orange.svg)](https://liberapay.com/gsantner/donate)
[![Chat - Matrix](https://img.shields.io/badge/chat-on%20matrix-blue.svg)](https://matrix.to/#/#markor:matrix.org) [![Chat - FreeNode IRC](https://img.shields.io/badge/chat-on%20irc-blue.svg)](https://kiwiirc.com/client/irc.freenode.net/?nick=markor-anon|?#markor)

  <a href="https://f-droid.org/repository/browse/?fdid=net.gsantner.markor">
    <img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">
  </a>
  
  <a href="https://play.google.com/store/apps/details?id=net.gsantner.markor">
    <img alt="Get it on Google Play" height="80" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
  </a>
</div>

</div>

Markor is a text editor for [Android](https://www.android.com/). This project aims to make an editor that is versatile, flexible, and lightweight. Markor utilizes simple markup formats like Markdown and <a href="http://todotxt.org/">todo.txt</a> for note-taking and list management.

## What Markor can do

Markor can also be used for

<ul>
  <li>Previewing and creating Markdown documents</li>
  <li>Tracking tasks with the <a href="http://todotxt.org/">todo.txt</a> format</li>
  <li>ordering documents with bookmarks</li>
  <li>quickly opening a link from text</li>
</ul>

Markor also supports the creation of Jekyll posts.
Markor uses open formats like Markdown to achieve the utmost compatibility with other platforms.

<ul>
  <li><b>Works offline</b> - whenever, wherever. Internet is only used for external resources.</li>
  <li><b>Syntax highlighting</b> and format related actions -- quick insert pictures and to-do</li>
  <li><b>Share documents as HTML and PDF</b>, or convert / preview them as HTML / PDF</li>
  <li><b>Jot something down quickly</b> (without creating a file) with QuickNote</li>
  <li><b>Track tasks</b> with the <a href="http://todotxt.org/">todo.txt</a> format</li>
  <li><b>Save links to pages</b> with the LinkBox function</li>
  <li><b>Append QuickNote / a document</b> with a section of text via the share button</li>
  <li><b>Highly customizable</b>, dark theme available</li>
  <li><b>Auto-save</b> with options for undo/redo</li>
  <li><b>No ads</b>, tracking or unnecessary permissions</b>
  <li><b>Change where files are saved</b> in Settings. QuickNote and Todo are just textfiles.</li>
  <li><b>Internationalization (i18n)</b>: The language can be changed</li>
  <li>Compatibility with sync apps, <i>but</i> they have to do syncing respectively. Sync clients known to work with Markor include:
    <ul>
      <li>BitTorrent Sync</li>
      <li>Dropbox</li>
      <li>FolderSync</li>
      <li>OwnCloud</li>
      <li>NextCloud</li>
      <li>Seafile</li>
      <li>Syncthing</li>
      <li>Syncopoli</li>
    </ul>
  </li>
</ul>
<!-- <br/>👀 These apps may also be in your interest if you like Markor: OneNote, EverNote, Google Keep, Wunderlist, Read-It-Later, Pocket, Epsilon Notes, iA Writer, Todoist, Shaarli, Wallabag, Simple Notes, Simpletask, Share to clipboard, NextCloud Bookmarks, Easy Open Link -->

### QuickNote

This feature allows you to hastily write something down. The text is then saved to the "QuickNote" document.

### LinkBox

You can use the LinkBox section to record URLs for deferred reading of links. Basically, it's a textual bookmark system.

### Task Tracking (Todo)

Creating tasks is easy. Just follow the <a href="http://todotxt.org/">todo.txt</a> specification, and write your notes in the `To-Do` section of the app, or create a new document with the `todo.txt` format.

---

## Privacy<a name="privacy"></a>

<b>No personal data will be requested or shared (i.e. calendar or contacts).</b>

The only permission needed is the "Storage" one, which allows Markor to save files to the disk (this is used for documents).

Documents can be shared to other apps from inside the app by pressing the share button, which is controlled by Android.

Documents are stored locally in the device documents folder or in a custom folder, which can be controlled from the Settings area.

## Contributions

The project is always open for contributions and welcomes pull requests. Take a look at our [issue tracker](https://github.com/gsantner/markor/issues) for open issues, especially those tagged with [good first issue](https://github.com/gsantner/markor/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) and [help wanted](https://github.com/gsantner/markor/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22).
The project uses [AOSP Java Code Style](https://source.android.com/source/code-style#follow-field-naming-conventions), with one exception: private members are `_camelCase` instead of `mBigCamel`. You may use Android Studios _auto reformat feature_ before sending a PR.

Translations can be contributed via [Crowdin](https://crowdin.com/project/markor/invite).

#### Resources

- Project: [Changelog](/CHANGELOG.md) | [Issues](https://github.com/gsantner/markor/issues?q=is%3Aissue+is%3Aopen) [Help Wanted Issues](https://github.com/gsantner/markor/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22) | [License](/LICENSE.txt) | [CoC](/CODE_OF_CONDUCT.md)
- F-Droid: [Listing](https://f-droid.org/packages/net.gsantner.markor/) [2](https://f-droid.org/repository/browse/?fdid=net.gsantner.markor) | [Wiki](https://f-droid.org/wiki/page/net.gsantner.markor) | [Metadata](https://gitlab.com/fdroid/fdroiddata/blob/master/metadata/net.gsantner.markor.txt) | [Build log](https://f-droid.org/wiki/page/net.gsantner.markor/lastbuild)
- Google Play: [Listing](https://play.google.com/store/apps/details?id=net.gsantner.markor&utm_source=reporeadme) | [Dev Console](https://play.google.com/apps/publish/?p=net.gsantner.markor&#AppDashboardPlace:p=net.gsantner.markor) | [Crash60](https://play.google.com/apps/publish/?p=net.gsantner.markor&#AndroidMetricsErrorsPlace:p=net.gsantner.markor&appVersion=PRODUCTION&lastReportedRange=LAST_60_DAYS)

## Licensing

The code of the app is licensed Apache 2.0 (See [LICENSE](/LICENSE.txt) for details).  
Localization files and resources (string\*.xml) are licensed CC0 1.0.  
Project is based on unmaintained projects writeily and writeily-pro.

## Screenshots

<table style="width:100%">
  <tr>
    <th>Editing text</th>
    <th>A Todo list (Dark mode)</th>
    <th>In the Notebook (Dark mode)</th>
	<th>Importing text</th>
  </tr>
  <tr>
    <td><img src="https://raw.githubusercontent.com/gsantner/markor-metadata-latest/master/en-US/phoneScreenshots/01.png"></td>
    <td><img src="https://raw.githubusercontent.com/gsantner/markor-metadata-latest/master/en-US/phoneScreenshots/02.png"></td> 
    <td><img src="https://raw.githubusercontent.com/gsantner/markor-metadata-latest/master/en-US/phoneScreenshots/04.png"></td>
	<td><img src="https://raw.githubusercontent.com/gsantner/markor-metadata-latest/master/en-US/phoneScreenshots/06.png"></td>
  </tr>
</table>

<!--
### Notice
-->

Apache License @ [gsantner](https://github.com/gsantner).

### begin DUMP section >>> append-below

  <li>💡 Notebook is the root folder of documents and can be changed to any location on filesystem. LinkBox, QuickNote and ToDo are textfiles</li>
