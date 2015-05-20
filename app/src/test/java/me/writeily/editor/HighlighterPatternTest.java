package me.writeily.editor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class HighlighterPatternTest {


    @Parameterized.Parameters(name = "{index}: text {1} shoud be found {2} times")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {HighlighterPattern.HEADER.getPattern(), " # Hi", 0},
                {HighlighterPattern.HEADER.getPattern(), "# Hi\n", 1},
                {HighlighterPattern.HEADER.getPattern(), "#Hi\n", 1},
                {HighlighterPattern.HEADER.getPattern(), "## Hi\n", 1},
                {HighlighterPattern.HEADER.getPattern(), "############ Hi\n", 1},
                {HighlighterPattern.HEADER.getPattern(), "# Hi\n#Hi again\n", 1},
                {HighlighterPattern.HEADER.getPattern(), "# Hi\nWhatever\n# Hi again\n", 2},
                {HighlighterPattern.HEADER.getPattern(), "# Hi\n======", 1},
                {HighlighterPattern.HEADER.getPattern(), "# Hi\n--------", 1},
                {HighlighterPattern.LINK.getPattern(), "[sometext](some://url.com)", 1},
                {HighlighterPattern.LINK.getPattern(), "[some text ]( some://url.com)", 1},
                {HighlighterPattern.LINK.getPattern(), "[sometext] some://url.com)", 0},
                {HighlighterPattern.LINK.getPattern(), "[sometext]( some://url.com )", 1},
                {HighlighterPattern.LINK.getPattern(), "[sometext] ( some://url.com )", 0},
                {HighlighterPattern.STRIKETHROUGH.getPattern(), "~~s~~", 1},
                {HighlighterPattern.STRIKETHROUGH.getPattern(), "~~ s 4 ~~df ~~", 1},
                {HighlighterPattern.STRIKETHROUGH.getPattern(), "~~ s 4 ~~d\n\nf ~~", 1},
                {HighlighterPattern.STRIKETHROUGH.getPattern(), "~~ s 4 ~\n~d\n\nf ~~", 0},
                {HighlighterPattern.STRIKETHROUGH.getPattern(), "~ s 4 ~~df ~\n ff ~~", 0},
                {HighlighterPattern.STRIKETHROUGH.getPattern(), "~~s~", 0},
                {HighlighterPattern.STRIKETHROUGH.getPattern(), "~s~~", 0},
                {HighlighterPattern.MONOSPACED.getPattern(), "`s`", 1},
                {HighlighterPattern.MONOSPACED.getPattern(), "``s`", 1},
                {HighlighterPattern.MONOSPACED.getPattern(), "`s``", 1},
                {HighlighterPattern.MONOSPACED.getPattern(), "`s`", 1},
                {HighlighterPattern.MONOSPACED.getPattern(), "`s\n\n`", 0},
                {HighlighterPattern.QUOTATION.getPattern(), "> asdfasdfas\n> sadfasdfasdf", 2},
                {HighlighterPattern.QUOTATION.getPattern(), "> asdfa > sdfas\n", 1},
                {HighlighterPattern.QUOTATION.getPattern(), ">> sdfas", 1},
                {HighlighterPattern.QUOTATION.getPattern(), "\n> sdfas", 1},
                {HighlighterPattern.LIST.getPattern(), "* asdfasdfas\n* sadfasdfasdf", 2},
                {HighlighterPattern.LIST.getPattern(), "* asdfa > sdfas\n", 1},
                {HighlighterPattern.LIST.getPattern(), "\n* sdfas", 1},
                {HighlighterPattern.LIST.getPattern(), "1. asdfasdfas\n2. sadfasdfasdf", 2},
                {HighlighterPattern.LIST.getPattern(), "1. asdfa 2. sdfas\n", 1},
                {HighlighterPattern.LIST.getPattern(), "\n99. sdfas", 1},
                {HighlighterPattern.BOLD.getPattern(), "**s**", 1},
                {HighlighterPattern.BOLD.getPattern(), "****s**", 1},
                {HighlighterPattern.BOLD.getPattern(), "**s****", 1},
                {HighlighterPattern.BOLD.getPattern(), "**s**", 1},
                {HighlighterPattern.BOLD.getPattern(), "**s\n\n**", 0},
                {HighlighterPattern.BOLD.getPattern(), "__s__", 1},
                {HighlighterPattern.BOLD.getPattern(), "____s__", 1},
                {HighlighterPattern.BOLD.getPattern(), "__s____", 1},
                {HighlighterPattern.BOLD.getPattern(), "__s__", 1},
                {HighlighterPattern.BOLD.getPattern(), "__s\n\n__", 0},
                {HighlighterPattern.ITALICS.getPattern(), "*s*", 1},
                {HighlighterPattern.ITALICS.getPattern(), "**s*", 1},
                {HighlighterPattern.ITALICS.getPattern(), "*s**", 1},
                {HighlighterPattern.ITALICS.getPattern(), "*s*", 1},
                {HighlighterPattern.ITALICS.getPattern(), "*s\n\n*", 0},                
                {HighlighterPattern.ITALICS.getPattern(), "_s_", 1},
                {HighlighterPattern.ITALICS.getPattern(), "__s_", 1},
                {HighlighterPattern.ITALICS.getPattern(), "_s__", 1},
                {HighlighterPattern.ITALICS.getPattern(), "_s_", 1},
                {HighlighterPattern.ITALICS.getPattern(), "_s\n\n_", 0},
        });
    }

    private final Pattern highlighterPattern;
    private final String string;
    private final int foundCount;

    public HighlighterPatternTest(Pattern highlighterPattern, String string, int foundCount) {
        this.string = string;
        this.foundCount = foundCount;
        this.highlighterPattern = highlighterPattern;
    }

    @Test
    public void testHeaders(){
        int count = 0;

        for(Matcher m = highlighterPattern.matcher(string); m.find(); ) {
            count++;
        }

        assertThat(count).isEqualTo(foundCount);

    }
}