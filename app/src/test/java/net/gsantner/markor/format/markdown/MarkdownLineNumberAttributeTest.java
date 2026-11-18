package net.gsantner.markor.format.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladsch.flexmark.util.ast.Document;

import org.junit.Test;

/**
 * Task list items with 3+ child blocks used to crash rendering with
 * "NullPointerException: ... Document.getLineNumber(int) on a null object reference"
 * (#2596, #2711, #2718).
 */
public class MarkdownLineNumberAttributeTest {

    private static String render(final String markdown) {
        final Document document = MarkdownTextConverter.flexmarkParser.parse(markdown);
        return MarkdownTextConverter.flexmarkRenderer.render(document);
    }

    @Test
    public void taskItemWithLazyContinuationAndIndentedParagraphs() {
        // #2711
        final String html = render("- [ ] lorem\nIpsum\n\n    Dolor\n\n    Sit\n");
        assertThat(html).contains("<p line=\"3\">Dolor</p>");
        assertThat(html).contains("<p line=\"5\">Sit</p>");
    }

    @Test
    public void taskItemWithParagraphAndNestedList() {
        // #2718
        final String html = render("Title\n-----\n* [ ] Mackie Live\n\n    En point retrait.\n    \n    * Order\n    * Payment\n");
        assertThat(html).contains("<p line=\"4\">En point retrait.</p>");
        assertThat(html).contains("Payment");
    }

    @Test
    public void zimChecklistWithNestedItems() {
        // #2596: Zim wiki checklist, as transpiled to Markdown by WikitextTextConverter
        final String html = render("- [ ] plan  \n\t* foo  \n  \n  \n\t- [ ] bar  \n  \n");
        assertThat(html).contains("foo");
        assertThat(html).contains("bar");
        assertThat(html).contains("line=\"1\"");
    }
}
