package io.helidon.build.publisher.model.events;

import java.util.Objects;

import io.helidon.build.publisher.model.Status.Result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#PIPELINE_COMPLETED} event.
 */
@JsonPropertyOrder({"runId", "eventType", "state", "result", "endTime"})
public final class PipelineCompletedEvent extends PipelineEvent {

    final Result result;
    final long endTime;

    /**
     * Create a new {@link PipelineEventType#PIPELINE_COMPLETED} event.
     *
     * @param runId runId
     * @param result pipeline result
     * @param endTime pipeline end timestamp
     */
    public PipelineCompletedEvent(@JsonProperty("runId") String runId, @JsonProperty("result") Result result,
            @JsonProperty("endTime") long endTime) {

        super(runId);
        this.result = result;
        this.endTime = endTime;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.PIPELINE_COMPLETED;
    }

    /**
     * Get the result.
     *
     * @return Result
     */
    @JsonProperty
    public Result result() {
        return result;
    }

    /**
     * Get the start timestamp.
     *
     * @return long
     */
    @JsonProperty
    public long endTime() {
        return endTime;
    }

    @Override
    public int hashCode() {
        int hash = 8;
        hash = 89 * hash + Objects.hashCode(this.runId);
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
        final PipelineCompletedEvent other = (PipelineCompletedEvent) obj;
        return Objects.equals(this.runId, other.runId);
    }

    @Override
    public String toString() {
        return PipelineCompletedEvent.class.getSimpleName() + "{"
                + " runId=" + runId
                + " }";
    }
}
