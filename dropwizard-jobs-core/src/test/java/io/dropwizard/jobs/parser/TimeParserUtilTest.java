package io.dropwizard.jobs.parser;

import static io.dropwizard.jobs.parser.TimeParserUtil.parseDuration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

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
    public void timeParserShouldCheckUnitBoundaries() {
        Map.of("d", 86_400_000L, "h", 3_600_000L, "m", 60_000L, "mn", 60_000L, "min", 60_000L, "s", 1_000L)
                .forEach((unit, multiplier) -> {
                    long maximum = Long.MAX_VALUE / multiplier;
                    assertThat(parseDuration(maximum + unit), is(maximum * multiplier));
                    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                            () -> parseDuration((maximum + 1) + unit));
                    assertInstanceOf(ArithmeticException.class, failure.getCause());
                    assertThrows(IllegalArgumentException.class, () -> parseDuration(Long.MAX_VALUE + unit));
                });
    }

    @Test
    public void timeParserShouldPreserveMaximumMillisecondsAndZeroDelays() {
        assertThat(parseDuration(Long.MAX_VALUE + "ms"), is(Long.MAX_VALUE));
        assertThat(parseDuration("0ms"), is(0L));
        assertThat(parseDuration("0d"), is(0L));
    }

    @Test
    public void timeParserShouldRejectNumbersOutsideLongRange() {
        assertThrows(IllegalArgumentException.class, () -> parseDuration("9223372036854775808ms"));
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
