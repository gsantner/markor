package net.gsantner.markor.format.csv;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvMatcherTest {

    @Test
    public void patternCsv() {
        Pattern csvMatcher = CsvMatcher.patternCsv(new CsvConfig(';', '\''));
        //             0123456 78901 23456789012345678
        String line = "number;'text';finishing date;";
        Matcher matcher = csvMatcher.matcher(line);
        matcher.find(); assertEquals("number", matcher.group());
        matcher.find(); assertEquals(";'text'", matcher.group());
        matcher.find(); assertEquals(";finishing date", matcher.group());
        matcher.find(); assertEquals(";", matcher.group());
        assertFalse(matcher.find());
    }
}