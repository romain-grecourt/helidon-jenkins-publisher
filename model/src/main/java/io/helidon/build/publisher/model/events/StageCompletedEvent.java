package io.helidon.build.publisher.model.events;

import io.helidon.build.publisher.model.Status;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#STAGE_COMPLETED} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "id", "state", "result", "duration"})
public final class StageCompletedEvent extends NodeCompletedEvent {

    /**
     * Create a new {@link PipelineEventType#STAGE_COMPLETED} event.
     *
     * @param pipelineId pipelineId
     * @param id node id
     * @param result node result
     * @param duration node duration
     */
    public StageCompletedEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("id") String id,
            @JsonProperty("result") Status.Result result, @JsonProperty("duration") long duration) {

        super(pipelineId, PipelineEventType.STAGE_COMPLETED, id, result, duration);
    }
}
