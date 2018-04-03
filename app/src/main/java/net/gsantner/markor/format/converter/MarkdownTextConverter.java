/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.converter;

import android.content.Context;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

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
    // See https://github.com/vsch/flexmark-java/wiki/Extensions#tables
    private static final List<Extension> MARKDOWN_ENABLED_EXTENSIONS = Arrays.asList(
            StrikethroughExtension.create(),
            AutolinkExtension.create(),
            InsExtension.create(),
            JekyllTagExtension.create(),
            JekyllFrontMatterExtension.create(),
            TablesExtension.create(),
            TaskListExtension.create(),
            EmojiExtension.create(),
            AnchorLinkExtension.create(),
            YamlFrontMatterExtension.create());
    private static final Parser parser = Parser.builder().extensions(MARKDOWN_ENABLED_EXTENSIONS).build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().extensions(MARKDOWN_ENABLED_EXTENSIONS).build();


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, MARKDOWN_ENABLED_EXTENSIONS);

        options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY);
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, true);
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        options.set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "header_no_underline");


        String markupRendered = renderer.withOptions(options).render(parser.parse(markup));
        return putContentIntoTemplate(context, markupRendered);
    }
}
