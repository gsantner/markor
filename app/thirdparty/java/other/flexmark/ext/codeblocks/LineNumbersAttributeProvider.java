package other.flexmark.ext.codeblocks;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.AttributeProviderFactory;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.html.Attributes;

import java.util.Objects;

/**
 * AttributeProvider of flexmark-java extension for prism syntax highlighter.
 */
public class LineNumbersAttributeProvider implements AttributeProvider {

    static AttributeProviderFactory Factory() {
        return new IndependentAttributeProviderFactory() {

            @Override
            public AttributeProvider create(LinkResolverContext context) {
                return new LineNumbersAttributeProvider();
            }
        };
    }

    @Override
    public void setAttributes(Node node, AttributablePart part, Attributes attributes) {
        if (node instanceof FencedCodeBlock && part == AttributablePart.NODE) {
            if (Objects.equals(((FencedCodeBlock) node).getInfo(), "mermaid")) {
                return;
            }
            attributes.replaceValue("class", "line-numbers");
        }
    }
}