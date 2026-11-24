package net.gsantner.markor.service;

import net.gsantner.markor.format.FormatRegistry;

public class HtmlConversionServiceFactory {
    public static HtmlConversionService getHtmlConversionService(int targetFormat) {
        if (targetFormat == FormatRegistry.FORMAT_MARKDOWN) {
            return new Html2MarkdownService();
        } else {
            return null;
        }
    }
}
