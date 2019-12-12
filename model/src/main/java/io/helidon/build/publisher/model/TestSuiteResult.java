package io.helidon.build.publisher.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Objects;

/**
 * Test suite result.
 */
@JsonPropertyOrder({"name", "total", "passed", "failed", "skipped", "tests"})
public final class TestSuiteResult {

    final String name;
    final int passed;
    final int failed;
    final int skipped;
    final int total;
    final List<TestResult> tests;

    /**
     * Create a new test suite.
     * @param name the test suite name
     * @param total the total count
     * @param passed the passed count
     * @param failed the failed count
     * @param skipped the skipped count
     * @param tests the enclosed tests
     */
    @JsonCreator
    public TestSuiteResult(@JsonProperty("name") String name, @JsonProperty("total") int total, @JsonProperty("passed") int passed,
        @JsonProperty("failed") int failed, @JsonProperty("skipped") int skipped,
        @JsonProperty("tests") List<TestResult> tests) {

        this.name = name;
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
        this.tests = tests;
    }

    @JsonProperty
    public String name() {
        return name;
    }

    @JsonProperty
    public int total() {
        return total;
    }

    @JsonProperty
    public int passed() {
        return passed;
    }

    @JsonProperty
    public int failed() {
        return failed;
    }

    @JsonProperty
    public int skipped() {
        return skipped;
    }

    @JsonProperty
    public List<TestResult> tests() {
        return tests;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.name);
        hash = 71 * hash + this.total;
        hash = 71 * hash + this.passed;
        hash = 71 * hash + this.failed;
        hash = 71 * hash + this.skipped;
        hash = 71 * hash + Objects.hashCode(this.tests);
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
        final TestSuiteResult other = (TestSuiteResult) obj;
        if (this.total != other.total) {
            return false;
        }
        if (this.passed != other.passed) {
            return false;
        }
        if (this.failed != other.failed) {
            return false;
        }
        if (this.skipped != other.skipped) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.tests, other.tests);
    }

    @Override
    public final String toString() {
        return TestSuiteResult.class.getSimpleName() + " {"
                + " name=" + name
                + ", total=" + total
                + ", passed=" + passed
                + ", failed=" + failed
                + ", skipped=" + skipped
                + ", tests=" + tests
                + " }";
    }

    /**
     * Single test result.
     */
    @JsonPropertyOrder({"name", "status", "output"})
    public static final class TestResult {

        final String name;
        final TestStatus status;
        final String output;

        @JsonCreator
        public TestResult(@JsonProperty("name") String name, @JsonProperty("status") TestStatus status,
            @JsonProperty("output") String output) {

            this.name = name;
            this.status = status;
            this.output = output;
        }

        @JsonProperty
        public String name() {
            return name;
        }

        @JsonProperty
        public TestStatus status() {
            return status;
        }

        @JsonProperty
        public String output() {
            return output;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.name);
            hash = 71 * hash + Objects.hashCode(this.status);
            hash = 71 * hash + Objects.hashCode(this.output);
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
            final TestResult other = (TestResult) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            if (!Objects.equals(this.output, other.output)) {
                return false;
            }
            return this.status == other.status;
        }

        @Override
        public final String toString() {
            return TestResult.class.getSimpleName() + " {"
                    + " name=" + name
                    + ", status=" + status
                    + ", hasOutput=" + (output != null)
                    + " }";
        }
    }

    /**
     * Status.
     */
    public enum TestStatus {
        PASSED,
        SKIPPED,
        FAILED,
    }
}
