package io.helidon.build.publisher.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * List of {@link TestSuiteResult}.
 */
public final class TestSuiteResults {

    final List<TestSuiteResult> items;

    @JsonCreator
    public TestSuiteResults(@JsonProperty("results") List<TestSuiteResult> items) {
        this.items = items;
    }

    @JsonProperty
    public List<TestSuiteResult> items() {
        return items;
    }
}
