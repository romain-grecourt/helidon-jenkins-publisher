package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.helidon.build.publisher.model.Status;

/**
 * {@link PipelineEventType#STEP_COMPLETED} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "id", "state", "result", "endTime"})
public final class StepCompletedEvent extends NodeCompletedEvent {

    /**
     * Create a new {@link PipelineEventType#STEP_COMPLETED} event.
     *
     * @param pipelineId pipelineId
     * @param id node id
     * @param result node result
     * @param endTime node end timestamp
     */
    public StepCompletedEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("id") int id,
            @JsonProperty("result") Status.Result result, @JsonProperty("endTime") long endTime) {

        super(pipelineId, PipelineEventType.STEP_COMPLETED, id, result, endTime);
    }
}
