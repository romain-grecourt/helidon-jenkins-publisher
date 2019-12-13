package io.helidon.build.publisher.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.helidon.build.publisher.model.events.StageCompletedEvent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A steps stage.
 */
@JsonPropertyOrder({"id", "type", "state", "result", "startTime", "endTime", "artifacts", "tests", "children"})
public final class Steps extends Stage {

    final List<Step> children = new LinkedList<>();
    int artifacts = 0;
    TestsInfo tests;

    /**
     * Create a new steps stage.
     *
     * @param parent the parent stage that this step is part of
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if type, status or timings is {@code null}
     */
    public Steps(Node parent, Status status, Timings timings) {
        super(parent, null, null, status, timings);
    }

    Steps(int id, Node parent, Status status, Timings timings) {
        super(id, parent, null, null, status, timings);
    }

    @JsonIgnore // steps is synthetic, it doesn't have a name
    @Override
    public String name() {
        return null;
    }

    /**
     * Get the artifacts count.
     *
     * @return int
     */
    @JsonProperty
    public final int artifacts() {
        return artifacts;
    }

    /**
     * Get the test results info.
     *
     * @return TestsInfo or {@code null} if this steps stage has no tests
     */
    @JsonProperty
    public TestsInfo tests() {
        return tests;
    }

    @Override
    public StageType type() {
        return StageType.STEPS;
    }

    /**
     * Get the steps in sequence order.
     *
     * @return immutable list of {@link Step}
     */
    @JsonProperty
    public List<Step> children() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Add a nested step.
     *
     * @param step to add
     */
    public void addStep(Step step) {
        children.add(step);
    }

    @Override
    public void fireCompleted() {
        Step last = null;
        if (!children.isEmpty()) {
            last = children.get(children.size() - 1);
            last.fireCompleted();
        }
        status.state = Status.State.FINISHED;
        status.result = last != null ? last.result() : Status.Result.SUCCESS;
        timings.endTime = last != null ? last.endTime() : System.currentTimeMillis();
        fireEvent(new StageCompletedEvent(info.id, id, status.result, timings.endTime));
    }
}
