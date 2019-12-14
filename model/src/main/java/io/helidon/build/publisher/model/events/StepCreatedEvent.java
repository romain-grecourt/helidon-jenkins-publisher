package io.helidon.build.publisher.model.events;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#STEP_CREATED} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "id", "parentId", "index", "name", "startTime", "state", "args"})
public final class StepCreatedEvent extends NodeCreatedEvent {

    final String args;

    /**
     * Create a new {@link PipelineEventType#STEP_CREATED} event.
     *
     * @param pipelineId pipeline id
     * @param id node id
     * @param parentId node parent id
     * @param index index in the parent node
     * @param name node name
     * @param startTime start timestamp
     * @param args step arguments
     */
    public StepCreatedEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("id") String id,
            @JsonProperty("parentId") String parentId, @JsonProperty("index") int index, @JsonProperty("name") String name,
            @JsonProperty("startTime") long startTime, @JsonProperty("args") String args) {

        super(pipelineId, id, parentId, index, name, startTime);
        this.args = args;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.STEP_CREATED;
    }

    /**
     * Get the step arguments.
     *
     * @return String
     */
    @JsonProperty
    public String args() {
        return args;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StepCreatedEvent other = (StepCreatedEvent) obj;
        if (!Objects.equals(this.args, other.args)) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        // hash code based on the node id only
        return super.hashCode();
    }

    @Override
    public String toString() {
        return StepCreatedEvent.class.getSimpleName() + "{"
                + " pipelineId=" + pipelineId
                + ", id=" + id
                + ", parentId=" + parentId
                + ", index=" + index
                + ", name=" + name
                + ", args=" + args
                + ", startTime=" + startTime
                + " }";
    }
}
