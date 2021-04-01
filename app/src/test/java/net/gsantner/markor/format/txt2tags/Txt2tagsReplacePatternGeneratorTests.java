/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.txt2tags;

import net.gsantner.markor.ui.hleditor.TextActions;

import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

public class Txt2tagsReplacePatternGeneratorTests {


    public static class VariousTests {


        private List<TextActions.ReplacePattern> replacePatterns;
        private String result;

        @Test
        public void createHeadingsWithSpecifiedLevel() {
        }

        @Test
        public void removeHeadingCharsForExactHeadingLevel() {
            setLevelFourHeadingAction();
            assertCorrectReplacement("==== My Heading ====", "My Heading");
        }

        @Test
        public void replaceDifferentLevelHeadings() {
            setLevelFourHeadingAction();
            assertCorrectReplacement("=== My Heading ===", "==== My Heading ====");
        }


        @Test
        public void addHeadingCharactersToText() {
            setLevelFourHeadingAction();
            assertCorrectReplacement("My Heading", "==== My Heading ====");
        }


        private void setLevelFourHeadingAction() {
            int headingLevel = 4;
            replacePatterns = Txt2tagsReplacePatternGenerator.setOrUnsetHeadingWithLevel(headingLevel);
        }

        @Test
        public void toggleFromUncheckedToCheckedBox() {
            replacePatterns = Txt2tagsReplacePatternGenerator.replaceWithNextStateCheckbox();
            String uncheckedItem = "[ ] some item";
            assertCorrectReplacement(uncheckedItem, "[*] some item");
        }

        @Test
        public void toggleCheckBoxInCorrectOrder() {
            replacePatterns = Txt2tagsReplacePatternGenerator.replaceWithNextStateCheckbox();
            String[] orderedCheckboxStates = {" ", "*", "x", ">"};
            // create checkbox
            String currentLine = "some item";
            currentLine = replaceWithFirstMatchingPattern(replacePatterns, currentLine);
            for (int i = 0; i < orderedCheckboxStates.length + 1; i++) {
                assertThat(currentLine).isEqualTo("[" + orderedCheckboxStates[i % orderedCheckboxStates.length] + "] some item");
                currentLine = replaceWithFirstMatchingPattern(replacePatterns, currentLine);
            }
        }




        private void assertCorrectReplacement(String original, String expectedReplacement) {
            result = replaceWithFirstMatchingPattern(replacePatterns, original);
            assertThat(result).isEqualTo(expectedReplacement);
        }

        private String replaceWithFirstMatchingPattern(List<TextActions.ReplacePattern> replacePatterns, String input) {
            for (TextActions.ReplacePattern replacePattern : replacePatterns) {
                Matcher matcher = replacePattern.searchPattern.matcher(input);
                if (matcher.find()) {
                    return matcher.replaceFirst(replacePattern.replacePattern);
                }
            }
            return null;
        }
    }
}