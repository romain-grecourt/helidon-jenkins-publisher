/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#ARTIFACTS_INFO} event.
 */
@JsonPropertyOrder({"runId", "eventType", "stageId"})
public final class ArtifactsInfoEvent extends PipelineEvent {

    final int stepsId;
    final int count;

    /**
     * Create a new {@link PipelineEventType#ARTIFACTS_INFO} event.
     *
     * @param runId runId
     * @param stepsId the corresponding steps stage id
     * @param count the count of archived files
     */
    public ArtifactsInfoEvent(@JsonProperty("runId") String runId, @JsonProperty("stepsId") int stepsId,
            @JsonProperty("files") int count) {
        super(runId);
        this.stepsId = stepsId;
        this.count = count;
    }

    /**
     * Get the steps id.
     *
     * @return String
     */
    @JsonProperty
    public int stepsId() {
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
        int hash = 7;
        hash = 29 * hash + this.stepsId;
        hash = 29 * hash + this.count;
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
        if (this.stepsId != other.stepsId) {
            return false;
        }
        return this.count == other.count;
    }

    @Override
    public String toString() {
        return ArtifactsInfoEvent.class.getSimpleName() + "{"
                + " runId=" + runId
                + ", stepsId=" + stepsId
                + ", count=" + count
                + " }";
    }
}
