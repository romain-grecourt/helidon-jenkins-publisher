package io.helidon.build.publisher.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Timings.
 */
public class Timings {

    final long startTime;
    protected long endTime;

    /**
     * Create a new timings instance.
     */
    public Timings() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Create a new timings instance.
     * @param startTime start timestamp
     */
    public Timings(long startTime) {
        this.startTime = startTime > 0 ? startTime : System.currentTimeMillis();
        this.endTime = 0;
    }

    /**
     * Create a new timings instance.
     * @param startTime start timestamp
     * @param endTime end timestamp
     * @throws IllegalArgumentException if startTime is not a positive value
     */
    @JsonCreator
    public Timings(@JsonProperty("startTime") long startTime, @JsonProperty("endTime") long endTime) {
        this(startTime);
        this.endTime = endTime;
    }

    /**
     * Get the start timestamp.
     * @return long
     */
    @JsonProperty
    public final long startTime() {
        return startTime;
    }

    @JsonProperty
    public final long endTime() {
        return endTime;
    }

    /**
     * Refresh the end timestamp.
     */
    protected void refresh() {
    }
}
