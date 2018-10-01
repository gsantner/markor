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
import net.gsantner.markor.format.markdown.MarkdownHighlighter;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.format.markdown.MarkdownTextModuleActions;
import net.gsantner.markor.format.plaintext.PlaintextConverter;
import net.gsantner.markor.format.plaintext.PlaintextHighlighter;
import net.gsantner.markor.format.plaintext.PlaintextModuleActions;
import net.gsantner.markor.format.todotxt.TodoTxtHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTextConverter;
import net.gsantner.markor.format.todotxt.TodoTxtTextModuleActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.TextModuleActions;

public class TextFormat {
    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_MARKDOWN = R.id.action_format_markdown;
    public static final int FORMAT_PLAIN = R.id.action_format_plaintext;
    public static final int FORMAT_TODOTXT = R.id.action_format_todotxt;

    public interface TextFormatApplier {
        void applyTextFormat(int textFormatId);
    }

    public static TextFormat getFormat(int formatType, Activity activity, Document document) {
        TextFormat format = new TextFormat();
        switch (formatType) {
            case FORMAT_PLAIN: {
                format.setConverter(new PlaintextConverter());
                format.setHighlighter(new PlaintextHighlighter());
                format.setTextModuleActions(new PlaintextModuleActions(activity, document));
                break;
            }
            case FORMAT_TODOTXT: {
                format.setConverter(new TodoTxtTextConverter());
                format.setHighlighter(new TodoTxtHighlighter());
                format.setTextModuleActions(new TodoTxtTextModuleActions(activity, document));
                break;
            }
            default:
            case FORMAT_MARKDOWN: {
                format.setConverter(new MarkdownTextConverter());
                format.setHighlighter(new MarkdownHighlighter());
                format.setTextModuleActions(new MarkdownTextModuleActions(activity, document));
                break;
            }
        }
        return format;
    }

    //
    //
    //
    private TextModuleActions _textModuleActions;
    private Highlighter _highlighter;
    private TextConverter _converter;

    public TextFormat() {
    }

    public TextFormat(TextModuleActions textModuleActions, Highlighter highlighter, MarkdownTextConverter converter) {
        _textModuleActions = textModuleActions;
        _highlighter = highlighter;
        _converter = converter;
    }


    //
    //
    //

    public TextModuleActions getTextModuleActions() {
        return _textModuleActions;
    }

    public void setTextModuleActions(TextModuleActions textModuleActions) {
        _textModuleActions = textModuleActions;
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
