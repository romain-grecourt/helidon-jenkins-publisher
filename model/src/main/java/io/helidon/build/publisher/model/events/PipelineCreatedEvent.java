package io.helidon.build.publisher.model.events;

import io.helidon.build.publisher.model.PipelineInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#PIPELINE_CREATED} event.
 */
@JsonPropertyOrder({"runId", "info"})
public final class PipelineCreatedEvent extends PipelineEvent {

    final PipelineInfo info;
    final long startTime;

    /**
     * Create a new {@link PipelineEventType#PIPELINE_CREATED} event.
     *
     * @param info pipeline info
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
    public String toString() {
        return PipelineCreatedEvent.class.getSimpleName() + "{"
                + " runId=" + runId
                + ", info=" + info
                + " }";
    }
}
