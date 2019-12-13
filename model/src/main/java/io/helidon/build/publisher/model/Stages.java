package io.helidon.build.publisher.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A stage node with nested stage nodes.
 */
@JsonPropertyOrder({"id", "type", "name", "state", "result", "startTime", "endTime", "children"})
public abstract class Stages extends Stage {

    final LinkedList<Stage> children = new LinkedList<>();

    /**
     * Create a new non parented stages.
     *
     * @param info pipeline info
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if info, status or timings is {@code null}
     */
    protected Stages(PipelineInfo info, Status status, Timings timings) {
        super(info, status, timings);
    }

    /**
     * Create a new stages.
     *
     * @param parent parent node, must not be {@code null}
     * @param name node name, may be {@code null}
     * @param path node path
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if parent, status or timings is {@code null}
     */
    protected Stages(Node parent, String name, String path, Status status, Timings timings) {
        super(parent, name, path, status, timings);
    }

    /**
     * Create a new stages.
     *
     * @param id node id, must not be used in the parent graph
     * @param parent parent node, must not be {@code null}
     * @param name node name, may be {@code null}
     * @param path stage node path
     * @param status the status object
     * @param timings the timings object
     * @throws NullPointerException if parent, status or timings is {@code null}
     */
    protected Stages(int id, Node parent, String name, String path, Status status, Timings timings) {
        super(id, parent, name, path, status, timings);
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
    public final List<Stage> children() {
        return Collections.unmodifiableList(children);
    }
}
