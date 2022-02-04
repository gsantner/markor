package net.gsantner.markor.katex.internal;

import net.gsantner.markor.katex.KatexExtension;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSetter;

import java.util.HashSet;

public class KatexOptions implements MutableDataSetter {
    public final boolean inlineMathParser;
    public final boolean renderBlockMath;
    public final String inlineMathClass;
    public final String blockMathClass;
    public final String blockInfoDelimiters;

    public KatexOptions(DataHolder options) {
        inlineMathParser = KatexExtension.INLINE_MATH_PARSER.getFrom(options);
        inlineMathClass = KatexExtension.INLINE_MATH_CLASS.getFrom(options);
        renderBlockMath = KatexExtension.RENDER_BLOCK_MATH.getFrom(options);
        blockMathClass = KatexExtension.BLOCK_MATH_CLASS.getFrom(options);
        blockInfoDelimiters = KatexExtension.BLOCK_INFO_DELIMITERS.getFrom(options);
    }

    @Override
    public MutableDataHolder setIn(final MutableDataHolder dataHolder) {
        dataHolder.set(KatexExtension.INLINE_MATH_PARSER, inlineMathParser);
        dataHolder.set(KatexExtension.INLINE_MATH_CLASS, inlineMathClass);
        dataHolder.set(KatexExtension.RENDER_BLOCK_MATH, renderBlockMath);
        dataHolder.set(KatexExtension.BLOCK_MATH_CLASS, blockMathClass);
        dataHolder.set(KatexExtension.BLOCK_INFO_DELIMITERS, blockInfoDelimiters);
        return dataHolder;
    }
}
