package io.helidon.build.publisher.model;

import java.util.Objects;

/**
 * Status.
 */
public class Status {

    /**
     * Result.
     */
    public enum Result {
        UNKNOWN,
        ABORTED,
        FAILURE,
        NOT_BUILT,
        UNSTABLE,
        SUCCESS;
    }

    /**
     * State.
     */
    public enum State {
        RUNNING,
        FINISHED
    }

    protected Result result;
    protected State state;

    /**
     * Create a new status.
     */
    protected Status() {
        this.state = State.RUNNING;
        this.result = Result.UNKNOWN;
    }

    /**
     * Create a new status.
     * @param state initial state
     * @throws NullPointerException if state is {@code null}
     */
    public Status(State state) {
        this.state = Objects.requireNonNull(state, "state is null");
        this.result = Result.UNKNOWN;
    }

    /**
     * Create a new status.
     * @param state the state
     * @param result the result
     * @throws NullPointerException if state is {@code null}
     */
    public Status(State state, Result result) {
        this.state = Objects.requireNonNull(state, "state is null");
        this.result = Objects.requireNonNull(result, "result is null");
    }

    /**
     * Refresh the status.
     */
    protected void refresh() {
    }

    @Override
    public String toString() {
        return Status.class.getSimpleName() + "{"
                + " state=" + state
                + ", result=" + result
                + "}";
    }
}
