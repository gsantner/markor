/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
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
import net.gsantner.markor.format.orgmode.OrgmodeReplacePatternGenerator;
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
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FormatRegistry {
    // Format ID
    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_WIKITEXT = R.string.action_format_wikitext;
    public static final int FORMAT_MARKDOWN = R.string.action_format_markdown;
    public static final int FORMAT_CSV = R.string.action_format_csv;
    public static final int FORMAT_PLAIN = R.string.action_format_plaintext;
    public static final int FORMAT_CODE = R.string.action_format_code; // For HTML, CSS, JavaScript, C++, Java, Python, ...
    public static final int FORMAT_ASCIIDOC = R.string.action_format_asciidoc;
    public static final int FORMAT_TODO_TXT = R.string.action_format_todotxt;
    public static final int FORMAT_KEY_VALUE = R.string.action_format_keyvalue;
    public static final int FORMAT_EMBED_BINARY = R.string.action_format_embedbinary;
    public static final int FORMAT_ORG_MODE = R.string.action_format_orgmode;

    public final static MarkdownTextConverter CONVERTER_MARKDOWN = new MarkdownTextConverter();
    public final static WikitextTextConverter CONVERTER_WIKITEXT = new WikitextTextConverter();
    public final static TodoTxtTextConverter CONVERTER_TODO_TXT = new TodoTxtTextConverter();
    public final static KeyValueTextConverter CONVERTER_KEY_VALUE = new KeyValueTextConverter();
    public final static CsvTextConverter CONVERTER_CSV = new CsvTextConverter();
    public final static PlaintextTextConverter CONVERTER_PLAINTEXT = new PlaintextTextConverter();
    public final static AsciidocTextConverter CONVERTER_ASCIIDOC = new AsciidocTextConverter();
    public final static EmbedBinaryTextConverter CONVERTER_EMBED_BINARY = new EmbedBinaryTextConverter();
    public final static OrgmodeTextConverter CONVERTER_ORG_MODE = new OrgmodeTextConverter();

    // File extensions that are known not to be supported by Markor
    private static final List<String> EXTERNAL_FILE_EXTENSIONS = Collections.singletonList(".pdf");

    public static class Format {
        public final @StringRes int format; // Format ID
        public final @StringRes int name;
        public final String extension; // File extension with dot
        public final String language;
        public final TextConverterBase converter;

        public Format(@StringRes final int format, @StringRes final int name, final String extension, final String language, final TextConverterBase converter) {
            this.format = format;
            this.name = name;
            this.language = language;
            this.extension = extension;
            this.converter = converter;
        }
    }

    // Order here is used to **determine** format by its file extension and/or content heading
    public static final List<Format> FORMATS = Arrays.asList(
            new Format(FormatRegistry.FORMAT_MARKDOWN, R.string.markdown, ".md", "markdown", CONVERTER_MARKDOWN),
            new Format(FormatRegistry.FORMAT_TODO_TXT, R.string.todo_txt, ".todo.txt", "todo", CONVERTER_TODO_TXT),
            new Format(FormatRegistry.FORMAT_ASCIIDOC, R.string.asciidoc, ".adoc", "ascii_doc", CONVERTER_ASCIIDOC),
            new Format(FormatRegistry.FORMAT_ORG_MODE, R.string.orgmode, ".org", "org_mode", CONVERTER_ORG_MODE),
            new Format(FormatRegistry.FORMAT_WIKITEXT, R.string.wikitext, ".txt", "wiki", CONVERTER_WIKITEXT),
            new Format(FormatRegistry.FORMAT_KEY_VALUE, R.string.key_value, ".json", "json", CONVERTER_KEY_VALUE),
            new Format(FormatRegistry.FORMAT_CSV, R.string.csv, ".csv", "csv", CONVERTER_CSV),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".html", "html", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".css", "css", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".js", "javascript", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".c", "c", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".cpp", "cpp", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".java", "java", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".py", "python", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".diff", "diff", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".kt", "kotlin", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".lua", "lua", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".php", "php", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".sh", "shell", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_CODE, R.string.code, ".xml", "xml", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_PLAIN, R.string.plaintext, ".txt", "", CONVERTER_PLAINTEXT),
            new Format(FormatRegistry.FORMAT_EMBED_BINARY, R.string.embed_binary, ".jpg", "", CONVERTER_EMBED_BINARY),
            new Format(FormatRegistry.FORMAT_UNKNOWN, R.string.none, "", "", null)
    );

    public static boolean isFileSupported(final File file, final boolean... textOnly) {
        final boolean isTextOnly = textOnly != null && textOnly.length > 0 && textOnly[0];
        if (file != null) {
            for (final Format format : FORMATS) {
                if (isTextOnly && format.converter instanceof EmbedBinaryTextConverter) {
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
        final AppSettings appSettings = AppSettings.get(context);

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
                format._highlighter = new PlaintextSyntaxHighlighter(appSettings, document.extension);
                // Should implement code action buttons for PlaintextActionButtons
                format._textActions = new PlaintextActionButtons(context, document);
                format._autoFormatInputFilter = new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns);
                format._autoFormatTextWatcher = new ListHandler(MarkdownReplacePatternGenerator.formatPatterns);
                break;
            }
            case FORMAT_CODE: {
                format._converter = CONVERTER_PLAINTEXT;
                format._highlighter = null; // No need for CodeMirror editor
                // We should implement some code action buttons (e.g. {}, [], ...) for PlaintextActionButtons in the future
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
            case FORMAT_TODO_TXT: {
                format._converter = CONVERTER_TODO_TXT;
                format._highlighter = new TodoTxtSyntaxHighlighter(appSettings);
                format._textActions = new TodoTxtActionButtons(context, document);
                format._autoFormatInputFilter = new TodoTxtAutoTextFormatter();
                break;
            }
            case FORMAT_KEY_VALUE: {
                format._converter = CONVERTER_KEY_VALUE;
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
            case FORMAT_EMBED_BINARY: {
                format._converter = CONVERTER_EMBED_BINARY;
                format._highlighter = new PlaintextSyntaxHighlighter(appSettings);
                format._textActions = new PlaintextActionButtons(context, document);
                break;
            }
            case FORMAT_ORG_MODE: {
                format._converter = CONVERTER_ORG_MODE;
                format._highlighter = new OrgmodeSyntaxHighlighter(appSettings);
                format._textActions = new OrgmodeActionButtons(context, document);
                format._autoFormatInputFilter = new AutoTextFormatter(OrgmodeReplacePatternGenerator.formatPatterns);
                format._autoFormatTextWatcher = new ListHandler(OrgmodeReplacePatternGenerator.formatPatterns);
                break;
            }
            case FORMAT_MARKDOWN:
            default: {
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

    /**
     * Get default syntax highlighting language by format id and extension.
     *
     * @param formatId  the format id.
     * @param extension the file extension (with dot).
     * @return the name of the syntax highlighting language.
     */
    public static String getDefaultLanguage(int formatId, final String extension) {
        for (Format format : FORMATS) {
            if (format.format == formatId) {
                if (formatId == FORMAT_CODE) {
                    if (format.extension.equals(extension)) {
                        return format.language;
                    }
                } else {
                    return format.language;
                }
            }
        }
        return "";
    }

    public static boolean isCodeFormat(String extension) {
        for (Format format : FORMATS) {
            return format.extension.equals(extension);
        }
        return false;
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

    public static boolean isExternalFile(final File file) {
        final String ext = GsFileUtils.getFilenameExtension(file).toLowerCase();
        return EXTERNAL_FILE_EXTENSIONS.contains(ext);
    }
}
