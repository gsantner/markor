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

import net.gsantner.markor.R;
import net.gsantner.markor.format.keyvalue.KeyValueConverter;
import net.gsantner.markor.format.keyvalue.KeyValueHighlighter;
import net.gsantner.markor.format.markdown.MarkdownHighlighter;
import net.gsantner.markor.format.markdown.MarkdownTextActions;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.format.plaintext.PlaintextConverter;
import net.gsantner.markor.format.plaintext.PlaintextHighlighter;
import net.gsantner.markor.format.plaintext.PlaintextTextActions;
import net.gsantner.markor.format.todotxt.TodoTxtHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTextActions;
import net.gsantner.markor.format.todotxt.TodoTxtTextConverter;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.ui.hleditor.TextActions;

import java.io.File;
import java.util.Locale;

public class TextFormat {
    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_MARKDOWN = R.id.action_format_markdown;
    public static final int FORMAT_PLAIN = R.id.action_format_plaintext;
    public static final int FORMAT_TODOTXT = R.id.action_format_todotxt;
    public static final int FORMAT_KEYVALUE = R.id.action_format_keyvalue;

    public final static MarkdownTextConverter CONVERTER_MARKDOWN = new MarkdownTextConverter();
    public final static TodoTxtTextConverter CONVERTER_TODOTXT = new TodoTxtTextConverter();
    public final static KeyValueConverter CONVERTER_KEYVALUE = new KeyValueConverter();
    public final static PlaintextConverter CONVERTER_PLAINTEXT = new PlaintextConverter();
    private final static TextConverter[] CONVERTERS = new TextConverter[]{CONVERTER_MARKDOWN, CONVERTER_TODOTXT, CONVERTER_KEYVALUE, CONVERTER_PLAINTEXT};

    // Either pass file or null and absolutePath
    public static boolean isTextFile(File file, String... absolutePath) {
        if (file == null && (absolutePath == null || absolutePath.length < 1)) {
            return false;
        }

        String path = (absolutePath != null && absolutePath.length > 0) ? absolutePath[0] : file.getAbsolutePath();
        path = path.toLowerCase(Locale.ROOT);

        for (TextConverter converter : CONVERTERS) {
            if (converter.isFileOutOfThisFormat(path)) {
                return true;
            }
        }
        return false;
    }

    public interface TextFormatApplier {
        void applyTextFormat(int textFormatId);
    }

    public static TextFormat getFormat(int formatId, Activity activity, Document document, HighlightingEditor hlEditor) {
        TextFormat format = new TextFormat();
        switch (formatId) {
            case FORMAT_PLAIN: {
                format.setConverter(CONVERTER_PLAINTEXT);
                format.setHighlighter(new PlaintextHighlighter(hlEditor, document));
                format.setTextActions(new PlaintextTextActions(activity, document));
                break;
            }
            case FORMAT_TODOTXT: {
                format.setConverter(CONVERTER_TODOTXT);
                format.setHighlighter(new TodoTxtHighlighter(hlEditor, document));
                format.setTextActions(new TodoTxtTextActions(activity, document));
                break;
            }

            case FORMAT_KEYVALUE: {
                format.setConverter(CONVERTER_KEYVALUE);
                format.setHighlighter(new KeyValueHighlighter(hlEditor, document));
                format.setTextActions(new PlaintextTextActions(activity, document));
                break;
            }
            default:
            case FORMAT_MARKDOWN: {
                format.setConverter(CONVERTER_MARKDOWN);
                format.setHighlighter(new MarkdownHighlighter(hlEditor, document));
                format.setTextActions(new MarkdownTextActions(activity, document));
                break;
            }
        }
        return format;
    }

    //
    //
    //
    private TextActions _textActions;
    private Highlighter _highlighter;
    private TextConverter _converter;

    public TextFormat() {
    }

    public TextFormat(TextActions textActions, Highlighter highlighter, TextConverter converter) {
        _textActions = textActions;
        _highlighter = highlighter;
        _converter = converter;
    }


    //
    //
    //

    public TextActions getTextActions() {
        return _textActions;
    }

    public void setTextActions(TextActions textActions) {
        _textActions = textActions;
    }

    public Highlighter getHighlighter() {
        return _highlighter;
    }

    public void setHighlighter(Highlighter highlighter) {
        _highlighter = highlighter;
    }

    public TextConverter getConverter() {
        return _converter;
    }

    public void setConverter(TextConverter converter) {
        _converter = converter;
    }
}
