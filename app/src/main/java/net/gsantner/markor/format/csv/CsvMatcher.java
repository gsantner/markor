package net.gsantner.markor.format.csv;

import java.util.regex.Pattern;

public class CsvMatcher {
    // finds leading delimitter until next delimmiter
    // from https://stackoverflow.com/questions/1441556/parsing-csv-input-with-a-regex-in-java
    private final static String regExprCsv = "(?:^|;)\\s*(?:(?:(?=\")\"([^\"].*?)\")|(?:(?!\")(.*?)))(?=;|$|\n)";

    public static  Pattern patternCsv(CsvConfig config) {
        return Pattern.compile(
                regExprCsv
                        .replace(CsvConfig.DEFAULT.getFieldDelimiterChar(), config.getFieldDelimiterChar())
                        .replace(CsvConfig.DEFAULT.getQuoteChar(), config.getQuoteChar()));
    }

}
