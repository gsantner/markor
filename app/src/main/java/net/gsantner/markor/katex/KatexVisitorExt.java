package net.gsantner.markor.katex;

import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.ast.Visitor;

public class KatexVisitorExt {
    public static <V extends KatexVisitor> VisitHandler<?>[] VISIT_HANDLERS(final V visitor) {
        return new VisitHandler<?>[] {
// @formatter:off
                new VisitHandler<KatexInlineMath>(KatexInlineMath.class, new Visitor<KatexInlineMath>() { @Override public void visit(KatexInlineMath node) { visitor.visit(node); } }),
 // @formatter:on
        };
    }
}
