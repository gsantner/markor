package net.gsantner.markor.format.asciidoc;
/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

import android.graphics.Color;
import android.graphics.Paint;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.regex.Pattern;

import other.writeily.format.asciidoc.WrAsciidocHeaderSpanCreator;
import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class AsciidocSyntaxHighlighter extends SyntaxHighlighterBase {

    // check on https://regex101.com/
    // the syntax patterns are simplified
    // WARNING: wrong or invalid patterns causes the app to crash, when a file opens!

    // Monospace syntax (`) must be the outermost formatting pair (i.e., outside the
    // bold formatting pair).
    // Italic syntax (_) is always the innermost formatting pair.

    // simplified, OK for basic examples
    public final static Pattern BOLD = Pattern.compile("(?m)(\\*\\S(?!\\*)(.*?)\\S\\*(?!\\*))");
    // simplified, OK for basic examples
    public final static Pattern ITALICS = Pattern.compile("(?m)(_\\S(?!_)(.*?)\\S_(?!_))");
    // simplified, OK for basic examples, contains only inline code
    public final static Pattern SUBSCRIPT = Pattern.compile("(?m)(~(?!~)(.*?)~(?!~))");
    public final static Pattern SUPERSCRIPT = Pattern.compile("(?m)(\\^(?!\\^)(.*?)\\^(?!\\^))");
    public final static Pattern MONOSPACE = Pattern.compile("(?m)(`(?!`)(.*?)`(?!`))");

    //    public final static Pattern HEADING_ASCIIDOC = Pattern.compile("(?m)^(={1,6} {1}\\S.*$)");
    public final static Pattern HEADING = Pattern.compile("(?m)(^(={1,6}|#{1,6})[^\\S\\n][^\\n]+)");
    public final static Pattern HEADING_ASCIIDOC = Pattern.compile("(?m)(^={1,6}[^\\S\\n][^\\n]+)");
    public final static Pattern HEADING_MD = Pattern.compile("(?m)(^#{1,6}[^\\S\\n][^\\n]+)");
    // simplified syntax: In fact, leading spaces are also possible
    // could be extended, if users will request. But for now it is a good starting point
    public final static Pattern LIST_ORDERED = Pattern.compile("(?m)^(\\.{1,6})( {1})");
    // don't include checklist markers, they get squarebracket background color
    // public final static Pattern LIST_UNORDERED = Pattern.compile(
    // "(?m)^\\*{1,6}( \\[[ xX]\\]){0,1} {1}");
    public final static Pattern LIST_UNORDERED = Pattern.compile("(?m)^\\*{1,6} {1}");
    public final static Pattern LIST_DESCRIPTION = Pattern.compile(
            "(?m)^(.+\\S(:{2,4}|;{2,2}))( {1}|[\\r\\n])");
    // keep regex syntax for later for possible highlighting checklists:
    // public final static Pattern LIST_CHECKLIST = Pattern.compile("^\\*{1,6}( \\[[ xX]\\]) {1}");

    public final static Pattern ATTRIBUTE_DEFINITION = Pattern.compile("(?m)^:\\S+:");
    public final static Pattern ATTRIBUTE_REFERENCE = Pattern.compile("(?m)\\{\\S+\\}");
    public final static Pattern LINE_COMMENT = Pattern.compile("(?m)^\\/{2}(?!\\/).*$");
    public final static Pattern ADMONITION = Pattern.compile(
            "(?m)^(NOTE: |TIP: |IMPORTANT: |CAUTION: |WARNING: )");


    public final static Pattern SQUAREBRACKETS = Pattern.compile("\\[([^\\[]*)\\]");
    public final static Pattern HIGHLIGHT = Pattern.compile(
            "(?m)(?<!])((#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##))");
    public final static Pattern ROLE_GENERAL = Pattern.compile(
            "(?m)\\[([^\\[]*)\\]((#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##))");

    public final static Pattern ROLE_UNDERLINE = Pattern.compile(
            "(?m)\\[\\.underline\\]((#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##))");
    public final static Pattern ROLE_STRIKETHROUGH = Pattern.compile(
            "(?m)\\[\\.line-through\\]((#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##))");

    public final static Pattern HARD_LINE_BREAK = Pattern.compile(
            "(?m)(?<=\\S)([^\\S\\r\\n]{1})\\+[\\r\\n]");

    // simplified, OK for basic examples
    public final static Pattern LINK_PATTERN = Pattern.compile("link:\\S*?\\[([^\\[]*)\\]");
    public final static Pattern XREF_PATTERN = Pattern.compile("xref:\\S*?\\[([^\\[]*)\\]");
    public final static Pattern IMAGE_PATTERN = Pattern.compile("image:\\S*?\\[([^\\[]*)\\]");
    public final static Pattern INCLUDE_PATTERN = Pattern.compile("include:\\S*?\\[([^\\[]*)\\]");

    public final static Pattern BLOCKTITLE = Pattern.compile("(?m)^\\.[^(\\s|\\.)].*$");

    // block syntax
    // simplified, contains only the most common case, like "____", "....", "----", ...
    public final static Pattern BLOCK_DELIMITED_QUOTATION = Pattern.compile(
            "(?m)^\\_{4}[\\r\\n]([\\s\\S]+?(?=^\\_{4}[\\r\\n]))^\\_{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_EXAMPLE = Pattern.compile(
            "(?m)^\\={4}[\\r\\n]([\\s\\S]+?(?=^\\={4}[\\r\\n]))^\\={4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_LISTING = Pattern.compile(
            "(?m)^\\-{4}[\\r\\n]([\\s\\S]+?(?=^\\-{4}[\\r\\n]))^\\-{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_LITERAL = Pattern.compile(
            "(?m)^\\.{4}[\\r\\n]([\\s\\S]+?(?=^\\.{4}[\\r\\n]))^\\.{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_SIDEBAR = Pattern.compile(
            "(?m)^\\*{4}[\\r\\n]([\\s\\S]+?(?=^\\*{4}[\\r\\n]))^\\*{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_COMMENT = Pattern.compile(
            "(?m)^\\/{4}[\\r\\n]([\\s\\S]+?(?=^\\/{4}[\\r\\n]))^\\/{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_TABLE = Pattern.compile(
            "(?m)^\\|\\={3}[\\r\\n]([\\s\\S]+?(?=^\\|\\={3}[\\r\\n]))^\\|={3}[\\r\\n]");

    // original, adapted from Mardown: issues with content, created in Windows and directly copied to android
    // public final static Pattern DOUBLESPACE_LINE_ENDING = Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n");
    // corrected and adapted:
    public final static Pattern DOUBLESPACE_LINE_ENDING = Pattern.compile(
            "(?m)(?<=\\S)([^\\S\\r\\n]{2,})[\\r\\n]");

    /*
    https://personal.sron.nl/~pault/[Paul Tol's Notes, Colour schemes and templates, 18 August 2021]

    = INTRODUCTION TO COLOUR SCHEMES

    distinct for all people, including colour-blind readers;

    #default# colour scheme for qualitative data is the _bright_ scheme in https://personal.sron.nl/~pault/#fig:scheme_bright[Fig. 1]

    image::https://personal.sron.nl/~pault/images/scheme_bright.png[Figure 1]

    Colours in default order: '#4477AA', '#EE6677', '#228833', '#CCBB44', '#66CCEE', '#AA3377', '#BBBBBB'.

    BLUE, CYAN, GREEN, YELLOW, RED, PURPLE, GRAY

    blue, cyan, green, yellow, red, purple, gray

    Figure 6: #_Pale_ and _dark_# qualitative colour schemes where the colours are not very distinct in either normal or colour-blind vision;
    they are not meant for lines or maps, #but for marking text#. Use the *pale* colours for the *background of black text*,
    for example to highlight cells in a table.
    One of the *dark colours* can be chosen *for text itself on a white background*,
    for example when a large block of text has to be marked.
    In both cases, the text remains easily readable (see https://personal.sron.nl/~pault/#fig:orbits[Fig. 10]).

    image::https://personal.sron.nl/~pault/images/scheme_pale.png[]

    Colours: '#BBCCEE', '#CCEEFF', '#CCDDAA', '#EEEEBB', '#FFCCCC', '#DDDDDD'.


    image:https://personal.sron.nl/~pault/images/scheme_dark.png[Dark scheme]

    Colours: '#222255', '#225555', '#225522', '#666633', '#663333', '#555555'.

    */

    private static final int TOL_BLUE = Color.parseColor("#4477AA");
    private static final int TOL_CYAN = Color.parseColor("#66CCEE");
    private static final int TOL_GREEN = Color.parseColor("#228833");
    private static final int TOL_YELLOW = Color.parseColor("#CCBB44");
    private static final int TOL_RED = Color.parseColor("#EE6677");
    private static final int TOL_PURPLE = Color.parseColor("#AA3377");
    private static final int TOL_GRAY = Color.parseColor("#BBBBBB");

    private static final int TOL_PALE_BLUE = Color.parseColor("#BBCCEE");
    private static final int TOL_PALE_CYAN = Color.parseColor("#CCEEFF");
    private static final int TOL_PALE_GREEN = Color.parseColor("#CCDDAA");
    private static final int TOL_PALE_YELLOW = Color.parseColor("#EEEEBB");
    private static final int TOL_PALE_RED = Color.parseColor("#FFCCCC");
    private static final int TOL_PALE_GRAY = Color.parseColor("#DDDDDD");

    private static final int TOL_DARK_BLUE = Color.parseColor("#222255");
    private static final int TOL_DARK_CYAN = Color.parseColor("#225555");
    private static final int TOL_DARK_GREEN = Color.parseColor("#225522");
    private static final int TOL_DARK_YELLOW = Color.parseColor("#666633");
    private static final int TOL_DARK_RED = Color.parseColor("#663333");
    private static final int TOL_DARK_GRAY = Color.parseColor("#555555");

    //Here the concrete use of the colors for AsciiDoc:
    private static final int AD_FORECOLOR_LIGHT_HEADING = TOL_RED;
    private static final int AD_FORECOLOR_DARK_HEADING = TOL_RED;
    //TOL_BLUE link on background (attributes reference, squarebrackets) problematic
    //Test TOL_CYAN, better
    private static final int AD_FORECOLOR_LIGHT_LINK = TOL_DARK_CYAN;
    private static final int AD_FORECOLOR_DARK_LINK = TOL_CYAN;
    private static final int AD_FORECOLOR_LIGHT_LIST = TOL_DARK_YELLOW;
    private static final int AD_FORECOLOR_DARK_LIST = TOL_YELLOW; //OK
    private static final int AD_FORECOLOR_ADMONITION = TOL_RED; //OK

    private static final int AD_BACKCOLOR_LIGHT_QUOTE = TOL_PALE_GREEN;
    private static final int AD_BACKCOLOR_DARK_QUOTE = TOL_DARK_GREEN;
    private static final int AD_BACKCOLOR_LIGHT_EXAMPLE = TOL_PALE_BLUE;
    private static final int AD_BACKCOLOR_DARK_EXAMPLE = TOL_DARK_BLUE;
    private static final int AD_BACKCOLOR_LIGHT_SIDEBAR = TOL_PALE_RED;
    private static final int AD_BACKCOLOR_DARK_SIDEBAR = TOL_DARK_RED;
    private static final int AD_BACKCOLOR_LIGHT_TABLE = TOL_PALE_YELLOW;
    private static final int AD_BACKCOLOR_DARK_TABLE = TOL_DARK_YELLOW;
    private static final int AD_BACKCOLOR_LIGHT_ATTRIBUTE = TOL_PALE_CYAN;
    private static final int AD_BACKCOLOR_DARK_ATTRIBUTE = TOL_DARK_CYAN;
    // we use gray for miscellaneous to avoid too much variety
    private static final int AD_BACKCOLOR_LIGHT_MONOSPACE = TOL_PALE_GRAY;
    private static final int AD_BACKCOLOR_DARK_MONOSPACE = TOL_DARK_GRAY;
    private static final int AD_BACKCOLOR_LIGHT_SQUAREBRACKETS = TOL_PALE_GRAY;
    private static final int AD_BACKCOLOR_DARK_SQUAREBRACKETS = TOL_DARK_GRAY;
    private static final int AD_BACKCOLOR_LIGHT_BLOCKTITLE = TOL_PALE_GRAY;
    private static final int AD_BACKCOLOR_DARK_BLOCKTITLE = TOL_DARK_GRAY;

    // use the same highlight for light and dark theme
    private static final int AD_BACKCOLOR_LIGHT_HIGHLIGHT = Color.YELLOW;
    private static final int AD_BACKCOLOR_DARK_HIGHLIGHT = Color.YELLOW;
    // black on yellow
    private static final int AD_FORECOLOR_HIGHLIGHT = Color.BLACK;

    // ..._COLOR_LIGHT_... = Color for light theme
    // too little contrast to normal font with the TOL_ colors
    // private static final int AD_COLOR_LIGHT_COMMENT = TOL_DARK_GRAY;
    // private static final int AD_COLOR_DARK_COMMENT = TOL_PALE_GRAY;
    // private static final int AD_COLOR_COMMENT = Color.GRAY; //TOL_GRAY;
    private static final int AD_FORECOLOR_LIGHT_COMMENT = Color.GRAY;
    private static final int AD_FORECOLOR_DARK_COMMENT = Color.GRAY;


    private boolean _highlightLineEnding;
    private boolean _highlightCodeChangeFont;
    private boolean _highlightBiggerHeadings;
    private boolean _highlightCodeBlock;


    public AsciidocSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    @Override
    public SyntaxHighlighterBase configure(Paint paint) {
        _highlightLineEnding = _appSettings.isAsciidocHighlightLineEnding();
        _highlightBiggerHeadings = _appSettings.isHighlightBiggerHeadings();
        _highlightCodeChangeFont = _appSettings.isHighlightCodeMonospaceFont();
        _highlightCodeBlock = _appSettings.isHighlightCodeBlock();
        _delay = _appSettings.getAsciidocHighlightingDelay();
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        // createSmallBlueLinkSpans() - font is very small, currently general setting: 85% of common size
        // TODO: implement some AsciiDoc extensions later
        // hard to read on dark theme, but this is a general question for all formats,
        // not AsciiDoc specific
        // also it uses private static String formatLink(String text, String link), which is
        // adapted for Markdown
        // maybe, needs to be adapted for AsciiDoc?
        // but not in the current Pull Request
        createSmallBlueLinkSpans();

        if (_highlightBiggerHeadings) {
            createSpanForMatches(HEADING_ASCIIDOC, new WrAsciidocHeaderSpanCreator(_spannable,
                    _isDarkMode ? AD_FORECOLOR_DARK_HEADING : AD_FORECOLOR_LIGHT_HEADING));
            createSpanForMatches(HEADING_MD, new WrMarkdownHeaderSpanCreator(_spannable,
                    _isDarkMode ? AD_FORECOLOR_DARK_HEADING : AD_FORECOLOR_LIGHT_HEADING));
        } else {
            createSpanForMatches(HEADING, new HighlightSpan().setForeColor(
                    _isDarkMode ? AD_FORECOLOR_DARK_HEADING : AD_FORECOLOR_LIGHT_HEADING));
        }

        if (_highlightCodeChangeFont) {
            // setTypeface(Typeface.MONOSPACE) - invalid
            // setTypeface(0) - doesn't work
            // but the current solution works fine and allows to use MONOSPACE based on settings
            createMonospaceSpanForMatches(MONOSPACE);
            createMonospaceSpanForMatches(BLOCK_DELIMITED_LISTING);
            createMonospaceSpanForMatches(BLOCK_DELIMITED_LITERAL);
            createMonospaceSpanForMatches(LIST_UNORDERED);
            createMonospaceSpanForMatches(LIST_ORDERED);
            createMonospaceSpanForMatches(LIST_DESCRIPTION);
            createMonospaceSpanForMatches(ATTRIBUTE_DEFINITION);
            createMonospaceSpanForMatches(ATTRIBUTE_REFERENCE);
            createMonospaceSpanForMatches(ADMONITION);
            createMonospaceSpanForMatches(SQUAREBRACKETS);
        }

        createSpanForMatches(BOLD, new HighlightSpan().setBold(true));
        createSpanForMatches(ITALICS, new HighlightSpan().setItalic(true));

        createSpanForMatches(LINK_PATTERN, new HighlightSpan().setForeColor(
                _isDarkMode ? AD_FORECOLOR_DARK_LINK : AD_FORECOLOR_LIGHT_LINK));
        createSpanForMatches(XREF_PATTERN, new HighlightSpan().setForeColor(
                _isDarkMode ? AD_FORECOLOR_DARK_LINK : AD_FORECOLOR_LIGHT_LINK));
        createSpanForMatches(IMAGE_PATTERN, new HighlightSpan().setForeColor(
                _isDarkMode ? AD_FORECOLOR_DARK_LINK : AD_FORECOLOR_LIGHT_LINK));
        createSpanForMatches(INCLUDE_PATTERN, new HighlightSpan().setForeColor(
                _isDarkMode ? AD_FORECOLOR_DARK_LINK : AD_FORECOLOR_LIGHT_LINK));
        createSpanForMatches(LIST_UNORDERED, new HighlightSpan().setForeColor(
                _isDarkMode ? AD_FORECOLOR_DARK_LINK : AD_FORECOLOR_LIGHT_LINK));
        createSpanForMatches(LIST_ORDERED, new HighlightSpan().setForeColor(
                _isDarkMode ? AD_FORECOLOR_DARK_LINK : AD_FORECOLOR_LIGHT_LINK));
        createSpanForMatches(LIST_DESCRIPTION, new HighlightSpan().setForeColor(
                _isDarkMode ? AD_FORECOLOR_DARK_LINK : AD_FORECOLOR_LIGHT_LINK));

        // keep one old code example, how it was before:
        // createStyleSpanForMatches(ADMONITION, Typeface.BOLD);
        // createColorSpanForMatches(ADMONITION, AD_COLOR_ADMONITION);
        createSpanForMatches(ADMONITION,
                new HighlightSpan().setBold(true).setForeColor(AD_FORECOLOR_ADMONITION));

        createSpanForMatches(SQUAREBRACKETS, new HighlightSpan().setBackColor(
                _isDarkMode ? AD_BACKCOLOR_DARK_SQUAREBRACKETS
                        : AD_BACKCOLOR_LIGHT_SQUAREBRACKETS));
        createSpanForMatches(BLOCKTITLE, new HighlightSpan().setBackColor(
                _isDarkMode ? AD_BACKCOLOR_DARK_BLOCKTITLE : AD_BACKCOLOR_LIGHT_BLOCKTITLE));
        createSpanForMatches(MONOSPACE, new HighlightSpan().setBackColor(
                _isDarkMode ? AD_BACKCOLOR_DARK_MONOSPACE : AD_BACKCOLOR_LIGHT_MONOSPACE));


        if (_highlightLineEnding) {
            createSpanForMatches(HARD_LINE_BREAK, new HighlightSpan().setBackColor(
                    _isDarkMode ? AD_BACKCOLOR_DARK_MONOSPACE : AD_BACKCOLOR_LIGHT_MONOSPACE));
        }

        if (_highlightCodeBlock) {
            createSpanForMatches(BLOCK_DELIMITED_LISTING, new HighlightSpan().setBackColor(
                    _isDarkMode ? AD_BACKCOLOR_DARK_MONOSPACE : AD_BACKCOLOR_LIGHT_MONOSPACE));
            createSpanForMatches(BLOCK_DELIMITED_LITERAL, new HighlightSpan().setBackColor(
                    _isDarkMode ? AD_BACKCOLOR_DARK_MONOSPACE : AD_BACKCOLOR_LIGHT_MONOSPACE));
            createSpanForMatches(BLOCK_DELIMITED_QUOTATION, new HighlightSpan().setBackColor(
                    _isDarkMode ? AD_BACKCOLOR_DARK_QUOTE : AD_BACKCOLOR_LIGHT_QUOTE));
            createSpanForMatches(BLOCK_DELIMITED_EXAMPLE, new HighlightSpan().setBackColor(
                    _isDarkMode ? AD_BACKCOLOR_DARK_EXAMPLE : AD_BACKCOLOR_LIGHT_EXAMPLE));
            createSpanForMatches(BLOCK_DELIMITED_SIDEBAR, new HighlightSpan().setBackColor(
                    _isDarkMode ? AD_BACKCOLOR_DARK_SIDEBAR : AD_BACKCOLOR_LIGHT_SIDEBAR));
            createSpanForMatches(BLOCK_DELIMITED_TABLE, new HighlightSpan().setBackColor(
                    _isDarkMode ? AD_BACKCOLOR_DARK_TABLE : AD_BACKCOLOR_LIGHT_TABLE));
            // Comment: change text color, but not background
            createSpanForMatches(BLOCK_DELIMITED_COMMENT, new HighlightSpan().setForeColor(
                    _isDarkMode ? AD_FORECOLOR_DARK_COMMENT : AD_FORECOLOR_LIGHT_COMMENT));
        }

        createSpanForMatches(LINE_COMMENT, new HighlightSpan().setForeColor(
                _isDarkMode ? AD_FORECOLOR_DARK_COMMENT : AD_FORECOLOR_LIGHT_COMMENT));
        createSpanForMatches(HIGHLIGHT,
                new HighlightSpan().setForeColor(AD_FORECOLOR_HIGHLIGHT).setBackColor(
                        _isDarkMode ? AD_BACKCOLOR_DARK_HIGHLIGHT : AD_BACKCOLOR_LIGHT_HIGHLIGHT));
        createSpanForMatches(ATTRIBUTE_DEFINITION, new HighlightSpan().setBackColor(
                _isDarkMode ? AD_BACKCOLOR_DARK_ATTRIBUTE : AD_BACKCOLOR_LIGHT_ATTRIBUTE));
        createSpanForMatches(ATTRIBUTE_REFERENCE, new HighlightSpan().setBackColor(
                _isDarkMode ? AD_BACKCOLOR_DARK_ATTRIBUTE : AD_BACKCOLOR_LIGHT_ATTRIBUTE));

        createSubscriptStyleSpanForMatches(SUBSCRIPT);
        createSuperscriptStyleSpanForMatches(SUPERSCRIPT);
        createStrikeThroughSpanForMatches(ROLE_STRIKETHROUGH);

        // TODO: is only very thin and hardly visible; try to understand, why
        // createColoredUnderlineSpanForMatches(ROLE_UNDERLINE, AD_COLOR_UNDERLINE_ROLE_UNDERLINE);
    }
}

