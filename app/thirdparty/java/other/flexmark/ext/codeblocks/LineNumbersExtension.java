package other.flexmark.ext.codeblocks;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.options.MutableDataHolder;

/**
 * flexmark-java extension for prism syntax highlighter.
 */
public class LineNumbersExtension implements HtmlRenderer.HtmlRendererExtension {
    @Override
    public void rendererOptions(final MutableDataHolder options) {
    }

    @Override
    public void extend(final HtmlRenderer.Builder rendererBuilder, final String rendererType) {
        rendererBuilder.attributeProviderFactory(LineNumbersAttributeProvider.Factory());
    }

    public static LineNumbersExtension create() {
        return new LineNumbersExtension();
    }
}