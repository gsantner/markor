package me.writeily.renderer;

import android.content.Context;
import android.preference.PreferenceManager;

import com.commonsware.cwac.anddown.AndDown;

import me.writeily.R;
import me.writeily.model.Constants;

public class MarkDownRenderer {
    AndDown andDown = new AndDown();

    public String renderMarkdown(String markdownRaw, Context context) {
        return  themeStringFromContext(context) +
                andDown.markdownToHtml(markdownRaw) +
                Constants.MD_HTML_SUFFIX;
    }

    private String themeStringFromContext(Context context) {
        String theme = getThemeFromPrefs(context);
	    String s = "";
        if (!theme.equals("")) {
            if (theme.equals(context.getString(R.string.theme_dark))) {
                s += Constants.DARK_MD_HTML_PREFIX;
            } else {
                s += Constants.MD_HTML_PREFIX;
            }
	        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.pref_render_rtl_key), false))
		        s += Constants.MD_HTML_RTL_CSS;
	        s += Constants.MD_HTML_PREFIX_END;
        }
        return s;
    }

    private String getThemeFromPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_theme_key), "");
    }
}
