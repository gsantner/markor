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
import net.gsantner.markor.format.markdown.MarkdownTextActions;
import net.gsantner.markor.format.plaintext.PlaintextConverter;
import net.gsantner.markor.format.plaintext.PlaintextHighlighter;
import net.gsantner.markor.format.plaintext.PlaintextTextActions;
import net.gsantner.markor.format.todotxt.TodoTxtHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTextConverter;
import net.gsantner.markor.format.todotxt.TodoTxtTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.TextActions;

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
                format.setTextActions(new PlaintextTextActions(activity, document));
                break;
            }
            case FORMAT_TODOTXT: {
                format.setConverter(new TodoTxtTextConverter());
                format.setHighlighter(new TodoTxtHighlighter());
                format.setTextActions(new TodoTxtTextActions(activity, document));
                break;
            }
            default:
            case FORMAT_MARKDOWN: {
                format.setConverter(new MarkdownTextConverter());
                format.setHighlighter(new MarkdownHighlighter());
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

    public TextFormat(TextActions textActions, Highlighter highlighter, MarkdownTextConverter converter) {
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
