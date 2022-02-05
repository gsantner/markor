package net.gsantner.markor.util.flexmark.ext.katex;

public interface KatexVisitor {
    void visit(final KatexInlineMath node);
    void visit(final KatexDisplayMath node);
}
