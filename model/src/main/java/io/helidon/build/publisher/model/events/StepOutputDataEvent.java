package io.helidon.build.publisher.model.events;

import java.util.Arrays;
import java.util.Objects;

/**
 * {@link PipelineEventType#STEP_OUTPUT_DATA} event.
 */
public final class StepOutputDataEvent extends PipelineEvent {

    final int stepId;
    final byte[] data;

    /**
     * Create a new {@link PipelineEventType#OUTPUT_DATA} event.
     *
     * @param pipelineId pipelineId
     * @param stepId the corresponding stepId
     * @param data the output data
     */
    public StepOutputDataEvent(String pipelineId, int stepId, byte[] data) {
        super(pipelineId);
        this.stepId = stepId;
        this.data = data;
    }

    /**
     * Get the step id.
     *
     * @return String
     */
    public int stepId() {
        return stepId;
    }

    /**
     * Get the output data.
     *
     * @return byte[]
     */
    public byte[] data() {
        return data;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.STEP_OUTPUT_DATA;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.pipelineId);
        hash = 53 * hash + Objects.hashCode(this.stepId);
        hash = 53 * hash + Arrays.hashCode(this.data);
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
        final StepOutputDataEvent other = (StepOutputDataEvent) obj;
        if (!Objects.equals(this.pipelineId, other.pipelineId)) {
            return false;
        }
        if (!Objects.equals(this.stepId, other.stepId)) {
            return false;
        }
        return Arrays.equals(this.data, other.data);
    }

    @Override
    public String toString() {
        return StepOutputDataEvent.class.getSimpleName() + "{"
                + " pipelineId=" + pipelineId
                + ", stepId=" + stepId
                + " }";
    }
}
