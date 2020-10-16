/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ZimWikiHighlighterPatternCheckListTest {

    private Pattern pattern;

    @Test
    public void uncheckedItem() {
        pattern = ZimWikiHighlighterPattern.CHECKLIST_UNCHECKED.pattern;
        Matcher m = pattern.matcher("[ ] Unchecked Item");
        assertCorrectCheckboxWithInnerSymbol(m, " ");
    }

    @Test
    public void checkedItem() {
        pattern = ZimWikiHighlighterPattern.CHECKLIST_CHECKED.pattern;
        Matcher m = pattern.matcher("[*] Checked Item");
        assertCorrectCheckboxWithInnerSymbol(m, "*");
    }

    @Test
    public void crossedItem() {
        pattern = ZimWikiHighlighterPattern.CHECKLIST_CROSSED.pattern;
        Matcher m = pattern.matcher("[x] Crossed Item");
        assertCorrectCheckboxWithInnerSymbol(m, "x");
    }

    @Test
    public void itemWithArrow() {
        pattern = ZimWikiHighlighterPattern.CHECKLIST_ARROW.pattern;
        Matcher m = pattern.matcher("[>] Marked Item with a yellow arrow");
        assertCorrectCheckboxWithInnerSymbol(m, ">");
    }

    @Test
    public void roundBracesDoNotMatch() {
        Pattern[] checklistPatterns = {ZimWikiHighlighterPattern.CHECKLIST.pattern,
                ZimWikiHighlighterPattern.CHECKLIST_CHECKED.pattern,
                ZimWikiHighlighterPattern.CHECKLIST_CROSSED.pattern,
                ZimWikiHighlighterPattern.CHECKLIST_ARROW.pattern};
        for (Pattern checklistPattern : checklistPatterns) {
            Matcher m = checklistPattern.matcher("( ) invalid item");
            assertThat(m.find()).isFalse();
        }
    }

    private void assertCorrectCheckboxWithInnerSymbol(Matcher m, String symbol) {
        assertThat(m.find()).isTrue();
        assertThat(m.group()).isEqualTo("["+symbol+"]");
        assertThat(m.group(ZimWikiHighlighterPattern.CHECKBOX_LEFT_BRACKET_GROUP)).isEqualTo("[");
        assertThat(m.group(ZimWikiHighlighterPattern.CHECKBOX_SYMBOL_GROUP)).isEqualTo(symbol);
        assertThat(m.group(ZimWikiHighlighterPattern.CHECKBOX_RIGHT_BRACKET_GROUP)).isEqualTo("]");
    }

}
