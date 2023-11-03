package net.gsantner.markor.format.wikitext;

import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
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
                // try the current directory as a possible notebook root dir
                _notebookRootDir = _currentPage.getParentFile();
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

        // Try to resolve the path as relative to the wiki page's attachment directory.
        // Return an absolute path, or the original path in case it cannot be resolved,
        // it might be a URL.
        return resolveAttachmentPath(wikitextPath, _notebookRootDir, _currentPage, _shouldDynamicallyDetermineRoot);
    }

    private String stripInnerPageReference(String wikitextPath) {
        Matcher pathWithInnerPageReferenceMatcher = Patterns.PATH_WITH_INNER_PAGE_REFERENCE.pattern.matcher(wikitextPath);
        if (pathWithInnerPageReferenceMatcher.matches()) {
            String pagePath = pathWithInnerPageReferenceMatcher.group(1);
            return pagePath;
        }
        return wikitextPath;
    }

    private static File findNotebookRootDir(File currentPage) {
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

    /**
     * Return a wiki file's Attachment Directory.<p></p>
     *
     * <p> By design, a Zim wiki file's Attachment Directory should
     * have the same path of the wiki file itself, without the .txt
     * suffix.</p><p></p>
     *
     * <p>Here, a difference from Zim is that any file can derive a
     * properly named Attachment Directory, as long as that file is
     * with a suffix.</p>
     *
     * @param currentPage Current wiki file's path.
     * @return {@code currentPage}'s path without suffix.
     *
     * @see GsFileUtils#getFilenameWithoutExtension(File)
     */
    public static File findAttachmentDir(File currentPage) {
        return GsFileUtils.join(currentPage.getParentFile(), GsFileUtils.getFilenameWithoutExtension(currentPage));
    }

    /**
     * Return a Notebook's Root Directory.<p></p>
     *
     * <p> By design ({@code shouldDynamicallyDetermineRoot = true}),
     * a Zim Notebook's Root Directory is the closest ancestor of the
     * {@code currentPage} with a notebook.zim file in it.</p><p></p>
     *
     * <p> Here, a difference from Zim is that if a notebook.zim file
     * can't be located, a {@code currentPage}'s current directory is
     * taken as Root Directory.</p><p></p>
     *
     * <p> WARNING: Removing a notebook.zim file from the Notebook or
     * changing the value of {@code notebookRootDir}, may switch to a
     * Root Directory different than where the Notebook was organized
     * originally.</p>
     *
     * @param notebookRootDir Root Directory when {@code shouldDynamicallyDetermineRoot == false}.
     * @param currentPage Current wiki file's path used to determine the current directory.
     * @param shouldDynamicallyDetermineRoot If {@code true}, return the closest ancestor of the
     * {@code currentPage} with a notebook.zim file in it, or fall back to the current directory
     * on failure.  If {@code false}, return {@code notebookRootDir}.
     * @return Identified Notebook's Root Directory.
     *
     * @see WikitextLinkResolver#findNotebookRootDir(File)
     */
    public static File findNotebookRootDir(File notebookRootDir, File currentPage, boolean shouldDynamicallyDetermineRoot) {
        if (shouldDynamicallyDetermineRoot) {
            notebookRootDir = findNotebookRootDir(currentPage);
            if (notebookRootDir == null) {
                notebookRootDir = currentPage.getParentFile();
            }
        }
        return notebookRootDir;
    }

    /**
     * Return a system file's path as wiki attachment's path.<p></p>
     *
     * <p> If both {@code file} and {@code currentPage} are children
     * of the same Notebook's Root Directory, return a path relative
     * to the {@code currentPage}'s Attachment Directory, otherwise,
     * return the original {@code file}'s path.</p><p></p>
     *
     * <p> Here, a difference from Zim is to always consider as Root
     * Directory the {@code currentPage}'s directory before anything
     * else.</p>
     *
     * @param file System file's path to resolve as wiki attachment's path.
     * @param notebookRootDir Root Directory when {@code shouldDynamicallyDetermineRoot == false}.
     * @param currentPage Current wiki file's path used to determine the current Attachment Directory.
     * @param shouldDynamicallyDetermineRoot If {@code true}, the Root Directory is the closest ancestor
     * of the {@code currentPage} with a notebook.zim file in it, or the {@code currentPage}'s directory
     * on failure.  If {@code false}, the Root Directory is {@code notebookRootDir}.  In either cases, a
     * {@code currentPage}'s directory is always considered as Root Directory before anything else.
     * @return {@code file}'s path relative to the {@code currentPage}'s Attachment Directory, when both
     * {@code file} and {@code currentPage} are children of the identified Notebook's Root Directory, or
     * the original {@code file}'s path otherwise.
     *
     * @see WikitextLinkResolver#findAttachmentDir(File)
     * @see WikitextLinkResolver#findNotebookRootDir(File, File, boolean)
     */
    public static String resolveSystemFilePath(File file, File notebookRootDir, File currentPage, boolean shouldDynamicallyDetermineRoot) {
        final File currentDir = currentPage.getParentFile();
        notebookRootDir = findNotebookRootDir(notebookRootDir, currentPage, shouldDynamicallyDetermineRoot);

        if (GsFileUtils.isChild(currentDir, file) ||
            (GsFileUtils.isChild(notebookRootDir, file) && GsFileUtils.isChild(notebookRootDir, currentPage))) {
            final File attachmentDir = findAttachmentDir(currentPage);
            String path = GsFileUtils.relativePath(attachmentDir, file);

            // Zim prefixes also children of the Attachment Directory.
            if (file.toString().endsWith("/" + path)) {
                path = "./" + path;
            }

            return path;
        }

        return file.toString();
    }

    /**
     * Return a wiki attachment's path as system absolute path.<p></p>
     *
     * <p> If {@code path} can be resolved as a relative path to the
     * {@code currentPage}'s Attachment Directory, return the result
     * of {@code path} as a system absolute path.  Otherwise, return
     * the original {@code path}.</p><p></p>
     *
     * <p> Here, a difference from Zim is to always consider as Root
     * Directory the {@code currentPage}'s directory before anything
     * else.</p>
     *
     * @param path Path that might be relative to the {@code currentPage}'s Attachment Directory.
     * @param notebookRootDir Root Directory when {@code shouldDynamicallyDetermineRoot == false}.
     * @param currentPage Current wiki file's path used to determine the current Attachment Directory.
     * @param shouldDynamicallyDetermineRoot If {@code true}, the Root Directory is the closest ancestor
     * of the {@code currentPage} with a notebook.zim file in it, or the {@code currentPage}'s directory
     * on failure.  If {@code false}, the Root Directory is {@code notebookRootDir}.  In either cases, a
     * {@code currentPage}'s directory is always considered as Root Directory before anything else.
     * @return {@code path} as a system absolute path, if it can be resolved as a relative path to the
     * {@code currentPage}'s Attachment Directory, or the original {@code path} otherwise.
     *
     * @see WikitextLinkResolver#findAttachmentDir(File)
     * @see WikitextLinkResolver#resolveSystemFilePath(File, File, File, boolean)
     */
    public static String resolveAttachmentPath(String path, File notebookRootDir, File currentPage, boolean shouldDynamicallyDetermineRoot) {
        if (path.startsWith("./") || path.startsWith("../")) {
            final File attachmentDir = findAttachmentDir(currentPage);
            final File file = new File(attachmentDir, path).getAbsoluteFile();
            final String resolved = resolveSystemFilePath(file, notebookRootDir, currentPage, shouldDynamicallyDetermineRoot);

            if (path.equals(resolved)) {
                try {
                    return file.getCanonicalPath();
                } catch (IOException e) {
                    return file.toString();
                }
            }
        }

        return path;
    }
}
