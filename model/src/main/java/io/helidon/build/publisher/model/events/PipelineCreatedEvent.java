package io.helidon.build.publisher.model.events;

import io.helidon.build.publisher.model.PipelineInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 * {@link PipelineEventType#PIPELINE_CREATED} event.
 */
@JsonPropertyOrder({"pipelineId", "info"})
public final class PipelineCreatedEvent extends PipelineEvent {

    final PipelineInfo info;
    final long startTime;

    /**
     * Create a new {@link PipelineEventType#PIPELINE_CREATED} event.
     *
     * @param info pipeline info
     * @param startTime pipeline start time
     */
    @JsonCreator
    public PipelineCreatedEvent(@JsonProperty("info") PipelineInfo info, @JsonProperty("startTime") long startTime) {
        super(info.id());
        this.info = info;
        this.startTime = startTime;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.PIPELINE_CREATED;
    }

    /**
     * Get the pipeline info.
     *
     * @return PipelineInfo
     */
    @JsonProperty
    public PipelineInfo info() {
        return info;
    }

    /**
     * Get the start timestamp.
     *
     * @return long
     */
    @JsonProperty
    public long startTime() {
        return startTime;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.info);
        hash = 83 * hash + (int) (this.startTime ^ (this.startTime >>> 32));
        return hash;
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
        final PipelineCreatedEvent other = (PipelineCreatedEvent) obj;
        if (this.startTime != other.startTime) {
            return false;
        }
        return Objects.equals(this.info, other.info);
    }

    @Override
    public String toString() {
        return PipelineCreatedEvent.class.getSimpleName() + "{"
                + " pipelineId=" + pipelineId
                + ", startTime=" + startTime
                + " }";
    }
}
