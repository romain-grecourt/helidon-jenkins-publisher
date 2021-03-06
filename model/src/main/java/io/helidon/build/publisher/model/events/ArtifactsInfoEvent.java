package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 * {@link PipelineEventType#ARTIFACTS_INFO} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "stageId"})
public final class ArtifactsInfoEvent extends PipelineEvent {

    final String stepsId;
    final int count;

    /**
     * Create a new {@link PipelineEventType#ARTIFACTS_INFO} event.
     *
     * @param pipelineId pipeline id
     * @param stepsId the corresponding steps stage id
     * @param count the count of archived files
     */
    public ArtifactsInfoEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("stepsId") String stepsId,
            @JsonProperty("files") int count) {

        super(pipelineId);
        this.stepsId = stepsId;
        this.count = count;
    }

    /**
     * Get the steps id.
     *
     * @return String
     */
    @JsonProperty
    public String stepsId() {
        return stepsId;
    }

    /**
     * Get the count of archived file names.
     *
     * @return int
     */
    @JsonProperty
    public int count() {
        return count;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.ARTIFACTS_INFO;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.stepsId);
        hash = 53 * hash + this.count;
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
        final ArtifactsInfoEvent other = (ArtifactsInfoEvent) obj;
        if (this.count != other.count) {
            return false;
        }
        return Objects.equals(this.stepsId, other.stepsId);
    }

    @Override
    public String toString() {
        return ArtifactsInfoEvent.class.getSimpleName() + "{"
                + " pipelineId=" + pipelineId
                + ", stepsId=" + stepsId
                + ", count=" + count
                + " }";
    }
}
