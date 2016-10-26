package de.spinscale.dropwizard.jobs.parser;

import org.junit.Test;

import static de.spinscale.dropwizard.jobs.parser.TimeParserUtil.parseDuration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class TimeParserUtilTest {

    @Test
    public void timeParserShouldWork() {
        assertThat(parseDuration("1mn"), is(60000L));
        assertThat(parseDuration("1min"), is(60000L));
        assertThat(parseDuration("10d"), is(864000000L));
        assertThat(parseDuration("20h"), is(72000000L));
        assertThat(parseDuration("40s"), is(40000L));
        assertThat(parseDuration("500ms"), is(500L));
    }

    @Test
    public void timeParserShouldThrowException() {
        shouldThrowException("1w");
        shouldThrowException("foo");
        shouldThrowException("0");
        shouldThrowException(null);
    }

    private void shouldThrowException(String duration) {
        try {
            parseDuration(duration);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail(String.format("Duration %s should have thrown an IllegalArgumentException", duration));
    }
}
