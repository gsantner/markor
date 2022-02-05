package net.gsantner.markor.util.flexmark.ext.katex.internal;

import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.BlockContent;
import com.vladsch.flexmark.util.ast.Node;
import net.gsantner.markor.util.flexmark.ext.katex.KatexBlockQuote;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.block.*;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KatexBlockQuoteParser extends AbstractBlockParser {
    static Pattern GIT_LAB_BLOCK_START = Pattern.compile(">>>(\\s*$)");
    static Pattern GIT_LAB_BLOCK_END = Pattern.compile("<<<(\\s*$)");

    private final KatexBlockQuote block = new KatexBlockQuote();
    private BlockContent content = new BlockContent();
    private final KatexOptions options;
    private boolean hadClose = false;

    KatexBlockQuoteParser(DataHolder options, BasedSequence openMarker, BasedSequence openTrailing) {
        this.options = new KatexOptions(options);
        this.block.setOpeningMarker(openMarker);
        this.block.setOpeningTrailing(openTrailing);
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        if (hadClose) {
            return BlockContinue.none();
        }

        final int index = state.getIndex();

        BasedSequence line = state.getLineWithEOL();
        final Matcher matcher = GIT_LAB_BLOCK_END.matcher(line.subSequence(index));
        if (!matcher.matches()) {
            return BlockContinue.atIndex(index);
        } else {
            // if have open Katex block quote last child then let them handle it
            Node lastChild = block.getLastChild();
            if (lastChild instanceof KatexBlockQuote) {
                final BlockParser parser = state.getActiveBlockParser((Block) lastChild);
                if (parser instanceof KatexBlockQuoteParser && !((KatexBlockQuoteParser) parser).hadClose) {
                    // let the child handle it
                    return BlockContinue.atIndex(index);
                }
            }
            hadClose = true;
            block.setClosingMarker(state.getLine().subSequence(index, index + 3));
            block.setClosingTrailing(state.getLineWithEOL().subSequence(matcher.start(1), matcher.end(1)));
            return BlockContinue.atIndex(state.getLineEndIndex());
        }
    }

    @Override
    public void addLine(ParserState state, BasedSequence line) {
        content.add(line, state.getIndent());
    }

    @Override
    public void closeBlock(ParserState state) {
        block.setContent(content);
        block.setCharsFromContent();
        content = null;
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public boolean canContain(final ParserState state, final BlockParser blockParser, final Block block) {
        return true; //options.nestedBlockQuotes || !(blockParser instanceof KatexBlockQuoteParser);
    }

    @Override
    public void parseInlines(InlineParser inlineParser) {
    }

    public static class Factory implements CustomBlockParserFactory {
        @Override
        public Set<Class<? extends CustomBlockParserFactory>> getAfterDependents() {
            return null;
            //return new HashSet<>(Arrays.asList(
            //        BlockQuoteParser.Factory.class,
            //        HeadingParser.Factory.class,
            //        FencedCodeBlockParser.Factory.class,
            //        HtmlBlockParser.Factory.class,
            //        ThematicBreakParser.Factory.class,
            //        ListBlockParser.Factory.class,
            //        IndentedCodeBlockParser.Factory.class
            //));
        }

        @Override
        public Set<Class<? extends CustomBlockParserFactory>> getBeforeDependents() {
            return null;
            //return new HashSet<>(Arrays.asList(
            //        BlockQuoteParser.Factory.class,
            //        HeadingParser.Factory.class,
            //        FencedCodeBlockParser.Factory.class,
            //        HtmlBlockParser.Factory.class,
            //        ThematicBreakParser.Factory.class,
            //        ListBlockParser.Factory.class,
            //        IndentedCodeBlockParser.Factory.class
            //));
        }

        @Override
        public boolean affectsGlobalScope() {
            return false;
        }

        @Override
        public BlockParserFactory create(DataHolder options) {
            return new BlockFactory(options);
        }
    }

    private static class BlockFactory extends AbstractBlockParserFactory {
        private final KatexOptions options;

        BlockFactory(DataHolder options) {
            super(options);
            this.options = new KatexOptions(options);
        }

        boolean haveBlockQuoteParser(ParserState state) {
            final List<BlockParser> parsers = state.getActiveBlockParsers();
            int i = parsers.size();
            while (i-- > 0) {
                if (parsers.get(i) instanceof KatexBlockQuoteParser) return true;
            }
            return false;
        }

        @Override
        public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
            if (!haveBlockQuoteParser(state)) {
                BasedSequence line = state.getLineWithEOL();
                final Matcher matcher = GIT_LAB_BLOCK_START.matcher(line);
                if (matcher.matches()) {
                    return BlockStart.of(new KatexBlockQuoteParser(state.getProperties(), line.subSequence(0, 3), line.subSequence(matcher.start(1), matcher.end(1))))
                            .atIndex(state.getLineEndIndex())
                            //.replaceActiveBlockParser()
                            ;
                }
            }
            return BlockStart.none();
        }
    }
}
