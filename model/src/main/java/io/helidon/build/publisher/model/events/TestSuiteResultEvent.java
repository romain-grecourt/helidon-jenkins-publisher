/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.helidon.build.publisher.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.helidon.build.publisher.model.TestSuiteResult;
import java.util.Objects;

/**
 * {@link PipelineEventType#TESTSUITE_RESULT} event.
 */
@JsonPropertyOrder({"runId", "eventType", "stageId"})
public final class TestSuiteResultEvent extends PipelineEvent {

    final int stepsId;
    final TestSuiteResult suite;

    /**
     * Create a new {@link PipelineEventType#TEST_SUITE} event.
     *
     * @param runId runId
     * @param stepsId the corresponding steps stage id
     * @param suite the tests suite result
     */
    public TestSuiteResultEvent(@JsonProperty("runId") String runId, @JsonProperty("stepsId") int stepsId,
            @JsonProperty("suite") TestSuiteResult suite) {

        super(runId);
        this.stepsId = stepsId;
        this.suite = suite;
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
        hash = 53 * hash + this.stepsId;
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
        if (this.stepsId != other.stepsId) {
            return false;
        }
        return Objects.equals(this.suite, other.suite);
    }

    
    @Override
    public String toString() {
        return TestSuiteResultEvent.class.getSimpleName() + "{"
                + " runId=" + runId
                + ", stepsId=" + stepsId
                + ", suite=" + suite.name()
                + " }";
    }
}
