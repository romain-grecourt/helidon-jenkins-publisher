package io.helidon.build.publisher.model.events;

import java.util.Objects;

import io.helidon.build.publisher.model.Status.Result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#PIPELINE_COMPLETED} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "state", "result", "endTime"})
public final class PipelineCompletedEvent extends PipelineEvent {

    final Result result;
    final long duration;

    /**
     * Create a new {@link PipelineEventType#PIPELINE_COMPLETED} event.
     *
     * @param pipelineId pipelineId
     * @param result pipeline result
     * @param duration pipeline duration
     */
    public PipelineCompletedEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("result") Result result,
            @JsonProperty("duration") long duration) {

        super(pipelineId);
        this.result = result;
        this.duration = duration;
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
     * Get the duration in seconds.
     *
     * @return long
     */
    @JsonProperty
    public long duration() {
        return duration;
    }

    @Override
    public int hashCode() {
        int hash = 8;
        hash = 89 * hash + Objects.hashCode(this.pipelineId);
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
        return Objects.equals(this.pipelineId, other.pipelineId);
    }

    @Override
    public String toString() {
        return PipelineCompletedEvent.class.getSimpleName() + "{"
                + " pipelineId=" + pipelineId
                + ", duration=" + duration
                + " }";
    }
}
