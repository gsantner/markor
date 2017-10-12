package net.gsantner.markor.editor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class HighlighterPatternMatchedTest {

    @Parameterized.Parameters(name = "{index}: Pattern {0} should match \"{2}\" in string \"{1}\"")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {HighlighterPattern.LIST, "- Item", "-"},
                {HighlighterPattern.LIST, "- [ ] Unchecked", "- [ ]"},
                {HighlighterPattern.LIST, "- [x] Checked Lower", "- [x]"},
                {HighlighterPattern.LIST, "- [X] Checked Upper", "- [X]"}
        });
    }

    private final HighlighterPattern highlighterPattern;
    private final String string;
    private final String expected;

    public HighlighterPatternMatchedTest(HighlighterPattern highlighterPattern, String string,
                                         String expected) {
        this.highlighterPattern = highlighterPattern;
        this.string = string;
        this.expected = expected;
    }

    @Test
    public void match() {
        Matcher m = highlighterPattern.getPattern().matcher(string);
        assertThat(m.find()).isTrue();
        String matched = m.group();
        assertThat(matched).isEqualTo(expected);
    }
}
