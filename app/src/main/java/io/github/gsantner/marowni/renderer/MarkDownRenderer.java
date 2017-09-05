package io.github.gsantner.marowni.renderer;

import android.content.Context;
import android.preference.PreferenceManager;

import com.commonsware.cwac.anddown.AndDown;

import io.github.gsantner.marowni.R;
import io.github.gsantner.marowni.model.Constants;
import io.github.gsantner.marowni.util.AppSettings;

public class MarkDownRenderer {
    AndDown andDown = new AndDown();

    public String renderMarkdown(String markdownRaw, Context context) {
        return themeStringFromContext(context) +
                andDown.markdownToHtml(markdownRaw) +
                Constants.MD_HTML_SUFFIX;
    }

    private String themeStringFromContext(Context context) {
        String s = "";
        if (AppSettings.get().isDarkThemeEnabled()) {
            s += Constants.DARK_MD_HTML_PREFIX;
        } else {
            s += Constants.MD_HTML_PREFIX;
        }
        if (AppSettings.get().isRenderRtl())
            s += Constants.MD_HTML_RTL_CSS;
        s += Constants.MD_HTML_PREFIX_END;
        return s;
    }
}
