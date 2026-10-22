/*#######################################################
 *
 * SPDX-License-Identifier: Apache-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MarkdownTextConverterTest {

    @Test
    public void taskWithMultipleParagraphs() {
        final String markdown = "- [ ] lorem\nIpsum\n\n    Dolor\n\n    Sit";

        final String html = render(markdown);

        assertThat(html).contains("task-list-item-checkbox", "lorem", "Ipsum", "Dolor", "Sit");
    }

    @Test
    public void taskWithNestedList() {
        final String markdown =
                "Acheté\n----------\n* [ ] Mackie Live\n\n"
                        + "    En point retrait.\n    \n"
                        + "    * N° de commande : 2601071231HH768\n"
                        + "    * Paiement par scl";

        final String html = render(markdown);

        assertThat(html)
                .contains(
                        "task-list-item-checkbox",
                        "Mackie Live",
                        "En point retrait.",
                        "N° de commande : 2601071231HH768",
                        "Paiement par scl");
    }

    @Test
    public void headingsRetainLineNumbers() {
        final String html = render("# First\n\nParagraph\n\n## Second");

        assertThat(html).containsPattern("<h1\\b[^>]*\\bline=\"0\"[^>]*>");
        assertThat(html).containsPattern("<h2\\b[^>]*\\bline=\"4\"[^>]*>");
    }

    private String render(final String markdown) {
        return MarkdownTextConverter.flexmarkRenderer.render(
                MarkdownTextConverter.flexmarkParser.parse(markdown));
    }
}
