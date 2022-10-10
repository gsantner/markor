package net.gsantner.markor.format.todotxt;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TodoTxtQuerySyntaxTests {

    private String strip(final String in) {
        return in.replace(" ", "");
    }

    @Test
    public void ParseQuery() {
        final String query = "(A | B | C) & !+work & overdue & future";
        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(A) 2000-01-01 go to +work"), query)).isEqualTo(strip("(T | F | F) & !T & F & F"));
        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(B) 2000-01-01 go to +work due:2000-01-01"), query)).isEqualTo(strip("(F | T | F) & !T & T & F"));
        assertThat(TodoTxtFilter.parseQuery(new TodoTxtTask("(D) 2000-01-01 go to work due:9999-01-01"), query)).isEqualTo(strip("(F | T | F) & !T & F & T"));
    }

    @Test
    public void EvaluateExpression() {
        assertThat(TodoTxtFilter.shuntingYard(strip("T"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.shuntingYard(strip("!T"))).isEqualTo(false);
        assertThat(TodoTxtFilter.shuntingYard(strip("!F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("!(F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("(!F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("(!(F))"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("!(!(F))"))).isEqualTo(false);
        assertThat(TodoTxtFilter.shuntingYard(strip("T | F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("T & F"))).isEqualTo(false);
        assertThat(TodoTxtFilter.shuntingYard(strip("T | T | T | T | F"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("F | F | F | F | T"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("!(T | F) & (T | F)"))).isEqualTo(false);
        assertThat(TodoTxtFilter.shuntingYard(strip("!(T | F) | (T | F)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("!(T | F | F) & (T | F) | (F & (!T) | T)"))).isEqualTo(true);
        assertThat(TodoTxtFilter.shuntingYard(strip("!!!!!!F"))).isEqualTo(false);
    }
}
