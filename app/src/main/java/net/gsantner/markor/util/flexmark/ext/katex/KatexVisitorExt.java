package net.gsantner.markor.util.flexmark.ext.katex;

import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.ast.Visitor;

public class KatexVisitorExt {
    public static <V extends KatexVisitor> VisitHandler<?>[] VISIT_HANDLERS(final V visitor) {
        return new VisitHandler<?>[] {
// @formatter:off
                new VisitHandler<KatexInlineMath>(KatexInlineMath.class, new Visitor<KatexInlineMath>() { @Override public void visit(KatexInlineMath node) { visitor.visit(node); } }),
                new VisitHandler<KatexDisplayMath>(KatexDisplayMath.class, new Visitor<KatexDisplayMath>() { @Override public void visit(KatexDisplayMath node) { visitor.visit(node); } }),
                new VisitHandler<KatexAltInlineMath>(KatexAltInlineMath.class, new Visitor<KatexAltInlineMath>() { @Override public void visit(KatexAltInlineMath node) { visitor.visit(node); } }),
                new VisitHandler<KatexAltDisplayMath>(KatexAltDisplayMath.class, new Visitor<KatexAltDisplayMath>() { @Override public void visit(KatexAltDisplayMath node) { visitor.visit(node); } }),
 // @formatter:on
        };
    }
}
