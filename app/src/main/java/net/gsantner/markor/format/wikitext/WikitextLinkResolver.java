package net.gsantner.markor.format.wikitext;

import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves wikitext links and converts them to file paths if necessary.
 * The logic follows the specification of wikitext links as stated here: https://zim-wiki.org/manual/Help/Links.html
 * If the link contains an additional description, it is parsed as well.
 */
public class WikitextLinkResolver {
    private File _notebookRootDir;
    private File _currentPage;
    private boolean _shouldDynamicallyDetermineRoot;

    private String _wikitextPath;

    private String _resolvedLink;
    private String _linkDescription;
    private boolean _isWebLink;

    public enum Patterns {
        LINK(Pattern.compile("\\[\\[(?!\\[)((.+?)(\\|(.+?))?\\]*)]\\]")),

        SUBPAGE_PATH(Pattern.compile("\\+(.*)")),
        TOPLEVEL_PATH(Pattern.compile(":(.*)")),
        RELATIVE_PATH(Pattern.compile("[^#/]+")), // no weblinks, no inner page references
        PATH_WITH_INNER_PAGE_REFERENCE(Pattern.compile("(.*)#(.+)")),
        WEBLINK(Pattern.compile("^[a-z]+://.+"));
        // TODO: external file links
        // TODO: interwiki links

        public final Pattern pattern;

        Patterns(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    private WikitextLinkResolver(File notebookRootDir, File currentPage, boolean shouldDynamicallyDetermineRoot) {
        _notebookRootDir = notebookRootDir;
        _currentPage = currentPage;
        _shouldDynamicallyDetermineRoot = shouldDynamicallyDetermineRoot;
    }

    public static WikitextLinkResolver resolve(String wikitextLink, File notebookRootDir, File currentPage, boolean shouldDynamicallyDetermineRoot) {
        return new WikitextLinkResolver(notebookRootDir, currentPage, shouldDynamicallyDetermineRoot).resolve(wikitextLink);
    }

    private WikitextLinkResolver resolve(String wikitextLink) {

        Matcher m = Patterns.LINK.pattern.matcher(wikitextLink);
        if (m.matches()) {
            _wikitextPath = m.group(2);
            _linkDescription = m.group(4);
            _resolvedLink = resolveWikitextPath(_wikitextPath);
        }
        return this;
    }

    private String resolveWikitextPath(String wikitextPath) {
        Matcher webLinkMatcher = Patterns.WEBLINK.pattern.matcher(wikitextPath);
        if (webLinkMatcher.matches()) {
            _isWebLink = true;
            return wikitextPath;
        } else {
            _isWebLink = false;
        }

        // inner page references are not yet supported - for compatibility just get the page
        wikitextPath = stripInnerPageReference(wikitextPath);
        if (GsTextUtils.isNullOrEmpty(wikitextPath)) {
            return null;
        }

        Matcher subpageMatcher = Patterns.SUBPAGE_PATH.pattern.matcher(wikitextPath);
        if (subpageMatcher.matches()) {
            String folderForSubpagesOfCurrentPage = _currentPage.getPath().replace(".txt", "");
            String wikitextPagePath = subpageMatcher.group(1);
            return FilenameUtils.concat(folderForSubpagesOfCurrentPage, wikitextPagePathToRelativeFilePath(wikitextPagePath));
        }

        // the link types below need knowledge of the notebook root dir
        if (_shouldDynamicallyDetermineRoot) {
            _notebookRootDir = findNotebookRootDir(_currentPage);
            if (_notebookRootDir == null) {
                return null;
            }
        }

        Matcher toplevelMatcher = Patterns.TOPLEVEL_PATH.pattern.matcher(wikitextPath);
        if (toplevelMatcher.matches()) {
            String wikitextPagePath = toplevelMatcher.group(1);
            return FilenameUtils.concat(_notebookRootDir.getPath(), wikitextPagePathToRelativeFilePath(wikitextPagePath));
        }

        Matcher relativeMatcher = Patterns.RELATIVE_PATH.pattern.matcher(wikitextPath);
        if (relativeMatcher.matches()) {
            String relativeWikitextPagePath = relativeMatcher.group();
            String relativeLinkToCheck = wikitextPagePathToRelativeFilePath(relativeWikitextPagePath);
            return findFirstPageTraversingUpToRoot(_currentPage, relativeLinkToCheck);
        }

        return wikitextPath; // just return the original path in case the link cannot be resolved (might be a URL)
    }

    private String stripInnerPageReference(String wikitextPath) {
        Matcher pathWithInnerPageReferenceMatcher = Patterns.PATH_WITH_INNER_PAGE_REFERENCE.pattern.matcher(wikitextPath);
        if (pathWithInnerPageReferenceMatcher.matches()) {
            String pagePath = pathWithInnerPageReferenceMatcher.group(1);
            return pagePath;
        }
        return wikitextPath;
    }

    private File findNotebookRootDir(File currentPage) {
        if (currentPage != null && currentPage.exists()) {
            if (GsFileUtils.join(currentPage, "notebook.zim").exists()) {
                return currentPage;
            } else {
                return findNotebookRootDir(currentPage.getParentFile());
            }
        }
        return null;
    }

    private String findFirstPageTraversingUpToRoot(File currentPage, String relativeLinkToCheck) {
        // if the notebook directory is set incorrectly/cannot be reached,
        // dynamic traversal can go up to the root of the filesystem - thus the null-check
        if (currentPage == null || currentPage.equals(_notebookRootDir)) {
            return null;
        }

        File parentFolder = currentPage.getParentFile();
        File candidateFile = GsFileUtils.join(parentFolder, relativeLinkToCheck);
        if (candidateFile.exists()) {
            return candidateFile.toString();
        } else {
            return findFirstPageTraversingUpToRoot(parentFolder, relativeLinkToCheck);
        }
    }

    /**
     * @param wikitextPagePath the page path (separator: double colons)
     * @return
     */
    private String wikitextPagePathToRelativeFilePath(String wikitextPagePath) {
        String result = wikitextPagePath.replaceAll(":", File.separator);
        result = result.replaceAll(" ", "_");
        result = result + ".txt";
        return result;
    }

    public String getWikitextPath() {
        return _wikitextPath;
    }

    public String getResolvedLink() {
        return _resolvedLink;
    }

    public String getLinkDescription() {
        return _linkDescription;
    }

    public boolean isWebLink() {
        return _isWebLink;
    }

    public File getNotebookRootDir() {
        return _notebookRootDir;
    }
}
