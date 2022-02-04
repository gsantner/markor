package net.gsantner.markor.katex;

public interface KatexVisitor {
    void visit(final KatexInlineMath node);
    void visit(final KatexDisplayMath node);
}
