package io.helidon.build.publisher.model;

/**
 * Timings.
 */
public class Timings {

    final long startTime;
    protected long endTime;

    /**
     * Create a new timings instance.
     * @param startTime start timestamp
     * @throws IllegalArgumentException if startTime is not a positive value
     */
    public Timings(long startTime) {
        if (startTime <= 0) {
            throw new IllegalArgumentException("Invalid startTime: " + startTime);
        }
        this.startTime = startTime;
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
     * Get the start time.
     * @return long
     */
    public final long startTime() {
        return startTime;
    }

    /**
     * Get the end time.
     * @param node the node to compute the endTime of
     * @return long
     */
    public final long endTime(Pipeline.Node node) {
        if (endTime == 0) {
            long computed = computeEndTime();
            if (computed > 0) {
                endTime = computed;
            } else {
                Pipeline.Node next = node.next();
                if (next != null) {
                    endTime = next.startTime();
                }
            }
        }
        return endTime;
    }

    /**
     * Implementations should override this method.
     * @return {@code 0}
     */
    protected long computeEndTime() {
        return 0;
    }
}
