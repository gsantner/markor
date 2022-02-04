package net.gsantner.markor.katex.internal;

import net.gsantner.markor.katex.KatexExtension;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSetter;

import java.util.HashSet;

public class KatexOptions implements MutableDataSetter {
    public final boolean inlineMathParser;
    public final boolean displayMathParser;
    public final String inlineMathClass;
    public final String displayMathClass;
    public final String blockInfoDelimiters;

    public KatexOptions(DataHolder options) {
        inlineMathParser = KatexExtension.INLINE_MATH_PARSER.getFrom(options);
        inlineMathClass = KatexExtension.INLINE_MATH_CLASS.getFrom(options);
        displayMathParser = KatexExtension.DISPLAY_MATH_PARSER.getFrom(options);
        displayMathClass = KatexExtension.DISPLAY_MATH_CLASS.getFrom(options);
        blockInfoDelimiters = KatexExtension.BLOCK_INFO_DELIMITERS.getFrom(options);
    }

    @Override
    public MutableDataHolder setIn(final MutableDataHolder dataHolder) {
        dataHolder.set(KatexExtension.INLINE_MATH_PARSER, inlineMathParser);
        dataHolder.set(KatexExtension.INLINE_MATH_CLASS, inlineMathClass);
        dataHolder.set(KatexExtension.DISPLAY_MATH_PARSER, displayMathParser);
        dataHolder.set(KatexExtension.DISPLAY_MATH_CLASS, displayMathClass);
        dataHolder.set(KatexExtension.BLOCK_INFO_DELIMITERS, blockInfoDelimiters);
        return dataHolder;
    }
}
