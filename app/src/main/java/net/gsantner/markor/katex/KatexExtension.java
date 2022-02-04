package net.gsantner.markor.katex;

import net.gsantner.markor.katex.internal.*;
import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.builder.Extension;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;

/**
 * Extension for KaTeX
 */
public class KatexExtension implements Parser.ParserExtension
        , HtmlRenderer.HtmlRendererExtension
        , Formatter.FormatterExtension
{
    public static final DataKey<Boolean> INLINE_MATH_PARSER = new DataKey<>("INLINE_MATH_PARSER", true);
    public static final DataKey<Boolean> DISPLAY_MATH_PARSER = new DataKey<>("DISPLAY_MATH_PARSER", true);

    public static final DataKey<String> INLINE_MATH_CLASS = new DataKey<>("INLINE_MATH_CLASS", "katex");
    public static final DataKey<String> DISPLAY_MATH_CLASS = new DataKey<>("DISPLAY_MATH_CLASS", "katex");

    public static final DataKey<Boolean> RENDER_BLOCK_MATH = new DataKey<>("RENDER_BLOCK_MATH", true);
    public static final DataKey<String> BLOCK_MATH_CLASS = new DataKey<>("BLOCK_MATH_CLASS", "katex");
    public static final DataKey<String> BLOCK_INFO_DELIMITERS = new DataKey<>("BLOCK_INFO_DELIMITERS", " ");

    private KatexExtension() {
    }

    public static Extension create() {
        return new KatexExtension();
    }

    @Override
    public void rendererOptions(final MutableDataHolder options) {

    }

    @Override
    public void parserOptions(final MutableDataHolder options) {

    }

    @Override
    public void extend(final Formatter.Builder builder) {
        builder.nodeFormatterFactory(new KatexNodeFormatter.Factory());
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        KatexOptions options = new KatexOptions(parserBuilder);
        if (options.inlineMathParser) {
            parserBuilder.customInlineParserExtensionFactory(new KatexInlineMathParser.Factory());
        }
        if (options.displayMathParser) {
            parserBuilder.customInlineParserExtensionFactory(new KatexDisplayMathParser.Factory());
        }
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
        if (rendererBuilder.isRendererType("HTML")) {
            rendererBuilder.nodeRendererFactory(new KatexNodeRenderer.Factory());
        } else if (rendererBuilder.isRendererType("JIRA")) {
            //rendererBuilder.nodeRendererFactory(new KatexJiraRenderer.Factory());
        }
    }
}
