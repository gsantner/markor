package other.writeily;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;

public class UrlPatternTest {

    @Test
    public void test() {
        check("http://test/test2", "http://test/test2");
        check("begin http://test/test2 end", "http://test/test2");
        check("begin https://test/test2 end", "https://test/test2");
        check("begin https://www.test/test2 end", "https://www.test/test2");
        check("[foo](http://bar.baz )", "http://bar.baz");
        check("[foo](http://bar.baz)", "http://bar.baz");

        check("begin /test/test2 end");
        check("begin ftp://test/test2 end");
        check("begin smb://test/test2 end");
        check("begin HtTp://test/test2 end");
    }

    private void check(String inputString, String expectedUrl) {
        final Matcher matcher = SyntaxHighlighterBase.URL.matcher(inputString);
        Assert.assertTrue(matcher.find());
        Assert.assertEquals(expectedUrl, matcher.group(0));
    }

    private void check(String inputString) {
        final Matcher matcher = SyntaxHighlighterBase.URL.matcher(inputString);
        Assert.assertFalse(matcher.find());
    }

}
