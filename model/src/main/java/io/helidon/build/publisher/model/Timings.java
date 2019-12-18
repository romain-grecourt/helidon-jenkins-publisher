package io.helidon.build.publisher.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Timings.
 */
public class Timings {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    final String date;
    final long startTime;
    protected long endTime;

    /**
     * Create a new timings instance.
     *
     * @param startTime start timestamp
     */
    public Timings(long startTime) {
        this.startTime = startTime > 0 ? startTime : System.currentTimeMillis();
        endTime = 0;
        date = DATE_FORMATTER.format(Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()));
    }

    /**
     * Create a new timings instance.
     */
    public Timings() {
        this(System.currentTimeMillis());
    }

    /**
     * Create a new timings instance.
     *
     * @param startTime start timestamp
     * @param endTime end timestamp
     * @throws IllegalArgumentException if startTime is not a positive value
     */
    public Timings(long startTime, long endTime) {
        this(startTime);
        this.endTime = endTime;
    }

    /**
     * Create a new timings instance.
     *
     * @param date the date
     * @param duration duration in seconds
     * @throws IllegalArgumentException if startTime is not a positive value
     */
    Timings(String date, long duration) {
        this.date = Objects.requireNonNull(date, "date is null!");
        this.startTime = Instant.from(DATE_FORMATTER.parse(date)).toEpochMilli();
        this.endTime = duration == 0 ? 0 : startTime + (duration * 1000);
    }

    /**
     * Get the date.
     *
     * @return String
     */
    public final String date() {
        return date;
    }

    /**
     * Set the end timestamp from the given duration in seconds.
     * @param duration duration in seconds
     */
    public void duration(long duration) {
        endTime = startTime + (duration * 1000);
    }

    /**
     * Get the duration in seconds
     * @return long
     */
    public final long duration() {
        return endTime > startTime? (endTime - startTime) / 1000 : 0;
    }

    /**
     * Refresh the end timestamp.
     */
    protected void refresh() {
    }
}
