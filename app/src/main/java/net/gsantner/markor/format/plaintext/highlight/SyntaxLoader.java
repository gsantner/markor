package net.gsantner.markor.format.plaintext.highlight;

import android.content.Context;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class SyntaxLoader {
    private final Gson gson = new Gson();
    private final Properties map = new Properties();
    private final SyntaxCache syntaxCache = new SyntaxCache();

    public static final String MAP_PATH = "languages/map.properties";

    private void load(InputStream inputStream, String lang) {
        try {
            Syntax syntax = gson.fromJson(new InputStreamReader(inputStream), Syntax.class);
            syntaxCache.putSyntax(lang, syntax);
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void load(Context context, String lang) {
        try {
            load(context.getAssets().open("languages/" + lang + ".json"), lang);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get language syntax.
     *
     * @param context Android Context.
     * @param lang    Language name or file extension without dot.
     * @return
     */
    public Syntax getSyntax(Context context, String lang) {
        if (map.isEmpty()) {
            try {
                map.load(context.getAssets().open(MAP_PATH));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String key = map.getProperty(lang.toLowerCase());
        Syntax syntax = syntaxCache.getSyntax(key);
        if (syntax == null) {
            load(context, key);
        }

        return syntaxCache.getSyntax(key);
    }

    public Syntax getSyntax(String extension, Context context) {
        if (extension.startsWith(".")) {
            return getSyntax(context, extension.substring(1));
        } else {
            return getSyntax(context, extension);
        }
    }
}
