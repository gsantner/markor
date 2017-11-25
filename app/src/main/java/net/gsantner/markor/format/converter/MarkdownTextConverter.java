/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.converter;

import android.content.Context;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class MarkdownTextConverter extends TextConverter {
    //########################
    //## Extensions
    //########################
    public static final String EXT_MARKDOWN__TXT = ".txt";
    public static final String EXT_MARKDOWN__MD = ".md";
    public static final String EXT_MARKDOWN__MARKDOWN = ".markdown";
    public static final String EXT_MARKDOWN__MKD = ".mkd";
    public static final String EXT_MARKDOWN__MDOWN = ".mdown";
    public static final String EXT_MARKDOWN__MKDN = ".mkdn";
    public static final String EXT_MARKDOWN__MDWN = ".mdwn";
    public static final String EXT_MARKDOWN__TEXT = ".text";
    public static final String EXT_MARKDOWN__RMD = ".rmd";

    public static final Pattern MD_EXTENSION_PATTERN = Pattern.compile("((?i)\\.((md)|(markdown)|(mkd)|(mdown)|(mkdn)|(txt)|(mdwn)|(text)|(rmd))$)");
    public static final String[] MD_EXTENSIONS = new String[]{
            EXT_MARKDOWN__MD, EXT_MARKDOWN__MARKDOWN, EXT_MARKDOWN__MKD, EXT_MARKDOWN__MDOWN,
            EXT_MARKDOWN__MKDN, EXT_MARKDOWN__TXT, EXT_MARKDOWN__MDWN, EXT_MARKDOWN__TEXT, EXT_MARKDOWN__RMD
    };

    //########################
    //## Converter library
    //########################
    // See https://github.com/atlassian/commonmark-java/blob/master/README.md#extensions
    private static final List<Extension> COMMONMARK_JAVA_EXTENSIONS = Arrays.asList(
            StrikethroughExtension.create(),
            TablesExtension.create(),
            AutolinkExtension.create(),
            InsExtension.create(),
            HeadingAnchorExtension.create(),
            YamlFrontMatterExtension.create());
    private static final Parser parser = Parser.builder().extensions(COMMONMARK_JAVA_EXTENSIONS).build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().extensions(COMMONMARK_JAVA_EXTENSIONS).build();


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context) {
        String markupRendered = renderer.render(parser.parse(markup));
        return putContentIntoTemplate(context, markupRendered);
    }
}
