package io.helidon.build.publisher.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A stage node with nested stage nodes.
 */
@JsonPropertyOrder({"id", "type", "name", "state", "result", "startTime", "endTime", "stages"})
public abstract class Stages extends Stage {

    final LinkedList<Stage> children = new LinkedList<>();

    /**
     * Create a new non parented stages.
     *
     * @param info pipeline info
     * @param type the stage type, must not be {@code null}
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if info, type, status or timings is {@code null}
     */
    protected Stages(PipelineInfo info, StageType type, Status status, Timings timings) {
        super(info, type, status, timings);
    }

    /**
     * Create a new stages.
     *
     * @param type stage type, must not be {@code null}
     * @param parent parent node, must not be {@code null}
     * @param name node name, may be {@code null}
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if type, parent, status or timings is {@code null}
     */
    protected Stages(StageType type, Node parent, String name, Status status, Timings timings) {
        super(type, parent, name, status, timings);
    }

    /**
     * Create a new stages.
     *
     * @param type stage type, must not be {@code null}
     * @param id node id, must not be used in the parent graph
     * @param parent parent node, must not be {@code null}
     * @param name node name, may be {@code null}
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if type, parent, status or timings is {@code null}
     */
    protected Stages(StageType type, int id, Node parent, String name, Status status, Timings timings) {
        super(type, id, parent, name, status, timings);
    }

    /**
     * Add a nested stage.
     *
     * @param stage stage to add
     */
    public final void addStage(Stage stage) {
        children.add(stage);
    }

    /**
     * Get the nested stages.
     *
     * @return immutable list of {@link Stage}
     */
    @JsonProperty
    public final List<Stage> chidren() {
        return Collections.unmodifiableList(children);
    }
}
