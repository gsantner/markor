package net.gsantner.markor.format.zimwiki;

import net.gsantner.opoc.util.FileUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves zim links and converts them to file paths if necessary.
 * The logic follows the specification of zim links as stated here: https://zim-wiki.org/manual/Help/Links.html
 * If the link contains an additional description, it is parsed as well.
 */
public class ZimWikiLinkResolver {
    private File _notebookRootDir;
    private File _currentPage;
    private boolean _shouldDynamicallyDetermineRoot;

    private String _zimPath;

    private String _resolvedLink;
    private String _linkDescription;
    private boolean _isWebLink;

    public enum Patterns {
        LINK(Pattern.compile("\\[\\[(?!\\[)((.+?)(\\|(.+?))?\\]*)]\\]")),

        SUBPAGE_PATH(Pattern.compile("\\+(.*)")),
        TOPLEVEL_PATH(Pattern.compile(":(.*)")),
        RELATIVE_PATH(Pattern.compile("[^/]+")), // do not match weblinks
        WEBLINK(Pattern.compile("^[a-z]+://.+"));
        // TODO: external file links
        // TODO: interwiki links

        public final Pattern pattern;

        Patterns(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    private ZimWikiLinkResolver(File notebookRootDir, File currentPage, boolean shouldDynamicallyDetermineRoot) {
        _notebookRootDir = notebookRootDir;
        _currentPage = currentPage;
        _shouldDynamicallyDetermineRoot = shouldDynamicallyDetermineRoot;
    }

    public static ZimWikiLinkResolver resolve(String zimLink, File notebookRootDir, File currentPage, boolean shouldDynamicallyDetermineRoot) {
        return new ZimWikiLinkResolver(notebookRootDir, currentPage, shouldDynamicallyDetermineRoot).resolve(zimLink);
    }

    private ZimWikiLinkResolver resolve(String zimLink) {

        Matcher m = Patterns.LINK.pattern.matcher(zimLink);
        if (m.matches()) {
            _zimPath = m.group(2);
            _linkDescription = m.group(4);
            _resolvedLink = resolveZimPath(_zimPath);
        }
        return this;
    }

    private String resolveZimPath(String zimPath) {
        Matcher webLinkMatcher = Patterns.WEBLINK.pattern.matcher(zimPath);
        if (webLinkMatcher.matches()) {
            _isWebLink = true;
            return zimPath;
        } else {
            _isWebLink = false;
        }

        Matcher subpageMatcher = Patterns.SUBPAGE_PATH.pattern.matcher(zimPath);
        if (subpageMatcher.matches()) {
            String folderForSubpagesOfCurrentPage = _currentPage.getPath().replace(".txt", "");
            String zimPagePath = subpageMatcher.group(1);
            return FilenameUtils.concat(folderForSubpagesOfCurrentPage, zimPagePathToRelativeFilePath(zimPagePath));
        }

        // the link types below need knowledge of the notebook root dir
        if (_shouldDynamicallyDetermineRoot) {
            _notebookRootDir = findNotebookRootDir(_currentPage);
            if (_notebookRootDir==null) {
                return null;
            }
        }

        Matcher toplevelMatcher = Patterns.TOPLEVEL_PATH.pattern.matcher(zimPath);
        if (toplevelMatcher.matches()) {
            String zimPagePath = toplevelMatcher.group(1);
            return FilenameUtils.concat(_notebookRootDir.getPath(), zimPagePathToRelativeFilePath(zimPagePath));
        }

        Matcher relativeMatcher = Patterns.RELATIVE_PATH.pattern.matcher(zimPath);
        if (relativeMatcher.matches()) {
            String relativeZimPagePath = relativeMatcher.group();
            String relativeLinkToCheck = zimPagePathToRelativeFilePath(relativeZimPagePath);
            return findFirstPageTraversingUpToRoot(_currentPage, relativeLinkToCheck);
        }

        return zimPath; // just return the original path in case the link cannot be resolved (might be a URL)
    }

    private File findNotebookRootDir(File currentPage) {
        if (currentPage!= null && currentPage.exists()) {
            if (FileUtils.join(currentPage, "notebook.zim").exists()) {
                return currentPage;
            } else {
                return findNotebookRootDir(currentPage.getParentFile());
            }
        }
        return null;
    }

    private String findFirstPageTraversingUpToRoot(File currentPage, String relativeLinkToCheck) {
        if (currentPage.equals(_notebookRootDir)) {
            return null;
        }

        File parentFolder = currentPage.getParentFile();
        File candidateFile = FileUtils.join(parentFolder, relativeLinkToCheck);
        if (candidateFile.exists()) {
            return candidateFile.toString();
        } else {
            return findFirstPageTraversingUpToRoot(parentFolder, relativeLinkToCheck);
        }
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

    public String getZimPath() {
        return _zimPath;
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
