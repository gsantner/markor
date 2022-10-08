package net.gsantner.markor.format.asciidoc;
/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.regex.Pattern;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class AsciidocSyntaxHighlighter extends SyntaxHighlighterBase {

    // check on https://regex101.com/
    // the syntax patterns are simplified
    // WARNING: wrong or invalid patterns causes the app to crash, when a file opens!

    // Monospace syntax (`) must be the outermost formatting pair (i.e., outside the
    // bold formatting pair).
    // Italic syntax (_) is always the innermost formatting pair.

    // simplified, OK for basic examples
    public final static Pattern BOLD = Pattern
            .compile(
                    "(?m)(\\*(?!\\*)\\b(.*?)\\b\\*(?!\\*))|(\\*(?!\\*)_(.*?)_\\*(?!\\*))|(\\*\\*(?!\\*)(.*?)\\*\\*)");
    // simplified, OK for basic examples
    public final static Pattern ITALICS = Pattern.compile(
            "(?m)(\\b_(?!_)(.*?)_(?!_)\\b)|(__(?!_)(.*?)__)");
    // simplified, OK for basic examples, contains only inline code
    public final static Pattern SUBSCRIPT = Pattern.compile("(?m)(~(?!~)(.*?)~(?!~))");
    public final static Pattern SUPERSCRIPT = Pattern.compile("(?m)(\\^(?!\\^)(.*?)\\^(?!\\^))");
    public final static Pattern CODE = Pattern
            .compile(
                    "(?m)(`(?!`)\\b(.*?)`(?!`))|(`(?!`)_(.*?)_`(?!`))|(`(?!`)\\*(.*?)\\*`(?!`))|(``(?!`)(.*?)``)");
    public final static Pattern HEADING_SIMPLE = Pattern.compile("(?m)^(={1,6} {1}\\S.*$)");
    // simplified syntax: In fact, leading spaces are also possible
    public final static Pattern LIST_ORDERED = Pattern.compile("(?m)^(\\.{1,6})( {1})");
    public final static Pattern LIST_UNORDERED = Pattern.compile(
            "(?m)^\\*{1,6}( \\[[ xX]\\]){0,1} {1}");
    // TODO: use later for highlighting checklists.
    // public final static Pattern LIST_CHECKLIST = Pattern.compile("^\\*{1,6}( \\[[ xX]\\]) {1}");

    public final static Pattern ATTRIBUTE_DEFINITION = Pattern.compile("(?m)^:\\S+:");
    public final static Pattern ATTRIBUTE_REFERENCE = Pattern.compile("(?m)\\{\\S+\\}");
    public final static Pattern LINE_COMMENT = Pattern.compile("(?m)^\\/{2}(?!\\/).*$");
    // simplified, OK for basic examples
    public final static Pattern HIGHLIGHT = Pattern.compile(
            "(?m)(?<!])((#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##))");
    // simplified, OK for basic examples
    public final static Pattern UNDERLINE = Pattern
            .compile("(?m)\\[\\.underline\\]((#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##))");
    public final static Pattern STRIKETHROUGH = Pattern
            .compile("(?m)\\[\\.line-through\\]((#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##))");
    public final static Pattern HARD_LINE_BREAK = Pattern.compile(
            "(?m)(?<=\\S)([^\\S\\r\\n]{1})\\+[\\r\\n]");

    // simplified, OK for basic examples
    public final static Pattern LINK_PATTERN = Pattern.compile("link:\\S*?\\[([^\\[]*)\\]");
    public final static Pattern XREF_PATTERN = Pattern.compile("xref:\\S*?\\[([^\\[]*)\\]");
    public final static Pattern IMAGE_PATTERN = Pattern.compile("image:\\S*?\\[([^\\[]*)\\]");
    public final static Pattern INCLUDE_PATTERN = Pattern.compile("include:\\S*?\\[([^\\[]*)\\]");

    // block syntax
    // simplified, contains only the most common case, like "____", "....", "----", ...
    public final static Pattern BLOCK_DELIMITED_QUOTATION = Pattern.compile(
            "(?m)^\\_{4}[\\r\\n]([\\s\\S]+?(?=^\\_{4}[\\r\\n]))\\_{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_EXAMPLE = Pattern.compile(
            "(?m)^\\={4}[\\r\\n]([\\s\\S]+?(?=^\\={4}[\\r\\n]))\\={4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_LISTING = Pattern.compile(
            "(?m)^\\-{4}[\\r\\n]([\\s\\S]+?(?=^\\-{4}[\\r\\n]))\\-{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_LITERAL = Pattern.compile(
            "(?m)^\\.{4}[\\r\\n]([\\s\\S]+?(?=^\\.{4}[\\r\\n]))\\.{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_SIDEBAR = Pattern.compile(
            "(?m)^\\*{4}[\\r\\n]([\\s\\S]+?(?=^\\*{4}[\\r\\n]))\\*{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_COMMENT = Pattern.compile(
            "(?m)^\\/{4}[\\r\\n]([\\s\\S]+?(?=^\\/{4}[\\r\\n]))\\/{4}[\\r\\n]");

    // original
    // issues with content, created in Windows and directly copied to android
    // public final static Pattern DOUBLESPACE_LINE_ENDING = Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n");
    // corrected:
    public final static Pattern DOUBLESPACE_LINE_ENDING = Pattern.compile(
            "(?m)(?<=\\S)([^\\S\\r\\n]{2,})[\\r\\n]");
//
//    // TODO: still markdown, but it is not used
//    public final static Pattern ACTION_LINK_PATTERN = Pattern.compile("(?m)\\[(.*?)\\]\\((.*?)\\)");
//

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



TODO: test on dark and black theme, maybe need to adapt
white text on areas with changed background is hard to read
use explicit text color, when background changes?

*/

    private static final int TOL_BLUE = Color.parseColor("#4477AA");
    private static final int TOL_CYAN = Color.parseColor("#EE6677");
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


    private static final int AD_COLOR_HEADING = TOL_RED;
    private static final int AD_COLOR_LINK = TOL_BLUE;
    private static final int AD_COLOR_LIST = TOL_CYAN;
    private static final int AD_COLOR_UNDERLINE = TOL_PURPLE;
    private static final int AD_COLOR_ATTRIBUTE = TOL_CYAN;

    private static final int AD_COLORBACKGROUND_CODEBLOCK = TOL_PALE_GRAY;
    private static final int AD_COLORBACKGROUND_QUOTE = TOL_PALE_GREEN;
    private static final int AD_COLORBACKGROUND_EXAMPLE = TOL_PALE_BLUE;
    private static final int AD_COLORBACKGROUND_SIDEBAR = TOL_PALE_RED;
    private static final int AD_COLORBACKGROUND_HIGHLIGHT = TOL_PALE_YELLOW;
    private static final int AD_COLORBACKGROUND_COMMENT = TOL_PALE_GRAY;
    private static final int AD_COLORBACKGROUND_ATTRIBUTE = TOL_PALE_CYAN;

    // TODO: consider different AD_COLOR_TEXT_ON_COLORBACKGROUND instead of only one
    private static final int AD_COLOR_TEXT_ON_COLORBACKGROUND = TOL_DARK_GRAY;
    private static final int AD_COLOR_TEXT_ON_COLORBACKGROUND_COMMENT = TOL_DARK_YELLOW;


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
// TODO: does not work yet
        _highlightBiggerHeadings = _appSettings.isAsciidocBiggerHeadings();
        _highlightCodeChangeFont = _appSettings.isHighlightCodeMonospaceFont();
        _highlightCodeBlock = _appSettings.isHighlightCodeBlock();
        _delay = _appSettings.getAsciidocHighlightingDelay();
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        // TODO: createSmallBlueLinkSpans() - font is very small, currently general setting: 85% of common size
        // hard to read on dark theme, but this is a general question for all formats,
        // not AsciiDoc specific
        // also it uses private static String formatLink(String text, String link), which is
        // adapted for Markdown
        // maybe, needs to be adapted for AsciiDoc?
        // but not in the current Pull Request
        // createSmallBlueLinkSpans();

        //TODO: doesn't yet work, but it is called
        if (_highlightBiggerHeadings) {
            createSpanForMatches(HEADING_SIMPLE,
                    new WrMarkdownHeaderSpanCreator(_spannable, AD_COLOR_HEADING, _textSize));
        } else {
            createColorSpanForMatches(HEADING_SIMPLE, AD_COLOR_HEADING);
        }

        // TODO: Attribute definitions and usage

        //        createColorSpanForMatches(BLOCK_DELIMITED_QUOTATION, AD_COLOR_QUOTE);

        createColorSpanForMatches(LINK_PATTERN, AD_COLOR_LINK);
        createColorSpanForMatches(XREF_PATTERN, AD_COLOR_LINK);
        createColorSpanForMatches(IMAGE_PATTERN, AD_COLOR_LINK);
        createColorSpanForMatches(INCLUDE_PATTERN, AD_COLOR_LINK);
        createColorSpanForMatches(LIST_UNORDERED, AD_COLOR_LIST);
        createColorSpanForMatches(LIST_ORDERED, AD_COLOR_LIST);
        createColorSpanForMatches(ATTRIBUTE_DEFINITION, AD_COLOR_ATTRIBUTE);

        createStrikeThroughSpanForMatches(STRIKETHROUGH);
        createSubscriptStyleSpanForMatches(SUBSCRIPT);
        createSuperscriptStyleSpanForMatches(SUPERSCRIPT);
        createColoredUnderlineSpanForMatches(UNDERLINE, AD_COLOR_UNDERLINE);

        createStyleSpanForMatches(BOLD, Typeface.BOLD);
        createStyleSpanForMatches(ITALICS, Typeface.ITALIC);

        if (_highlightCodeChangeFont) {
            createMonospaceSpanForMatches(CODE);
            createMonospaceSpanForMatches(BLOCK_DELIMITED_LISTING);
            createMonospaceSpanForMatches(BLOCK_DELIMITED_LITERAL);
            createMonospaceSpanForMatches(LIST_UNORDERED);
            createMonospaceSpanForMatches(LIST_ORDERED);
            createMonospaceSpanForMatches(ATTRIBUTE_DEFINITION);
        }

        if (_highlightLineEnding) {
            createColorBackgroundSpan(HARD_LINE_BREAK, AD_COLORBACKGROUND_CODEBLOCK);
//            //test markdown original pattern, same issues, when content is created in windows
//            createColorBackgroundSpan(DOUBLESPACE_LINE_ENDING, AD_COLOR_CODEBLOCK);
        }

        if (_highlightCodeBlock) {
            createColorBackgroundSpan(CODE, AD_COLORBACKGROUND_CODEBLOCK);
            createColorSpanForMatches(CODE, AD_COLOR_TEXT_ON_COLORBACKGROUND);
            createColorBackgroundSpan(BLOCK_DELIMITED_LISTING, AD_COLORBACKGROUND_CODEBLOCK);
            createColorSpanForMatches(BLOCK_DELIMITED_LISTING, AD_COLOR_TEXT_ON_COLORBACKGROUND);
            createColorBackgroundSpan(BLOCK_DELIMITED_LITERAL, AD_COLORBACKGROUND_CODEBLOCK);
            createColorSpanForMatches(BLOCK_DELIMITED_LITERAL, AD_COLOR_TEXT_ON_COLORBACKGROUND);
            createColorBackgroundSpan(BLOCK_DELIMITED_QUOTATION, AD_COLORBACKGROUND_QUOTE);
            createColorSpanForMatches(BLOCK_DELIMITED_QUOTATION, AD_COLOR_TEXT_ON_COLORBACKGROUND);
            createColorBackgroundSpan(BLOCK_DELIMITED_EXAMPLE, AD_COLORBACKGROUND_EXAMPLE);
            createColorSpanForMatches(BLOCK_DELIMITED_EXAMPLE, AD_COLOR_TEXT_ON_COLORBACKGROUND);
            createColorBackgroundSpan(BLOCK_DELIMITED_SIDEBAR, AD_COLORBACKGROUND_SIDEBAR);
            createColorSpanForMatches(BLOCK_DELIMITED_SIDEBAR, AD_COLOR_TEXT_ON_COLORBACKGROUND);
            createColorBackgroundSpan(BLOCK_DELIMITED_COMMENT, AD_COLORBACKGROUND_COMMENT);
            createColorSpanForMatches(BLOCK_DELIMITED_COMMENT, AD_COLOR_TEXT_ON_COLORBACKGROUND_COMMENT);
        }

        createColorBackgroundSpan(LINE_COMMENT, AD_COLORBACKGROUND_COMMENT);
        createColorSpanForMatches(LINE_COMMENT, AD_COLOR_TEXT_ON_COLORBACKGROUND_COMMENT);
        createColorBackgroundSpan(HIGHLIGHT, AD_COLORBACKGROUND_HIGHLIGHT);
        createColorSpanForMatches(HIGHLIGHT, AD_COLOR_TEXT_ON_COLORBACKGROUND);
        createColorBackgroundSpan(ATTRIBUTE_REFERENCE, AD_COLORBACKGROUND_ATTRIBUTE);
        createColorSpanForMatches(ATTRIBUTE_REFERENCE, AD_COLOR_TEXT_ON_COLORBACKGROUND);
    }
}

