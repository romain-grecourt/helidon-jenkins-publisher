package io.helidon.build.publisher.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Test results summary.
 */
@JsonPropertyOrder({"total", "passed", "failed", "skipped"})
public final class TestsInfo {

    final int total;
    final int passed;
    final int failed;
    final int skipped;

    /**
     * Create a new test info.
     *
     * @param total total count
     * @param passed passed count
     * @param failed failed count
     * @param skipped skipped count
     */
    @JsonCreator
    public TestsInfo(@JsonProperty("total") int total, @JsonProperty("passed") int passed, @JsonProperty("failed") int failed,
            @JsonProperty("skipped") int skipped) {

        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.total);
        hash = 41 * hash + Objects.hashCode(this.passed);
        hash = 41 * hash + Objects.hashCode(this.failed);
        hash = 41 * hash + Objects.hashCode(this.skipped);
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
        final TestsInfo other = (TestsInfo) obj;
        if (!Objects.equals(this.total, other.total)) {
            return false;
        }
        if (!Objects.equals(this.passed, other.passed)) {
            return false;
        }
        if (!Objects.equals(this.failed, other.failed)) {
            return false;
        }
        return Objects.equals(this.skipped, other.skipped);
    }

    @Override
    public final String toString() {
        return TestsInfo.class.getSimpleName() + " {"
                + ", total=" + total
                + ", passed=" + passed
                + ", failed=" + failed
                + ", skipped=" + skipped
                + " }";
    }
}
