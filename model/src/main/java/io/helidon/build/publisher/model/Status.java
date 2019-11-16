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
        QUEUED,
        RUNNING,
        FINISHED
    }

    protected Result result;
    protected State state;

    /**
     * Create a new status.
     */
    protected Status() {
        this.state = State.QUEUED;
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
     * Compute the result.
     * @param node the node to compute the result of
     * @return Result
     */
    public final Result result(Pipeline.Node node) {
        if (state != State.FINISHED) {
            refresh(node);
        }
        return result;
    }

    /**
     * Compute the state.
     * @param node the node to compute the state of
     * @return State
     */
    public final State state(Pipeline.Node node) {
        if (state != State.FINISHED) {
            refresh(node);
        }
        return state;
    }

    /**
     * refresh the status for the given node.
     * @param node the node to compute the status of
     */
    protected void refresh(Pipeline.Node node) {
        if (node instanceof Pipeline.Stage) {
            Status delegate = ((Pipeline.Stage)node).status();
            state = delegate.state;
            result = delegate.result;
        }
    }

    @Override
    public String toString() {
        return Status.class.getSimpleName() + "{"
                + " state=" + state
                + ", result=" + result
                + "}";
    }
}
