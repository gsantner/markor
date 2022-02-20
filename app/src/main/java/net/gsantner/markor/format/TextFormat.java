/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import android.app.Activity;
import android.text.InputFilter;
import android.text.TextWatcher;

import net.gsantner.markor.R;
import net.gsantner.markor.format.keyvalue.KeyValueConverter;
import net.gsantner.markor.format.keyvalue.KeyValueHighlighter;
import net.gsantner.markor.format.markdown.MarkdownAutoFormat;
import net.gsantner.markor.format.markdown.MarkdownHighlighter;
import net.gsantner.markor.format.markdown.MarkdownTextActions;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.format.plaintext.PlaintextConverter;
import net.gsantner.markor.format.plaintext.PlaintextHighlighter;
import net.gsantner.markor.format.plaintext.PlaintextTextActions;
import net.gsantner.markor.format.todotxt.TodoTxtAutoFormat;
import net.gsantner.markor.format.todotxt.TodoTxtHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTextActions;
import net.gsantner.markor.format.todotxt.TodoTxtTextConverter;
import net.gsantner.markor.format.zimwiki.ZimWikiAutoFormat;
import net.gsantner.markor.format.zimwiki.ZimWikiHighlighter;
import net.gsantner.markor.format.zimwiki.ZimWikiTextActions;
import net.gsantner.markor.format.zimwiki.ZimWikiTextConverter;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.Locale;

public class TextFormat {
    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_ZIMWIKI = R.string.action_format_zimwiki;
    public static final int FORMAT_MARKDOWN = R.string.action_format_markdown;
    public static final int FORMAT_PLAIN = R.string.action_format_plaintext;
    public static final int FORMAT_TODOTXT = R.string.action_format_todotxt;
    public static final int FORMAT_KEYVALUE = R.string.action_format_keyvalue;

    public final static MarkdownTextConverter CONVERTER_MARKDOWN = new MarkdownTextConverter();
    public final static ZimWikiTextConverter CONVERTER_ZIMWIKI = new ZimWikiTextConverter();
    public final static TodoTxtTextConverter CONVERTER_TODOTXT = new TodoTxtTextConverter();
    public final static KeyValueConverter CONVERTER_KEYVALUE = new KeyValueConverter();
    public final static PlaintextConverter CONVERTER_PLAINTEXT = new PlaintextConverter();

    // Order here is used to **determine** format by it's file extension and/or content heading
    private final static TextConverter[] CONVERTERS = new TextConverter[]{
            CONVERTER_MARKDOWN,
            CONVERTER_TODOTXT,
            CONVERTER_ZIMWIKI,
            CONVERTER_KEYVALUE,
            CONVERTER_PLAINTEXT,
    };

    public static boolean isTextFile(final String absolutePath) {
        return isTextFile(new File(absolutePath));
    }

    public static boolean isTextFile(final File file) {
        if (file == null) {
            return false;
        }
        final String path = file.getAbsolutePath().toLowerCase(Locale.ROOT);
        for (TextConverter converter : CONVERTERS) {
            if (converter.isFileOutOfThisFormat(path)) {
                return true;
            }
        }

        return FileUtils.isTextFile(file);
    }

    public interface TextFormatApplier {
        void applyTextFormat(int textFormatId);
    }

    public static TextFormat getFormat(int formatId, Activity activity, Document document, HighlightingEditor hlEditor) {
        TextFormat format = new TextFormat();
        switch (formatId) {
            case FORMAT_PLAIN: {
                format._converter = CONVERTER_PLAINTEXT;
                format._highlighter = new PlaintextHighlighter(hlEditor, document);
                format._textActions = new PlaintextTextActions(activity, document);
                format._autoFormatInputFilter = new MarkdownAutoFormat(); // Using the markdown syntax for plain text
                format._autoFormatTextWatcher = new ListHandler(MarkdownAutoFormat.getPrefixPatterns());
                break;
            }
            case FORMAT_TODOTXT: {
                format._converter = CONVERTER_TODOTXT;
                format._highlighter = new TodoTxtHighlighter(hlEditor, document);
                format._textActions = new TodoTxtTextActions(activity, document);
                format._autoFormatInputFilter = new TodoTxtAutoFormat();
                break;
            }
            case FORMAT_KEYVALUE: {
                format._converter = CONVERTER_KEYVALUE;
                format._highlighter = new KeyValueHighlighter(hlEditor, document);
                format._textActions = new PlaintextTextActions(activity, document);
                break;
            }
            case FORMAT_ZIMWIKI: {
                format._converter = CONVERTER_ZIMWIKI;
                format._highlighter = new ZimWikiHighlighter(hlEditor, document);
                format._textActions = new ZimWikiTextActions(activity, document);
                format._autoFormatInputFilter = new ZimWikiAutoFormat();
                format._autoFormatTextWatcher = new ListHandler(ZimWikiAutoFormat.getPrefixPatterns());
                break;
            }
            default:
            case FORMAT_MARKDOWN: {
                format._converter = CONVERTER_MARKDOWN;
                format._highlighter = new MarkdownHighlighter(hlEditor, document);
                format._textActions = new MarkdownTextActions(activity, document);
                format._autoFormatInputFilter = new MarkdownAutoFormat();
                format._autoFormatTextWatcher = new ListHandler(MarkdownAutoFormat.getPrefixPatterns());
                break;
            }
        }
        return format;
    }

    private TextActions _textActions;
    private Highlighter _highlighter;
    private TextConverter _converter;
    private InputFilter _autoFormatInputFilter;
    private TextWatcher _autoFormatTextWatcher;

    public TextActions getTextActions() {
        return _textActions;
    }

    public TextWatcher getAutoFormatTextWatcher() {
        return _autoFormatTextWatcher;
    }

    public InputFilter getAutoFormatInputFilter() {
        return _autoFormatInputFilter;
    }

    public Highlighter getHighlighter() {
        return _highlighter;
    }

    public TextConverter getConverter() {
        return _converter;
    }
}
