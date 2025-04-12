package net.gsantner.markor.format.plaintext;
/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

import android.graphics.Paint;

import net.gsantner.markor.format.plaintext.highlight.HighlightConfigLoader;
import net.gsantner.markor.format.plaintext.highlight.Style;
import net.gsantner.markor.format.plaintext.highlight.Syntax;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.ArrayList;

public class PlaintextSyntaxHighlighter extends SyntaxHighlighterBase {
    public final static HighlightConfigLoader configLoader = new HighlightConfigLoader();
    private Syntax syntax;
    private Style style;

    public PlaintextSyntaxHighlighter(AppSettings appSettings) {
        super(appSettings);
    }

    public PlaintextSyntaxHighlighter(AppSettings appSettings, String extension) {
        super(appSettings);
        syntax = configLoader.getSyntax(extension, appSettings.getContext());
        style = configLoader.getStyle(appSettings.getContext(), "default");
    }

    @Override
    public SyntaxHighlighterBase configure(Paint paint) {
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        createSmallBlueLinkSpans();

        if (syntax == null) {
            return;
        }

        ArrayList<Syntax.Rule> rules = syntax.getRules();
        ArrayList<Style.Define> defines = style.getDefines();
        for (Syntax.Rule rule : rules) {
            for (Style.Define define : defines) {
                if (define.getType().equals(rule.getType())) {
                    createColorSpanForMatches(rule.getPattern(), define.getColor());
                }
            }
        }
    }

}

