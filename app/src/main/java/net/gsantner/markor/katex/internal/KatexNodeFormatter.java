package net.gsantner.markor.katex.internal;

import net.gsantner.markor.katex.KatexBlockQuote;
import com.vladsch.flexmark.formatter.*;
import com.vladsch.flexmark.util.options.DataHolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class KatexNodeFormatter implements NodeFormatter {

    public KatexNodeFormatter(DataHolder options) {

    }

    @Override
    public Set<NodeFormattingHandler<?>> getNodeFormattingHandlers() {
        return new HashSet<NodeFormattingHandler<?>>(Arrays.asList(
                new NodeFormattingHandler<KatexBlockQuote>(KatexBlockQuote.class, new CustomNodeFormatter<KatexBlockQuote>() {
                    @Override
                    public void render(KatexBlockQuote node, NodeFormatterContext context, MarkdownWriter markdown) {
                        KatexNodeFormatter.this.render(node, context, markdown);
                    }
                })
        ));
    }

    @Override
    public Set<Class<?>> getNodeClasses() {
        return new HashSet<Class<?>>(Arrays.asList(
                KatexBlockQuote.class
        ));
    }

    private void render(KatexBlockQuote node, NodeFormatterContext context, MarkdownWriter markdown) {
        markdown.append(">>>").line();
        context.renderChildren(node);
        markdown.append("<<<").line();
    }

    public static class Factory implements NodeFormatterFactory {
        @Override
        public NodeFormatter create(final DataHolder options) {
            return new KatexNodeFormatter(options);
        }
    }
}
