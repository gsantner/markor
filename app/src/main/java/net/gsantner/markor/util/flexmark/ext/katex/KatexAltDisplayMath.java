package net.gsantner.markor.util.flexmark.ext.katex;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.ast.DelimitedNode;
import com.vladsch.flexmark.util.sequence.BasedSequence;

/**
 * A Ins node
 */
public class KatexAltDisplayMath extends Node implements DelimitedNode {
    protected BasedSequence openingMarker = BasedSequence.NULL;
    protected BasedSequence text = BasedSequence.NULL;
    protected BasedSequence closingMarker = BasedSequence.NULL;

    @Override
    public BasedSequence[] getSegments() {
        //return EMPTY_SEGMENTS;
        return new BasedSequence[] { openingMarker, text, closingMarker };
    }

    @Override
    public void getAstExtra(StringBuilder out) {
        delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
    }

    public KatexAltDisplayMath() {
    }

    public KatexAltDisplayMath(BasedSequence chars) {
        super(chars);
    }

    public KatexAltDisplayMath(BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
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
