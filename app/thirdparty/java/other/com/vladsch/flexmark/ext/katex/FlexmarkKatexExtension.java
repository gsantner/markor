/*##############################################################################
 *
 * Copyright (c) 2015-2016, Atlassian Pty Ltd
 * All rights reserved.
 *
 * Copyright (c) 2016-2018, Vladimir Schneider,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *#############################################################################*/

package other.com.vladsch.flexmark.ext.katex;

import com.vladsch.flexmark.ast.DelimitedNode;
import com.vladsch.flexmark.ast.util.ReferenceRepository;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.ast.Visitor;
import com.vladsch.flexmark.util.builder.Extension;
import com.vladsch.flexmark.util.html.Attribute;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSetter;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* This extension is based on Flexmark's GitLab Flavoured Markdown Extension[1].
 * The sources [2] were stripped of everything not relevant to math parsing.
 *
 * The extension implements parsers for inline math mode, `$...$`, and for display
 * math mode, `$$...$$`. Both are based on `internal/GitLabInlineMathParser.java`,
 * i.e. both are InlineParsers, not BlockParsers.
 *
 * [1]: https://github.com/vsch/flexmark-java/wiki/Extensions#gitlab-flavoured-markdown
 * [2]: https://github.com/vsch/flexmark-java/tree/0.42.14/flexmark-ext-gitlab/src/main/java/com/vladsch/flexmark/ext/gitlab
 */
public class FlexmarkKatexExtension {

    /**
     * Extension for KaTeX
     */
    public static class KatexExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
        public static final DataKey<Boolean> INLINE_MATH_PARSER = new DataKey<>("INLINE_MATH_PARSER", true);
        public static final DataKey<Boolean> DISPLAY_MATH_PARSER = new DataKey<>("DISPLAY_MATH_PARSER", true);

        public static final DataKey<String> INLINE_MATH_CLASS = new DataKey<>("INLINE_MATH_CLASS", "katex");
        public static final DataKey<String> DISPLAY_MATH_CLASS = new DataKey<>("DISPLAY_MATH_CLASS", "katex");

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
        public void extend(Parser.Builder parserBuilder) {
            FlexmarkKatexExtensionInternal.KatexOptions options = new FlexmarkKatexExtensionInternal.KatexOptions(parserBuilder);
            if (options.inlineMathParser) {
                parserBuilder.customInlineParserExtensionFactory(new FlexmarkKatexExtensionInternal.KatexInlineMathParser.Factory());
            }
            if (options.displayMathParser) {
                parserBuilder.customInlineParserExtensionFactory(new FlexmarkKatexExtensionInternal.KatexDisplayMathParser.Factory());
            }
        }

        @Override
        public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
            if (rendererBuilder.isRendererType("HTML")) {
                rendererBuilder.nodeRendererFactory(new FlexmarkKatexExtensionInternal.KatexNodeRenderer.Factory());
            } else if (rendererBuilder.isRendererType("JIRA")) {
                //rendererBuilder.nodeRendererFactory(new KatexJiraRenderer.Factory());
            }
        }
    }

    public static interface KatexVisitor {
        void visit(final KatexInlineMath node);

        void visit(final KatexDisplayMath node);
    }

    public static class KatexDisplayMath extends Node implements DelimitedNode {
        protected BasedSequence openingMarker = BasedSequence.NULL;
        protected BasedSequence text = BasedSequence.NULL;
        protected BasedSequence closingMarker = BasedSequence.NULL;

        public KatexDisplayMath() {
        }

        public KatexDisplayMath(BasedSequence chars) {
            super(chars);
        }

        public KatexDisplayMath(BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
            super(openingMarker.baseSubSequence(openingMarker.getStartOffset(), closingMarker.getEndOffset()));
            this.openingMarker = openingMarker;
            this.text = text;
            this.closingMarker = closingMarker;
        }

        @Override
        public BasedSequence[] getSegments() {
            //return EMPTY_SEGMENTS;
            return new BasedSequence[]{openingMarker, text, closingMarker};
        }

        @Override
        public void getAstExtra(StringBuilder out) {
            delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
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

    public static class KatexInlineMath extends Node implements DelimitedNode {
        protected BasedSequence openingMarker = BasedSequence.NULL;
        protected BasedSequence text = BasedSequence.NULL;
        protected BasedSequence closingMarker = BasedSequence.NULL;

        public KatexInlineMath() {
        }

        public KatexInlineMath(BasedSequence chars) {
            super(chars);
        }

        public KatexInlineMath(BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
            super(openingMarker.baseSubSequence(openingMarker.getStartOffset(), closingMarker.getEndOffset()));
            this.openingMarker = openingMarker;
            this.text = text;
            this.closingMarker = closingMarker;
        }

        @Override
        public BasedSequence[] getSegments() {
            //return EMPTY_SEGMENTS;
            return new BasedSequence[]{openingMarker, text, closingMarker};
        }

        @Override
        public void getAstExtra(StringBuilder out) {
            delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
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

    public static class KatexVisitorExt {
        public static <V extends KatexVisitor> VisitHandler<?>[] VISIT_HANDLERS(final V visitor) {
            return new VisitHandler<?>[]{
                    // @formatter:off
                    new VisitHandler<KatexInlineMath>(KatexInlineMath.class, new Visitor<KatexInlineMath>() {
                        @Override
                        public void visit(KatexInlineMath node) {
                            visitor.visit(node);
                        }
                    }),
                    new VisitHandler<KatexDisplayMath>(KatexDisplayMath.class, new Visitor<KatexDisplayMath>() {
                        @Override
                        public void visit(KatexDisplayMath node) {
                            visitor.visit(node);
                        }
                    }),
                    // @formatter:on
            };
        }
    }

    public static class FlexmarkKatexExtensionInternal {
        public static class KatexDisplayMathParser implements InlineParserExtension {
            private final KatexOptions options;
            Pattern MATH_PATTERN = Pattern.compile("\\$\\$((?:.|\n)+?)\\$\\$");

            public KatexDisplayMathParser(final InlineParser inlineParser) {
                options = new KatexOptions(inlineParser.getDocument());
            }

            @Override
            public void finalizeDocument(final InlineParser inlineParser) {
            }

            @Override
            public void finalizeBlock(final InlineParser inlineParser) {
            }

            @Override
            public boolean parse(final InlineParser inlineParser) {
                if (inlineParser.peek(1) == '$') {
                    BasedSequence input = inlineParser.getInput();
                    Matcher matcher = inlineParser.matcher(MATH_PATTERN);
                    if (matcher != null) {
                        inlineParser.flushTextNode();

                        BasedSequence mathOpen = input.subSequence(matcher.start(), matcher.start(1));
                        BasedSequence mathClosed = input.subSequence(matcher.end(1), matcher.end());
                        KatexDisplayMath displayMath = new KatexDisplayMath(mathOpen, mathOpen.baseSubSequence(mathOpen.getEndOffset(), mathClosed.getStartOffset()), mathClosed);
                        inlineParser.getBlock().appendChild(displayMath);
                        return true;
                    }
                }
                return false;
            }

            public static class Factory implements InlineParserExtensionFactory {
                @Override
                public Set<Class<? extends InlineParserExtensionFactory>> getAfterDependents() {
                    return null;
                }

                @Override
                public CharSequence getCharacters() {
                    return "$";
                }

                @Override
                public Set<Class<? extends InlineParserExtensionFactory>> getBeforeDependents() {
                    return null;
                }

                @Override
                public InlineParserExtension create(final InlineParser inlineParser) {
                    return new KatexDisplayMathParser(inlineParser);
                }

                @Override
                public boolean affectsGlobalScope() {
                    return false;
                }
            }
        }

        public static class KatexInlineMathParser implements InlineParserExtension {
            private final KatexOptions options;
            Pattern MATH_PATTERN = Pattern.compile("(?<!\\$)\\$((?:.|\n)+?)\\$(?!\\$)");

            public KatexInlineMathParser(final InlineParser inlineParser) {
                options = new KatexOptions(inlineParser.getDocument());
            }

            @Override
            public void finalizeDocument(final InlineParser inlineParser) {
            }

            @Override
            public void finalizeBlock(final InlineParser inlineParser) {
            }

            @Override
            public boolean parse(final InlineParser inlineParser) {
                if (inlineParser.peek(1) != '$') {
                    BasedSequence input = inlineParser.getInput();
                    Matcher matcher = inlineParser.matcher(MATH_PATTERN);
                    if (matcher != null) {
                        inlineParser.flushTextNode();

                        BasedSequence mathOpen = input.subSequence(matcher.start(), matcher.start(1));
                        BasedSequence mathClosed = input.subSequence(matcher.end(1), matcher.end());
                        KatexInlineMath inlineMath = new KatexInlineMath(mathOpen, mathOpen.baseSubSequence(mathOpen.getEndOffset(), mathClosed.getStartOffset()), mathClosed);
                        inlineParser.getBlock().appendChild(inlineMath);
                        return true;
                    }
                }
                return false;
            }

            public static class Factory implements InlineParserExtensionFactory {
                @Override
                public Set<Class<? extends InlineParserExtensionFactory>> getAfterDependents() {
                    return null;
                }

                @Override
                public CharSequence getCharacters() {
                    return "$";
                }

                @Override
                public Set<Class<? extends InlineParserExtensionFactory>> getBeforeDependents() {
                    return null;
                }

                @Override
                public InlineParserExtension create(final InlineParser inlineParser) {
                    return new KatexInlineMathParser(inlineParser);
                }

                @Override
                public boolean affectsGlobalScope() {
                    return false;
                }
            }
        }

        public static class KatexNodeRenderer implements NodeRenderer {
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
                set.add(new NodeRenderingHandler<KatexInlineMath>(KatexInlineMath.class, new CustomNodeRenderer<KatexInlineMath>() {
                    @Override
                    public void render(KatexInlineMath node, NodeRendererContext context, HtmlWriter html) {
                        KatexNodeRenderer.this.render(node, context, html);
                    }
                }));
                set.add(new NodeRenderingHandler<KatexDisplayMath>(KatexDisplayMath.class, new CustomNodeRenderer<KatexDisplayMath>() {
                    @Override
                    public void render(KatexDisplayMath node, NodeRendererContext context, HtmlWriter html) {
                        KatexNodeRenderer.this.render(node, context, html);
                    }
                }));
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

        public static class KatexOptions implements MutableDataSetter {
            public final boolean inlineMathParser;
            public final boolean displayMathParser;
            public final String inlineMathClass;
            public final String displayMathClass;

            public KatexOptions(DataHolder options) {
                inlineMathParser = KatexExtension.INLINE_MATH_PARSER.getFrom(options);
                inlineMathClass = KatexExtension.INLINE_MATH_CLASS.getFrom(options);
                displayMathParser = KatexExtension.DISPLAY_MATH_PARSER.getFrom(options);
                displayMathClass = KatexExtension.DISPLAY_MATH_CLASS.getFrom(options);
            }

            @Override
            public MutableDataHolder setIn(final MutableDataHolder dataHolder) {
                dataHolder.set(KatexExtension.INLINE_MATH_PARSER, inlineMathParser);
                dataHolder.set(KatexExtension.INLINE_MATH_CLASS, inlineMathClass);
                dataHolder.set(KatexExtension.DISPLAY_MATH_PARSER, displayMathParser);
                dataHolder.set(KatexExtension.DISPLAY_MATH_CLASS, displayMathClass);
                return dataHolder;
            }
        }
    }
}
