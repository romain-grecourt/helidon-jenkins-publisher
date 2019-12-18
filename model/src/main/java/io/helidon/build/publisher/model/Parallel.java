package io.helidon.build.publisher.model;

import io.helidon.build.publisher.model.events.StageCompletedEvent;
import java.util.Objects;

/**
 * A set of stages running in parallel.
 */
public final class Parallel extends Stages {

    /**
     * Create a new parallel stages.
     *
     * @param parent parent stage, may be {@code null}
     * @param name stage name, may be {@code null}
     * @param status the status object
     * @param timings the timings object
     * @throws IllegalArgumentException if path does not start with a {@code /}
     * @throws NullPointerException if parent, status or timings is {@code null}
     */
    public Parallel(Node parent, String name, Status status, Timings timings) {
        super(parent, name, Objects.requireNonNull(parent, "parent is null!").path, status, timings);
    }

    Parallel(Node parent, String id, String name, Status status, Timings timings) {
        super(parent, id, name, Objects.requireNonNull(parent, "parent is null!").path, status, timings);
    }

    @Override
    public String type() {
        return "PARALLEL";
    }

    @Override
    public void fireCompleted() {
        Status.Result result = Status.Result.SUCCESS;
        long endTime = 0;
        // result is the worst nested result
        // endTime is the longest nested endTime
        for (Stage stage : children) {
            stage.fireCompleted();
            Status.Result res = stage.status.result;
            if (res.ordinal() < result.ordinal()) {
                result = res;
            }
            if (stage.timings.endTime > endTime) {
                endTime = stage.timings.endTime;
            }
        }
        status.state = Status.State.FINISHED;
        status.result = result;
        timings.endTime = endTime > 0 ? endTime : System.currentTimeMillis();
        fireEvent(new StageCompletedEvent(info.id, id, status.result, timings.duration()));
    }
}
