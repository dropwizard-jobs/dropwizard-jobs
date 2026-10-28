package io.dropwizard.jobs.parser;

import static io.dropwizard.jobs.parser.TimeParserUtil.parseDuration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TimeParserUtilTest {

    @Test
    public void timeParserShouldWork() {
        assertThat(parseDuration("1m"), is(60000L));
        assertThat(parseDuration("2mn"), is(120000L));
        assertThat(parseDuration("3min"), is(180000L));
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

    @Test
    public void rejectsMultiplicationOverflowInsteadOfWrapping() {
        for (String duration : java.util.List.of(Long.MAX_VALUE + "s", "106751991168d")) {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> parseDuration(duration), duration);
            assertInstanceOf(ArithmeticException.class, failure.getCause());
            assertTrue(failure.getMessage().contains(duration));
        }
    }

    @Test
    public void acceptsLargestRepresentableAndZeroDurations() {
        assertEquals(Long.MAX_VALUE, parseDuration(Long.MAX_VALUE + "ms"));
        long seconds = Long.MAX_VALUE / 1_000;
        assertEquals(seconds * 1_000, parseDuration(seconds + "s"));
        assertEquals(0, parseDuration("0ms"));
    }

    @Test
    public void rejectsMalformedAndOutOfRangeNumbers() {
        for (String duration : java.util.List.of("", " 1s", "1s ", "-1s", "+1s", "1.5s", "1S", "9223372036854775808ms")) {
            assertThrows(IllegalArgumentException.class, () -> parseDuration(duration), duration);
        }
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
