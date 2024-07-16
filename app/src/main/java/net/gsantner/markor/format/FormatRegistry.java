/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import android.content.Context;
import android.text.InputFilter;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.asciidoc.AsciidocActionButtons;
import net.gsantner.markor.format.asciidoc.AsciidocSyntaxHighlighter;
import net.gsantner.markor.format.asciidoc.AsciidocTextConverter;
import net.gsantner.markor.format.binary.EmbedBinaryTextConverter;
import net.gsantner.markor.format.csv.CsvSyntaxHighlighter;
import net.gsantner.markor.format.csv.CsvTextConverter;
import net.gsantner.markor.format.keyvalue.KeyValueSyntaxHighlighter;
import net.gsantner.markor.format.keyvalue.KeyValueTextConverter;
import net.gsantner.markor.format.markdown.MarkdownActionButtons;
import net.gsantner.markor.format.markdown.MarkdownReplacePatternGenerator;
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.format.orgmode.OrgmodeActionButtons;
import net.gsantner.markor.format.orgmode.OrgmodeSyntaxHighlighter;
import net.gsantner.markor.format.orgmode.OrgmodeTextConverter;
import net.gsantner.markor.format.plaintext.PlaintextActionButtons;
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter;
import net.gsantner.markor.format.plaintext.PlaintextTextConverter;
import net.gsantner.markor.format.todotxt.TodoTxtActionButtons;
import net.gsantner.markor.format.todotxt.TodoTxtAutoTextFormatter;
import net.gsantner.markor.format.todotxt.TodoTxtSyntaxHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTextConverter;
import net.gsantner.markor.format.wikitext.WikitextActionButtons;
import net.gsantner.markor.format.wikitext.WikitextReplacePatternGenerator;
import net.gsantner.markor.format.wikitext.WikitextSyntaxHighlighter;
import net.gsantner.markor.format.wikitext.WikitextTextConverter;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.ListHandler;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FormatRegistry {
    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_WIKITEXT = R.string.action_format_wikitext;
    public static final int FORMAT_MARKDOWN = R.string.action_format_markdown;
    public static final int FORMAT_CSV = R.string.action_format_csv;
    public static final int FORMAT_PLAIN = R.string.action_format_plaintext;
    public static final int FORMAT_ASCIIDOC = R.string.action_format_asciidoc;
    public static final int FORMAT_TODOTXT = R.string.action_format_todotxt;
    public static final int FORMAT_KEYVALUE = R.string.action_format_keyvalue;
    public static final int FORMAT_EMBEDBINARY = R.string.action_format_embedbinary;
    public static final int FORMAT_ORGMODE = R.string.action_format_orgmode;


    public final static MarkdownTextConverter CONVERTER_MARKDOWN = new MarkdownTextConverter();
    public final static WikitextTextConverter CONVERTER_WIKITEXT = new WikitextTextConverter();
    public final static TodoTxtTextConverter CONVERTER_TODOTXT = new TodoTxtTextConverter();
    public final static KeyValueTextConverter CONVERTER_KEYVALUE = new KeyValueTextConverter();
    public final static CsvTextConverter CONVERTER_CSV = new CsvTextConverter();
    public final static PlaintextTextConverter CONVERTER_PLAINTEXT = new PlaintextTextConverter();
    public final static AsciidocTextConverter CONVERTER_ASCIIDOC = new AsciidocTextConverter();
    public final static EmbedBinaryTextConverter CONVERTER_EMBEDBINARY = new EmbedBinaryTextConverter();
    public final static OrgmodeTextConverter CONVERTER_ORGMODE = new OrgmodeTextConverter();


    public static class Format {
        public final @StringRes int format, name;
        public final String defaultExtensionWithDot;
        public final TextConverterBase converter;

        public Format(@StringRes final int a_format, @StringRes final int a_name, final String a_defaultFileExtension, final TextConverterBase a_converter) {
            format = a_format;
            name = a_name;
            defaultExtensionWithDot = a_defaultFileExtension;
            converter = a_converter;
        }
    }

    // Order here is used to **determine** format by it's file extension and/or content heading
    public static final List<Format> FORMATS = Arrays.asList(
            new Format(FormatRegistry.FORMAT_MARKDOWN, R.string.markdown, ".md", CONVERTER_MARKDOWN),
            new Format(FormatRegistry.FORMAT_TODOTXT, R.string.todo_txt, ".todo.txt", CONVERTER_TODOTXT),
            new Format(FormatRegistry.FORMAT_CSV, R.string.csv, ".csv", CONVERTER_CSV),
            new Format(FormatRegistry.FORMAT_WIKITEXT, R.string.wikitext, ".txt", CONVERTER_WIKITEXT),
            new Format(FormatRegistry.FORMAT_KEYVALUE, R.string.key_value, ".json", CONVERTER_KEYVALUE),
            new Format(FormatRegistry.FORMAT_ASCIIDOC, R.string.asciidoc, ".adoc", CONVERTER_ASCIIDOC),
            new Format(FormatRegistry.FORMAT_ORGMODE, R.string.orgmode, ".org", CONVERTER_ORGMODE),
            new Format(FormatRegistry.FORMAT_EMBEDBINARY, R.string.embed_binary, ".jpg", CONVERTER_EMBEDBINARY),
            new Format(FormatRegistry.FORMAT_PLAIN, R.string.plaintext, ".txt", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_UNKNOWN, R.string.none, "", null)
    );

    public static boolean isFileSupported(final File file, final boolean... textOnly) {
        final boolean textonly = textOnly != null && textOnly.length > 0 && textOnly[0];
        if (file != null) {
            for (final Format format : FORMATS) {
                if (textonly && format.converter instanceof EmbedBinaryTextConverter) {
                    continue;
                }
                if (format.converter != null && format.converter.isFileOutOfThisFormat(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    public interface TextFormatApplier {
        void applyTextFormat(int textFormatId);
    }

    public static FormatRegistry getFormat(int formatId, @NonNull final Context context, final Document document) {
        final FormatRegistry format = new FormatRegistry();
        final AppSettings appSettings = ApplicationObject.settings();

        switch (formatId) {
            case FORMAT_CSV: {
                format._converter = CONVERTER_CSV;
                format._highlighter = new CsvSyntaxHighlighter(appSettings);

                // TODO k3b ????
                format._textActions = new PlaintextActionButtons(context, document);
                format._autoFormatInputFilter = new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns);
                format._autoFormatTextWatcher = new ListHandler(MarkdownReplacePatternGenerator.formatPatterns);
                break;
            }
            case FORMAT_PLAIN: {
                format._converter = CONVERTER_PLAINTEXT;
                format._highlighter = new PlaintextSyntaxHighlighter(appSettings);
                format._textActions = new PlaintextActionButtons(context, document);
                format._autoFormatInputFilter = new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns);
                format._autoFormatTextWatcher = new ListHandler(MarkdownReplacePatternGenerator.formatPatterns);
                break;
            }
            case FORMAT_ASCIIDOC: {
                format._converter = CONVERTER_ASCIIDOC;
                format._highlighter = new AsciidocSyntaxHighlighter(appSettings);
                format._textActions = new AsciidocActionButtons(context, document);
                format._autoFormatInputFilter = new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns);
                format._autoFormatTextWatcher = new ListHandler(MarkdownReplacePatternGenerator.formatPatterns);
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
                format._autoFormatInputFilter = new AutoTextFormatter(WikitextReplacePatternGenerator.formatPatterns);
                format._autoFormatTextWatcher = new ListHandler(WikitextReplacePatternGenerator.formatPatterns);
                break;
            }
            case FORMAT_EMBEDBINARY: {
                format._converter = CONVERTER_EMBEDBINARY;
                format._highlighter = new PlaintextSyntaxHighlighter(appSettings);
                format._textActions = new PlaintextActionButtons(context, document);
                break;
            }
            case FORMAT_ORGMODE: {
                format._converter = CONVERTER_ORGMODE;
                format._highlighter = new OrgmodeSyntaxHighlighter(appSettings);
                format._textActions = new OrgmodeActionButtons(context, document);
                format._autoFormatInputFilter = new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns);
                format._autoFormatTextWatcher = new ListHandler(MarkdownReplacePatternGenerator.formatPatterns);
                break;
            }
            default:
            case FORMAT_MARKDOWN: {
                formatId = FORMAT_MARKDOWN;
                format._converter = CONVERTER_MARKDOWN;
                format._highlighter = new MarkdownSyntaxHighlighter(appSettings);
                format._textActions = new MarkdownActionButtons(context, document);
                format._autoFormatInputFilter = new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns);
                format._autoFormatTextWatcher = new ListHandler(MarkdownReplacePatternGenerator.formatPatterns);
                break;
            }
        }
        format._formatId = formatId;
        return format;
    }

    private ActionButtonBase _textActions;
    private SyntaxHighlighterBase _highlighter;
    private TextConverterBase _converter;
    private InputFilter _autoFormatInputFilter;
    private TextWatcher _autoFormatTextWatcher;
    private int _formatId;

    public ActionButtonBase getActions() {
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

    public int getFormatId() {
        return _formatId;
    }
}
