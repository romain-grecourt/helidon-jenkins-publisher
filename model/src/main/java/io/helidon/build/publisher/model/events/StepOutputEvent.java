package io.helidon.build.publisher.model.events;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#STEP_OUTPUT} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "stepId"})
public final class StepOutputEvent extends PipelineEvent {

    final int stepId;

    /**
     * Create a new {@link PipelineEventType#OUTPUT} event.
     *
     * @param pipelineId pipelineId
     * @param stepId the corresponding step id
     */
    public StepOutputEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("stepId") int stepId) {
        super(pipelineId);
        this.stepId = stepId;
    }

    /**
     * Get the step id.
     *
     * @return String
     */
    @JsonProperty
    public int stepId() {
        return stepId;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.STEP_OUTPUT;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.pipelineId);
        hash = 47 * hash + Objects.hashCode(this.stepId);
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
        final StepOutputEvent other = (StepOutputEvent) obj;
        if (!Objects.equals(this.pipelineId, other.pipelineId)) {
            return false;
        }
        return Objects.equals(this.stepId, other.stepId);
    }

    @Override
    public String toString() {
        return StepOutputEvent.class.getSimpleName() + "{"
                + " stepId=" + stepId
                + " }";
    }
}
