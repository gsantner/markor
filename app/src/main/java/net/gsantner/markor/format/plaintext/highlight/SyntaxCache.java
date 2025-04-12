package net.gsantner.markor.format.plaintext.highlight;

import java.util.HashMap;

public class SyntaxCache {
    public static final int CACHE_SIZE = 5;
    private final HashMap<String, Syntax> syntaxMap = new HashMap<>();
    private final HashMap<String, Integer> usageMap = new HashMap<>();

    public Syntax getSyntax(String key) {
        Syntax syntax = syntaxMap.get(key);
        if (syntax == null) {
            return null;
        }

        Integer usage = usageMap.get(key);
        if (usage == null) {
            usageMap.put(key, 1);
        } else {
            usageMap.put(key, ++usage);
        }

        return syntax;
    }

    public void putSyntax(String key, Syntax syntax) {
        if (syntaxMap.size() > CACHE_SIZE) {
            String entryKey = null;
            int min = Integer.MAX_VALUE;
            for (HashMap.Entry<String, Integer> entry : usageMap.entrySet()) {
                int value = entry.getValue();
                if (min < value) {
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
