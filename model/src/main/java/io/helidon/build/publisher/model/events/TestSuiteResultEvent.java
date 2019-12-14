package io.helidon.build.publisher.model.events;

import java.util.Objects;

import io.helidon.build.publisher.model.TestSuiteResult;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * {@link PipelineEventType#TESTSUITE_RESULT} event.
 */
@JsonPropertyOrder({"pipelineId", "eventType", "stageId"})
public final class TestSuiteResultEvent extends PipelineEvent {

    final String stepsId;
    final TestSuiteResult suite;

    /**
     * Create a new {@link PipelineEventType#TEST_SUITE} event.
     *
     * @param pipelineId pipelineId
     * @param stepsId the corresponding steps stage id
     * @param suite the tests suite result
     */
    public TestSuiteResultEvent(@JsonProperty("pipelineId") String pipelineId, @JsonProperty("stepsId") String stepsId,
            @JsonProperty("suite") TestSuiteResult suite) {

        super(pipelineId);
        this.stepsId = stepsId;
        this.suite = suite;
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
    public TestSuiteResult suite() {
        return suite;
    }

    @Override
    public PipelineEventType eventType() {
        return PipelineEventType.TESTSUITE_RESULT;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.stepsId);
        hash = 53 * hash + Objects.hashCode(this.suite);
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
        final TestSuiteResultEvent other = (TestSuiteResultEvent) obj;
        if (!Objects.equals(this.suite, other.suite)) {
            return false;
        }
        return Objects.equals(this.suite, other.suite);
    }

    
    @Override
    public String toString() {
        return TestSuiteResultEvent.class.getSimpleName() + "{"
                + " pipelineId=" + pipelineId
                + ", stepsId=" + stepsId
                + ", suite=" + suite.name()
                + " }";
    }
}
