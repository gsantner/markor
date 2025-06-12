package net.gsantner.markor.format.plaintext.highlight;

import android.content.Context;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;

public class HighlightConfigLoader {
    private final Gson gson = new Gson();
    private final Properties map = new Properties();
    private final SyntaxCache syntaxCache = new SyntaxCache();
    private Theme theme;

    public static final String MAP_PATH = "highlight/languages/map.properties";

    private <T> T loadConfig(Context context, String path, Class<T> classT) {
        try (InputStream input = context.getAssets().open(path)) {
            return gson.fromJson(new InputStreamReader(input), classT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadSyntax(Context context, String lang) {
        Syntax syntax = loadConfig(context, "highlight/languages/" + lang + ".json", Syntax.class);
        syntaxCache.putSyntax(lang, syntax);
    }

    private void loadTheme(Context context, String name) {
        theme = loadConfig(context, "highlight/themes/" + name + ".json", Theme.class);
    }

    /**
     * Get language syntax.
     *
     * @param context Android Context.
     * @param lang    Language name.
     * @return Language syntax.
     */
    public Syntax getSyntax(Context context, String lang) {
        lang = lang.replaceAll("^\\.+", "").toLowerCase();
        if (map.isEmpty()) {
            try {
                map.load(context.getAssets().open(MAP_PATH));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String key = map.getProperty(lang);
        Syntax syntax = syntaxCache.getSyntax(key);
        if (syntax == null) {
            loadSyntax(context, key);
        }

        return syntaxCache.getSyntax(key);
    }

    public Theme getTheme(Context context, String name) {
        if (theme == null || !theme.getName().equals(name)) {
            loadTheme(context, name);
        }
        return theme;
    }

    static class SyntaxCache {
        public static final int CACHE_SIZE = 5;
        private final HashMap<String, Syntax> syntaxMap = new HashMap<>();
        private final HashMap<String, Integer> usageMap = new HashMap<>();

        public Syntax getSyntax(String key) {
            Syntax syntax = syntaxMap.get(key);
            if (syntax == null) {
                return null;
            }

            Integer usage = usageMap.get(key);
            usageMap.put(key, 1 + (usage == null ? 0 : usage));
            return syntax;
        }

        public void putSyntax(String key, Syntax syntax) {
            if (syntaxMap.size() > CACHE_SIZE) {
                String entryKey = null;
                int min = Integer.MAX_VALUE;
                for (HashMap.Entry<String, Integer> entry : usageMap.entrySet()) {
                    int value = entry.getValue();
                    if (value < min) {
                        min = value;
                        entryKey = entry.getKey();
                    }
                }

                if (entryKey != null) {
                    syntaxMap.remove(entryKey);
                    usageMap.remove(entryKey);
                }
            }

            syntaxMap.put(key, syntax);
            usageMap.put(key, 0);
        }
    }
}
