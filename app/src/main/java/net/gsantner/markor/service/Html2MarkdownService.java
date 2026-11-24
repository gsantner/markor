package net.gsantner.markor.service;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import net.gsantner.markor.format.markdown.html2md.HtmlConverterTextExtension;

import java.util.Collections;

public class Html2MarkdownService implements HtmlConversionService {
    /**
     * Convert HTML to Markdown.
     *
     * @param htmlText the HTML text
     * @return Markdown text converted from htmlText
     */
    @Override
    public String convert(String htmlText) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Collections.singletonList(HtmlConverterTextExtension.create()));
        options.set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false);
        return FlexmarkHtmlConverter.builder(options).build().convert(htmlText);
    }
}
