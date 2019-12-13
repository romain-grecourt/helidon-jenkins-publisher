package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.helidon.build.publisher.model.Status;

/**
 * {@link PipelineEventType#STEP_COMPLETED} event.
 */
@JsonPropertyOrder({"runId", "eventType", "id", "state", "result", "endTime"})
public final class StepCompletedEvent extends NodeCompletedEvent {

    /**
     * Create a new {@link PipelineEventType#STEP_COMPLETED} event.
     *
     * @param runId runId
     * @param id node id
     * @param result node result
     * @param endTime node end timestamp
     */
    public StepCompletedEvent(@JsonProperty("runId") String runId, @JsonProperty("id") int id,
            @JsonProperty("result") Status.Result result, @JsonProperty("endTime") long endTime) {

        super(runId, PipelineEventType.STEP_COMPLETED, id, result, endTime);
    }
}
