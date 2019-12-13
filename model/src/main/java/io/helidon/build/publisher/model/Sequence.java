package io.helidon.build.publisher.model;

import io.helidon.build.publisher.model.events.StageCompletedEvent;

/**
 * An ordered sequence of stages.
 */
public final class Sequence extends Stages {

    /**
     * Create a new sequence stages.
     *
     * @param parent parent stage, may be {@code null}
     * @param name stage name, may be {@code null}
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if status or timings is {@code null}
     */
    public Sequence(Node parent, String name, Status status, Timings timings) {
        super(StageType.SEQUENCE, parent, name, status, timings);
    }

    Sequence(int id, Node parent, String name, Status status, Timings timings) {
        super(StageType.SEQUENCE, id, parent, name, status, timings);
    }

    @Override
    public void fireCompleted() {
        Stage last = null;
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
