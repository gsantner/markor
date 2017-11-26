/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format;

import android.app.Activity;

import net.gsantner.markor.R;
import net.gsantner.markor.format.converter.MarkdownTextConverter;
import net.gsantner.markor.format.converter.PlainTextConverter;
import net.gsantner.markor.format.converter.TextConverter;
import net.gsantner.markor.format.converter.TodoTxtTextConverter;
import net.gsantner.markor.format.highlighter.Highlighter;
import net.gsantner.markor.format.highlighter.markdown.MarkdownHighlighter;
import net.gsantner.markor.format.highlighter.plain.PlainHighlighter;
import net.gsantner.markor.format.highlighter.todotxt.TodoTxtHighlighter;
import net.gsantner.markor.format.moduleactions.MarkdownTextModuleActions;
import net.gsantner.markor.format.moduleactions.PlainTextModuleActions;
import net.gsantner.markor.format.moduleactions.TextModuleActions;
import net.gsantner.markor.format.moduleactions.TodoTxtTextModuleActions;
import net.gsantner.markor.model.Document;

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
                format.setConverter(new PlainTextConverter());
                format.setHighlighter(new PlainHighlighter());
                format.setTextModuleActions(new PlainTextModuleActions(activity, document));
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
