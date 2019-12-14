package io.helidon.build.publisher.model.events;

import java.util.Objects;

import io.helidon.build.publisher.model.TestsInfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#TESTS_INFO} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "stageId"})
public final class TestsInfoEvent extends PipelineEvent {

    final String stepsId;
    final TestsInfo info;

    /**
     * Create a new {@link PipelineEventType#TESTS} event.
     *
     * @param pipelineId pipelineId
     * @param stepsId the corresponding steps stage id
     * @param info the tests info
     */
    public TestsInfoEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("stepsId") String stepsId,
            @JsonProperty("info") TestsInfo info) {

        super(pipelineId);
        this.stepsId = stepsId;
        this.info = info;
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

    @JsonProperty
    public TestsInfo info() {
        return info;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.TESTS_INFO;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.stepsId);
        hash = 79 * hash + Objects.hashCode(this.info);
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
        final TestsInfoEvent other = (TestsInfoEvent) obj;
        if (!Objects.equals(this.stepsId, other.stepsId)) {
            return false;
        }
        return Objects.equals(this.info, other.info);
    }

    @Override
    public String toString() {
        return TestsInfoEvent.class.getSimpleName() + "{"
                + " pipelineId=" + pipelineId
                + ", stepsId=" + stepsId
                + ", info=" + info
                + " }";
    }
}
