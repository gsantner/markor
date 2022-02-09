package net.gsantner.markor.util.flexmark.ext.katex.internal;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ast.util.ReferenceRepository;
import com.vladsch.flexmark.ast.util.TextCollectingVisitor;
import net.gsantner.markor.util.flexmark.ext.katex.KatexInlineMath;
import net.gsantner.markor.util.flexmark.ext.katex.KatexDisplayMath;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.html.Attribute;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.HashSet;
import java.util.Set;

public class KatexNodeRenderer implements NodeRenderer
{
    final KatexOptions options;
    private final ReferenceRepository referenceRepository;
    private final boolean recheckUndefinedReferences;

    public KatexNodeRenderer(DataHolder options) {
        this.options = new KatexOptions(options);
        this.referenceRepository = options.get(Parser.REFERENCES);
        this.recheckUndefinedReferences = HtmlRenderer.RECHECK_UNDEFINED_REFERENCES.getFrom(options);
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
        // @formatter:off
        set.add(new NodeRenderingHandler<KatexInlineMath>(KatexInlineMath.class, new CustomNodeRenderer<KatexInlineMath>() { @Override public void render(KatexInlineMath node, NodeRendererContext context, HtmlWriter html) { KatexNodeRenderer.this.render(node, context, html); } }));
        set.add(new NodeRenderingHandler<KatexDisplayMath>(KatexDisplayMath.class, new CustomNodeRenderer<KatexDisplayMath>() { @Override public void render(KatexDisplayMath node, NodeRendererContext context, HtmlWriter html) { KatexNodeRenderer.this.render(node, context, html); } }));
        // @formatter:on
        return set;
    }

    private void render(final KatexInlineMath node, final NodeRendererContext context, final HtmlWriter html) {
        html.withAttr().attr(Attribute.CLASS_ATTR, options.inlineMathClass).withAttr().tag("span");
        html.text(node.getText());
        html.tag("/span");
    }

    private void render(final KatexDisplayMath node, final NodeRendererContext context, final HtmlWriter html) {
        html.withAttr().attr(Attribute.CLASS_ATTR, options.inlineMathClass).withAttr().tag("div");
        html.text(node.getText());
        html.tag("/div");
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer create(final DataHolder options) {
            return new KatexNodeRenderer(options);
        }
    }
}
