package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.helidon.build.publisher.model.Stage;
import java.util.Objects;

/**
 * {@link PipelineEventType#STAGE_CREATED} event.
 */
@JsonPropertyOrder({"runId", "eventType", "id", "parentId", "index", "name", "startTime", "state", "stageType"})
public final class StageCreatedEvent extends NodeCreatedEvent {

    final Stage.StageType stageType;

    /**
     * Create a new {@link PipelineEventType#STAGE_CREATED} event.
     *
     * @param id node id
     * @param runId runId
     * @param parentId node parent id
     * @param index index in the parent node
     * @param name node name
     * @param startTime start timestamp
     * @param stageType stage type
     */
    public StageCreatedEvent(@JsonProperty("runId") String runId, @JsonProperty("id") int id,
            @JsonProperty("parentId") int parentId, @JsonProperty("index") int index, @JsonProperty("name") String name,
            @JsonProperty("startTime") long startTime,
            @JsonProperty("stageType") Stage.StageType stageType) {

        super(runId, id, parentId, index, name, startTime);
        this.stageType = stageType;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.STAGE_CREATED;
    }

    /**
     * Get the stage type.
     *
     * @return Stage.Type
     */
    @JsonProperty
    public Stage.StageType stageType() {
        return stageType;
    }

    @Override
    public int hashCode() {
        // hash code based on the node id only
        return super.hashCode();
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
        final StageCreatedEvent other = (StageCreatedEvent) obj;
        if (!Objects.equals(this.runId, other.runId)) {
            return false;
        }
        if (this.stageType != other.stageType) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return StageCreatedEvent.class.getSimpleName() + "{"
                + " runId=" + runId
                + ", type=" + stageType
                + ", id=" + id
                + ", parentId=" + parentId
                + ", index=" + index
                + ", name=" + name
                + ", startTime=" + startTime
                + " }";
    }
}
