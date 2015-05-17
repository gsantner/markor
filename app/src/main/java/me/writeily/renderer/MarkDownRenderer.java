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
        if (!theme.equals("")) {
            if (theme.equals(context.getString(R.string.theme_dark))) {
                return Constants.DARK_MD_HTML_PREFIX;
            } else {
                return Constants.MD_HTML_PREFIX;
            }
        }
        return "";
    }

    private String getThemeFromPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_theme_key), "");
    }
}
