/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/

/*
 * Parses most common markdown tags. Only inline tags are supported, multiline/block syntax
 * is not supported (citation, multiline code, ..). This is intended to stay as easy as possible.
 *
 * You can e.g. apply a accent color by replacing #000001 with your accentColor string.
 *
 * FILTER_ANDROID_TEXTVIEW output is intended to be used at simple Android TextViews,
 * were a limited set of _html tags is supported. This allow to still display e.g. a simple
 * CHANGELOG.md file without including a WebView for showing HTML, or other additional UI-libraries.
 *
 * FILTER_WEB is intended to be used at engines understanding most common HTML tags.
 */

package net.gsantner.opoc.format;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Simple Markdown Parser
 */
@SuppressWarnings({"WeakerAccess", "CaughtExceptionImmediatelyRethrown", "SameParameterValue", "unused", "SpellCheckingInspection", "RepeatedSpace", "SingleCharAlternation", "Convert2Lambda"})
public class GsSimpleMarkdownParser {
    //########################
    //## Statics
    //########################
    public interface SmpFilter {
        String filter(String text);
    }

    public final static SmpFilter FILTER_ANDROID_TEXTVIEW = new SmpFilter() {
        @Override
        public String filter(String text) {
            // TextView supports a limited set of html tags, most notably
            // a href, b, big, font size&color, i, li, small, u

            // Don't start new line if 2 empty lines and heading
            while (text.contains("\n\n#")) {
                text = text.replace("\n\n#", "\n#");
            }

            return text
                    .replaceAll("(?s)<!--.*?-->", "")  // HTML comments
                    .replace("\n\n", "\n<br/>\n") // Start new line if 2 empty lines
                    .replace("~°", "&nbsp;&nbsp;") // double space/half tab
                    .replaceAll("(?m)^### (.*)$", "<br/><big><b><font color='#000000'>$1</font></b></big><br/>") // h3
                    .replaceAll("(?m)^## (.*)$", "<br/><big><big><b><font color='#000000'>$1</font></b></big></big><br/><br/>") // h2 (DEP: h3)
                    .replaceAll("(?m)^# (.*)$", "<br/><big><big><big><b><font color='#000000'>$1</font></b></big></big></big><br/><br/>") // h1 (DEP: h2,h3)
                    .replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "<a href=\\'$2\\'>$1</a>") // img
                    .replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href=\\'$2\\'>$1</a>") // a href (DEP: img)
                    .replaceAll("<(http|https):\\/\\/(.*)>", "<a href='$1://$2'>$1://$2</a>") // a href (DEP: img)
                    .replaceAll("(?m)^([-*] )(.*)$", "<font color='#000001'>&#8226;</font> $2<br/>") // unordered list + end line
                    .replaceAll("(?m)^  (-|\\*) ([^<]*)$", "&nbsp;&nbsp;<font color='#000001'>&#8226;</font> $2<br/>") // unordered list2 + end line
                    .replaceAll("`([^<]*)`", "<font face='monospace'>$1</font>") // code
                    .replace("\\*", "●") // temporary replace escaped star symbol
                    .replaceAll("(?m)\\*\\*(.*)\\*\\*", "<b>$1</b>") // bold (DEP: temp star)
                    .replaceAll("(?m)\\*(.*)\\*", "<i>$1</i>") // italic (DEP: temp star code)
                    .replace("●", "*") // restore escaped star symbol (DEP: b,i)
                    .replaceAll("(?m)  $", "<br/>") // new line (DEP: ul)
                    ;
        }
    };

    public final static SmpFilter FILTER_WEB = new SmpFilter() {
        @Override
        public String filter(String text) {
            // Don't start new line if 2 empty lines and heading
            while (text.contains("\n\n#")) {
                text = text.replace("\n\n#", "\n#");
            }

            text = text
                    .replaceAll("(?s)<!--.*?-->", "")  // HTML comments
                    .replace("\n\n", "\n<br/>\n") // Start new line if 2 empty lines
                    .replaceAll("~°", "&nbsp;&nbsp;") // double space/half tab
                    .replaceAll("(?m)^### (.*)$", "<h3>$1</h3>") // h3
                    .replaceAll("(?m)^## (.*)$", "<h2>$1</h2>") /// h2 (DEP: h3)
                    .replaceAll("(?m)^# (.*)$", "<h1>$1</h1>") // h1 (DEP: h2,h3)
                    .replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "<img src=\\'$2\\' alt='$1' />") // img
                    .replaceAll("<(http|https):\\/\\/(.*)>", "<a href='$1://$2'>$1://$2</a>") // a href (DEP: img)
                    .replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href=\\'$2\\'>$1</a>") // a href (DEP: img)
                    .replaceAll("(?m)^[-*] (.*)$", "<font color='#000001'>&#8226;</font> $1  ") // unordered list + end line
                    .replaceAll("(?m)^  [-*] (.*)$", "&nbsp;&nbsp;<font color='#000001'>&#8226;</font> $1  ") // unordered list2 + end line
                    .replaceAll("`([^<]*)`", "<code>$1</code>") // code
                    .replace("\\*", "●") // temporary replace escaped star symbol
                    .replaceAll("(?m)\\*\\*(.*)\\*\\*", "<b>$1</b>") // bold (DEP: temp star)
                    .replaceAll("(?m)\\*(.*)\\*", "<i>$1</i>") // italic (DEP: temp star code)
                    .replace("●", "*") // restore escaped star symbol (DEP: b,i)
                    .replaceAll("(?m)  $", "<br/>") // new line (DEP: ul)
            ;
            return text;
        }
    };

    public final static SmpFilter FILTER_CHANGELOG = new SmpFilter() {
        @Override
        public String filter(String text) {
            text = text
                    .replace("New:", "<font color='#276230'>New:</font>")
                    .replace("New features:", "<font color='#276230'>New:</font>")
                    .replace("Added:", "<font color='#276230'>Added:</font>")
                    .replace("Add:", "<font color='#276230'>Add:</font>")
                    .replace("Fixed:", "<font color='#005688'>Fixed:</font>")
                    .replace("Fix:", "<font color='#005688'>Fix:</font>")
                    .replace("Removed:", "<font color='#C13524'>Removed:</font>")
                    .replace("Updated:", "<font color='#555555'>Updated:</font>")
                    .replace("Improved:", "<font color='#555555'>Improved:</font>")
                    .replace("Modified:", "<font color='#555555'>Modified:</font>")
                    .replace("Mod:", "<font color='#555555'>Mod:</font>")
            ;
            return text;
        }
    };
    public final static SmpFilter FILTER_H_TO_SUP = new SmpFilter() {
        @Override
        public String filter(String text) {
            text = text
                    .replace("<h1>", "<sup><sup><sup>")
                    .replace("</h1>", "</sup></sup></sup>")
                    .replace("<h2>", "<sup><sup>")
                    .replace("</h2>", "</sup></sup>")
                    .replace("<h3>", "<sup>")
                    .replace("</h3>", "</sup>")
            ;
            return text;
        }
    };
    public final static SmpFilter FILTER_NONE = new SmpFilter() {
        @Override
        public String filter(String text) {
            return text;
        }
    };

    //########################
    //## Singleton
    //########################
    private static GsSimpleMarkdownParser __instance;

    public static GsSimpleMarkdownParser get() {
        if (__instance == null) {
            __instance = new GsSimpleMarkdownParser();
        }
        return __instance;
    }

    //########################
    //## Members, Constructors
    //########################
    private SmpFilter _defaultSmpFilter;
    private String _html;

    public GsSimpleMarkdownParser() {
        setDefaultSmpFilter(FILTER_WEB);
    }

    //########################
    //## Methods
    //########################
    public GsSimpleMarkdownParser setDefaultSmpFilter(SmpFilter defaultSmpFilter) {
        _defaultSmpFilter = defaultSmpFilter;
        return this;
    }

    public GsSimpleMarkdownParser parse(String filepath, SmpFilter... smpFilters) throws IOException {
        return parse(new FileInputStream(filepath), "", smpFilters);
    }

    public GsSimpleMarkdownParser parse(InputStream inputStream, String lineMdPrefix, SmpFilter... smpFilters) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        String line;

        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = br.readLine()) != null) {
                sb.append(lineMdPrefix);
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException rethrow) {
            _html = "";
            throw rethrow;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        _html = parse(sb.toString(), "", smpFilters).getHtml();
        return this;
    }

    public GsSimpleMarkdownParser parse(String markdown, String lineMdPrefix, SmpFilter... smpFilters) throws IOException {
        _html = markdown;
        if (smpFilters.length == 0) {
            smpFilters = new SmpFilter[]{_defaultSmpFilter};
        }
        for (SmpFilter smpFilter : smpFilters) {
            _html = smpFilter.filter(_html).trim();
        }
        return this;
    }

    public String getHtml() {
        return _html;
    }

    public GsSimpleMarkdownParser setHtml(String html) {
        _html = html;
        return this;
    }

    public GsSimpleMarkdownParser removeMultiNewlines() {
        _html = _html.replace("\n", "").replaceAll("(<br/>){3,}", "<br/><br/>");
        return this;
    }

    public GsSimpleMarkdownParser replaceBulletCharacter(String replacment) {
        _html = _html.replace("&#8226;", replacment);
        return this;
    }

    public GsSimpleMarkdownParser replaceColor(String hexColor, int newIntColor) {
        _html = _html.replace(hexColor, String.format("#%06X", 0xFFFFFF & newIntColor));
        return this;
    }

    @Override
    public String toString() {
        return _html != null ? _html : "";
    }
}
