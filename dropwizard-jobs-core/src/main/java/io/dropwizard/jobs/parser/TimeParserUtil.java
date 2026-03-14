package io.dropwizard.jobs.parser;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This code is ripped of the playframework, see original here
 * https://github.com/playframework/play/blob/master/framework/src/play/libs/Time.java
 */
public class TimeParserUtil {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^([0-9]+)([a-z]+)$");

    private static final List<Entry<Pattern, Long>> UNIT_MULTIPLIERS = List.of(
        new SimpleEntry<>(Pattern.compile("^d$"), 86_400_000L),      // days
        new SimpleEntry<>(Pattern.compile("^h$"), 3_600_000L),       // hours
        new SimpleEntry<>(Pattern.compile("^mi?n?$"), 60_000L),      // minutes (m, mn, min)
        new SimpleEntry<>(Pattern.compile("^s$"), 1_000L),           // seconds
        new SimpleEntry<>(Pattern.compile("^ms$"), 1L)               // milliseconds
    );

    /**
     * Parse a duration
     * 
     * @param duration
     *            1d, 3h, 2mn, 7s, 50ms
     * @return The number of milliseconds
     */
    public static long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            throw new IllegalArgumentException("duration may not be null");
        }
        
        Matcher matcher = DURATION_PATTERN.matcher(duration);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration pattern : " + duration);
        }
        
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        
        for (Entry<Pattern, Long> entry : UNIT_MULTIPLIERS) {
            if (entry.getKey().matcher(unit).matches()) {
                return value * entry.getValue();
            }
        }
        
        throw new IllegalArgumentException("Invalid duration pattern : " + duration);
    }

}
