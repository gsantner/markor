package net.gsantner.markor.format.orgmode;

import android.graphics.Paint;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.regex.Pattern;

public class OrgmodeSyntaxHighlighter extends SyntaxHighlighterBase {

    public final static Pattern HEADING = Pattern.compile("(?m)^(\\*+)\\s(.*?)(?=\\n|$)");
    public final static Pattern BLOCK = Pattern.compile("(?m)(?<=#\\+BEGIN_.{1,15}$\\s)[\\s\\S]*?(?=#\\+END)");
    public final static Pattern PREAMBLE = Pattern.compile("(?m)^(#\\+)(.*?)(?=\\n|$)");
    public final static Pattern COMMENT = Pattern.compile("(?m)^(#+)\\s(.*?)(?=\\n|$)");
    public final static Pattern LIST_UNORDERED = Pattern.compile("(\\n|^)\\s{0,16}([*+-])( \\[[ xX]\\])?(?= )");
    public final static Pattern LIST_ORDERED = Pattern.compile("(?m)^\\s{0,16}(\\d+)(:?\\.|\\))\\s");
    public final static Pattern LINK = Pattern.compile("\\[\\[.*?]]|<.*?>|https?://\\S+|\\[.*?]\\[.*?]|\\[.*?]\n");
    private static final int ORG_COLOR_HEADING = 0xffef6D00;
    private static final int ORG_COLOR_LINK = 0xff1ea3fe;
    private static final int ORG_COLOR_LIST = 0xffdaa521;
    private static final int ORG_COLOR_DIM = 0xff8c8c8c;
    private static final int ORG_COLOR_BLOCK = 0xdddddddd;

    public OrgmodeSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    @Override
    public SyntaxHighlighterBase configure(Paint paint) {
        _delay = _appSettings.getOrgmodeHighlightingDelay();
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        createSmallBlueLinkSpans();
        createColorSpanForMatches(HEADING, ORG_COLOR_HEADING);
        createColorSpanForMatches(LINK, ORG_COLOR_LINK);
        createColorSpanForMatches(LIST_UNORDERED, ORG_COLOR_LIST);
        createColorSpanForMatches(LIST_ORDERED, ORG_COLOR_LIST);
        createColorSpanForMatches(PREAMBLE, ORG_COLOR_DIM);
        createColorSpanForMatches(COMMENT, ORG_COLOR_DIM);
        createColorBackgroundSpan(BLOCK, ORG_COLOR_BLOCK);
    }

}

