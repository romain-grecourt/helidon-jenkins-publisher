package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 * {@link PipelineEventType#STEP_OUTPUT} event.
 */
@JsonPropertyOrder({"runId", "eventType", "stepId"})
public final class StepOutputEvent extends PipelineEvent {

    final int stepId;

    /**
     * Create a new {@link PipelineEventType#OUTPUT} event.
     *
     * @param runId runId
     * @param stepId the corresponding step id
     */
    public StepOutputEvent(@JsonProperty("runId") String runId, @JsonProperty("stepId") int stepId) {
        super(runId);
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
        hash = 47 * hash + Objects.hashCode(this.runId);
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
        if (!Objects.equals(this.runId, other.runId)) {
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
