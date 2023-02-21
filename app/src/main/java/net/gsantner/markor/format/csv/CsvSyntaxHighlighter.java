/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import android.graphics.Color;

import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.model.AppSettings;

public class CsvSyntaxHighlighter extends MarkdownSyntaxHighlighter {
    private static final GsCallback.r1[] COLORS_FOR_WHITE_BACKGROUND = {new HighlightSpan().setForeColor(Color.RED),
            new HighlightSpan().setForeColor(Color.BLUE),
            new HighlightSpan().setForeColor(Color.GREEN),
            new HighlightSpan().setForeColor(Color.MAGENTA),
            new HighlightSpan().setForeColor(Color.CYAN)};

    // todo make dynamic
    private CsvConfig csvConfig = CsvConfig.DEFAULT;
    Pattern patternCsv = CsvMatcher.patternCsv(csvConfig);

    public CsvSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    @Override
    protected void generateSpans() {
        super.generateSpans();

        // todo: get colors from resources
        createSpanForMatches(patternCsv, COLORS_FOR_WHITE_BACKGROUND);
    }

    protected final void createSpanForMatches(final Pattern pattern, GsCallback.r1<Object, Matcher>[] creators) {
        final Matcher m = pattern.matcher(_spannable);

        // do not color first column
        int colNumner = 0;

        while (m.find()) {
            String columnContent = m.group();
            if (!columnContent.startsWith("" + csvConfig.getFieldDelimiterChar())) {
                // first column of line is not colorated (remains black)
                colNumner = 0;
                continue;
            }
            final Object span = creators[colNumner].callback(m);

            if (span != null) {
                final int start = m.start(0);
                final int end = m.end(0);
                if (Math.abs(end - start) > 0) {
                    addSpanGroup(span, start, end);
                }
            }
            colNumner++;
            if (colNumner >= creators.length) {
                colNumner = 0;
            }
        }
    }
}
