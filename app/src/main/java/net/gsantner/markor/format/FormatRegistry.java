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

import android.content.Context;
import android.text.InputFilter;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.binary.EmbedBinaryTextConverter;
import net.gsantner.markor.format.keyvalue.KeyValueSyntaxHighlighter;
import net.gsantner.markor.format.keyvalue.KeyValueTextConverter;
import net.gsantner.markor.format.markdown.MarkdownActionButtons;
import net.gsantner.markor.format.markdown.MarkdownAutoTextFormatter;
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.format.plaintext.PlaintextActionButtons;
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter;
import net.gsantner.markor.format.plaintext.PlaintextTextConverter;
import net.gsantner.markor.format.todotxt.TodoTxtActionButtons;
import net.gsantner.markor.format.todotxt.TodoTxtAutoTextFormatter;
import net.gsantner.markor.format.todotxt.TodoTxtSyntaxHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTextConverter;
import net.gsantner.markor.format.wikitext.WikitextActionButtons;
import net.gsantner.markor.format.wikitext.WikitextAutoTextFormatter;
import net.gsantner.markor.format.wikitext.WikitextSyntaxHighlighter;
import net.gsantner.markor.format.wikitext.WikitextTextConverter;
import net.gsantner.markor.frontend.textview.ListHandler;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.Locale;

public class FormatRegistry {
    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_WIKITEXT = R.string.action_format_wikitext;
    public static final int FORMAT_MARKDOWN = R.string.action_format_markdown;
    public static final int FORMAT_PLAIN = R.string.action_format_plaintext;
    public static final int FORMAT_TODOTXT = R.string.action_format_todotxt;
    public static final int FORMAT_KEYVALUE = R.string.action_format_keyvalue;
    public static final int FORMAT_EMBEDBINARY = R.string.action_format_embedbinary;


    public final static MarkdownTextConverter CONVERTER_MARKDOWN = new MarkdownTextConverter();
    public final static WikitextTextConverter CONVERTER_WIKITEXT = new WikitextTextConverter();
    public final static TodoTxtTextConverter CONVERTER_TODOTXT = new TodoTxtTextConverter();
    public final static KeyValueTextConverter CONVERTER_KEYVALUE = new KeyValueTextConverter();
    public final static PlaintextTextConverter CONVERTER_PLAINTEXT = new PlaintextTextConverter();
    public final static EmbedBinaryTextConverter CONVERTER_EMBEDBINARY = new EmbedBinaryTextConverter();


    // Order here is used to **determine** format by it's file extension and/or content heading
    private final static TextConverterBase[] CONVERTERS = new TextConverterBase[]{
            CONVERTER_MARKDOWN,
            CONVERTER_TODOTXT,
            CONVERTER_WIKITEXT,
            CONVERTER_KEYVALUE,
            CONVERTER_PLAINTEXT,
            CONVERTER_EMBEDBINARY,
    };

    public static boolean isTextFile(final String absolutePath) {
        return isTextFile(new File(absolutePath));
    }

    public static boolean isTextFile(final File file) {
        if (file == null) {
            return false;
        }
        final String filepath = file.getAbsolutePath().toLowerCase(Locale.ROOT);
        for (TextConverterBase converter : CONVERTERS) {
            if (converter.isFileOutOfThisFormat(filepath)) {
                return true;
            }
        }

        return GsFileUtils.isTextFile(file);
    }

    public interface TextFormatApplier {
        void applyTextFormat(int textFormatId);
    }

    public static FormatRegistry getFormat(int formatId, @NonNull final Context context, final Document document) {
        final FormatRegistry format = new FormatRegistry();
        final AppSettings appSettings = ApplicationObject.settings();

        switch (formatId) {
            case FORMAT_PLAIN: {
                format._converter = CONVERTER_PLAINTEXT;
                format._highlighter = new PlaintextSyntaxHighlighter(appSettings);
                format._textActions = new PlaintextActionButtons(context, document);
                format._autoFormatInputFilter = new MarkdownAutoTextFormatter(); // Using the markdown syntax for plain text
                format._autoFormatTextWatcher = new ListHandler(MarkdownAutoTextFormatter.getPrefixPatterns());
                break;
            }
            case FORMAT_TODOTXT: {
                format._converter = CONVERTER_TODOTXT;
                format._highlighter = new TodoTxtSyntaxHighlighter(appSettings);
                format._textActions = new TodoTxtActionButtons(context, document);
                format._autoFormatInputFilter = new TodoTxtAutoTextFormatter();
                break;
            }
            case FORMAT_KEYVALUE: {
                format._converter = CONVERTER_KEYVALUE;
                format._highlighter = new KeyValueSyntaxHighlighter(appSettings);
                format._textActions = new PlaintextActionButtons(context, document);
                break;
            }
            case FORMAT_WIKITEXT: {
                format._converter = CONVERTER_WIKITEXT;
                format._highlighter = new WikitextSyntaxHighlighter(appSettings);
                format._textActions = new WikitextActionButtons(context, document);
                format._autoFormatInputFilter = new WikitextAutoTextFormatter();
                format._autoFormatTextWatcher = new ListHandler(WikitextAutoTextFormatter.getPrefixPatterns());
                break;
            }
            case FORMAT_EMBEDBINARY: {
                format._converter = CONVERTER_EMBEDBINARY;
                format._highlighter = new PlaintextSyntaxHighlighter(appSettings);
                format._textActions = new PlaintextActionButtons(context, document);
                break;
            }
            default:
            case FORMAT_MARKDOWN: {
                format._converter = CONVERTER_MARKDOWN;
                format._highlighter = new MarkdownSyntaxHighlighter(appSettings);
                format._textActions = new MarkdownActionButtons(context, document);
                format._autoFormatInputFilter = new MarkdownAutoTextFormatter();
                format._autoFormatTextWatcher = new ListHandler(MarkdownAutoTextFormatter.getPrefixPatterns());
                break;
            }
        }
        return format;
    }

    private ActionButtonBase _textActions;
    private SyntaxHighlighterBase _highlighter;
    private TextConverterBase _converter;
    private InputFilter _autoFormatInputFilter;
    private TextWatcher _autoFormatTextWatcher;

    public ActionButtonBase getTextActions() {
        return _textActions;
    }

    public TextWatcher getAutoFormatTextWatcher() {
        return _autoFormatTextWatcher;
    }

    public InputFilter getAutoFormatInputFilter() {
        return _autoFormatInputFilter;
    }

    public SyntaxHighlighterBase getHighlighter() {
        return _highlighter;
    }

    public TextConverterBase getConverter() {
        return _converter;
    }
}
