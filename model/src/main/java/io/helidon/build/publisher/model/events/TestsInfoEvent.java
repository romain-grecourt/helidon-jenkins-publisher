package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.helidon.build.publisher.model.TestsInfo;
import java.util.Objects;

/**
 * {@link PipelineEventType#TESTS_INFO} event.
 */
@JsonPropertyOrder({"runId", "eventType", "stageId"})
public final class TestsInfoEvent extends PipelineEvent {

    final int stepsId;
    final TestsInfo info;

    /**
     * Create a new {@link PipelineEventType#TESTS} event.
     *
     * @param runId runId
     * @param stepsId the corresponding steps stage id
     * @param info the tests info
     */
    public TestsInfoEvent(@JsonProperty("runId") String runId, @JsonProperty("stepsId") int stepsId,
            @JsonProperty("info") TestsInfo info) {

        super(runId);
        this.stepsId = stepsId;
        this.info = info;
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
        int hash = 3;
        hash = 97 * hash + this.stepsId;
        hash = 97 * hash + Objects.hashCode(this.info);
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
        if (this.stepsId != other.stepsId) {
            return false;
        }
        return Objects.equals(this.info, other.info);
    }

    @Override
    public String toString() {
        return TestsInfoEvent.class.getSimpleName() + "{"
                + " runId=" + runId
                + ", stepsId=" + stepsId
                + ", info=" + info
                + " }";
    }
}
