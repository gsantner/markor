package net.gsantner.markor.katex.internal;

import net.gsantner.markor.katex.KatexInlineMath;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.InlineParserExtension;
import com.vladsch.flexmark.parser.InlineParserExtensionFactory;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KatexInlineMathParser implements InlineParserExtension {
    Pattern MATH_PATTERN = Pattern.compile("(?<!\\$)\\$((?:.|\n)+?)\\$(?!\\$)");
    private final KatexOptions options;

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
                KatexInlineMath inlineMath = new KatexInlineMath(mathOpen, mathOpen.baseSubSequence(mathOpen.getEndOffset(),mathClosed.getStartOffset()), mathClosed);
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
