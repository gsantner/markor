package net.gsantner.markor.format.zimwiki;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZimWikiLinkResolver {
    private File _notebookRootDir;
    private File _currentPage;

    private String zimPath;
    private String resolvedLink;
    private String linkDescription;

    public enum Patterns {
        LINK(Pattern.compile("\\[\\[(?!\\[)((.+?)(\\|(.+?))?\\]*)]\\]")),

        SUBPAGE_PATH(Pattern.compile("\\+(.*)")),
        TOPLEVEL_PATH(Pattern.compile(":(.*)"));
        // TODO: link within path from root to current page
        // TODO: external file links
        // TODO: interwiki links

        public final Pattern pattern;

        Patterns(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    private ZimWikiLinkResolver(File notebookRootDir, File currentPage) {
        _notebookRootDir = notebookRootDir;
        _currentPage = currentPage;
    }

    public static ZimWikiLinkResolver resolve(String zimLink, File notebookRootDir, File currentPage) {
        return new ZimWikiLinkResolver(notebookRootDir, currentPage).resolve(zimLink);
    }

    private ZimWikiLinkResolver resolve(String zimLink) {
        Matcher m = Patterns.LINK.pattern.matcher(zimLink);
        if (m.matches()) {
            zimPath = m.group(2);
            linkDescription = m.group(4);
        }
        resolvedLink = resolveZimPath(zimPath);
        return this;
    }

    private String resolveZimPath(String zimPath) {
        Matcher subpageMatcher = Patterns.SUBPAGE_PATH.pattern.matcher(zimPath);
        if (subpageMatcher.matches()) {
            String folderForSubpagesOfCurrentPage = _currentPage.getPath().replace(".txt", "");
            String zimPagePath = subpageMatcher.group(1);
            return FilenameUtils.concat(folderForSubpagesOfCurrentPage, zimPagePathToRelativeFilePath(zimPagePath));
        }

        Matcher toplevelMatcher = Patterns.TOPLEVEL_PATH.pattern.matcher(zimPath);
        if (toplevelMatcher.matches()) {
            String zimPagePath = toplevelMatcher.group(1);
            return FilenameUtils.concat(_notebookRootDir.getPath(), zimPagePathToRelativeFilePath(zimPagePath));
        }

        return zimPath; // just return the original path in case the link cannot be resolved (might be a URL)
    }

    /**
     * @param zimPagePath the page path (separator: double colons)
     * @return
     */
    private String zimPagePathToRelativeFilePath(String zimPagePath) {
        String result = zimPagePath.replaceAll(":", File.separator);
        result = result.replaceAll(" ", "_");
        result = result + ".txt";
        return result;
    }

    public String getResolvedLink() {
        return resolvedLink;
    }

    public String getLinkDescription() {
        return linkDescription;
    }
}
