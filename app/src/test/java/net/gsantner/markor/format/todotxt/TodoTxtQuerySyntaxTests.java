package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TodoTxtQuerySyntaxTests {

    private String strip(final String in) {
        return in.replace(" ", "");
    }

    @Test
    public void ParseQuery() {
        final String query = "(pri:A | pri:B | pri:C) & !+work & due< & due> | !+ | @";

        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(A) 2000-01-01 go to +work"), query))
                .isEqualTo(strip("(T | F | F) & !T & F & F | !T | F"));

        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(B) 2000-01-01 go to +work due:2000-01-01"), query))
                .isEqualTo(strip("(F | T | F) & !T & T & F | !T | F"));

        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(D) 2000-01-01 go to @work due:9999-01-01"), query))
                .isEqualTo(strip("(F | F | F) & !F & F & T| !F | T"));
    }

    @Test
    public void EvaluateExpressionTest() {
        assertThat(TodoTxtFilter.evaluateExpression(strip("T"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!T"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("(!F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("(!(F))"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(!(F))"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("T | F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("T & F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("T | T | T | T | F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("F | F | F | F | T"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(T | F) & (T | F)"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(T | F) | (T | F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!(T | F | F) & (T | F) | (F & (!T) | T)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.evaluateExpression(strip("!!!!!!F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("T | T | T & F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.evaluateExpression(strip("F & F & F | T & T"))).isEqualTo(true);
    }
}
