package net.gsantner.markor.format.markdown.html2md;

import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.html2md.converter.HtmlLinkResolver;
import com.vladsch.flexmark.html2md.converter.HtmlLinkResolverFactory;
import com.vladsch.flexmark.html2md.converter.HtmlNodeConverterContext;

import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Node;

import java.util.Set;

public class CustomLinkResolver implements HtmlLinkResolver {
    public CustomLinkResolver(HtmlNodeConverterContext context) {
    }

    @Override
    public ResolvedLink resolveLink(Node node, HtmlNodeConverterContext context, ResolvedLink link) {
        // Convert all links from http:// to https://
        if (link.getUrl().startsWith("http:")) {
            return link.withUrl("https:" + link.getUrl().substring("http:".length()));
        }
        return link;
    }

    static class Factory implements HtmlLinkResolverFactory {
        @Nullable
        @Override
        public Set<Class<?>> getAfterDependents() {
            return null;
        }

        @Nullable
        @Override
        public Set<Class<?>> getBeforeDependents() {
            return null;
        }

        @Override
        public boolean affectsGlobalScope() {
            return false;
        }

        @Override
        public HtmlLinkResolver apply(HtmlNodeConverterContext context) {
            return new CustomLinkResolver(context);
        }
    }
}
