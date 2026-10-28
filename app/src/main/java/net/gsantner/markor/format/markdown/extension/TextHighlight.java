package net.gsantner.markor.format.markdown.extension;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.core.delimiter.Delimiter;
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor;
import com.vladsch.flexmark.parser.delimiter.DelimiterRun;
import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Add HTML rendering support for the custom text token {@code '=='},
 * which will be rendered as HTML {@code mark} tag,
 * to implement the extended syntax ==text== used to highlight/mark the text.
 */
public class TextHighlight {

    static class HighlightNode extends Node implements DelimitedNode {

        private BasedSequence openingMarker = BasedSequence.NULL;
        private BasedSequence text = BasedSequence.NULL;
        private BasedSequence closingMarker = BasedSequence.NULL;

        @NotNull
        @Override
        public BasedSequence[] getSegments() {
            return new BasedSequence[]{openingMarker, text, closingMarker};
        }

        @Override
        public void getAstExtra(@NotNull StringBuilder out) {
            Node.delimitedSegmentSpan(out, openingMarker, text, closingMarker, "text");
        }

        public HighlightNode() {
        }

        public HighlightNode(BasedSequence chars) {
            super(chars);
        }

        public HighlightNode(BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
            super(openingMarker.baseSubSequence(openingMarker.getStartOffset(), closingMarker.getEndOffset()));
            this.openingMarker = openingMarker;
            this.text = text;
            this.closingMarker = closingMarker;
        }

        public BasedSequence getOpeningMarker() {
            return openingMarker;
        }

        public void setOpeningMarker(BasedSequence openingMarker) {
            this.openingMarker = openingMarker;
        }

        public BasedSequence getText() {
            return text;
        }

        public void setText(BasedSequence text) {
            this.text = text;
        }

        public BasedSequence getClosingMarker() {
            return closingMarker;
        }

        public void setClosingMarker(BasedSequence closingMarker) {
            this.closingMarker = closingMarker;
        }
    }

    static class TextHighlightDelimiterProcessor implements DelimiterProcessor {

        @Override
        public char getOpeningCharacter() {
            return '=';
        }

        @Override
        public char getClosingCharacter() {
            return '=';
        }

        @Override
        public int getMinLength() {
            return 2;
        }

        @Override
        public int getDelimiterUse(DelimiterRun opener, DelimiterRun closer) {
            if (opener.length() >= 2 && closer.length() >= 2) {
                return 2;
            } else {
                return 0;
            }
        }

        @Override
        public boolean canBeOpener(String before, String after, boolean leftFlanking, boolean rightFlanking, boolean beforeIsPunctuation, boolean afterIsPunctuation, boolean beforeIsWhitespace, boolean afterIsWhiteSpace) {
            return leftFlanking;
        }

        @Override
        public boolean canBeCloser(String before, String after, boolean leftFlanking, boolean rightFlanking, boolean beforeIsPunctuation, boolean afterIsPunctuation, boolean beforeIsWhitespace, boolean afterIsWhiteSpace) {
            return rightFlanking;
        }

        @Override
        public Node unmatchedDelimiterNode(InlineParser inlineParser, DelimiterRun delimiter) {
            return null;
        }

        @Override
        public boolean skipNonOpenerCloser() {
            return false;
        }

        @Override
        public void process(Delimiter opener, Delimiter closer, int delimitersUsed) {
            HighlightNode highlightNode = new HighlightNode(opener.getTailChars(delimitersUsed), BasedSequence.NULL,
                    closer.getLeadChars(delimitersUsed));
            opener.moveNodesBetweenDelimitersTo(highlightNode, closer);
        }
    }

    static class HighlightNodeRenderer implements NodeRenderer {

        public static class Factory implements NodeRendererFactory {

            @NotNull
            @Override
            public NodeRenderer apply(@NotNull DataHolder options) {
                return new HighlightNodeRenderer();
            }
        }

        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            Set<NodeRenderingHandler<?>> set = new HashSet<>();
            set.add(new NodeRenderingHandler<>(HighlightNode.class, (node, context, html) -> {
                if (context.getHtmlOptions().sourcePositionParagraphLines) {
                    html.withAttr().tag("mark");
                } else {
                    html.srcPos(node.getText()).withAttr().tag("mark");
                }
                context.renderChildren(node);
                html.tag("/mark");
            }));

            return set;
        }
    }

    /**
     * A custom Flexmark Java extension to support the usage of {@code '=='} as a Markdown syntax
     * element/token for rendering highlighted text with the HTML {@code mark} tag.
     */
    public static class TextHighlightExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

        private TextHighlightExtension() {
        }

        @Override
        public void rendererOptions(@NotNull MutableDataHolder options) {
        }

        @Override
        public void parserOptions(MutableDataHolder options) {
        }

        @Override
        public void extend(Parser.Builder parserBuilder) {
            parserBuilder.customDelimiterProcessor(new TextHighlightDelimiterProcessor());
        }

        @Override
        public void extend(@NotNull HtmlRenderer.Builder htmlRendererBuilder, @NotNull String rendererType) {
            htmlRendererBuilder.nodeRendererFactory(new HighlightNodeRenderer.Factory());
        }

        public static TextHighlightExtension create() {
            return new TextHighlightExtension();
        }
    }

    public static void test() {
        DataHolder OPTIONS = new MutableDataSet()
                .set(Parser.EXTENSIONS, Collections.singletonList(TextHighlightExtension.create()))
                .toImmutable();
        Parser PARSER = Parser.builder(OPTIONS).build();
        HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

        String original = "**bold** _italic_ ==highlight== *==bold and highlight==*";
        Node document = PARSER.parse(original);
        String html = RENDERER.render(document);
        System.out.println(html);
        // <p><strong>bold</strong> <em>italic</em> <mark>highlight</mark> <em><mark>bold and highlight</mark></em></p>
    }
}
