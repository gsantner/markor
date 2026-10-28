package net.gsantner.markor.format.markdown.html2md;

import com.vladsch.flexmark.html2md.converter.HtmlMarkdownWriter;
import com.vladsch.flexmark.html2md.converter.HtmlNodeConverterContext;
import com.vladsch.flexmark.html2md.converter.HtmlNodeRenderer;
import com.vladsch.flexmark.html2md.converter.HtmlNodeRendererFactory;
import com.vladsch.flexmark.html2md.converter.HtmlNodeRendererHandler;
import com.vladsch.flexmark.util.data.DataHolder;

import org.jsoup.nodes.Element;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CustomHtmlNodeConverter implements HtmlNodeRenderer {
    public CustomHtmlNodeConverter(DataHolder options) {
    }

    @Override
    public Set<HtmlNodeRendererHandler<?>> getHtmlNodeRendererHandlers() {
        return new HashSet<>(Collections.singletonList(
                new HtmlNodeRendererHandler<>("kbd", Element.class, this::processKbd)
        ));
    }

    private void processKbd(Element node, HtmlNodeConverterContext context, HtmlMarkdownWriter out) {
        out.append("<<");
        context.renderChildren(node, false, null);
        out.append(">>");
    }

    static class Factory implements HtmlNodeRendererFactory {
        @Override
        public HtmlNodeRenderer apply(DataHolder options) {
            return new CustomHtmlNodeConverter(options);
        }
    }
}
