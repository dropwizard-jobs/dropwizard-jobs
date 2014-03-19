package de.spinscale.dropwizard.jobs.parser;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class TimeParserUtilTest {

    @Test
    public void timeParserShouldWork() {
        assertThat(getDuration(null),   is(2592000));
        assertThat(getDuration("1mn"),  is(60));
        assertThat(getDuration("1min"), is(60));
        assertThat(getDuration("10d"),  is(864000));
        assertThat(getDuration("20h"),  is(72000));
        assertThat(getDuration("40s"),  is(40));
    }

    @Test
    public void timeParserShouldThrowException() {
        shouldThrowException("1w");
        shouldThrowException("foo");
        shouldThrowException("0");
    }

    void shouldThrowException(String duration) {
        try {
            getDuration(duration);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail(String.format("Duration %s should have thrown an IllegalArgumentException", duration));
    }

    private int getDuration(String duration) {
        return TimeParserUtil.parseDuration(duration);
    }

}
