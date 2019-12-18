package io.helidon.build.publisher.model;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * List of {@link TestSuiteResult}.
 */
@JsonSerialize(using = JacksonSupport.TestSuiteResultsSerializer.class)
public final class TestSuiteResults {

    final List<TestSuiteResult> items;

    public TestSuiteResults( List<TestSuiteResult> items) {
        this.items = items;
    }

    public List<TestSuiteResult> items() {
        return items;
    }
}
