package net.gsantner.markor.format.plaintext;
/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

import net.gsantner.markor.format.plaintext.highlight.Rule;
import net.gsantner.markor.format.plaintext.highlight.Syntax;
import net.gsantner.markor.format.plaintext.highlight.SyntaxLoader;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.ArrayList;

public class PlaintextSyntaxHighlighter extends SyntaxHighlighterBase {
    private final SyntaxLoader syntaxLoader = new SyntaxLoader();
    private Syntax syntax;

    public PlaintextSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    public PlaintextSyntaxHighlighter(AppSettings appSettings, String lang) {
        super(appSettings);
        appSettings.getContext();
        syntax = syntaxLoader.getSyntax(appSettings.getContext(), lang);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        createSmallBlueLinkSpans();

        if (syntax == null) {
            return;
        }

        ArrayList<Rule> rules = syntax.getRules();
        for (Rule rule : rules) {
            createColorSpanForMatches(rule.getPattern(), rule.getParsedColor());
        }
    }

}

