package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.helidon.build.publisher.model.Status;

/**
 * {@link PipelineEventType#STAGE_COMPLETED} event.
 */
@JsonPropertyOrder({"runId", "eventType", "id", "state", "result", "endTime"})
public final class StageCompletedEvent extends NodeCompletedEvent {

    /**
     * Create a new {@link PipelineEventType#STAGE_COMPLETED} event.
     *
     * @param runId runId
     * @param id node id
     * @param result node result
     * @param endTime node end timestamp
     */
    public StageCompletedEvent(@JsonProperty("runId") String runId, @JsonProperty("id") int id,
            @JsonProperty("result") Status.Result result, @JsonProperty("endTime") long endTime) {

        super(runId, PipelineEventType.STAGE_COMPLETED, id, result, endTime);
    }
}
