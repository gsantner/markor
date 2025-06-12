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
import net.gsantner.markor.format.plaintext.highlight.Syntax;
import net.gsantner.markor.format.plaintext.highlight.Theme;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.ArrayList;
import java.util.HashMap;

public class PlaintextSyntaxHighlighter extends SyntaxHighlighterBase {
    public final static HighlightConfigLoader configLoader = new HighlightConfigLoader();
    private ArrayList<Syntax.Rule> rules;
    private HashMap<String, Theme.Style> styles;

    public PlaintextSyntaxHighlighter(AppSettings appSettings) {
        super(appSettings);
    }

    public PlaintextSyntaxHighlighter(AppSettings appSettings, String extension) {
        super(appSettings);

        Syntax syntax = configLoader.getSyntax(appSettings.getContext(), extension);
        if (syntax != null) {
            rules = syntax.getRules();
            Theme theme = configLoader.getTheme(appSettings.getContext(), "default");
            if (theme != null) {
                styles = theme.getStyles();
            }
        }
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

        if (rules == null || styles == null) {
            return;
        }

        for (Syntax.Rule rule : rules) {
            Theme.Style style = styles.get(rule.getType());
            if (style != null) {
                createColorSpanForMatches(rule.getPattern(), style.getColor());
            }
        }
    }
}
