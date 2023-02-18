/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter;
import net.gsantner.markor.model.AppSettings;

public class CsvSyntaxHighlighter extends MarkdownSyntaxHighlighter {
    public CsvSyntaxHighlighter(AppSettings as) {
        super(as);
    }
}
