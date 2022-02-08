package net.gsantner.markor.util.flexmark.ext.katex.internal;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ast.util.ReferenceRepository;
import com.vladsch.flexmark.ast.util.TextCollectingVisitor;
import net.gsantner.markor.util.flexmark.ext.katex.KatexInlineMath;
import net.gsantner.markor.util.flexmark.ext.katex.KatexDisplayMath;
import net.gsantner.markor.util.flexmark.ext.katex.KatexAltInlineMath;
import net.gsantner.markor.util.flexmark.ext.katex.KatexAltDisplayMath;
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
        // , PhasedNodeRenderer
{
    final KatexOptions options;
    private final boolean codeContentBlock;
    private final ReferenceRepository referenceRepository;
    private final boolean recheckUndefinedReferences;

    public KatexNodeRenderer(DataHolder options) {
        this.options = new KatexOptions(options);
        this.codeContentBlock = Parser.FENCED_CODE_CONTENT_BLOCK.getFrom(options);
        this.referenceRepository = options.get(Parser.REFERENCES);
        this.recheckUndefinedReferences = HtmlRenderer.RECHECK_UNDEFINED_REFERENCES.getFrom(options);
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
        // @formatter:off
        set.add(new NodeRenderingHandler<KatexInlineMath>(KatexInlineMath.class, new CustomNodeRenderer<KatexInlineMath>() { @Override public void render(KatexInlineMath node, NodeRendererContext context, HtmlWriter html) { KatexNodeRenderer.this.render(node, context, html); } }));
        set.add(new NodeRenderingHandler<KatexDisplayMath>(KatexDisplayMath.class, new CustomNodeRenderer<KatexDisplayMath>() { @Override public void render(KatexDisplayMath node, NodeRendererContext context, HtmlWriter html) { KatexNodeRenderer.this.render(node, context, html); } }));
        set.add(new NodeRenderingHandler<KatexAltInlineMath>(KatexAltInlineMath.class, new CustomNodeRenderer<KatexAltInlineMath>() { @Override public void render(KatexAltInlineMath node, NodeRendererContext context, HtmlWriter html) { KatexNodeRenderer.this.render(node, context, html); } }));
        set.add(new NodeRenderingHandler<KatexAltDisplayMath>(KatexAltDisplayMath.class, new CustomNodeRenderer<KatexAltDisplayMath>() { @Override public void render(KatexAltDisplayMath node, NodeRendererContext context, HtmlWriter html) { KatexNodeRenderer.this.render(node, context, html); } }));
        // @formatter:on
        return set;
    }

    private void render(final KatexInlineMath node, final NodeRendererContext context, final HtmlWriter html) {
        html.withAttr().attr(Attribute.CLASS_ATTR, options.inlineMathClass).withAttr().tag("span");
        html.text(node.getText());
        html.tag("/span");
    }

    private void render(final KatexAltInlineMath node, final NodeRendererContext context, final HtmlWriter html) {
        html.withAttr().attr(Attribute.CLASS_ATTR, options.inlineMathClass).withAttr().tag("span");
        html.text(node.getText());
        html.tag("/span");
    }

    private void render(final KatexDisplayMath node, final NodeRendererContext context, final HtmlWriter html) {
        html.withAttr().attr(Attribute.CLASS_ATTR, options.inlineMathClass).withAttr().tag("div");
        html.text(node.getText());
        html.tag("/div");
    }

    private void render(final KatexAltDisplayMath node, final NodeRendererContext context, final HtmlWriter html) {
        html.withAttr().attr(Attribute.CLASS_ATTR, options.inlineMathClass).withAttr().tag("div");
        html.text(node.getText());
        html.tag("/div");
    }

    private void render(final FencedCodeBlock node, final NodeRendererContext context, HtmlWriter html) {
        final BasedSequence info = node.getInfoDelimitedByAny(options.blockInfoDelimiters);

        context.delegateRender();
    }


    private void render(Image node, NodeRendererContext context, final HtmlWriter html) {
        if (!(context.isDoNotRenderLinks() || CoreNodeRenderer.isSuppressedLinkPrefix(node.getUrl(), context))) {
            final String altText = new TextCollectingVisitor().collectAndGetText(node);
            ResolvedLink resolvedLink = context.resolveLink(LinkType.IMAGE, node.getUrl().unescape(), null, null);
            final String url = resolvedLink.getUrl();

            if (node.getUrlContent().isEmpty()) {
                Attributes attributes = resolvedLink.getNonNullAttributes();

                // need to take attributes for reference definition, then overlay them with ours
                attributes = context.extendRenderingNodeAttributes(node, AttributablePart.NODE, attributes);

            }

            context.delegateRender();
        }
    }

    private void render(ImageRef node, NodeRendererContext context, HtmlWriter html) {
        ResolvedLink resolvedLink = null;
        boolean isSuppressed = false;

        if (!node.isDefined() && recheckUndefinedReferences) {
            if (node.getReferenceNode(referenceRepository) != null) {
                node.setDefined(true);
            }
        }

        Reference reference = null;

        if (node.isDefined()) {
            reference = node.getReferenceNode(referenceRepository);
            String url = reference.getUrl().unescape();
            isSuppressed = CoreNodeRenderer.isSuppressedLinkPrefix(url, context);

            resolvedLink = context.resolveLink(LinkType.IMAGE, url, null, null);
            if (reference.getTitle().isNotNull()) {
                resolvedLink.getNonNullAttributes().replaceValue(Attribute.TITLE_ATTR, reference.getTitle().unescape());
            } else {
                resolvedLink.getNonNullAttributes().remove(Attribute.TITLE_ATTR);
            }
        } else {
            // see if have reference resolver and this is resolved
            String normalizeRef = referenceRepository.normalizeKey(node.getReference());
            resolvedLink = context.resolveLink(LinkType.IMAGE_REF, normalizeRef, null, null);
            if (resolvedLink.getStatus() == LinkStatus.UNKNOWN) {
                resolvedLink = null;
            }
        }

        if (resolvedLink != null) {
            if (!(context.isDoNotRenderLinks() || isSuppressed)) {
                String altText = new TextCollectingVisitor().collectAndGetText(node);
                final String url = resolvedLink.getUrl();
                Attributes attributes = resolvedLink.getNonNullAttributes();
            }
        }

        context.delegateRender();
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer create(final DataHolder options) {
            return new KatexNodeRenderer(options);
        }
    }
}
