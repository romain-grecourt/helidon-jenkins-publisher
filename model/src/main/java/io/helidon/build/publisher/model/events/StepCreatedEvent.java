package io.helidon.build.publisher.model.events;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#STEP_CREATED} event.
 */
@JsonPropertyOrder({"runId", "eventType", "id", "parentId", "index", "name", "startTime", "state", "args", "declared"})
public final class StepCreatedEvent extends NodeCreatedEvent {

    final String args;
    final boolean meta;
    final boolean declared;

    /**
     * Create a new {@link PipelineEventType#STEP_CREATED} event.
     *
     * @param runId run id
     * @param id node id
     * @param parentId node parent id
     * @param index index in the parent node
     * @param name node name
     * @param startTime start timestamp
     * @param args step arguments
     * @param meta step meta flag
     * @param declared step declared flag
     */
    public StepCreatedEvent(@JsonProperty("runId") String runId, @JsonProperty("id") int id,
            @JsonProperty("parentId") int parentId, @JsonProperty("index") int index, @JsonProperty("name") String name,
            @JsonProperty("startTime") long startTime, @JsonProperty("args") String args,
            @JsonProperty("meta") boolean meta, @JsonProperty("declared") boolean declared) {

        super(runId, id, parentId, index, name, startTime);
        this.args = args;
        this.meta = meta;
        this.declared = declared;
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

    /**
     * Is the step declared.
     *
     * @return {@code boolean}
     */
    @JsonProperty
    public boolean declared() {
        return declared;
    }

    /**
     * Is the step meta.
     *
     * @return {@code boolean}
     */
    @JsonProperty
    public boolean meta() {
        return meta;
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
        if (this.meta != other.meta) {
            return false;
        }
        if (this.declared != other.declared) {
            return false;
        }
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
                + " runId=" + runId
                + ", id=" + id
                + ", parentId=" + parentId
                + ", index=" + index
                + ", name=" + name
                + ", args=" + args
                + ", meta=" + meta
                + ", declared=" + declared
                + ", startTime=" + startTime
                + " }";
    }
}
