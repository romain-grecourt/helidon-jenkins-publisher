package io.helidon.build.publisher.model;

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
    public Timings(long startTime, long endTime) {
        this(startTime);
        this.endTime = endTime;
    }

    /**
     * Get the start timestamp.
     * @return long
     */
    public final long startTime() {
        return startTime;
    }

    /**
     * Refresh the end timestamp.
     */
    protected void refresh() {
    }
}
