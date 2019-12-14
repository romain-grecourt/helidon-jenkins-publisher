package io.helidon.build.publisher.model.events;

import java.util.Objects;

import io.helidon.build.publisher.model.Stage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#STAGE_CREATED} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "id", "parentId", "index", "name", "startTime", "state", "stageType"})
public final class StageCreatedEvent extends NodeCreatedEvent {

    final Stage.StageType stageType;

    /**
     * Create a new {@link PipelineEventType#STAGE_CREATED} event.
     *
     * @param pipelineId pipelineId
     * @param id node id
     * @param parentId node parent id
     * @param index index in the parent node
     * @param name node name
     * @param startTime start timestamp
     * @param stageType stage type
     */
    public StageCreatedEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("id") String id,
            @JsonProperty("parentId") String parentId, @JsonProperty("index") int index, @JsonProperty("name") String name,
            @JsonProperty("startTime") long startTime,
            @JsonProperty("stageType") Stage.StageType stageType) {

        super(pipelineId, id, parentId, index, name, startTime);
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
        if (!Objects.equals(this.pipelineId, other.pipelineId)) {
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
                + " pipelineId=" + pipelineId
                + ", type=" + stageType
                + ", id=" + id
                + ", parentId=" + parentId
                + ", index=" + index
                + ", name=" + name
                + ", startTime=" + startTime
                + " }";
    }
}
